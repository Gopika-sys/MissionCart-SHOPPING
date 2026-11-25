package com.missioncart.shared;

import com.missioncart.common.ApiResponse;
import com.missioncart.missions.MissionDtos.MissionDetailDto;
import com.missioncart.security.AuthenticatedUser;
import com.missioncart.shared.SharedMissionService.SharedRoomDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SharedMissionController {

    private final SharedMissionService sharedService;

    public SharedMissionController(SharedMissionService sharedService) {
        this.sharedService = sharedService;
    }

    public record JoinRequest(String displayName) {}
    public record SharedItemUpdateRequest(String action, String claimName, Long replaceProductId) {}
    public record ContributionRequest(BigDecimal amount, String note) {}

    @PostMapping("/missions/{id}/share")
    public ApiResponse<SharedRoomDto> share(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok("Shared mission link ready", sharedService.createShareLink(id, user));
    }

    @GetMapping("/shared/my")
    public ApiResponse<List<MissionDetailDto>> myShared(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(sharedService.listSharedMissionsForUser(user.id()));
    }

    @GetMapping("/shared/{token}")
    public ApiResponse<SharedRoomDto> getRoom(@PathVariable String token) {
        return ApiResponse.ok(sharedService.getByToken(token));
    }

    @PostMapping("/shared/{token}/join")
    public ApiResponse<SharedRoomDto> join(
        @PathVariable String token,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody(required = false) JoinRequest req
    ) {
        return ApiResponse.ok("Joined shared mission", sharedService.join(token, user, req != null ? req.displayName() : null));
    }

    @PatchMapping("/shared/{token}/items/{itemId}")
    public ApiResponse<SharedRoomDto> updateItem(
        @PathVariable String token,
        @PathVariable Long itemId,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody SharedItemUpdateRequest req
    ) {
        return ApiResponse.ok("Shared item updated", sharedService.updateSharedItem(token, itemId, user, req.action(), req.claimName(), req.replaceProductId()));
    }

    @PostMapping("/shared/{token}/contributions")
    public ApiResponse<SharedRoomDto> contribute(
        @PathVariable String token,
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestBody ContributionRequest req
    ) {
        return ApiResponse.ok("Demo contribution logged", sharedService.recordDemoContribution(token, user, req.amount(), req.note()));
    }
}
