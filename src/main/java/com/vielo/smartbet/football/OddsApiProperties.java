package com.vielo.smartbet.football;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vielo.oddsapi")
public record OddsApiProperties(
        boolean enabled,
        String baseUrl,
        String key,
        String regions,
        String markets,
        int maxKickoffDiffMinutes
) {}
