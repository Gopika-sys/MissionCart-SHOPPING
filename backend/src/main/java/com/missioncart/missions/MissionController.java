package com.missioncart.missions;

import com.missioncart.budget.BudgetPlannerService.OptimizationPlan;
import com.missioncart.common.ApiResponse;
import com.missioncart.missions.MissionDtos.*;
import com.missioncart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @PostMapping("/plan")
    public ApiResponse<MissionDetailDto> plan(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody PlanMissionRequest req
    ) {
        Long uid = user != null ? user.id() : null;
        return ApiResponse.ok("Mission planned", missionService.planMission(uid, req));
    }

    @GetMapping
    public ApiResponse<List<MissionDetailDto>> myMissions(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(missionService.listForUser(user.id()));
    }

    @GetMapping("/{id}")
    public ApiResponse<MissionDetailDto> detail(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok(missionService.getMissionDetail(id, user != null ? user.id() : null));
    }

    @PatchMapping("/{id}")
    public ApiResponse<MissionDetailDto> update(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody UpdateMissionRequest req
    ) {
        return ApiResponse.ok("Mission updated", missionService.updateMission(id, user != null ? user.id() : null, req));
    }

    @PostMapping("/{id}/recalculate")
    public ApiResponse<MissionDetailDto> recalculate(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) UpdateMissionRequest req
    ) {
        UpdateMissionRequest payload = req != null ? req : new UpdateMissionRequest(null, null, null, List.of());
        return ApiResponse.ok("Budget recalculated", missionService.updateMission(id, user != null ? user.id() : null, payload));
    }

    @PostMapping("/{id}/optimize")
    public ApiResponse<OptimizationPlan> optimize(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.ok("Optimization suggestions ready", missionService.optimizeMission(id, user != null ? user.id() : null));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        missionService.deleteMission(id, user.id());
        return ApiResponse.ok("Mission deleted", null);
    }
}
