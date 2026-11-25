package com.missioncart.ai;

import com.missioncart.ai.AIModels.ParsedIntent;
import com.missioncart.ai.AIModels.RequirementSpec;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequirementExtractor {

    public List<RequirementSpec> extract(ParsedIntent intent) {
        if (intent == null || intent.requirements() == null) {
            return List.of();
        }
        return intent.requirements().stream()
            .filter(r -> r.label() != null && !r.label().isBlank())
            .toList();
    }
}
