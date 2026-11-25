package com.missioncart.missions;

import com.missioncart.budget.BudgetPlannerService.BudgetSummary;
import com.missioncart.compatibility.CompatibilityEngine.CompatibilityReport;
import com.missioncart.products.ProductDtos.ProductDto;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class MissionDtos {
    public record PlanMissionRequest(
        @NotBlank(message = "Please describe what you are trying to achieve") String prompt,
        BigDecimal budget,
        String timeframe,
        String location,
        String alreadyOwn,
        List<String> preferences,
        String constraints
    ) {}

    public record UpdateMissionItemAction(
        Long itemId,
        Boolean selected,
        Integer quantity,
        String priority,
        Long replaceWithProductId,
        Boolean excludedOwned,
        Long addProductId,
        String addRequirementLabel
    ) {}

    public record UpdateMissionRequest(
        String title,
        BigDecimal budget,
        String status,
        List<UpdateMissionItemAction> itemActions
    ) {}

    public record MissionItemDto(
        Long id,
        Long productId,
        ProductDto product,
        String requirementLabel,
        String priority,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        boolean selected,
        boolean excludedOwned,
        String compatibilityStatus,
        String compatibilityReason,
        String recommendationReason,
        Long assignedToUserId,
        String claimedByName
    ) {}

    public record MissionDetailDto(
        Long id,
        Long userId,
        String title,
        String slug,
        String rawPrompt,
        String goal,
        String situationSummary,
        String purpose,
        BigDecimal budget,
        String currency,
        String timeframe,
        Integer peopleCount,
        String aiProviderUsed,
        String aiExplanation,
        List<String> assumptions,
        List<String> existingPossessions,
        List<String> unmatchedRequirements,
        String followUpQuestion,
        String status,
        boolean shared,
        String shareToken,
        BudgetSummary budgetSummary,
        CompatibilityReport compatibilityReport,
        List<MissionItemDto> items,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
