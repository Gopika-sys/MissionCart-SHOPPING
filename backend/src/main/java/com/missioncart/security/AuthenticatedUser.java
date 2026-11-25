package com.missioncart.security;

public record AuthenticatedUser(Long id, String email, String role) {}
