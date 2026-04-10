package com.mycompany.security;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mycompany.service.TokenBlacklistService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@AllArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    JwtUtils jwtUtils;
    CustomUserDetailsService customUserDetailsService;
    TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Check if token is blacklisted (revoked) - should NOT process if blacklisted
            if (!tokenBlacklistService.isTokenBlacklisted(token)) {
                String userName = jwtUtils.getUserNameFromToken(token);
                if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);
                    if (jwtUtils.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userName, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            } else {
                throw new RuntimeException("Token is blacklisted");
            }
        }
        filterChain.doFilter(request, response);
    }

}
