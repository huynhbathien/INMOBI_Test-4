package com.mycompany.service;

import java.util.Map;

import com.mycompany.dto.request.LoginRequestDTO;

public interface AuthSessionService {

    Map<String, String> login(LoginRequestDTO dto, String clientIp);

    Map<String, String> issueTokenPair(String username);

    Map<String, String> refreshToken(String clientRefreshToken);

    void logout(String username);
}