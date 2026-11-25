package com.missioncart.retailers;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "retailer_sources")
public class RetailerSource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "official_website", nullable = false, length = 300)
    private String officialWebsite;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "category_focus", nullable = false)
    private String categoryFocus;

    @Column(name = "supported_categories", nullable = false, columnDefinition = "TEXT")
    private String supportedCategories;

    @Column(name = "import_method", nullable = false, length = 80)
    private String importMethod;

    @Column(name = "product_url_pattern", length = 300)
    private String productUrlPattern;

    @Column(name = "image_sourcing_method", nullable = false, length = 200)
    private String imageSourcingMethod;

    @Column(name = "connection_status", nullable = false, length = 60)
    private String connectionStatus = "CONFIGURED_FEED_PENDING";

    @Column(name = "is_native_checkout", nullable = false)
    private boolean nativeCheckout = false;

    @Column(name = "last_successful_import")
    private Instant lastSuccessfulImport;

    @Column(name = "last_price_verification")
    private Instant lastPriceVerification;

    @Column(name = "feed_config_json", columnDefinition = "TEXT")
    private String feedConfigJson;

    @Column(name = "normalization_rules_json", columnDefinition = "TEXT")
    private String normalizationRulesJson;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOfficialWebsite() { return officialWebsite; }
    public void setOfficialWebsite(String officialWebsite) { this.officialWebsite = officialWebsite; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getCategoryFocus() { return categoryFocus; }
    public void setCategoryFocus(String categoryFocus) { this.categoryFocus = categoryFocus; }
    public String getSupportedCategories() { return supportedCategories; }
    public void setSupportedCategories(String supportedCategories) { this.supportedCategories = supportedCategories; }
    public String getImportMethod() { return importMethod; }
    public void setImportMethod(String importMethod) { this.importMethod = importMethod; }
    public String getProductUrlPattern() { return productUrlPattern; }
    public void setProductUrlPattern(String productUrlPattern) { this.productUrlPattern = productUrlPattern; }
    public String getImageSourcingMethod() { return imageSourcingMethod; }
    public void setImageSourcingMethod(String imageSourcingMethod) { this.imageSourcingMethod = imageSourcingMethod; }
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
    public boolean isNativeCheckout() { return nativeCheckout; }
    public void setNativeCheckout(boolean nativeCheckout) { this.nativeCheckout = nativeCheckout; }
    public Instant getLastSuccessfulImport() { return lastSuccessfulImport; }
    public void setLastSuccessfulImport(Instant lastSuccessfulImport) { this.lastSuccessfulImport = lastSuccessfulImport; }
    public Instant getLastPriceVerification() { return lastPriceVerification; }
    public void setLastPriceVerification(Instant lastPriceVerification) { this.lastPriceVerification = lastPriceVerification; }
    public String getFeedConfigJson() { return feedConfigJson; }
    public void setFeedConfigJson(String feedConfigJson) { this.feedConfigJson = feedConfigJson; }
    public String getNormalizationRulesJson() { return normalizationRulesJson; }
    public void setNormalizationRulesJson(String normalizationRulesJson) { this.normalizationRulesJson = normalizationRulesJson; }
}
