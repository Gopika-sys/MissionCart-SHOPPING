package com.missioncart.missions;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.ai.*;
import com.missioncart.ai.AIModels.ParsedIntent;
import com.missioncart.ai.AIModels.RequirementSpec;
import com.missioncart.ai.MissionBundleBuilder.BuiltBundle;
import com.missioncart.ai.ProductRetrievalService.RetrievalMatch;
import com.missioncart.budget.BudgetPlannerService;
import com.missioncart.budget.BudgetPlannerService.BudgetSummary;
import com.missioncart.budget.BudgetPlannerService.OptimizationPlan;
import com.missioncart.categories.Brand;
import com.missioncart.categories.BrandRepository;
import com.missioncart.categories.Category;
import com.missioncart.categories.CategoryRepository;
import com.missioncart.compatibility.CompatibilityEngine;
import com.missioncart.compatibility.CompatibilityEngine.CompatibilityReport;
import com.missioncart.compatibility.CompatibilityEngine.ProductCompatibilityResult;
import com.missioncart.missions.MissionDtos.*;
import com.missioncart.possessions.PossessionService;
import com.missioncart.possessions.UserPossession;
import com.missioncart.products.Product;
import com.missioncart.products.ProductDtos.ProductDto;
import com.missioncart.products.ProductRepository;
import com.missioncart.products.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MissionService {

    private final MissionRepository missionRepo;
    private final MissionItemRepository itemRepo;
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final ProductService productService;
    private final MissionIntentParser intentParser;
    private final RequirementExtractor requirementExtractor;
    private final ProductRetrievalService retrievalService;
    private final MissionBundleBuilder bundleBuilder;
    private final MissionExplanationService explanationService;
    private final BudgetPlannerService budgetService;
    private final CompatibilityEngine compatibilityEngine;
    private final PossessionService possessionService;
    private final ObjectMapper objectMapper;

    public MissionService(
        MissionRepository missionRepo,
        MissionItemRepository itemRepo,
        ProductRepository productRepo,
        CategoryRepository categoryRepo,
        BrandRepository brandRepo,
        ProductService productService,
        MissionIntentParser intentParser,
        RequirementExtractor requirementExtractor,
        ProductRetrievalService retrievalService,
        MissionBundleBuilder bundleBuilder,
        MissionExplanationService explanationService,
        BudgetPlannerService budgetService,
        CompatibilityEngine compatibilityEngine,
        PossessionService possessionService,
        ObjectMapper objectMapper
    ) {
        this.missionRepo = missionRepo;
        this.itemRepo = itemRepo;
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.productService = productService;
        this.intentParser = intentParser;
        this.requirementExtractor = requirementExtractor;
        this.retrievalService = retrievalService;
        this.bundleBuilder = bundleBuilder;
        this.explanationService = explanationService;
        this.budgetService = budgetService;
        this.compatibilityEngine = compatibilityEngine;
        this.possessionService = possessionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MissionDetailDto planMission(Long userId, PlanMissionRequest req) {
        ParsedIntent intent = intentParser.parse(
            req.prompt(), req.budget(), req.timeframe(), req.alreadyOwn(), req.preferences(), req.constraints()
        );
        List<RequirementSpec> requirements = requirementExtractor.extract(intent);
        List<RetrievalMatch> matches = retrievalService.retrieveForRequirements(requirements, intent.budget(), Set.of());

        List<String> savedPossessions = userId != null
            ? possessionService.listForUser(userId).stream().map(UserPossession::getItemName).toList()
            : List.of();

        Mission mission = new Mission();
        mission.setUserId(userId);
        mission.setTitle(intent.title());
        mission.setSlug("mission-" + UUID.randomUUID().toString().substring(0, 8));
        mission.setRawPrompt(req.prompt().trim());
        mission.setGoal(intent.goal());
        mission.setSituationSummary(intent.situationSummary());
        mission.setPurpose(intent.purpose());
        mission.setBudget(intent.budget());
        mission.setCurrency("INR");
        mission.setTimeframe(intent.timeframe());
        mission.setPeopleCount(intent.peopleCount());
        mission.setAiProviderUsed(intent.providerUsed());
        mission.setContextJson(writeJson(intent.context()));
        mission.setRequirementsJson(writeJson(requirements));
        mission.setConstraintsJson(writeJson(intent.constraints()));
        mission.setAssumptionsJson(writeJson(intent.assumptions()));
        mission.setFollowUpQuestion(intent.followUpQuestion());
        mission = missionRepo.save(mission);

        BuiltBundle built = bundleBuilder.build(mission.getId(), intent, matches, savedPossessions);
        List<MissionItem> savedItems = itemRepo.saveAll(built.items());

        Map<Long, Product> productMap = loadProductMap(savedItems);
        BudgetSummary budgetSummary = budgetService.calculate(mission.getBudget(), savedItems, productMap);
        String explanation = explanationService.explain(intent, budgetSummary, built.compatibilityReport(), built.unmatchedRequirements());

        mission.setAiExplanation(explanation);
        Map<String, Object> ctx = new LinkedHashMap<>(intent.context() != null ? intent.context() : Map.of());
        ctx.put("unmatchedRequirements", built.unmatchedRequirements());
        ctx.put("existingPossessions", intent.existingPossessions());
        mission.setContextJson(writeJson(ctx));
        mission = missionRepo.save(mission);

        return assembleDetail(mission, savedItems, productMap, built.compatibilityReport(), built.unmatchedRequirements());
    }

    public List<MissionDetailDto> listForUser(Long userId) {
        return missionRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(m -> getMissionDetail(m.getId(), userId))
            .toList();
    }

    public MissionDetailDto getMissionDetail(Long missionId, Long requesterUserId) {
        Mission m = missionRepo.findById(missionId)
            .orElseThrow(() -> new EntityNotFoundException("Mission not found: " + missionId));
        if (m.getUserId() != null && requesterUserId != null && !m.getUserId().equals(requesterUserId) && !m.isShared()) {
            throw new IllegalArgumentException("You do not have access to this mission.");
        }
        List<MissionItem> items = itemRepo.findByMissionIdOrderBySortOrderAscIdAsc(m.getId());
        Map<Long, Product> productMap = loadProductMap(items);
        List<Long> activeIds = items.stream()
            .filter(i -> i.isSelected() && !i.isExcludedOwned())
            .map(MissionItem::getProductId).toList();
        CompatibilityReport report = compatibilityEngine.evaluate(activeIds, parseMap(m.getContextJson()));
        List<String> unmatched = extractStringListFromContext(m.getContextJson(), "unmatchedRequirements");
        return assembleDetail(m, items, productMap, report, unmatched);
    }

    @Transactional
    public MissionDetailDto updateMission(Long missionId, Long userId, UpdateMissionRequest req) {
        Mission m = missionRepo.findById(missionId)
            .orElseThrow(() -> new EntityNotFoundException("Mission not found: " + missionId));
        if (m.getUserId() != null && userId != null && !m.getUserId().equals(userId) && !m.isShared()) {
            throw new IllegalArgumentException("Not authorized to update this mission.");
        }
        if (req.title() != null && !req.title().isBlank()) m.setTitle(req.title().trim());
        if (req.budget() != null) m.setBudget(req.budget());
        if (req.status() != null && !req.status().isBlank()) m.setStatus(req.status());

        List<MissionItem> items = itemRepo.findByMissionIdOrderBySortOrderAscIdAsc(m.getId());
        Map<Long, MissionItem> byId = items.stream().collect(Collectors.toMap(MissionItem::getId, i -> i));

        if (req.itemActions() != null) {
            for (UpdateMissionItemAction action : req.itemActions()) {
                if (action.addProductId() != null) {
                    Product p = productRepo.findById(action.addProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Product not found"));
                    MissionItem added = new MissionItem();
                    added.setMissionId(m.getId());
                    added.setProductId(p.getId());
                    added.setRequirementLabel(action.addRequirementLabel() != null ? action.addRequirementLabel() : p.getName());
                    added.setPriority(action.priority() != null ? action.priority() : "OPTIONAL");
                    added.setQuantity(action.quantity() != null ? Math.max(1, action.quantity()) : 1);
                    added.setUnitPrice(p.getPrice());
                    added.setSelected(true);
                    added.setRecommendationReason("Added manually to mission");
                    added.setSortOrder(items.size());
                    items.add(itemRepo.save(added));
                    continue;
                }
                if (action.itemId() == null) continue;
                MissionItem item = byId.get(action.itemId());
                if (item == null) continue;
                if (action.selected() != null) item.setSelected(action.selected());
                if (action.quantity() != null) {
                    if (action.quantity() <= 0) throw new IllegalArgumentException("Quantity must be at least 1");
                    item.setQuantity(action.quantity());
                }
                if (action.priority() != null) item.setPriority(action.priority());
                if (action.excludedOwned() != null) {
                    item.setExcludedOwned(action.excludedOwned());
                    if (action.excludedOwned()) item.setSelected(false);
                }
                if (action.replaceWithProductId() != null) {
                    Product replacement = productRepo.findById(action.replaceWithProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Replacement product not found"));
                    item.setProductId(replacement.getId());
                    item.setUnitPrice(replacement.getPrice());
                    item.setRecommendationReason("Replaced with " + replacement.getName() + " from " + replacement.getSourcePlatform());
                }
            }
        }

        // Re-run compatibility after updates
        List<Long> activeIds = items.stream().filter(i -> i.isSelected() && !i.isExcludedOwned()).map(MissionItem::getProductId).toList();
        CompatibilityReport report = compatibilityEngine.evaluate(activeIds, parseMap(m.getContextJson()));
        for (MissionItem item : items) {
            if (item.isExcludedOwned()) {
                item.setCompatibilityStatus("COMPATIBLE");
                item.setCompatibilityReason("Excluded (already owned)");
            } else if (item.isSelected()) {
                ProductCompatibilityResult res = report.perProduct().get(item.getProductId());
                if (res != null) {
                    item.setCompatibilityStatus(res.status());
                    item.setCompatibilityReason(res.reason());
                }
            }
        }
        itemRepo.saveAll(items);
        missionRepo.save(m);

        Map<Long, Product> productMap = loadProductMap(items);
        List<String> unmatched = extractStringListFromContext(m.getContextJson(), "unmatchedRequirements");
        return assembleDetail(m, items, productMap, report, unmatched);
    }

    public OptimizationPlan optimizeMission(Long missionId, Long userId) {
        Mission m = missionRepo.findById(missionId)
            .orElseThrow(() -> new EntityNotFoundException("Mission not found"));
        if (m.getUserId() != null && userId != null && !m.getUserId().equals(userId) && !m.isShared()) {
            throw new IllegalArgumentException("Not authorized.");
        }
        List<MissionItem> items = itemRepo.findByMissionIdOrderBySortOrderAscIdAsc(m.getId());
        return budgetService.suggestOptimizations(m.getBudget(), items);
    }

    @Transactional
    public void deleteMission(Long missionId, Long userId) {
        Mission m = missionRepo.findById(missionId)
            .orElseThrow(() -> new EntityNotFoundException("Mission not found"));
        if (m.getUserId() != null && !m.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this mission.");
        }
        missionRepo.delete(m);
    }

    public MissionDetailDto assembleDetail(
        Mission m,
        List<MissionItem> items,
        Map<Long, Product> productMap,
        CompatibilityReport report,
        List<String> unmatched
    ) {
        Map<Long, Category> catMap = categoryRepo.findAll().stream().collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, Brand> brandMap = brandRepo.findAll().stream().collect(Collectors.toMap(Brand::getId, b -> b));
        BudgetSummary budgetSummary = budgetService.calculate(m.getBudget(), items, productMap);

        List<MissionItemDto> itemDtos = items.stream().map(i -> {
            Product p = productMap.get(i.getProductId());
            ProductDto pdto = p != null ? productService.toDto(p, catMap.get(p.getCategoryId()), p.getBrandId() != null ? brandMap.get(p.getBrandId()) : null, false) : null;
            BigDecimal unit = p != null ? p.getPrice() : i.getUnitPrice();
            BigDecimal line = unit != null ? unit.multiply(BigDecimal.valueOf(i.getQuantity())) : null;
            return new MissionItemDto(
                i.getId(), i.getProductId(), pdto, i.getRequirementLabel(), i.getPriority(),
                i.getQuantity(), unit, line, i.isSelected(), i.isExcludedOwned(),
                i.getCompatibilityStatus(), i.getCompatibilityReason(), i.getRecommendationReason(),
                i.getAssignedToUserId(), i.getClaimedByName()
            );
        }).toList();

        int covered = (int) items.stream().filter(i -> i.isSelected() || i.isExcludedOwned()).count();
        int totalReqs = Math.max(1, items.size() + (unmatched != null ? unmatched.size() : 0));
        int progress = Math.min(100, Math.round((covered * 100.0f) / totalReqs));

        return new MissionDetailDto(
            m.getId(), m.getUserId(), m.getTitle(), m.getSlug(), m.getRawPrompt(), m.getGoal(),
            m.getSituationSummary(), m.getPurpose(), m.getBudget(), m.getCurrency(), m.getTimeframe(),
            m.getPeopleCount(), m.getAiProviderUsed(), m.getAiExplanation(),
            parseStringList(m.getAssumptionsJson()),
            extractStringListFromContext(m.getContextJson(), "existingPossessions"),
            unmatched != null ? unmatched : List.of(),
            m.getFollowUpQuestion(), m.getStatus(), m.isShared(), m.getShareToken(),
            budgetSummary, report, itemDtos, progress, m.getCreatedAt(), m.getUpdatedAt()
        );
    }

    private Map<Long, Product> loadProductMap(List<MissionItem> items) {
        if (items.isEmpty()) return Map.of();
        return productRepo.findAllById(items.stream().map(MissionItem::getProductId).toList())
            .stream().collect(Collectors.toMap(Product::getId, p -> p));
    }

    private String writeJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringListFromContext(String json, String key) {
        Object val = parseMap(json).get(key);
        if (val instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
