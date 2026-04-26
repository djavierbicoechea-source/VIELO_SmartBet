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

    @Value("${vielo.free-tips-per-day:3}")
    private int freeTipsPerDay;

    @Value("${vielo.max-predictions-per-day:8}")
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

        if (fixtures.isEmpty()) {
            log.warn("Aucun fixture exploitable pour {}", date);
            return;
        }

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
                        .matchLabel(fixture.getHomeTeam() + " vs " + fixture.getAwayTeam())
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

    public void createDemoPredictionsIfEmpty() {

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        predRepo.deleteByForDate(today);
        predRepo.deleteByForDate(tomorrow);

        List<Prediction> demo = new ArrayList<>();

        // TODAY
        demo.add(Prediction.builder()
                .forDate(today)
                .fixtureId(5001L)
                .league("La Liga")
                .matchLabel("Espanyol vs Levante")
                .market("Match Winner")
                .pick("Espanyol")
                .odd(1.70)
                .impliedProb(0.58)
                .score(0.84)
                .tier(PredictionTier.FREE)
                .build());

        demo.add(Prediction.builder()
                .forDate(today)
                .fixtureId(5002L)
                .league("Premier League")
                .matchLabel("Man Utd vs Brentford")
                .market("Goals Over/Under")
                .pick("Over 2.5")
                .odd(1.75)
                .impliedProb(0.57)
                .score(0.82)
                .tier(PredictionTier.FREE)
                .build());

        demo.add(Prediction.builder()
                .forDate(today)
                .fixtureId(5003L)
                .league("Serie A")
                .matchLabel("Cagliari vs Atalanta")
                .market("Match Winner")
                .pick("Atalanta")
                .odd(1.68)
                .impliedProb(0.59)
                .score(0.80)
                .tier(PredictionTier.FREE)
                .build());

        demo.add(Prediction.builder()
                .forDate(today)
                .fixtureId(5004L)
                .league("Serie A")
                .matchLabel("Lazio vs Udinese")
                .market("Both Teams To Score")
                .pick("Yes")
                .odd(1.88)
                .impliedProb(0.53)
                .score(0.79)
                .tier(PredictionTier.PREMIUM)
                .build());

        demo.add(Prediction.builder()
                .forDate(today)
                .fixtureId(5005L)
                .league("Portugal")
                .matchLabel("Gil Vicente vs Casa Pia")
                .market("Double Chance")
                .pick("1X")
                .odd(1.55)
                .impliedProb(0.64)
                .score(0.78)
                .tier(PredictionTier.PREMIUM)
                .build());

        // TOMORROW
        demo.add(Prediction.builder()
                .forDate(tomorrow)
                .fixtureId(6001L)
                .league("Champions League")
                .matchLabel("PSG vs Bayern")
                .market("Both Teams To Score")
                .pick("Yes")
                .odd(1.72)
                .impliedProb(0.58)
                .score(0.85)
                .tier(PredictionTier.FREE)
                .build());

        demo.add(Prediction.builder()
                .forDate(tomorrow)
                .fixtureId(6002L)
                .league("Championship")
                .matchLabel("Southampton vs Ipswich")
                .market("Match Winner")
                .pick("Southampton")
                .odd(1.69)
                .impliedProb(0.59)
                .score(0.82)
                .tier(PredictionTier.FREE)
                .build());

        demo.add(Prediction.builder()
                .forDate(tomorrow)
                .fixtureId(6003L)
                .league("Libertadores")
                .matchLabel("Cruzeiro vs Boca Juniors")
                .market("Under 3.5")
                .pick("Yes")
                .odd(1.50)
                .impliedProb(0.66)
                .score(0.80)
                .tier(PredictionTier.PREMIUM)
                .build());

        demo.add(Prediction.builder()
                .forDate(tomorrow)
                .fixtureId(6004L)
                .league("Libertadores")
                .matchLabel("Sporting Cristal vs Junior Barranquilla")
                .market("Goals Over/Under")
                .pick("Over 2.5")
                .odd(1.81)
                .impliedProb(0.55)
                .score(0.78)
                .tier(PredictionTier.PREMIUM)
                .build());

        predRepo.saveAll(demo);

        log.info("Demo predictions créées.");
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
