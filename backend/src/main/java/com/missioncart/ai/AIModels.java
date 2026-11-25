package com.missioncart.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AIModels {
    public record RequirementSpec(
        String label,
        String categorySlug,
        List<String> keywords,
        String priority,
        int quantity,
        String reasoning
    ) {}

    public record ParsedIntent(
        String title,
        String goal,
        String situationSummary,
        String purpose,
        BigDecimal budget,
        String currency,
        String timeframe,
        int peopleCount,
        List<String> existingPossessions,
        List<String> preferences,
        List<String> constraints,
        List<String> assumptions,
        List<RequirementSpec> requirements,
        String followUpQuestion,
        String providerUsed,
        Map<String, Object> context
    ) {}
}
