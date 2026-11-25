package com.missioncart.compatibility;

import com.missioncart.common.ApiResponse;
import com.missioncart.compatibility.CompatibilityEngine.CompatibilityReport;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compatibility")
public class CompatibilityController {

    private final CompatibilityEngine engine;
    private final CompatibilityRuleRepository ruleRepo;

    public CompatibilityController(CompatibilityEngine engine, CompatibilityRuleRepository ruleRepo) {
        this.engine = engine;
        this.ruleRepo = ruleRepo;
    }

    public record CheckRequest(List<Long> productIds, Map<String, Object> context) {}

    @PostMapping("/check")
    public ApiResponse<CompatibilityReport> check(@RequestBody CheckRequest req) {
        return ApiResponse.ok(engine.evaluate(req.productIds(), req.context()));
    }

    @GetMapping("/rules")
    public ApiResponse<List<CompatibilityRule>> rules() {
        return ApiResponse.ok(ruleRepo.findByActiveTrue());
    }
}
