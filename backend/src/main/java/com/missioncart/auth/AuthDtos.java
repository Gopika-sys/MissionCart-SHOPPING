package com.missioncart.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank String fullName,
        String phone,
        String accountType
    ) {}

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    public record UserProfileDto(
        Long id,
        String email,
        String fullName,
        String phone,
        String role,
        String avatarUrl
    ) {}

    public record AuthResponse(
        UserProfileDto user,
        String accessToken
    ) {}
}
