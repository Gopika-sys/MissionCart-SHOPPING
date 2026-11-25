package com.missioncart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Window(long minuteEpoch, AtomicInteger count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") || path.startsWith("/api/missions/plan")) {
            String key = request.getRemoteAddr() + ":" + (path.startsWith("/api/auth/") ? "auth" : "plan");
            long currentMinute = Instant.now().getEpochSecond() / 60;
            Window w = windows.compute(key, (k, existing) -> {
                if (existing == null || existing.minuteEpoch() != currentMinute) {
                    return new Window(currentMinute, new AtomicInteger(1));
                }
                existing.count().incrementAndGet();
                return existing;
            });
            int limit = path.startsWith("/api/auth/") ? 60 : 40;
            if (w.count().get() > limit) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please slow down.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
