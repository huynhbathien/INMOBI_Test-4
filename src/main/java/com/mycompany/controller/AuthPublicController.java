package com.mycompany.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.dto.APIResponse;
import com.mycompany.dto.request.LoginRequestDTO;
import com.mycompany.dto.request.RegisterRequestDTO;
import com.mycompany.enums.EnumSuccess;
import com.mycompany.service.AuthService;
import com.mycompany.util.RequestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthPublicController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/auth/refresh";

    private final AuthService authService;

    @Value("${token.refresh-token-expiration:604800}")
    private long refreshTokenExpirationSeconds;

    @PostMapping("/login")
    public APIResponse<Object> login(@Valid @RequestBody LoginRequestDTO dto,
            HttpServletRequest request, HttpServletResponse response) {
        String clientIp = RequestUtils.resolveClientIp(request);
        Map<String, String> data = authService.login(dto, clientIp);
        RequestUtils.addHttpOnlyCookie(response, REFRESH_TOKEN_COOKIE,
                data.get(REFRESH_TOKEN_COOKIE), (int) refreshTokenExpirationSeconds, REFRESH_TOKEN_PATH);
        return APIResponse.success(EnumSuccess.LOGIN_SUCCESS.getCode(),
                EnumSuccess.LOGIN_SUCCESS.getMessage(), Map.of("token", data.get("token")));
    }

    @PostMapping("/register")
    public APIResponse<Object> register(@Valid @RequestBody RegisterRequestDTO dto) {
        Map<String, String> data = authService.register(dto);
        return APIResponse.success(EnumSuccess.REGISTRATION_SUCCESS.getCode(),
                EnumSuccess.REGISTRATION_SUCCESS.getMessage(), data);
    }
}
