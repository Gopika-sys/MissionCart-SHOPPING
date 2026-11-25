package com.missioncart.ai;

import com.missioncart.ai.AIModels.RequirementSpec;
import com.missioncart.categories.Category;
import com.missioncart.categories.CategoryRepository;
import com.missioncart.products.Product;
import com.missioncart.products.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductRetrievalService {

    public record RetrievalMatch(
        RequirementSpec requirement,
        Product product,
        double score
    ) {}

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;

    public ProductRetrievalService(ProductRepository productRepo, CategoryRepository categoryRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
    }

    public List<RetrievalMatch> retrieveForRequirements(
        List<RequirementSpec> requirements,
        BigDecimal totalBudget,
        Set<Long> alreadySelectedIds
    ) {
        Map<String, Long> categorySlugToId = categoryRepo.findAll().stream()
            .collect(Collectors.toMap(c -> c.getSlug().toLowerCase(), Category::getId, (a, b) -> a));

        Set<Long> usedProductIds = new HashSet<>(alreadySelectedIds != null ? alreadySelectedIds : Set.of());
        List<RetrievalMatch> matches = new ArrayList<>();

        BigDecimal perItemTarget = (totalBudget != null && totalBudget.compareTo(BigDecimal.ZERO) > 0 && !requirements.isEmpty())
            ? totalBudget.divide(BigDecimal.valueOf(Math.max(1, requirements.size())), java.math.RoundingMode.HALF_UP)
            : null;

        for (RequirementSpec req : requirements) {
            Long targetCategoryId = req.categorySlug() != null ? categorySlugToId.get(req.categorySlug().toLowerCase()) : null;
            List<Product> candidates = queryCandidates(req, targetCategoryId);

            Product best = null;
            double bestScore = -1.0;

            for (Product p : candidates) {
                if (usedProductIds.contains(p.getId())) continue;
                double score = scoreProduct(p, req, targetCategoryId, perItemTarget);
                // Require genuine keyword relevance (>= 4.5) so unrelated items are never forced in
                if (score > bestScore && score >= 4.5) {
                    bestScore = score;
                    best = p;
                }
            }

            if (best != null) {
                usedProductIds.add(best.getId());
                matches.add(new RetrievalMatch(req, best, bestScore));
            } else {
                matches.add(new RetrievalMatch(req, null, 0.0));
            }
        }
        return matches;
    }

    private List<Product> queryCandidates(RequirementSpec req, Long targetCategoryId) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> base = new ArrayList<>();
            base.add(cb.isTrue(root.get("published")));
            base.add(cb.isTrue(root.get("approved")));
            base.add(cb.notEqual(root.get("availabilityStatus"), "OUT_OF_STOCK"));
            base.add(cb.equal(root.get("imageStatus"), "VERIFIED"));

            List<Predicate> keywordOrCat = new ArrayList<>();
            if (targetCategoryId != null) {
                keywordOrCat.add(cb.equal(root.get("categoryId"), targetCategoryId));
                keywordOrCat.add(cb.equal(root.get("subcategoryId"), targetCategoryId));
            }
            if (req.keywords() != null) {
                for (String kw : req.keywords()) {
                    if (kw == null || kw.trim().length() < 3) continue;
                    String pat = "%" + kw.trim().toLowerCase() + "%";
                    keywordOrCat.add(cb.like(cb.lower(root.get("name")), pat));
                    keywordOrCat.add(cb.like(cb.lower(root.get("description")), pat));
                    keywordOrCat.add(cb.like(cb.lower(root.get("missionTags")), pat));
                }
            }
            if (!keywordOrCat.isEmpty()) {
                base.add(cb.or(keywordOrCat.toArray(new Predicate[0])));
            }
            return cb.and(base.toArray(new Predicate[0]));
        };

        return productRepo.findAll(spec, PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "id"))).getContent();
    }

    private double scoreProduct(Product p, RequirementSpec req, Long targetCategoryId, BigDecimal perItemTarget) {
        double score = 0.0;
        String nameLower = p.getName().toLowerCase();
        String haystack = (nameLower + " " + p.getDescription() + " " + Optional.ofNullable(p.getMissionTags()).orElse("")).toLowerCase();

        boolean keywordHit = false;
        for (String kw : req.keywords()) {
            String k = kw.toLowerCase().trim();
            if (k.length() < 2) continue;
            if (nameLower.contains(k)) {
                score += 4.5;
                keywordHit = true;
            } else if (haystack.contains(k)) {
                score += 2.5;
                keywordHit = true;
            }
        }
        // Never match solely on category without any keyword/tag relevance
        if (!keywordHit) return 0.0;

        if (targetCategoryId != null && (targetCategoryId.equals(p.getCategoryId()) || targetCategoryId.equals(p.getSubcategoryId()))) {
            score += 3.0;
        }
        if ("VERIFIED".equalsIgnoreCase(p.getImageStatus())) score += 1.0;
        if (p.getPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) > 0
            && !"UNAVAILABLE".equalsIgnoreCase(p.getPriceStatus())
            && !"EXPIRED".equalsIgnoreCase(p.getPriceStatus())) {
            score += 2.0; // Strongly prefer items with confirmed INR prices
            if (perItemTarget != null && p.getPrice().compareTo(perItemTarget.multiply(BigDecimal.valueOf(1.35))) <= 0) {
                score += 1.0;
            }
        }
        return score;
    }
}
