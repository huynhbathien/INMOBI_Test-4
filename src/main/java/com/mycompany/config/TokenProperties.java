package com.mycompany.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "token")
public class TokenProperties {

    /** Access token lifetime in seconds (default: 30 minutes). */
    private long accessTokenExpiration = 1800;

    /** Refresh token lifetime in seconds (default: 7 days). */
    private long refreshTokenExpiration = 604800;
}
