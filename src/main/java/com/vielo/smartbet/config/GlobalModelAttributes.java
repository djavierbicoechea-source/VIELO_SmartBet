package com.vielo.smartbet.config;

import com.vielo.smartbet.user.AppUser;
import com.vielo.smartbet.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("loggedIn")
    public boolean loggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !userService.isAnonymous(auth);
    }

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || userService.isAnonymous(auth)) {
            return null;
        }
        return auth.getName();
    }

    @ModelAttribute("currentUserPremium")
    public boolean currentUserPremium(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || userService.isAnonymous(auth)) {
            return false;
        }
        AppUser user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return false;
        }
        userService.ensurePremiumRoleConsistency(user);
        return userService.hasPremiumActive(user);
    }

    @ModelAttribute("currentUserPremiumUntil")
    public String currentUserPremiumUntil(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || userService.isAnonymous(auth)) {
            return null;
        }
        AppUser user = userService.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return null;
        }
        userService.ensurePremiumRoleConsistency(user);
        return userService.formatPremiumUntil(user);
    }
}
