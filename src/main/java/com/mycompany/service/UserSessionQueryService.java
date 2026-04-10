package com.mycompany.service;

public interface UserSessionQueryService {

    Long getUserId(String username);

    String getUserSessionInfo(String username);
}