package com.missioncart.possessions;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_possessions")
public class UserPossession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "category_slug", length = 120)
    private String categorySlug;

    @Column(length = 300)
    private String keywords;

    @Column(name = "matched_product_id")
    private Long matchedProductId;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Long getMatchedProductId() { return matchedProductId; }
    public void setMatchedProductId(Long matchedProductId) { this.matchedProductId = matchedProductId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
}
