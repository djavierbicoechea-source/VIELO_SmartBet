package com.vielo.smartbet.recommendation;

import com.vielo.smartbet.prediction.Prediction;
import com.vielo.smartbet.prediction.PredictionRepository;
import com.vielo.smartbet.prediction.PredictionTier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final PredictionRepository predictionRepository;

    @Value("${vielo.free-tips-per-day:3}")
    private int freeTipsPerDay;

    public RecommendationService(PredictionRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    public List<TicketRecommendation> generateVisible(GeneratorRequest req, boolean premiumUser) {
        List<Prediction> pool = loadNormalizedPool();
        if (!premiumUser) {
            pool = pool.stream().filter(p -> p.getTierOrDefault() == PredictionTier.FREE).toList();
        }
        return buildRecommendations(req, pool, premiumUser);
    }

    public List<TicketRecommendation> generateLockedPremiumPreview(GeneratorRequest req) {
        List<Prediction> pool = loadNormalizedPool().stream()
                .filter(p -> p.getTierOrDefault() == PredictionTier.PREMIUM)
                .toList();
        return buildRecommendations(req, pool, true).stream().limit(2).toList();
    }

    private List<Prediction> loadNormalizedPool() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        List<Prediction> pool = predictionRepository.findByForDateInOrderByForDateAscScoreDesc(List.of(today, tomorrow));
        if (pool == null || pool.isEmpty()) {
            pool = predictionRepository.findAllByOrderByForDateAscScoreDesc().stream().limit(20).toList();
        }
        return normalizeMissingTiers(pool);
    }

    private List<Prediction> normalizeMissingTiers(List<Prediction> pool) {
        if (pool == null || pool.isEmpty()) {
            return List.of();
        }
        Map<LocalDate, Integer> byDateCounter = new LinkedHashMap<>();
        for (Prediction prediction : pool) {
            if (prediction == null || prediction.getForDate() == null) {
                continue;
            }
            int rank = byDateCounter.merge(prediction.getForDate(), 1, Integer::sum);
            if (prediction.getTier() == null) {
                prediction.setTier(rank <= freeTipsPerDay ? PredictionTier.FREE : PredictionTier.PREMIUM);
            }
        }
        return pool;
    }

    private List<TicketRecommendation> buildRecommendations(GeneratorRequest req, List<Prediction> pool, boolean premiumMode) {
        if (pool == null || pool.isEmpty()) {
            return List.of(emptyRecommendation());
        }

        int maxSelections;
        int tickets;
        double maxStakePct;
        switch (req.getRisk()) {
            case LOW -> { maxSelections = 2; tickets = premiumMode ? 2 : 1; maxStakePct = 0.40; }
            case MEDIUM -> { maxSelections = 3; tickets = premiumMode ? 3 : 1; maxStakePct = 0.30; }
            case HIGH -> { maxSelections = 5; tickets = premiumMode ? 4 : 1; maxStakePct = 0.20; }
            default -> { maxSelections = 3; tickets = premiumMode ? 3 : 1; maxStakePct = 0.30; }
        }

        List<Prediction> top = pool.stream()
                .collect(Collectors.toMap(Prediction::getFixtureId, p -> p, (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparing(Prediction::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(30)
                .toList();

        List<TicketRecommendation> recs = new ArrayList<>();
        double remainingBudget = req.getBudget();

        for (int t = 0; t < tickets && remainingBudget > 0; t++) {
            List<Prediction> picks = pickForTicket(top, maxSelections, t);
            if (picks.isEmpty()) break;

            double totalOdd = picks.stream().map(Prediction::getOdd).filter(Objects::nonNull).reduce(1.0, (a, b) -> a * b);
            double neededStake = req.getTargetProfit() / Math.max(0.01, (totalOdd - 1.0));
            double stakeCap = req.getBudget() * maxStakePct;
            double stake = Math.min(Math.min(neededStake, stakeCap), remainingBudget);
            double profit = stake * (totalOdd - 1.0);

            recs.add(TicketRecommendation.builder()
                    .lines(picks.stream().map(p -> new TicketLine(p.getMatchLabel(), p.getMarket(), p.getPick(), p.getOdd())).toList())
                    .totalOdd(round2(totalOdd))
                    .stake(round2(stake))
                    .potentialProfit(round2(profit))
                    .note(buildNote(req, neededStake, stake))
                    .build());

            remainingBudget -= stake;
        }

        return recs.isEmpty() ? List.of(emptyRecommendation()) : recs;
    }

    private TicketRecommendation emptyRecommendation() {
        return TicketRecommendation.builder()
                .lines(List.of())
                .totalOdd(0.0)
                .stake(0.0)
                .potentialProfit(0.0)
                .note("Aucune recommandation disponible. Active l'API-FOOTBALL ou utilise les pronostics déjà chargés.")
                .build();
    }

    private List<Prediction> pickForTicket(List<Prediction> top, int maxSelections, int seed) {
        if (top.isEmpty()) return List.of();
        List<Prediction> rotated = new ArrayList<>(top);
        Collections.rotate(rotated, -seed * 2);
        return rotated.stream().limit(Math.min(maxSelections, rotated.size())).toList();
    }

    private String buildNote(GeneratorRequest req, double neededStake, double actualStake) {
        if (neededStake <= actualStake + 0.01) return "Objectif atteignable sur ce ticket selon les cotes actuelles.";
        return "Objectif élevé: mise requise ≈ " + round2(neededStake) + ". Ticket limité par la stratégie " + req.getRisk() + ".";
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
