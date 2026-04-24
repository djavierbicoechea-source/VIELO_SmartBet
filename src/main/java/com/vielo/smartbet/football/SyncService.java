package com.vielo.smartbet.football;

import com.vielo.smartbet.prediction.PredictionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final Set<String> UPCOMING_STATUSES = Set.of("NS", "TBD", "PST");

    private final ApiFootballProperties apiFootballProperties;
    private final OddsApiProperties oddsApiProperties;
    private final ApiFootballClient apiFootballClient;
    private final OddsApiClient oddsApiClient;
    private final FixtureRepository fixtureRepository;
    private final OddRepository oddRepository;
    private final PredictionService predictionService;

    @Value("${vielo.max-predictions-per-day:8}")
    private int maxPredictionsPerDay;

    public SyncService(ApiFootballProperties apiFootballProperties,
                       OddsApiProperties oddsApiProperties,
                       ApiFootballClient apiFootballClient,
                       OddsApiClient oddsApiClient,
                       FixtureRepository fixtureRepository,
                       OddRepository oddRepository,
                       PredictionService predictionService) {
        this.apiFootballProperties = apiFootballProperties;
        this.oddsApiProperties = oddsApiProperties;
        this.apiFootballClient = apiFootballClient;
        this.oddsApiClient = oddsApiClient;
        this.fixtureRepository = fixtureRepository;
        this.oddRepository = oddRepository;
        this.predictionService = predictionService;
    }

    @PostConstruct
    public void init() {
        if (!isReady()) {
            log.info("Sync initial ignorée: configuration API incomplète.");
            return;
        }
        runFullSync("init");
    }

    @Scheduled(cron = "0 0 8,14,20 * * *")
    public void scheduledSync() {
        runFullSync("scheduled");
    }

    public void runFullSync(String reason) {
        try {
            log.info("Démarrage sync {}", reason);

            syncFixtures();
            syncOdds();

            // génération protégée
            try {
                predictionService.generateForDate(LocalDate.now());
            } catch (Exception e) {
                log.error("Erreur génération prédictions aujourd'hui", e);
            }

            try {
                predictionService.generateForDate(LocalDate.now().plusDays(1));
            } catch (Exception e) {
                log.error("Erreur génération prédictions demain", e);
            }

        } catch (Exception e) {
            log.error("Erreur pendant la sync complète", e);
        }
    }

    public void syncFixtures() {
        if (!isApiFootballReady()) {
            log.info("API-Football non prête.");
            return;
        }

        LocalDate today = LocalDate.now();

        List<ApiFootballFixturesResponse.FixtureItem> allItems = new ArrayList<>();
        allItems.addAll(fetchFixtureItemsForDate(today));
        allItems.addAll(fetchFixtureItemsForDate(today.plusDays(1)));

        LocalDateTime now = LocalDateTime.now();

        List<FixtureEntity> selected = allItems.stream()
                .filter(Objects::nonNull)
                .filter(this::isMajorLeagueFixture)
                .map(this::toFixtureEntity)
                .filter(Objects::nonNull)
                .filter(f -> f.getKickoff() != null)
                .filter(f -> f.getKickoff().isAfter(now.plusMinutes(10)))
                .sorted(Comparator
                        .comparingInt((FixtureEntity f) ->
                                MajorLeague.byLeagueId(f.getLeagueId())
                                        .map(MajorLeague::getPriority)
                                        .orElse(0))
                        .reversed()
                        .thenComparing(FixtureEntity::getKickoff))
                .limit(Math.max(maxPredictionsPerDay * 2L, 20L))
                .toList();

        if (!selected.isEmpty()) {
            fixtureRepository.deleteAll();
            fixtureRepository.saveAll(selected);
        }

        log.info("Fixtures sauvegardés: {}", selected.size());
    }

    public void syncOdds() {
        if (!isOddsReady()) {
            log.info("Odds API non prête.");
            return;
        }

        List<FixtureEntity> fixtures = fixtureRepository.findAll().stream()
                .filter(f -> f.getKickoff() != null)
                .filter(f -> f.getKickoff().isAfter(LocalDateTime.now().plusMinutes(10)))
                .sorted(Comparator.comparing(FixtureEntity::getKickoff))
                .toList();

        if (fixtures.isEmpty()) {
            log.info("Aucun fixture à coter.");
            return;
        }

        Map<String, List<FixtureEntity>> fixturesBySportKey =
                fixtures.stream()
                        .map(f -> Map.entry(resolveSportKey(f), f))
                        .filter(e -> e.getKey() != null)
                        .collect(Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                        ));

        oddRepository.deleteAll();

        for (Map.Entry<String, List<FixtureEntity>> entry : fixturesBySportKey.entrySet()) {

            String sportKey = entry.getKey();
            List<FixtureEntity> leagueFixtures = entry.getValue();

            try {
                List<OddsApiResponse> events =
                        oddsApiClient.getUpcomingOdds(sportKey)
                                .block(Duration.ofSeconds(30));

                if (events == null || events.isEmpty()) {
                    continue;
                }

                for (FixtureEntity fixture : leagueFixtures) {
                    Optional<OddsApiResponse> matched =
                            findMatchingEvent(fixture, events);

                    matched.ifPresent(event ->
                            saveOddsForFixture(fixture, event));
                }

            } catch (Exception e) {
                log.warn("Erreur odds pour {}: {}", sportKey, e.getMessage());
            }
        }

        log.info("Odds sauvegardés: {}", oddRepository.count());
    }

    private List<ApiFootballFixturesResponse.FixtureItem> fetchFixtureItemsForDate(LocalDate date) {
        try {
            ApiFootballFixturesResponse response =
                    apiFootballClient.getFixtures(date)
                            .block(Duration.ofSeconds(30));

            if (response == null || response.getResponse() == null) {
                return List.of();
            }

            return response.getResponse().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> item.getFixture() != null)
                    .filter(item -> item.getTeams() != null)
                    .filter(item -> item.getLeague() != null)
                    .filter(item -> item.getTeams().getHome() != null)
                    .filter(item -> item.getTeams().getAway() != null)
                    .filter(item -> item.getFixture().getId() != null)
                    .filter(item -> {
                        String status = item.getFixture().getStatus() != null
                                ? item.getFixture().getStatus().getShort()
                                : null;
                        return status != null &&
                                UPCOMING_STATUSES.contains(status);
                    })
                    .toList();

        } catch (Exception e) {
            log.warn("Erreur récupération fixtures {}: {}", date, e.getMessage());
            return List.of();
        }
    }

    private boolean isMajorLeagueFixture(ApiFootballFixturesResponse.FixtureItem item) {
        Long leagueId =
                item.getLeague() != null ? item.getLeague().getId() : null;

        return leagueId != null &&
                MajorLeague.byLeagueId(leagueId).isPresent();
    }

    private FixtureEntity toFixtureEntity(ApiFootballFixturesResponse.FixtureItem item) {

        LocalDateTime kickoff =
                parseKickoff(item.getFixture().getDate());

        if (kickoff == null) return null;

        return FixtureEntity.builder()
                .id(item.getFixture().getId())
                .leagueId(item.getLeague().getId())
                .leagueName(item.getLeague().getName())
                .leagueCountry(item.getLeague().getCountry())
                .homeTeam(item.getTeams().getHome().getName())
                .awayTeam(item.getTeams().getAway().getName())
                .kickoff(kickoff)
                .statusShort(item.getFixture().getStatus() != null
                        ? item.getFixture().getStatus().getShort()
                        : null)
                .build();
    }

    private Optional<OddsApiResponse> findMatchingEvent(
            FixtureEntity fixture,
            List<OddsApiResponse> events) {

        String home = normalizeTeamName(fixture.getHomeTeam());
        String away = normalizeTeamName(fixture.getAwayTeam());

        long allowedDiff =
                Math.max(15, oddsApiProperties.maxKickoffDiffMinutes());

        return events.stream()
                .filter(e -> normalizeTeamName(e.getHome_team()).equals(home))
                .filter(e -> normalizeTeamName(e.getAway_team()).equals(away))
                .filter(e -> {
                    LocalDateTime commence =
                            parseKickoff(e.getCommence_time());

                    return commence != null &&
                            Math.abs(ChronoUnit.MINUTES.between(
                                    fixture.getKickoff(),
                                    commence)) <= allowedDiff;
                })
                .findFirst();
    }

    private void saveOddsForFixture(FixtureEntity fixture,
                                    OddsApiResponse event) {

        if (event.getBookmakers() == null) return;

        List<OddEntity> odds = new ArrayList<>();

        for (OddsApiResponse.Bookmaker bookmaker : event.getBookmakers()) {

            if (bookmaker == null ||
                    bookmaker.getMarkets() == null) continue;

            for (OddsApiResponse.Market market : bookmaker.getMarkets()) {

                if (market == null ||
                        market.getOutcomes() == null) continue;

                String mappedMarket = mapMarket(market.getKey());

                if (mappedMarket == null) continue;

                for (OddsApiResponse.Outcome outcome : market.getOutcomes()) {

                    if (outcome == null ||
                            outcome.getPrice() == null) continue;

                    String mappedOutcome =
                            mapOutcome(market.getKey(), outcome);

                    if (mappedOutcome == null) continue;

                    odds.add(
                            OddEntity.builder()
                                    .fixtureId(fixture.getId())
                                    .bookmaker(bookmaker.getTitle())
                                    .market(mappedMarket)
                                    .outcome(mappedOutcome)
                                    .odd(outcome.getPrice())
                                    .build()
                    );
                }
            }
        }

        if (!odds.isEmpty()) {
            oddRepository.saveAll(odds);
        }
    }

    private String mapMarket(String key) {
        if (key == null) return null;

        return switch (key) {
            case "h2h" -> "Match Winner";
            case "totals" -> "Goals Over/Under";
            default -> null;
        };
    }

    private String mapOutcome(String marketKey,
                              OddsApiResponse.Outcome outcome) {

        if (marketKey == null ||
                outcome == null ||
                outcome.getName() == null) return null;

        if ("h2h".equalsIgnoreCase(marketKey)) {
            return outcome.getName();
        }

        if ("totals".equalsIgnoreCase(marketKey)
                && outcome.getPoint() != null) {
            return outcome.getName() + " " + outcome.getPoint();
        }

        return null;
    }

    private String resolveSportKey(FixtureEntity fixture) {
        return MajorLeague.byLeagueId(fixture.getLeagueId())
                .map(MajorLeague::getOddsSportKey)
                .orElse(null);
    }

    private LocalDateTime parseKickoff(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) return null;

        try {
            return OffsetDateTime.parse(dateTime).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTime);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String normalizeTeamName(String input) {
        if (input == null) return "";

        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("fc", "")
                .replace("cf", "")
                .replace("sc", "")
                .replace("afc", "")
                .replace(" ", "")
                .replace("-", "")
                .replace(".", "")
                .replace("'", "");
    }

    private boolean isApiFootballReady() {
        return apiFootballProperties.enabled()
                && apiFootballProperties.key() != null
                && !apiFootballProperties.key().isBlank();
    }

    private boolean isOddsReady() {
        return oddsApiProperties.enabled()
                && oddsApiProperties.key() != null
                && !oddsApiProperties.key().isBlank();
    }

    private boolean isReady() {
        return isApiFootballReady() && isOddsReady();
    }
}
