package com.missioncart.auth;

import com.missioncart.auth.AuthDtos.*;
import com.missioncart.common.ApiResponse;
import com.missioncart.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req, HttpServletResponse res) {
        return ApiResponse.ok("Account created", authService.register(req, res));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        return ApiResponse.ok("Welcome back", authService.login(req, res));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        return ApiResponse.ok("Token refreshed", authService.refresh(req, res));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse res) {
        authService.logout(res);
        return ApiResponse.ok("Signed out", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileDto> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(authService.me(user));
    }
}
