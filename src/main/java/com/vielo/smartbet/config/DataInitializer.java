package com.vielo.smartbet.config;

import com.vielo.smartbet.prediction.PredictionService;
import com.vielo.smartbet.user.AppUser;
import com.vielo.smartbet.user.Role;
import com.vielo.smartbet.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository repo,
                                PasswordEncoder encoder,
                                PredictionService predictionService,
                                @Value("${vielo.demo.enabled:true}") boolean demoEnabled) {

        return args -> {

            if (repo.findByEmail("admin@vielosmartbet.com").isEmpty()) {
                AppUser admin = AppUser.builder()
                        .email("admin@vielosmartbet.com")
                        .passwordHash(encoder.encode("Admin@12345"))
                        .enabled(true)
                        .roles(Set.of(Role.ADMIN, Role.USER))
                        .build();
                repo.save(admin);
            }

            if (repo.findByEmail("user@vielosmartbet.com").isEmpty()) {
                AppUser user = AppUser.builder()
                        .email("user@vielosmartbet.com")
                        .passwordHash(encoder.encode("User@12345"))
                        .enabled(true)
                        .roles(Set.of(Role.USER))
                        .build();
                repo.save(user);
            }

            if (demoEnabled) {
                predictionService.createDemoPredictionsIfEmpty();
                System.out.println("NEW DEMO MATCHES LOADED");
            }
        };
    }
                    }
