package com.missioncart.products;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "subcategory_id")
    private Long subcategoryId;

    @Column(name = "source_platform", nullable = false, length = 60)
    private String sourcePlatform;

    @Column(name = "source_product_id", length = 150)
    private String sourceProductId;

    @Column(name = "source_url", length = 600)
    private String sourceUrl;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "product_type", nullable = false, length = 60)
    private String productType = "EXTERNAL_CATALOG";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "price_verified", nullable = false)
    private boolean priceVerified = false;

    @Column(name = "price_status", nullable = false, length = 25)
    private String priceStatus = "UNAVAILABLE";

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.valueOf(18.00);

    @Column(name = "shipping_estimate", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingEstimate = BigDecimal.ZERO;

    @Column(name = "price_last_verified_at")
    private Instant priceLastVerifiedAt;

    @Column(name = "availability_verified_at")
    private Instant availabilityVerifiedAt;

    @Column(name = "price_expires_at")
    private Instant priceExpiresAt;

    @Column(name = "availability_status", nullable = false, length = 40)
    private String availabilityStatus = "IN_STOCK";

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @Column(name = "rating_verified", nullable = false)
    private boolean ratingVerified = false;

    @Column(name = "data_verification_status", nullable = false, length = 60)
    private String dataVerificationStatus = "SAMPLE_IMPORT_PENDING";

    @Column(name = "specifications_json", columnDefinition = "TEXT")
    private String specificationsJson;

    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    @Column(name = "mission_tags", columnDefinition = "TEXT")
    private String missionTags;

    @Column(name = "priority_tier", nullable = false, length = 30)
    private String priorityTier = "BALANCED";

    @Column(name = "compatibility_rules_json", columnDefinition = "TEXT")
    private String compatibilityRulesJson;

    @Column(name = "lifecycle_options_json", columnDefinition = "TEXT")
    private String lifecycleOptionsJson;

    @Column(name = "image_url", length = 700)
    private String imageUrl;

    @Column(name = "image_source", length = 150)
    private String imageSource;

    @Column(name = "image_attribution", length = 300)
    private String imageAttribution;

    @Column(name = "image_license", length = 150)
    private String imageLicense;

    @Column(name = "image_status", nullable = false, length = 40)
    private String imageStatus = "REQUIRES_IMAGE";

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Column(name = "is_approved", nullable = false)
    private boolean approved = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getSubcategoryId() { return subcategoryId; }
    public void setSubcategoryId(Long subcategoryId) { this.subcategoryId = subcategoryId; }
    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }
    public String getSourceProductId() { return sourceProductId; }
    public void setSourceProductId(String sourceProductId) { this.sourceProductId = sourceProductId; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isPriceVerified() { return priceVerified; }
    public void setPriceVerified(boolean priceVerified) { this.priceVerified = priceVerified; }
    public String getPriceStatus() { return priceStatus; }
    public void setPriceStatus(String priceStatus) { this.priceStatus = priceStatus; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getShippingEstimate() { return shippingEstimate; }
    public void setShippingEstimate(BigDecimal shippingEstimate) { this.shippingEstimate = shippingEstimate; }
    public Instant getPriceLastVerifiedAt() { return priceLastVerifiedAt; }
    public void setPriceLastVerifiedAt(Instant priceLastVerifiedAt) { this.priceLastVerifiedAt = priceLastVerifiedAt; }
    public Instant getAvailabilityVerifiedAt() { return availabilityVerifiedAt; }
    public void setAvailabilityVerifiedAt(Instant availabilityVerifiedAt) { this.availabilityVerifiedAt = availabilityVerifiedAt; }
    public Instant getPriceExpiresAt() { return priceExpiresAt; }
    public void setPriceExpiresAt(Instant priceExpiresAt) { this.priceExpiresAt = priceExpiresAt; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public boolean isRatingVerified() { return ratingVerified; }
    public void setRatingVerified(boolean ratingVerified) { this.ratingVerified = ratingVerified; }
    public String getDataVerificationStatus() { return dataVerificationStatus; }
    public void setDataVerificationStatus(String dataVerificationStatus) { this.dataVerificationStatus = dataVerificationStatus; }
    public String getSpecificationsJson() { return specificationsJson; }
    public void setSpecificationsJson(String specificationsJson) { this.specificationsJson = specificationsJson; }
    public String getAttributesJson() { return attributesJson; }
    public void setAttributesJson(String attributesJson) { this.attributesJson = attributesJson; }
    public String getMissionTags() { return missionTags; }
    public void setMissionTags(String missionTags) { this.missionTags = missionTags; }
    public String getPriorityTier() { return priorityTier; }
    public void setPriorityTier(String priorityTier) { this.priorityTier = priorityTier; }
    public String getCompatibilityRulesJson() { return compatibilityRulesJson; }
    public void setCompatibilityRulesJson(String compatibilityRulesJson) { this.compatibilityRulesJson = compatibilityRulesJson; }
    public String getLifecycleOptionsJson() { return lifecycleOptionsJson; }
    public void setLifecycleOptionsJson(String lifecycleOptionsJson) { this.lifecycleOptionsJson = lifecycleOptionsJson; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getImageSource() { return imageSource; }
    public void setImageSource(String imageSource) { this.imageSource = imageSource; }
    public String getImageAttribution() { return imageAttribution; }
    public void setImageAttribution(String imageAttribution) { this.imageAttribution = imageAttribution; }
    public String getImageLicense() { return imageLicense; }
    public void setImageLicense(String imageLicense) { this.imageLicense = imageLicense; }
    public String getImageStatus() { return imageStatus; }
    public void setImageStatus(String imageStatus) { this.imageStatus = imageStatus; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
