package com.mycompany.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.dto.APIResponse;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate-limit filter using Bucket4j (token-bucket algorithm).
 *
 * Rules (per IP address):
 * • /auth/login – 5 requests / minute (brute-force guard)
 * • /auth/register – 3 requests / minute
 * • all other – 60 requests / minute
 */
@Slf4j
@Component
@Order(0) // runs before JwtExceptionHandlerFilter
public class RateLimitFilter extends OncePerRequestFilter {

    // Separate bucket-maps for each limit policy
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ---------------------------------------------------------------
    // Bucket factories
    // ---------------------------------------------------------------

    private Bucket loginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket registerBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(3)
                        .refillGreedy(3, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket generalBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(60)
                        .refillGreedy(60, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    // ---------------------------------------------------------------
    // Filter logic
    // ---------------------------------------------------------------

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String ip = resolveClientIp(request);
        String path = request.getServletPath();
        String method = request.getMethod();

        Bucket bucket = resolveBucket(ip, path, method);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} path={}", ip, path);
            sendRateLimitResponse(response, path);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Bucket resolveBucket(String ip, String path, String method) {
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/auth/login")) {
            return loginBuckets.computeIfAbsent(ip, k -> loginBucket());
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/auth/register")) {
            return registerBuckets.computeIfAbsent(ip, k -> registerBucket());
        }
        return generalBuckets.computeIfAbsent(ip, k -> generalBucket());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first address (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");

        String message = path.contains("/auth/login")
                ? "Too many login attempts. Please try again after 1 minute."
                : "Too many requests. Please slow down.";

        APIResponse<Object> body = APIResponse.error(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
