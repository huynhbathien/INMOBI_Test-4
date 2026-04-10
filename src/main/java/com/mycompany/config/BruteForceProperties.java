package com.mycompany.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security.brute-force")
public class BruteForceProperties {

    /** Number of failed attempts before the IP is blocked (default: 5). */
    private int maxAttempts = 5;

    /** How long a blocked IP stays locked, in minutes (default: 15). */
    private long blockDurationMinutes = 15;
}
