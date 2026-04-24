package com.vielo.smartbet.prediction;

import com.vielo.smartbet.football.FixtureEntity;
import com.vielo.smartbet.football.FixtureRepository;
import com.vielo.smartbet.football.OddEntity;
import com.vielo.smartbet.football.OddRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PredictionService {

    private static final Logger log =
            LoggerFactory.getLogger(PredictionService.class);

    private final FixtureRepository fixtureRepo;
    private final OddRepository oddRepo;
    private final PredictionRepository predRepo;

    @Value("${vielo.free-tips-per-day:5}")
    private int freeTipsPerDay;

    @Value("${vielo.max-predictions-per-day:10}")
    private int maxPredictionsPerDay;

    public PredictionService(FixtureRepository fixtureRepo,
                             OddRepository oddRepo,
                             PredictionRepository predRepo) {
        this.fixtureRepo = fixtureRepo;
        this.oddRepo = oddRepo;
        this.predRepo = predRepo;
    }

    public void generateForDate(LocalDate date) {

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<FixtureEntity> fixtures =
                fixtureRepo.findByKickoffBetweenOrderByKickoffAsc(start, end)
                        .stream()
                        .filter(fx -> fx.getKickoff() != null)
                        .filter(fx -> !fx.getKickoff().isBefore(now.minusHours(2)))
                        .limit(Math.max(maxPredictionsPerDay * 3L, 24L))
                        .toList();

        // Si aucun fixture => garder anciennes prédictions
        if (fixtures.isEmpty()) {
            log.warn("Aucun fixture exploitable pour {}", date);
            return;
        }

        // Supprime seulement quand nouvelles données trouvées
        predRepo.deleteByForDate(date);

        List<Prediction> all = new ArrayList<>();

        for (FixtureEntity fixture : fixtures) {

            List<OddEntity> odds = oddRepo.findByFixtureId(fixture.getId());

            if (odds == null || odds.isEmpty()) {
                continue;
            }

            Map<String, List<OddEntity>> grouped =
                    odds.stream()
                            .filter(o -> o.getMarket() != null)
                            .filter(o -> o.getOutcome() != null)
                            .filter(o -> o.getOdd() != null)
                            .filter(o -> o.getOdd() >= 1.18 && o.getOdd() <= 6.50)
                            .filter(o -> isSupportedMarket(o.getMarket()))
                            .collect(Collectors.groupingBy(
                                    o -> normalizeMarket(o.getMarket())
                                            + "::"
                                            + normalizeOutcome(o.getOutcome())
                            ));

            List<Prediction> candidatesForFixture = new ArrayList<>();

            for (Map.Entry<String, List<OddEntity>> entry : grouped.entrySet()) {

                List<OddEntity> group = entry.getValue();

                if (group.isEmpty()) continue;

                OddEntity representative = bestPriced(group);

                double avgOdd = group.stream()
                        .mapToDouble(OddEntity::getOdd)
                        .average()
                        .orElse(representative.getOdd());

                double maxOdd = representative.getOdd();

                double implied = 1.0 / avgOdd;

                double score = Math.max(0.01,
                        Math.min(0.99,
                                (implied * 0.70)
                                        + (marketWeight(representative.getMarket()) * 0.30)
                        ));

                Prediction current = Prediction.builder()
                        .forDate(date)
                        .fixtureId(fixture.getId())
                        .league(fixture.getLeagueName())
                        .matchLabel(
                                fixture.getHomeTeam()
                                        + " vs "
                                        + fixture.getAwayTeam()
                        )
                        .market(prettyMarket(representative.getMarket()))
                        .pick(prettyOutcome(representative.getOutcome()))
                        .odd(round2(maxOdd))
                        .impliedProb(round4(implied))
                        .score(round4(score))
                        .tier(PredictionTier.PREMIUM)
                        .build();

                candidatesForFixture.add(current);
            }

            candidatesForFixture.stream()
                    .sorted(Comparator.comparing(Prediction::getScore).reversed())
                    .filter(candidate -> isDiverseCandidate(candidate, all))
                    .limit(2)
                    .forEach(all::add);
        }

        List<Prediction> sorted =
                all.stream()
                        .sorted(Comparator.comparing(Prediction::getScore).reversed())
                        .limit(maxPredictionsPerDay)
                        .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setTier(
                    i < freeTipsPerDay
                            ? PredictionTier.FREE
                            : PredictionTier.PREMIUM
            );
        }

        predRepo.saveAll(sorted);

        log.info("Pronostics enregistrés pour {} : {}", date, sorted.size());
    }

    // Compatibilité DataInitializer
    public void createDemoPredictionsIfEmpty() {

        LocalDate today = LocalDate.now();

        if (!predRepo.findByForDateOrderByScoreDesc(today).isEmpty()) {
            return;
        }

        List<Prediction> demo = List.of(

                Prediction.builder()
                        .forDate(today)
                        .fixtureId(1001L)
                        .league("Demo League")
                        .matchLabel("Arsenal vs Chelsea")
                        .market("Match Winner")
                        .pick("Arsenal")
                        .odd(1.72)
                        .impliedProb(0.58)
                        .score(0.76)
                        .tier(PredictionTier.FREE)
                        .build(),

                Prediction.builder()
                        .forDate(today)
                        .fixtureId(1002L)
                        .league("Demo League")
                        .matchLabel("Real Madrid vs Sevilla")
                        .market("Goals Over/Under")
                        .pick("Over 2.5")
                        .odd(1.80)
                        .impliedProb(0.55)
                        .score(0.74)
                        .tier(PredictionTier.FREE)
                        .build(),

                Prediction.builder()
                        .forDate(today)
                        .fixtureId(1003L)
                        .league("Demo League")
                        .matchLabel("PSG vs Marseille")
                        .market("Both Teams To Score")
                        .pick("Yes")
                        .odd(1.67)
                        .impliedProb(0.60)
                        .score(0.73)
                        .tier(PredictionTier.FREE)
                        .build()
        );

        predRepo.saveAll(demo);
    }

    private OddEntity bestPriced(List<OddEntity> odds) {
        return odds.stream()
                .max(Comparator.comparing(OddEntity::getOdd))
                .orElse(odds.get(0));
    }

    private double marketWeight(String market) {
        String m = normalizeMarket(market);

        if (m.contains("match winner")) return 0.82;
        if (m.contains("both teams")) return 0.74;
        if (m.contains("goals over/under")) return 0.72;

        return 0.60;
    }

    private String prettyMarket(String market) {
        String m = normalizeMarket(market);

        if (m.contains("both teams")) return "Both Teams To Score";
        if (m.contains("goals over/under")) return "Goals Over/Under";
        if (m.contains("match winner")) return "Match Winner";

        return market;
    }

    private String prettyOutcome(String outcome) {
        if (outcome == null) return "";

        String o = outcome.trim();

        if (o.equalsIgnoreCase("home")) return "Home";
        if (o.equalsIgnoreCase("away")) return "Away";

        return o;
    }

    private boolean isDiverseCandidate(Prediction candidate,
                                       List<Prediction> existing) {

        return existing.stream().noneMatch(p ->
                Objects.equals(p.getFixtureId(), candidate.getFixtureId())
                        && normalizeMarket(p.getMarket())
                        .equals(normalizeMarket(candidate.getMarket()))
                        && normalizeOutcome(p.getPick())
                        .equals(normalizeOutcome(candidate.getPick()))
        );
    }

    private boolean isSupportedMarket(String market) {

        if (market == null) return false;

        String m = normalizeMarket(market);

        return m.contains("match winner")
                || m.contains("goals over/under")
                || m.contains("both teams");
    }

    private String normalizeMarket(String market) {
        return market == null ? "" : market.trim().toLowerCase();
    }

    private String normalizeOutcome(String outcome) {
        return outcome == null ? "" : outcome.trim().toLowerCase();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
