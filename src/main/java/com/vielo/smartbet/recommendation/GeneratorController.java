package com.vielo.smartbet.recommendation;

import com.vielo.smartbet.user.AppUser;
import com.vielo.smartbet.user.UserRepository;
import com.vielo.smartbet.user.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class GeneratorController {

    private final RecommendationService recommendationService;
    private final UserRepository userRepo;
    private final UserService userService;

    public GeneratorController(RecommendationService recommendationService, UserRepository userRepo, UserService userService) {
        this.recommendationService = recommendationService;
        this.userRepo = userRepo;
        this.userService = userService;
    }

    @GetMapping("/generator")
    public String generatorForm(Authentication auth, Model model) {
        AppUser u = userRepo.findByEmail(auth.getName()).orElse(null);
        if (u != null) userService.ensurePremiumRoleConsistency(u);
        boolean premium = u != null && u.isPremiumActive(LocalDateTime.now());

        model.addAttribute("premium", premium);
        model.addAttribute("form", new GeneratorRequest());
        return "generator";
    }

    @PostMapping("/generator")
    public String generatorSubmit(Authentication auth, @Valid @ModelAttribute("form") GeneratorRequest form,
                                  BindingResult br, Model model) {
        AppUser u = userRepo.findByEmail(auth.getName()).orElse(null);
        if (u != null) userService.ensurePremiumRoleConsistency(u);
        boolean premium = u != null && u.isPremiumActive(LocalDateTime.now());

        if (br.hasErrors()) {
            model.addAttribute("premium", premium);
            return "generator";
        }

        List<TicketRecommendation> visible = recommendationService.generateVisible(form, premium);
        List<TicketRecommendation> lockedPremium = premium ? List.of() : recommendationService.generateLockedPremiumPreview(form);

        model.addAttribute("premium", premium);
        model.addAttribute("recs", visible);
        model.addAttribute("lockedRecs", lockedPremium);
        return "generator";
    }
}
