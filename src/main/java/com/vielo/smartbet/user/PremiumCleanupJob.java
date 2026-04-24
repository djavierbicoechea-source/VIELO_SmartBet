package com.vielo.smartbet.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PremiumCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PremiumCleanupJob.class);
    private final UserRepository repo;
    private final UserService userService;

    public PremiumCleanupJob(UserRepository repo, UserService userService) {
        this.repo = repo;
        this.userService = userService;
    }

    @Scheduled(cron = "0 5 3 * * *")
    public void cleanup() {
        for (AppUser u : repo.findAll()) {
            userService.ensurePremiumRoleConsistency(u);
        }
        log.info("Premium cleanup done.");
    }
}
