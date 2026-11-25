package com.missioncart.products;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ProductDtos {
    public record ProductImageDto(
        Long id,
        String imageUrl,
        String imageSource,
        String imageAttribution,
        String imageLicense,
        String imageStatus,
        Integer imageWidth,
        Integer imageHeight,
        String imageAltText,
        boolean primaryImage
    ) {}

    public record ProductDto(
        Long id,
        String sku,
        String name,
        String slug,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String sourcePlatform,
        String sourceProductId,
        String sourceUrl,
        String productType,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        String currency,
        boolean priceVerified,
        String priceStatus,
        BigDecimal taxRate,
        BigDecimal shippingEstimate,
        Instant priceLastVerifiedAt,
        Instant availabilityVerifiedAt,
        String availabilityStatus,
        int stockQuantity,
        BigDecimal rating,
        Integer reviewCount,
        boolean ratingVerified,
        String dataVerificationStatus,
        Map<String, Object> specifications,
        Map<String, Object> attributes,
        List<String> missionTags,
        String priorityTier,
        List<String> lifecycleOptions,
        String imageUrl,
        String imageSource,
        String imageAttribution,
        String imageLicense,
        String imageStatus,
        List<ProductImageDto> additionalImages,
        boolean published,
        boolean approved,
        Instant createdAt
    ) {}

    public record AlternativesDto(
        List<ProductDto> budget,
        List<ProductDto> balanced,
        List<ProductDto> premium
    ) {}

    public record ProductCountsDto(
        long totalImportedProducts,
        long publishedProducts,
        long imageVerifiedProducts,
        long availableProducts,
        long pendingImageProducts,
        Map<String, Long> byRetailer,
        Map<String, Long> byCategory
    ) {}
}
