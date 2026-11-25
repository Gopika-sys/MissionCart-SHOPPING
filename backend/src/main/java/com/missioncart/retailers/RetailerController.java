package com.missioncart.retailers;

import com.missioncart.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RetailerController {

    private final RetailerSourceRepository retailerRepo;

    public RetailerController(RetailerSourceRepository retailerRepo) {
        this.retailerRepo = retailerRepo;
    }

    @GetMapping("/sources")
    public ApiResponse<List<RetailerSource>> listSources() {
        return ApiResponse.ok(retailerRepo.findAllByOrderByIdAsc());
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "service", "MissionCart API"));
    }
}
