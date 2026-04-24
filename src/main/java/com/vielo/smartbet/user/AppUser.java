package com.vielo.smartbet.user;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<Role> roles = new HashSet<>();

    private LocalDateTime premiumUntil;

    public AppUser() {}

    public AppUser(Long id, String email, String passwordHash, boolean enabled, Set<Role> roles, LocalDateTime premiumUntil) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.roles = roles != null ? roles : new HashSet<>();
        this.premiumUntil = premiumUntil;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isPremiumActive(LocalDateTime now) {
        return premiumUntil != null && premiumUntil.isAfter(now);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Set<Role> getRoles() {
        if (roles == null) roles = new HashSet<>();
        return roles;
    }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public LocalDateTime getPremiumUntil() { return premiumUntil; }
    public void setPremiumUntil(LocalDateTime premiumUntil) { this.premiumUntil = premiumUntil; }

    public static class Builder {
        private Long id;
        private String email;
        private String passwordHash;
        private boolean enabled = true;
        private Set<Role> roles = new HashSet<>();
        private LocalDateTime premiumUntil;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder roles(Set<Role> roles) { this.roles = roles; return this; }
        public Builder premiumUntil(LocalDateTime premiumUntil) { this.premiumUntil = premiumUntil; return this; }

        public AppUser build() {
            return new AppUser(id, email, passwordHash, enabled, roles, premiumUntil);
        }
    }
}
