package com.vielo.smartbet.user;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {
    private static final DateTimeFormatter PREMIUM_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserRepository repo;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;

    public UserService(UserRepository repo, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Transactional
    public AppUser register(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (repo.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered.");
        }
        AppUser u = AppUser.builder()
                .email(normalizedEmail)
                .passwordHash(encoder.encode(rawPassword))
                .enabled(true)
                .build();
        u.getRoles().add(Role.USER);
        return repo.save(u);
    }

    public Optional<AppUser> findByEmail(String email) {
        return repo.findByEmail(normalizeEmail(email));
    }

    public boolean hasPremiumActive(AppUser user) {
        return user != null && user.isPremiumActive(LocalDateTime.now());
    }

    public String formatPremiumUntil(AppUser user) {
        if (user == null || user.getPremiumUntil() == null) {
            return null;
        }
        return user.getPremiumUntil().format(PREMIUM_DATE_FORMAT);
    }

    public boolean isAnonymous(Authentication auth) {
        return auth == null || auth.getAuthorities().stream().anyMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()));
    }

    @Transactional
    public void grantPremium(AppUser user, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = user.getPremiumUntil() != null && user.getPremiumUntil().isAfter(now) ? user.getPremiumUntil() : now;
        user.setPremiumUntil(base.plusDays(days));
        user.getRoles().add(Role.PREMIUM);
        repo.save(user);
    }

    @Transactional
    public void ensurePremiumRoleConsistency(AppUser user) {
        LocalDateTime now = LocalDateTime.now();
        boolean active = user.isPremiumActive(now);
        if (!active && user.getRoles().contains(Role.PREMIUM)) {
            user.getRoles().remove(Role.PREMIUM);
            repo.save(user);
        }
        if (active && !user.getRoles().contains(Role.PREMIUM)) {
            user.getRoles().add(Role.PREMIUM);
            repo.save(user);
        }
    }

    public String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
