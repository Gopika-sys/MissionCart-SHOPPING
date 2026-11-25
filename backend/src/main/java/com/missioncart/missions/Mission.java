package com.missioncart.missions;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "missions")
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "raw_prompt", nullable = false, columnDefinition = "TEXT")
    private String rawPrompt;

    @Column(nullable = false, length = 300)
    private String goal;

    @Column(name = "situation_summary", columnDefinition = "TEXT")
    private String situationSummary;

    @Column(length = 150)
    private String purpose;

    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(length = 120)
    private String timeframe;

    @Column(name = "people_count")
    private Integer peopleCount = 1;

    @Column(name = "ai_provider_used", nullable = false, length = 60)
    private String aiProviderUsed;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "requirements_json", columnDefinition = "TEXT")
    private String requirementsJson;

    @Column(name = "constraints_json", columnDefinition = "TEXT")
    private String constraintsJson;

    @Column(name = "assumptions_json", columnDefinition = "TEXT")
    private String assumptionsJson;

    @Column(name = "follow_up_question", length = 350)
    private String followUpQuestion;

    @Column(nullable = false, length = 40)
    private String status = "ACTIVE";

    @Column(name = "share_token", unique = true, length = 120)
    private String shareToken;

    @Column(name = "is_shared", nullable = false)
    private boolean shared = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getRawPrompt() { return rawPrompt; }
    public void setRawPrompt(String rawPrompt) { this.rawPrompt = rawPrompt; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getSituationSummary() { return situationSummary; }
    public void setSituationSummary(String situationSummary) { this.situationSummary = situationSummary; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public Integer getPeopleCount() { return peopleCount; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public String getAiProviderUsed() { return aiProviderUsed; }
    public void setAiProviderUsed(String aiProviderUsed) { this.aiProviderUsed = aiProviderUsed; }
    public String getAiExplanation() { return aiExplanation; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public String getRequirementsJson() { return requirementsJson; }
    public void setRequirementsJson(String requirementsJson) { this.requirementsJson = requirementsJson; }
    public String getConstraintsJson() { return constraintsJson; }
    public void setConstraintsJson(String constraintsJson) { this.constraintsJson = constraintsJson; }
    public String getAssumptionsJson() { return assumptionsJson; }
    public void setAssumptionsJson(String assumptionsJson) { this.assumptionsJson = assumptionsJson; }
    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public boolean isShared() { return shared; }
    public void setShared(boolean shared) { this.shared = shared; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
