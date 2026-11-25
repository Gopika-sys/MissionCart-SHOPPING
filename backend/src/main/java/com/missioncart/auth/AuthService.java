package com.missioncart.auth;

import com.missioncart.auth.AuthDtos.*;
import com.missioncart.config.AppProperties;
import com.missioncart.security.AuthenticatedUser;
import com.missioncart.security.JwtService;
import com.missioncart.users.User;
import com.missioncart.users.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;

    public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtService jwtService, AppProperties props) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.props = props;
    }

    public AuthResponse register(RegisterRequest req, HttpServletResponse response) {
        if (userRepo.existsByEmailIgnoreCase(req.email().trim())) {
            throw new IllegalStateException("An account with this email already exists.");
        }
        User user = new User();
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        String requestedRole = "SELLER".equalsIgnoreCase(req.accountType()) ? "SELLER" : "CUSTOMER";
        user.setRole(requestedRole);
        user = userRepo.save(user);
        return issueTokens(user, response);
    }

    public AuthResponse login(LoginRequest req, HttpServletResponse response) {
        User user = userRepo.findByEmailIgnoreCase(req.email().trim())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));
        if (!user.isEnabled()) {
            throw new IllegalStateException("Account is disabled.");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }
        return issueTokens(user, response);
    }

    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("mc_refresh".equals(c.getName())) {
                    refreshToken = c.getValue();
                    break;
                }
            }
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Missing refresh token.");
        }
        Claims claims = jwtService.parseRefreshToken(refreshToken);
        String email = claims.getSubject();
        User user = userRepo.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new EntityNotFoundException("User not found."));
        return issueTokens(user, response);
    }

    public void logout(HttpServletResponse response) {
        clearCookie(response, "mc_access", "/");
        clearCookie(response, "mc_refresh", "/api/auth");
    }

    public UserProfileDto me(AuthenticatedUser principal) {
        if (principal == null) throw new IllegalArgumentException("Not authenticated.");
        User u = userRepo.findById(principal.id())
            .orElseThrow(() -> new EntityNotFoundException("User not found."));
        return toDto(u);
    }

    private AuthResponse issueTokens(User user, HttpServletResponse response) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail(), user.getRole());

        ResponseCookie accessCookie = ResponseCookie.from("mc_access", access)
            .httpOnly(true)
            .secure(props.jwt().cookieSecure())
            .path("/")
            .maxAge(props.jwt().accessExpirationMs() / 1000)
            .sameSite("Lax")
            .build();
        ResponseCookie refreshCookie = ResponseCookie.from("mc_refresh", refresh)
            .httpOnly(true)
            .secure(props.jwt().cookieSecure())
            .path("/api/auth")
            .maxAge(props.jwt().refreshExpirationMs() / 1000)
            .sameSite("Lax")
            .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
        return new AuthResponse(toDto(user), access);
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
            .httpOnly(true)
            .path(path)
            .maxAge(0)
            .sameSite("Lax")
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public UserProfileDto toDto(User u) {
        return new UserProfileDto(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(), u.getAvatarUrl());
    }
}
