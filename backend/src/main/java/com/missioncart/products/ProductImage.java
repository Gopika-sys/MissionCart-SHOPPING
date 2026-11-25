package com.missioncart.products;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "image_url", nullable = false, length = 700)
    private String imageUrl;

    @Column(name = "image_source", nullable = false, length = 150)
    private String imageSource;

    @Column(name = "image_attribution", length = 300)
    private String imageAttribution;

    @Column(name = "image_license", length = 150)
    private String imageLicense;

    @Column(name = "image_status", nullable = false, length = 40)
    private String imageStatus = "PENDING_REVIEW";

    @Column(name = "image_verified_at")
    private Instant imageVerifiedAt;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "image_alt_text", nullable = false)
    private String imageAltText;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryImage = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
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
    public Instant getImageVerifiedAt() { return imageVerifiedAt; }
    public void setImageVerifiedAt(Instant imageVerifiedAt) { this.imageVerifiedAt = imageVerifiedAt; }
    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    public String getImageAltText() { return imageAltText; }
    public void setImageAltText(String imageAltText) { this.imageAltText = imageAltText; }
    public boolean isPrimaryImage() { return primaryImage; }
    public void setPrimaryImage(boolean primaryImage) { this.primaryImage = primaryImage; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
