package com.skillbridge.security;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits POST /api/auth/* (register and login) per client address with a fixed window.
 * In memory: per instance, reset on restart. Keyed on the TCP peer address, never on X-Forwarded-For,
 * because any client can forge that header.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private record Window(long startMillis, int count) { }

    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(int maxRequests, long windowSeconds, LongSupplier clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1000;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.getAsLong();
        Window window = windows.compute(request.getRemoteAddr(), (client, old) ->
                old == null || now - old.startMillis() >= windowMillis
                        ? new Window(now, 1)
                        : new Window(old.startMillis(), old.count() + 1));
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(e -> now - e.getValue().startMillis() >= windowMillis);
        }
        if (window.count() > maxRequests) {
            long retryAfter = Math.max(1, (window.startMillis() + windowMillis - now + 999) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"error\":{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many requests. Try again later.\"}}");
            return;
        }
        chain.doFilter(request, response);
    }
}