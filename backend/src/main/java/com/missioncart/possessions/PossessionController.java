package com.missioncart.possessions;

import com.missioncart.common.ApiResponse;
import com.missioncart.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/possessions")
public class PossessionController {

    private final PossessionService possessionService;

    public PossessionController(PossessionService possessionService) {
        this.possessionService = possessionService;
    }

    public record AddPossessionRequest(@NotBlank String itemText, String categorySlug, Long matchedProductId) {}

    @GetMapping
    public ApiResponse<List<UserPossession>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(possessionService.listForUser(user.id()));
    }

    @PostMapping
    public ApiResponse<List<UserPossession>> add(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody AddPossessionRequest req
    ) {
        return ApiResponse.ok("Possession saved", possessionService.addFromInput(user.id(), req.itemText(), req.categorySlug(), req.matchedProductId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        possessionService.deletePossession(user.id(), id);
        return ApiResponse.ok("Removed", null);
    }
}
