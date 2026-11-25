package com.missioncart.ai;

import com.missioncart.ai.AIModels.ParsedIntent;
import java.math.BigDecimal;
import java.util.List;

public interface AIProvider {
    String providerName();
    boolean isConfigured();
    ParsedIntent parseMission(String prompt, BigDecimal explicitBudget, String timeframe, String alreadyOwn, List<String> preferences, String constraints);
}
