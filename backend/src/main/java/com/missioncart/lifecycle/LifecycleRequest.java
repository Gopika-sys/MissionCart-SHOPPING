package com.missioncart.lifecycle;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "lifecycle_requests")
public class LifecycleRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(nullable = false, length = 40)
    private String status = "SUBMITTED_DEMO_WORKFLOW";

    @Column(name = "details_json", nullable = false, columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "estimated_value", precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "evidence_urls_json", columnDefinition = "TEXT")
    private String evidenceUrlsJson;

    @Column(name = "provider_adapter_status", nullable = false, length = 60)
    private String providerAdapterStatus = "TRANSPARENT_REQUEST_QUEUE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getEvidenceUrlsJson() { return evidenceUrlsJson; }
    public void setEvidenceUrlsJson(String evidenceUrlsJson) { this.evidenceUrlsJson = evidenceUrlsJson; }
    public String getProviderAdapterStatus() { return providerAdapterStatus; }
    public void setProviderAdapterStatus(String providerAdapterStatus) { this.providerAdapterStatus = providerAdapterStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
