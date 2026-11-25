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
public class OpenAIProvider implements AIProvider {

    private final AppProperties props;
    private final ObjectMapper objectMapper;
    private final DeterministicFallbackEngine fallback;
    private final RestTemplate restTemplate = new RestTemplate();

    public OpenAIProvider(AppProperties props, ObjectMapper objectMapper, DeterministicFallbackEngine fallback) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.fallback = fallback;
    }

    @Override
    public String providerName() {
        return "OPENAI";
    }

    @Override
    public boolean isConfigured() {
        String key = props.ai().openaiApiKey();
        return key != null && !key.isBlank();
    }

    @Override
    public ParsedIntent parseMission(String prompt, BigDecimal explicitBudget, String timeframe, String alreadyOwn, List<String> preferences, String constraints) {
        if (!isConfigured()) {
            return fallback.parseMission(prompt, explicitBudget, timeframe, alreadyOwn, preferences, constraints);
        }
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            String sys = """
                You are MissionCart's purpose-first shopping intent parser.
                Return ONLY valid JSON with keys: title, goal, situationSummary, purpose, budget (INR number or null),
                timeframe, peopleCount, existingPossessions (string[]), preferences (string[]), constraints (string[]),
                assumptions (string[]), followUpQuestion, requirements ([{label, categorySlug, keywords, priority, quantity, reasoning}]).
                Valid categorySlug: electronics, fashion, skincare, beauty, home-decor, kitchen, groceries, travel, fitness, study-workspace, personal-care, storage-organization, everyday-essentials.
                """;
            Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.2,
                "messages", List.of(
                    Map.of("role", "system", "content", sys),
                    Map.of("role", "user", "content", "Situation: " + prompt + "\nBudget: " + explicitBudget + "\nTimeframe: " + timeframe + "\nAlready own: " + alreadyOwn)
                )
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.ai().openaiApiKey());
            String raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("choices").path(0).path("message").path("content").asText();
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
                toList(parsed.path("existingPossessions")),
                toList(parsed.path("preferences")),
                toList(parsed.path("constraints")),
                toList(parsed.path("assumptions")),
                reqs,
                parsed.path("followUpQuestion").isNull() ? null : parsed.path("followUpQuestion").asText(null),
                providerName(),
                Map.of("engineMode", "OpenAI API")
            );
        } catch (Exception ex) {
            return fallback.parseMission(prompt, explicitBudget, timeframe, alreadyOwn, preferences, constraints);
        }
    }

    private List<String> toList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) node.forEach(n -> list.add(n.asText()));
        return list;
    }
}
