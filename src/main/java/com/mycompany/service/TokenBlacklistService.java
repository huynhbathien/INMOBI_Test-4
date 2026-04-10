package com.mycompany.service;

public interface TokenBlacklistService {

    void addToBlacklist(String token, long expiryTime);

    boolean isTokenBlacklisted(String token);

    void removeFromBlacklist(String token);
}