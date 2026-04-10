package com.mycompany.util;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Utility methods for extracting request metadata and writing response cookies.
 */
public final class RequestUtils {

    private RequestUtils() {
    }

    /**
     * Resolves the real client IP address, respecting reverse-proxy headers.
     */
    public static String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Returns the value of the first cookie with the given {@code name}, or
     * {@code null} if no such cookie exists.
     */
    public static String resolveCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Appends an HttpOnly, Secure, SameSite=Strict cookie to the response.
     *
     * @param path          the cookie path (e.g. {@code "/auth/refresh"})
     * @param maxAgeSeconds {@code 0} to expire immediately (logout)
     */
    public static void addHttpOnlyCookie(HttpServletResponse response, String name, String value,
            int maxAgeSeconds, String path) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
