package com.vielo.smartbet.football;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ApiFootballProperties.class, OddsApiProperties.class})
public class FootballConfig {}
