package com.missioncart.admin;

import com.missioncart.catalog.CatalogImportJob;
import com.missioncart.catalog.CatalogImportJobRepository;
import com.missioncart.catalog.CatalogImportService;
import com.missioncart.catalog.CatalogImportService.ImportRecordInput;
import com.missioncart.catalog.CatalogImportService.ImportResult;
import com.missioncart.categories.*;
import com.missioncart.common.ApiResponse;
import com.missioncart.compatibility.CompatibilityRule;
import com.missioncart.compatibility.CompatibilityRuleRepository;
import com.missioncart.lifecycle.LifecycleRequest;
import com.missioncart.lifecycle.LifecycleRequestRepository;
import com.missioncart.missions.Mission;
import com.missioncart.missions.MissionRepository;
import com.missioncart.orders.Order;
import com.missioncart.orders.OrderRepository;
import com.missioncart.products.*;
import com.missioncart.retailers.RetailerSource;
import com.missioncart.retailers.RetailerSourceRepository;
import com.missioncart.security.AuthenticatedUser;
import com.missioncart.shared.ActivityLog;
import com.missioncart.shared.ActivityLogRepository;
import com.missioncart.users.User;
import com.missioncart.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AdminAndSellerController {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final RetailerSourceRepository retailerRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final MissionRepository missionRepo;
    private final LifecycleRequestRepository lifecycleRepo;
    private final CompatibilityRuleRepository ruleRepo;
    private final CatalogImportService importService;
    private final CatalogImportJobRepository importJobRepo;
    private final InventoryHistoryRepository invHistoryRepo;
    private final ReviewRepository reviewRepo;
    private final SystemSettingRepository settingRepo;
    private final ActivityLogRepository logRepo;

    public AdminAndSellerController(
        ProductRepository productRepo,
        CategoryRepository categoryRepo,
        RetailerSourceRepository retailerRepo,
        UserRepository userRepo,
        OrderRepository orderRepo,
        MissionRepository missionRepo,
        LifecycleRequestRepository lifecycleRepo,
        CompatibilityRuleRepository ruleRepo,
        CatalogImportService importService,
        CatalogImportJobRepository importJobRepo,
        InventoryHistoryRepository invHistoryRepo,
        ReviewRepository reviewRepo,
        SystemSettingRepository settingRepo,
        ActivityLogRepository logRepo
    ) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.retailerRepo = retailerRepo;
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.missionRepo = missionRepo;
        this.lifecycleRepo = lifecycleRepo;
        this.ruleRepo = ruleRepo;
        this.importService = importService;
        this.importJobRepo = importJobRepo;
        this.invHistoryRepo = invHistoryRepo;
        this.reviewRepo = reviewRepo;
        this.settingRepo = settingRepo;
        this.logRepo = logRepo;
    }

    public record OverviewStats(
        long totalProducts,
        long totalUsers,
        long totalOrders,
        BigDecimal totalRevenue,
        long activeMissions,
        long pendingLifecycleRequests,
        long lowStockProducts,
        long importJobsCount,
        long verifiedImagesCount,
        long requiresImageCount
    ) {}

    public record AdminProductRequest(
        String sku,
        String name,
        Long categoryId,
        String sourcePlatform,
        String sourceUrl,
        String productType,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stockQuantity,
        String missionTags,
        String priorityTier,
        String imageUrl,
        String imageSource,
        String imageLicense,
        String imageAttribution,
        String imageStatus,
        Boolean published,
        Boolean approved
    ) {}

    public record ImportPayloadRequest(
        String sourcePlatform,
        String format,
        String fileName,
        String content,
        boolean previewOnly,
        boolean updateExisting
    ) {}

    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OverviewStats> overview() {
        List<Product> allProds = productRepo.findAll();
        BigDecimal rev = orderRepo.findAll().stream()
            .filter(o -> !"PAYMENT_FAILED".equalsIgnoreCase(o.getStatus()))
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long verifiedImg = allProds.stream().filter(p -> "VERIFIED".equalsIgnoreCase(p.getImageStatus())).count();
        long reqImg = allProds.stream().filter(p -> "REQUIRES_IMAGE".equalsIgnoreCase(p.getImageStatus())).count();

        return ApiResponse.ok(new OverviewStats(
            allProds.size(),
            userRepo.count(),
            orderRepo.count(),
            rev,
            missionRepo.count(),
            lifecycleRepo.count(),
            productRepo.countByStockQuantityLessThanEqual(5),
            importJobRepo.count(),
            verifiedImg,
            reqImg
        ));
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Product>> adminProducts(
        @RequestParam(required = false, defaultValue = "100") int size,
        @RequestParam(required = false) String imageStatus
    ) {
        int safeSize = Math.min(Math.max(1, size), 250);
        if (imageStatus != null && !imageStatus.isBlank()) {
            return ApiResponse.ok(productRepo.findAll(
                (root, q, cb) -> cb.equal(root.get("imageStatus"), imageStatus.trim().toUpperCase()),
                org.springframework.data.domain.PageRequest.of(0, safeSize)
            ).getContent());
        }
        return ApiResponse.ok(productRepo.findAll(org.springframework.data.domain.PageRequest.of(0, safeSize)).getContent());
    }

    @PostMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Product> createProduct(@AuthenticationPrincipal AuthenticatedUser admin, @RequestBody AdminProductRequest req) {
        if (productRepo.findBySkuIgnoreCase(req.sku()).isPresent()) {
            throw new IllegalStateException("Product with SKU already exists: " + req.sku());
        }
        Product p = new Product();
        applyProductFields(p, req);
        p.setSlug(req.name().toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + req.sku().toLowerCase().replaceAll("[^a-z0-9]", ""));
        Product saved = productRepo.save(p);
        audit(admin, "PRODUCT", String.valueOf(saved.getId()), "CREATED", saved.getSku());
        return ApiResponse.ok("Product created", saved);
    }

    @PutMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Product> updateProduct(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @RequestBody AdminProductRequest req) {
        Product p = productRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        applyProductFields(p, req);
        Product saved = productRepo.save(p);
        audit(admin, "PRODUCT", String.valueOf(id), "UPDATED", saved.getSku());
        return ApiResponse.ok("Product updated", saved);
    }

    @DeleteMapping("/admin/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteProduct(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        productRepo.deleteById(id);
        audit(admin, "PRODUCT", String.valueOf(id), "DELETED", "Removed product");
        return ApiResponse.ok("Product deleted", null);
    }

    @PatchMapping("/admin/products/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Product> verifyProductImage(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Product p = productRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        if (body.containsKey("imageUrl")) p.setImageUrl(body.get("imageUrl"));
        if (body.containsKey("imageSource")) p.setImageSource(body.get("imageSource"));
        if (body.containsKey("imageLicense")) p.setImageLicense(body.get("imageLicense"));
        if (body.containsKey("imageAttribution")) p.setImageAttribution(body.get("imageAttribution"));
        if (body.containsKey("imageStatus")) p.setImageStatus(body.get("imageStatus"));
        Product saved = productRepo.save(p);
        audit(admin, "PRODUCT_IMAGE", String.valueOf(id), "IMAGE_STATUS_" + saved.getImageStatus(), saved.getImageUrl());
        return ApiResponse.ok("Product image updated", saved);
    }

    @PatchMapping("/admin/products/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Product> adjustStock(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Product p = productRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        int newQty = Integer.parseInt(String.valueOf(body.get("stockQuantity")));
        int delta = newQty - p.getStockQuantity();
        p.setStockQuantity(Math.max(0, newQty));
        p.setAvailabilityStatus(p.getStockQuantity() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
        productRepo.save(p);

        InventoryHistory ih = new InventoryHistory();
        ih.setProductId(p.getId());
        ih.setDelta(delta);
        ih.setReason(String.valueOf(body.getOrDefault("reason", "Admin stock adjustment")));
        ih.setActorEmail(admin.email());
        invHistoryRepo.save(ih);
        return ApiResponse.ok("Stock updated", p);
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Category> createCategory(@RequestBody Category c) {
        if (c.getSlug() == null || c.getSlug().isBlank()) {
            c.setSlug(c.getName().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        }
        return ApiResponse.ok("Category saved", categoryRepo.save(c));
    }

    @PatchMapping("/admin/sources/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RetailerSource> updateSource(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        RetailerSource s = retailerRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Source not found"));
        if (body.containsKey("connectionStatus")) s.setConnectionStatus(String.valueOf(body.get("connectionStatus")));
        if (body.containsKey("feedConfigJson")) s.setFeedConfigJson(String.valueOf(body.get("feedConfigJson")));
        return ApiResponse.ok("Retailer source updated", retailerRepo.save(s));
    }

    @PostMapping("/admin/catalog/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ImportResult> runImport(@AuthenticationPrincipal AuthenticatedUser admin, @RequestBody ImportPayloadRequest req) {
        String fmt = req.format() == null ? "JSON" : req.format().toUpperCase();
        List<ImportRecordInput> recs = "CSV".equals(fmt)
            ? importService.parseCsvRecords(req.content(), req.sourcePlatform())
            : importService.parseJsonRecords(req.content(), req.sourcePlatform());
        ImportResult result = importService.executeImport(
            recs, req.sourcePlatform(), fmt, req.fileName(), req.previewOnly(), req.updateExisting(), admin.email()
        );
        return ApiResponse.ok(req.previewOnly() ? "Preview validated" : "Catalog import completed", result);
    }

    @GetMapping("/admin/catalog/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CatalogImportJob>> importJobs() {
        return ApiResponse.ok(importJobRepo.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Map<String, Object>>> listUsers() {
        return ApiResponse.ok(userRepo.findAll().stream().map(u -> Map.<String, Object>of(
            "id", u.getId(), "email", u.getEmail(), "fullName", u.getFullName(),
            "role", u.getRole(), "enabled", u.isEnabled(), "createdAt", u.getCreatedAt()
        )).toList());
    }

    @PatchMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User u = userRepo.findById(id).orElseThrow();
        if (body.containsKey("role")) u.setRole(String.valueOf(body.get("role")));
        if (body.containsKey("enabled")) u.setEnabled(Boolean.parseBoolean(String.valueOf(body.get("enabled"))));
        userRepo.save(u);
        return ApiResponse.ok("User updated", null);
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Order>> allOrders() {
        return ApiResponse.ok(orderRepo.findAll());
    }

    @PatchMapping("/admin/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Order o = orderRepo.findById(id).orElseThrow();
        if (body.containsKey("status")) o.setStatus(body.get("status"));
        if (body.containsKey("trackingStatus")) o.setTrackingStatus(body.get("trackingStatus"));
        return ApiResponse.ok("Order updated", orderRepo.save(o));
    }

    @GetMapping("/admin/lifecycle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<LifecycleRequest>> allLifecycle() {
        return ApiResponse.ok(lifecycleRepo.findAllByOrderByCreatedAtDesc());
    }

    @PatchMapping("/admin/lifecycle/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LifecycleRequest> updateLifecycle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        LifecycleRequest lr = lifecycleRepo.findById(id).orElseThrow();
        if (body.containsKey("status")) lr.setStatus(body.get("status"));
        return ApiResponse.ok("Lifecycle status updated", lifecycleRepo.save(lr));
    }

    @PostMapping("/admin/compatibility/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompatibilityRule> saveRule(@RequestBody CompatibilityRule rule) {
        return ApiResponse.ok("Compatibility rule saved", ruleRepo.save(rule));
    }

    @GetMapping("/admin/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> analytics() {
        List<Mission> missions = missionRepo.findAll();
        Map<String, Long> byPurpose = new LinkedHashMap<>();
        Map<String, Long> byProvider = new LinkedHashMap<>();
        for (Mission m : missions) {
            byPurpose.merge(m.getPurpose() != null ? m.getPurpose() : "Other", 1L, Long::sum);
            byProvider.merge(m.getAiProviderUsed(), 1L, Long::sum);
        }
        return ApiResponse.ok(Map.of(
            "totalMissions", missions.size(),
            "missionsByPurpose", byPurpose,
            "missionsByAiProvider", byProvider,
            "recentAuditLogs", logRepo.findTop50ByOrderByCreatedAtDesc()
        ));
    }

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Review>> adminReviews() {
        return ApiResponse.ok(reviewRepo.findAll());
    }

    @GetMapping("/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SystemSetting>> getSettings() {
        return ApiResponse.ok(settingRepo.findAll());
    }

    @PostMapping("/admin/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SystemSetting> saveSetting(@RequestBody SystemSetting setting) {
        setting.setUpdatedAt(Instant.now());
        return ApiResponse.ok("Setting saved", settingRepo.save(setting));
    }

    // Seller Portal Endpoints
    @GetMapping("/seller/products")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<List<Product>> sellerProducts(@AuthenticationPrincipal AuthenticatedUser seller) {
        return ApiResponse.ok(productRepo.findBySellerId(seller.id()));
    }

    @PostMapping("/seller/products")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ApiResponse<Product> sellerSubmitProduct(@AuthenticationPrincipal AuthenticatedUser seller, @RequestBody AdminProductRequest req) {
        Product p = new Product();
        applyProductFields(p, req);
        p.setSellerId(seller.id());
        p.setProductType("SELLER_MANAGED");
        p.setSourcePlatform("MISSIONCART_DIRECT");
        p.setApproved(false); // Requires admin approval before public showcase
        p.setSlug(req.name().toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + req.sku().toLowerCase().replaceAll("[^a-z0-9]", ""));
        return ApiResponse.ok("Product submitted for admin approval", productRepo.save(p));
    }

    private void applyProductFields(Product p, AdminProductRequest req) {
        if (req.sku() != null) p.setSku(req.sku().trim().toUpperCase());
        if (req.name() != null) p.setName(req.name().trim());
        if (req.categoryId() != null) p.setCategoryId(req.categoryId());
        if (req.sourcePlatform() != null) p.setSourcePlatform(req.sourcePlatform());
        if (req.sourceUrl() != null) p.setSourceUrl(req.sourceUrl());
        if (req.productType() != null) p.setProductType(req.productType());
        if (req.description() != null) p.setDescription(req.description());
        if (req.price() != null) p.setPrice(req.price());
        if (req.originalPrice() != null) p.setOriginalPrice(req.originalPrice());
        if (req.stockQuantity() != null) {
            p.setStockQuantity(Math.max(0, req.stockQuantity()));
            p.setAvailabilityStatus(p.getStockQuantity() > 0 ? "IN_STOCK" : "OUT_OF_STOCK");
        }
        if (req.missionTags() != null) p.setMissionTags(req.missionTags());
        if (req.priorityTier() != null) p.setPriorityTier(req.priorityTier());
        if (req.imageUrl() != null) p.setImageUrl(req.imageUrl());
        if (req.imageSource() != null) p.setImageSource(req.imageSource());
        if (req.imageLicense() != null) p.setImageLicense(req.imageLicense());
        if (req.imageAttribution() != null) p.setImageAttribution(req.imageAttribution());
        if (req.imageStatus() != null) p.setImageStatus(req.imageStatus());
        if (req.published() != null) p.setPublished(req.published());
        if (req.approved() != null) p.setApproved(req.approved());
    }

    private void audit(AuthenticatedUser admin, String entityType, String entityId, String action, String details) {
        ActivityLog log = new ActivityLog();
        log.setActorEmail(admin.email());
        log.setActorRole(admin.role());
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        logRepo.save(log);
    }
}
