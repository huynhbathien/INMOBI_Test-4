package com.mycompany.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.dto.APIResponse;
import com.mycompany.dto.request.ForgotPasswordRequestDTO;
import com.mycompany.dto.request.ResendOtpRequestDTO;
import com.mycompany.dto.request.ResetPasswordRequestDTO;
import com.mycompany.dto.request.VerifyOtpRequestDTO;
import com.mycompany.enums.EnumAuthError;
import com.mycompany.enums.EnumSuccess;
import com.mycompany.service.AuthService;
import com.mycompany.util.RequestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthController {

    AuthService authService;

    static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    static final String REFRESH_TOKEN_PATH = "/auth/refresh";

    @Value("${token.refresh-token-expiration:604800}")
    long refreshTokenExpirationSeconds;

    @PostMapping("/verify-email")
    public APIResponse<Object> verifyEmail(@Valid @RequestBody VerifyOtpRequestDTO dto,
            HttpServletResponse response) {
        Map<String, String> data = authService.verifyEmailAndLogin(dto);
        setRefreshTokenCookie(response, data.get(REFRESH_TOKEN_COOKIE));
        return APIResponse.success(EnumSuccess.EMAIL_VERIFIED.getCode(),
                EnumSuccess.EMAIL_VERIFIED.getMessage(), buildAccessTokenResponse(data));
    }

    @PostMapping("/resend-otp")
    public APIResponse<Object> resendOtp(@Valid @RequestBody ResendOtpRequestDTO dto) {
        authService.resendOtp(dto);
        return APIResponse.success(EnumSuccess.OTP_SENT.getCode(),
                EnumSuccess.OTP_SENT.getMessage(), null);
    }

    @PostMapping("/forgot-password")
    public APIResponse<Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        authService.forgotPassword(dto);
        return APIResponse.success(EnumSuccess.OTP_SENT.getCode(),
                "Password reset OTP has been sent to your email.", null);
    }

    @PostMapping("/reset-password")
    public APIResponse<Object> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        authService.resetPassword(dto);
        return APIResponse.success(EnumSuccess.PASSWORD_RESET_SUCCESS.getCode(),
                EnumSuccess.PASSWORD_RESET_SUCCESS.getMessage(), null);
    }

    // ─── Authenticated endpoints
    // ──────────────────────────────────────────────────

    @PostMapping("/refresh")
    public APIResponse<Object> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = RequestUtils.resolveCookieValue(request, REFRESH_TOKEN_COOKIE);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.REFRESH_TOKEN_NOT_FOUND.getMessage());
        }
        Map<String, String> data = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, data.get(REFRESH_TOKEN_COOKIE));
        return APIResponse.success(EnumSuccess.LOGIN_SUCCESS.getCode(),
                EnumSuccess.LOGIN_SUCCESS.getMessage(), buildAccessTokenResponse(data));
    }

    @PostMapping("/logout")
    public APIResponse<Object> logout(HttpServletRequest request, HttpServletResponse response) {
        String username = resolveAuthenticatedUsername();
        authService.logout(username);
        RequestUtils.addHttpOnlyCookie(response, REFRESH_TOKEN_COOKIE, "", 0, REFRESH_TOKEN_PATH);
        return APIResponse.success(EnumSuccess.SUCCESS.getCode(), "Logged out successfully", null);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private String resolveAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    EnumAuthError.UNAUTHORIZED.getMessage());
        }
        return auth.getName();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        RequestUtils.addHttpOnlyCookie(response, REFRESH_TOKEN_COOKIE, token,
                (int) refreshTokenExpirationSeconds, REFRESH_TOKEN_PATH);
    }

    private Map<String, String> buildAccessTokenResponse(Map<String, String> tokenPair) {
        return Map.of("token", tokenPair.get("token"));
    }
}
