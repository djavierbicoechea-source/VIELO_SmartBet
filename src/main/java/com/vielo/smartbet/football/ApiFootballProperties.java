package com.vielo.smartbet.football;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vielo.apifootball")
public record ApiFootballProperties(
        boolean enabled,
        String baseUrl,
        String key,
        String leagues
) {}
