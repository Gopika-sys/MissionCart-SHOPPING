package com.missioncart.ai;

import com.missioncart.ai.AIModels.ParsedIntent;
import com.missioncart.config.AppProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MissionIntentParser {

    private final AppProperties props;
    private final GeminiAIProvider geminiProvider;
    private final OpenAIProvider openAIProvider;
    private final DeterministicFallbackEngine fallbackEngine;

    public MissionIntentParser(
        AppProperties props,
        GeminiAIProvider geminiProvider,
        OpenAIProvider openAIProvider,
        DeterministicFallbackEngine fallbackEngine
    ) {
        this.props = props;
        this.geminiProvider = geminiProvider;
        this.openAIProvider = openAIProvider;
        this.fallbackEngine = fallbackEngine;
    }

    public ParsedIntent parse(String prompt, BigDecimal budget, String timeframe, String alreadyOwn, List<String> preferences, String constraints) {
        String configured = props.ai().provider() == null ? "fallback" : props.ai().provider().trim().toLowerCase();
        if ("gemini".equals(configured) && geminiProvider.isConfigured()) {
            return geminiProvider.parseMission(prompt, budget, timeframe, alreadyOwn, preferences, constraints);
        }
        if ("openai".equals(configured) && openAIProvider.isConfigured()) {
            return openAIProvider.parseMission(prompt, budget, timeframe, alreadyOwn, preferences, constraints);
        }
        if (geminiProvider.isConfigured() && !"fallback".equals(configured)) {
            return geminiProvider.parseMission(prompt, budget, timeframe, alreadyOwn, preferences, constraints);
        }
        if (openAIProvider.isConfigured() && !"fallback".equals(configured)) {
            return openAIProvider.parseMission(prompt, budget, timeframe, alreadyOwn, preferences, constraints);
        }
        return fallbackEngine.parseMission(prompt, budget, timeframe, alreadyOwn, preferences, constraints);
    }
}
