package com.vielo.smartbet.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !userService.isAnonymous(auth)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated() && !userService.isAnonymous(auth)) {
            return "redirect:/dashboard";
        }
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("form") RegisterForm form, BindingResult br, Model model) {
        if (br.hasErrors()) return "register";
        try {
            userService.register(form.getEmail(), form.getPassword());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        AppUser user = auth != null ? userService.findByEmail(auth.getName()).orElse(null) : null;
        boolean premium = false;
        String premiumUntil = null;

        if (user != null) {
            userService.ensurePremiumRoleConsistency(user);
            premium = userService.hasPremiumActive(user);
            premiumUntil = userService.formatPremiumUntil(user);
        }

        model.addAttribute("email", auth != null ? auth.getName() : null);
        model.addAttribute("premium", premium);
        model.addAttribute("premiumUntil", premiumUntil);
        return "dashboard";
    }

    public static class RegisterForm {
        @Email @NotBlank
        private String email;
        @NotBlank
        private String password;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
