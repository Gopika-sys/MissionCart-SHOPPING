package com.missioncart.products;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.categories.Brand;
import com.missioncart.categories.BrandRepository;
import com.missioncart.categories.Category;
import com.missioncart.categories.CategoryRepository;
import com.missioncart.common.PageResponse;
import com.missioncart.products.ProductDtos.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;
    private final ProductImageRepository imageRepo;
    private final ObjectMapper objectMapper;

    public ProductService(
        ProductRepository productRepo,
        CategoryRepository categoryRepo,
        BrandRepository brandRepo,
        ProductImageRepository imageRepo,
        ObjectMapper objectMapper
    ) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.brandRepo = brandRepo;
        this.imageRepo = imageRepo;
        this.objectMapper = objectMapper;
    }

    public ProductCountsDto getCounts() {
        long total = productRepo.count();
        long published = productRepo.countByPublishedTrueAndApprovedTrue();
        long imgVerified = productRepo.countByPublishedTrueAndApprovedTrueAndImageStatus("VERIFIED");
        long available = productRepo.countByAvailabilityStatus("IN_STOCK");
        long pendingImg = productRepo.countByImageStatus("REQUIRES_IMAGE");

        Map<String, Long> byRetailer = new LinkedHashMap<>();
        for (Object[] row : productRepo.countGroupedByRetailer()) {
            byRetailer.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        Map<Long, String> catNames = categoryRepo.findAll().stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : productRepo.countGroupedByCategory()) {
            Long cid = ((Number) row[0]).longValue();
            byCategory.put(catNames.getOrDefault(cid, "Category #" + cid), ((Number) row[1]).longValue());
        }

        return new ProductCountsDto(total, published, imgVerified, available, pendingImg, byRetailer, byCategory);
    }

    public PageResponse<ProductDto> searchProducts(
        String q,
        String categoryCsv,
        String sourceCsv,
        Long brandId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String priorityTier,
        Boolean verifiedImagesOnly,
        Boolean inStockOnly,
        String sortBy,
        int page,
        int size
    ) {
        Map<String, Category> catsBySlug = categoryRepo.findAll().stream()
            .collect(Collectors.toMap(c -> c.getSlug().toLowerCase(), c -> c, (a, b) -> a));

        boolean enforceVerifiedImages = verifiedImagesOnly == null || Boolean.TRUE.equals(verifiedImagesOnly);

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("published")));
            predicates.add(cb.isTrue(root.get("approved")));

            if (enforceVerifiedImages) {
                predicates.add(cb.equal(root.get("imageStatus"), "VERIFIED"));
                predicates.add(cb.isNotNull(root.get("imageUrl")));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("missionTags")), pattern),
                    cb.like(cb.lower(root.get("sourcePlatform")), pattern)
                ));
            }
            if (categoryCsv != null && !categoryCsv.isBlank()) {
                List<Long> catIds = Arrays.stream(categoryCsv.split(","))
                    .map(String::trim).map(String::toLowerCase)
                    .map(catsBySlug::get)
                    .filter(Objects::nonNull)
                    .map(Category::getId)
                    .toList();
                if (!catIds.isEmpty()) {
                    predicates.add(cb.or(root.get("categoryId").in(catIds), root.get("subcategoryId").in(catIds)));
                }
            }
            if (sourceCsv != null && !sourceCsv.isBlank()) {
                List<String> srcCodes = Arrays.stream(sourceCsv.split(","))
                    .map(String::trim).map(String::toUpperCase).filter(s -> !s.isEmpty()).toList();
                if (!srcCodes.isEmpty()) {
                    predicates.add(cb.upper(root.get("sourcePlatform")).in(srcCodes));
                }
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (priorityTier != null && !priorityTier.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("priorityTier")), priorityTier.trim().toUpperCase()));
            }
            if (Boolean.TRUE.equals(inStockOnly)) {
                predicates.add(cb.equal(root.get("availabilityStatus"), "IN_STOCK"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = switch (sortBy == null ? "relevance" : sortBy) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.ASC, "id");
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), sort);
        Page<Product> result = productRepo.findAll(spec, pageable);
        Map<Long, Category> catMap = categoryRepo.findAll().stream().collect(Collectors.toMap(Category::getId, c -> c));
        Set<Long> brandIds = result.getContent().stream().map(Product::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Brand> brandMap = brandIds.isEmpty() ? Map.of() : brandRepo.findAllById(brandIds).stream().collect(Collectors.toMap(Brand::getId, b -> b));

        List<ProductDto> dtos = result.getContent().stream()
            .map(p -> toDto(p, catMap.get(p.getCategoryId()), p.getBrandId() != null ? brandMap.get(p.getBrandId()) : null, false))
            .toList();

        return new PageResponse<>(dtos, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public ProductDto getByIdOrSlug(String idOrSlug) {
        Product p = null;
        if (idOrSlug.chars().allMatch(Character::isDigit)) {
            p = productRepo.findById(Long.parseLong(idOrSlug)).orElse(null);
        }
        if (p == null) {
            p = productRepo.findBySlug(idOrSlug)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + idOrSlug));
        }
        Category cat = categoryRepo.findById(p.getCategoryId()).orElse(null);
        Brand brand = p.getBrandId() != null ? brandRepo.findById(p.getBrandId()).orElse(null) : null;
        return toDto(p, cat, brand, true);
    }

    public AlternativesDto getAlternatives(Long productId) {
        Product base = productRepo.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        Pageable limit = PageRequest.of(0, 24, Sort.by(Sort.Direction.ASC, "price"));
        Specification<Product> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get("categoryId"), base.getCategoryId()),
            cb.notEqual(root.get("id"), productId),
            cb.isTrue(root.get("published")),
            cb.isTrue(root.get("approved")),
            cb.equal(root.get("imageStatus"), "VERIFIED")
        );
        List<Product> sameCategory = productRepo.findAll(spec, limit).getContent();
        Map<Long, Category> catMap = categoryRepo.findAll().stream().collect(Collectors.toMap(Category::getId, c -> c));
        Set<Long> brandIds = sameCategory.stream().map(Product::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Brand> brandMap = brandIds.isEmpty() ? Map.of() : brandRepo.findAllById(brandIds).stream().collect(Collectors.toMap(Brand::getId, b -> b));

        List<ProductDto> all = sameCategory.stream()
            .map(p -> toDto(p, catMap.get(p.getCategoryId()), p.getBrandId() != null ? brandMap.get(p.getBrandId()) : null, false))
            .toList();

        List<ProductDto> budget = all.stream()
            .filter(p -> p.price() != null && (base.getPrice() == null || p.price().compareTo(base.getPrice()) < 0 || "BUDGET".equalsIgnoreCase(p.priorityTier())))
            .limit(4).toList();
        List<ProductDto> balanced = all.stream()
            .filter(p -> "BALANCED".equalsIgnoreCase(p.priorityTier()))
            .limit(4).toList();
        List<ProductDto> premium = all.stream()
            .filter(p -> p.price() != null && ((base.getPrice() != null && p.price().compareTo(base.getPrice()) > 0) || "PREMIUM".equalsIgnoreCase(p.priorityTier())))
            .limit(4).toList();

        return new AlternativesDto(budget, balanced, premium);
    }

    public ProductDto toDto(Product p, Category cat, Brand brand, boolean includeImages) {
        List<String> tags = p.getMissionTags() == null || p.getMissionTags().isBlank()
            ? List.of()
            : Arrays.stream(p.getMissionTags().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        List<ProductImageDto> extras = includeImages
            ? imageRepo.findByProductIdOrderByPrimaryImageDescSortOrderAsc(p.getId()).stream()
                .map(img -> new ProductImageDto(
                    img.getId(), img.getImageUrl(), img.getImageSource(), img.getImageAttribution(),
                    img.getImageLicense(), img.getImageStatus(), img.getImageWidth(), img.getImageHeight(),
                    img.getImageAltText(), img.isPrimaryImage()
                )).toList()
            : List.of();

        return new ProductDto(
            p.getId(), p.getSku(), p.getName(), p.getSlug(),
            p.getBrandId(), brand != null ? brand.getName() : null,
            p.getCategoryId(), cat != null ? cat.getName() : null, cat != null ? cat.getSlug() : null,
            p.getSourcePlatform(), p.getSourceProductId(), p.getSourceUrl(), p.getProductType(),
            p.getDescription(), p.getPrice(), p.getOriginalPrice(), p.getCurrency(),
            p.isPriceVerified(), p.getPriceStatus(), p.getTaxRate(), p.getShippingEstimate(),
            p.getPriceLastVerifiedAt(), p.getAvailabilityVerifiedAt(),
            p.getAvailabilityStatus(), p.getStockQuantity(),
            p.getRating(), p.getReviewCount(), p.isRatingVerified(), p.getDataVerificationStatus(),
            parseMap(p.getSpecificationsJson()), parseMap(p.getAttributesJson()),
            tags, p.getPriorityTier(), parseList(p.getLifecycleOptionsJson()),
            p.getImageUrl(), p.getImageSource(), p.getImageAttribution(), p.getImageLicense(), p.getImageStatus(),
            extras, p.isPublished(), p.isApproved(), p.getCreatedAt()
        );
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); }
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return List.of(); }
    }
}
