package com.missioncart.missions;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "mission_items")
public class MissionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "requirement_label", nullable = false, length = 150)
    private String requirementLabel;

    @Column(nullable = false, length = 20)
    private String priority = "ESSENTIAL";

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "is_selected", nullable = false)
    private boolean selected = true;

    @Column(name = "is_excluded_owned", nullable = false)
    private boolean excludedOwned = false;

    @Column(name = "compatibility_status", nullable = false, length = 30)
    private String compatibilityStatus = "COMPATIBLE";

    @Column(name = "compatibility_reason", length = 400)
    private String compatibilityReason;

    @Column(name = "recommendation_reason", length = 400)
    private String recommendationReason;

    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @Column(name = "claimed_by_name", length = 150)
    private String claimedByName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getMissionId() { return missionId; }
    public void setMissionId(Long missionId) { this.missionId = missionId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getRequirementLabel() { return requirementLabel; }
    public void setRequirementLabel(String requirementLabel) { this.requirementLabel = requirementLabel; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isExcludedOwned() { return excludedOwned; }
    public void setExcludedOwned(boolean excludedOwned) { this.excludedOwned = excludedOwned; }
    public String getCompatibilityStatus() { return compatibilityStatus; }
    public void setCompatibilityStatus(String compatibilityStatus) { this.compatibilityStatus = compatibilityStatus; }
    public String getCompatibilityReason() { return compatibilityReason; }
    public void setCompatibilityReason(String compatibilityReason) { this.compatibilityReason = compatibilityReason; }
    public String getRecommendationReason() { return recommendationReason; }
    public void setRecommendationReason(String recommendationReason) { this.recommendationReason = recommendationReason; }
    public Long getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(Long assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public String getClaimedByName() { return claimedByName; }
    public void setClaimedByName(String claimedByName) { this.claimedByName = claimedByName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
