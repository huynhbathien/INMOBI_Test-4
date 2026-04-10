package com.mycompany.service;

public interface AccessTokenStore {

    void saveAccessToken(String username, Long userId, String accessToken);

    void deleteAccessToken(String username);
}