package com.mycompany.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretFile;
    private long expiration;
    private long refreshExpiration;

    public JwtProperties() {
    }

    public JwtProperties(String secretFile, long expiration, long refreshExpiration) {
        this.secretFile = secretFile;
        this.expiration = expiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String getSecretFile() {
        return secretFile;
    }

    public void setSecretFile(String secretFile) {
        this.secretFile = secretFile;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
