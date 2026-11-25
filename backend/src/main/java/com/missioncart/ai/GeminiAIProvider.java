package com.missioncart.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missioncart.ai.AIModels.ParsedIntent;
import com.missioncart.ai.AIModels.RequirementSpec;
import com.missioncart.config.AppProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GeminiAIProvider implements AIProvider {

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final DeterministicFallbackEngine fallback;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiAIProvider(AppProperties props, ObjectMapper objectMapper, DeterministicFallbackEngine fallback) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
    }

    @Override
    public String providerName() {
        return "GEMINI";
    }

    @Override
    public boolean isConfigured() {
        String key = props.ai().geminiApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public ParsedIntent parseMission(String prompt, BigDecimal explicitBudget, String timeframe, String alreadyOwn, List<String> preferences, String constraints) {
        if (!isConfigured()) {
            return fallback.parseMission(prompt, explicitBudget, timeframe, alreadyOwn, preferences, constraints);
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + props.ai().geminiApiKey();
            String systemInstruction = """
                You are MissionCart's purpose-first shopping intent parser.
                Extract a strict JSON object with keys:
                title (string), goal (string), situationSummary (string), purpose (string),
                budget (number or null in INR), timeframe (string), peopleCount (int),
                existingPossessions (string[]), preferences (string[]), constraints (string[]),
                assumptions (string[]), followUpQuestion (string or null),
                requirements (array of {label, categorySlug, keywords (string[]), priority ("ESSENTIAL"|"OPTIONAL"), quantity (int), reasoning}).
                Valid categorySlug values: electronics, fashion, skincare, beauty, home-decor, kitchen, groceries, travel, fitness, study-workspace, personal-care, storage-organization, everyday-essentials.
                Never invent product IDs, URLs, or prices. Return ONLY valid JSON.
                """;
            String userText = "Situation: " + prompt
                + "\nExplicit Budget (INR): " + explicitBudget
                + "\nTimeframe: " + timeframe
                + "\nAlready own: " + alreadyOwn
                + "\nPreferences: " + preferences
                + "\nConstraints: " + constraints;

            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", systemInstruction + "\n\n" + userText)))),
                "generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.2)
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            JsonNode parsed = objectMapper.readTree(text);

            List<RequirementSpec> reqs = new ArrayList<>();
            for (JsonNode r : parsed.path("requirements")) {
                List<String> kws = new ArrayList<>();
                r.path("keywords").forEach(k -> kws.add(k.asText()));
                reqs.add(new RequirementSpec(
                    r.path("label").asText("Requirement"),
                    r.path("categorySlug").isNull() ? null : r.path("categorySlug").asText(),
                    kws,
                    r.path("priority").asText("ESSENTIAL"),
                    Math.max(1, r.path("quantity").asInt(1)),
                    r.path("reasoning").asText("Matched to your situation")
                ));
            }
            if (reqs.isEmpty()) {
                return fallback.parseMission(prompt, explicitBudget, timeframe, alreadyOwn, preferences, constraints);
            }
            BigDecimal budgetVal = explicitBudget != null ? explicitBudget
                : (parsed.path("budget").isNumber() ? parsed.path("budget").decimalValue() : null);

            return new ParsedIntent(
                parsed.path("title").asText("Custom Mission"),
                parsed.path("goal").asText(prompt),
                parsed.path("situationSummary").asText(""),
                parsed.path("purpose").asText("Custom Purpose"),
                budgetVal,
                "INR",
                parsed.path("timeframe").asText("Flexible"),
                Math.max(1, parsed.path("peopleCount").asInt(1)),
                toStringList(parsed.path("existingPossessions")),
                toStringList(parsed.path("preferences")),
                toStringList(parsed.path("constraints")),
                toStringList(parsed.path("assumptions")),
                reqs,
                parsed.path("followUpQuestion").isNull() ? null : parsed.path("followUpQuestion").asText(null),
                providerName(),
                Map.of("engineMode", "Google Gemini API")
            );
        } catch (Exception ex) {
            return fallback.parseMission(prompt, explicitBudget, timeframe, alreadyOwn, preferences, constraints);
        }
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> list.add(n.asText()));
        }
        return list;
    }
}
