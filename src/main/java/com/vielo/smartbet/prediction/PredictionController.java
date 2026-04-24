package com.vielo.smartbet.prediction;

import com.vielo.smartbet.user.AppUser;
import com.vielo.smartbet.user.UserRepository;
import com.vielo.smartbet.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PredictionController {

    private final PredictionRepository repo;
    private final UserRepository userRepo;
    private final UserService userService;

    @Value("${vielo.free-tips-per-day:3}")
    private int freeTipsPerDay;

    public PredictionController(PredictionRepository repo, UserRepository userRepo, UserService userService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Authentication auth, Model model) {
        fillPredictionPage(auth, model);
        return "home";
    }

    @GetMapping("/predictions")
    public String predictions(Authentication auth, Model model) {
        fillPredictionPage(auth, model);
        return "predictions";
    }

    @GetMapping("/pricing")
    public String pricing(Authentication auth, Model model) {
        AppUser user = auth != null ? userRepo.findByEmail(auth.getName()).orElse(null) : null;
        if (user != null) {
            userService.ensurePremiumRoleConsistency(user);
        }

        boolean premiumActive = userService.hasPremiumActive(user);
        model.addAttribute("premium", premiumActive);
        model.addAttribute("premiumUntil", userService.formatPremiumUntil(user));
        model.addAttribute("loggedIn", auth != null && auth.isAuthenticated() && !userService.isAnonymous(auth));
        return "pricing";
    }

    private void fillPredictionPage(Authentication auth, Model model) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Prediction> preds = repo.findByForDateInOrderByForDateAscScoreDesc(List.of(today, tomorrow));
        if (preds == null || preds.isEmpty()) {
            preds = fallbackPredictions();
        }
        preds = normalizePredictionTiers(preds);

        boolean premium = false;
        if (auth != null) {
            AppUser u = userRepo.findByEmail(auth.getName()).orElse(null);
            if (u != null) {
                userService.ensurePremiumRoleConsistency(u);
                premium = userService.hasPremiumActive(u);
            }
        }

        List<PredictionDaySection> sections = buildSections(preds);
        List<Prediction> preview = buildHomePreview(preds);

        long freeCount = preds.stream().filter(Prediction::isFree).count();
        long premiumCount = preds.stream().filter(Prediction::isPremium).count();

        model.addAttribute("preds", preds);
        model.addAttribute("predictionSections", sections);
        model.addAttribute("previewPreds", preview);
        model.addAttribute("premium", premium);
        model.addAttribute("today", today);
        model.addAttribute("tomorrow", tomorrow);
        model.addAttribute("freeCount", freeCount);
        model.addAttribute("premiumCount", premiumCount);
    }

    private List<Prediction> fallbackPredictions() {
        List<Prediction> all = repo.findAllByOrderByForDateAscScoreDesc();
        if (all == null || all.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<Prediction>> byDate = new LinkedHashMap<>();
        for (Prediction prediction : all) {
            if (prediction.getForDate() == null) continue;
            byDate.computeIfAbsent(prediction.getForDate(), ignored -> new ArrayList<>()).add(prediction);
            if (byDate.size() >= 2) break;
        }

        return byDate.values().stream().flatMap(List::stream).toList();
    }

    private List<Prediction> normalizePredictionTiers(List<Prediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, Integer> counters = new LinkedHashMap<>();
        boolean changed = false;
        for (Prediction prediction : predictions) {
            if (prediction == null || prediction.getForDate() == null) {
                continue;
            }
            int rank = counters.merge(prediction.getForDate(), 1, Integer::sum);
            if (prediction.getTier() == null) {
                prediction.setTier(rank <= freeTipsPerDay ? PredictionTier.FREE : PredictionTier.PREMIUM);
                changed = true;
            }
        }

        if (changed) {
            repo.saveAll(predictions.stream().filter(p -> p != null && p.getId() != null).toList());
        }
        return predictions;
    }

    private List<Prediction> buildHomePreview(List<Prediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return List.of();
        }

        Comparator<Prediction> byDateAndScore = Comparator
                .comparing(Prediction::getForDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Prediction::getScore, Comparator.nullsLast(Comparator.reverseOrder()));

        List<Prediction> free = predictions.stream()
                .filter(Prediction::isFree)
                .sorted(byDateAndScore)
                .limit(2)
                .toList();

        List<Prediction> premium = predictions.stream()
                .filter(Prediction::isPremium)
                .sorted(byDateAndScore)
                .limit(4)
                .toList();

        List<Prediction> preview = new ArrayList<>();
        preview.addAll(free);
        preview.addAll(premium);
        preview.sort(byDateAndScore);
        return preview;
    }

    private List<PredictionDaySection> buildSections(List<Prediction> predictions) {
        if (predictions == null || predictions.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<Prediction>> grouped = predictions.stream()
                .filter(prediction -> prediction.getForDate() != null)
                .collect(LinkedHashMap::new,
                        (map, prediction) -> map.computeIfAbsent(prediction.getForDate(), ignored -> new ArrayList<>()).add(prediction),
                        LinkedHashMap::putAll);

        return grouped.entrySet().stream()
                .map(entry -> new PredictionDaySection(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record PredictionDaySection(LocalDate date, List<Prediction> predictions) {}
}
