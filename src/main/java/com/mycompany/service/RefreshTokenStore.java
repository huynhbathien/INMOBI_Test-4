package com.mycompany.service;

public interface RefreshTokenStore {

    void saveRefreshToken(String username, Long userId, String refreshToken);

    String getRefreshToken(String username);

    void deleteRefreshToken(String username);
}