package com.vielo.smartbet.football;

import java.util.*;

public enum MajorLeague {
    PREMIER_LEAGUE(39L, "soccer_epl", 100),
    LA_LIGA(140L, "soccer_spain_la_liga", 95),
    SERIE_A(135L, "soccer_italy_serie_a", 93),
    BUNDESLIGA(78L, "soccer_germany_bundesliga", 92),
    LIGUE_1(61L, "soccer_france_ligue_one", 90),
    CHAMPIONS_LEAGUE(2L, "soccer_uefa_champs_league", 98),
    EUROPA_LEAGUE(3L, "soccer_uefa_europa_league", 82),
    CONFERENCE_LEAGUE(848L, "soccer_uefa_europa_conference_league", 75),
    NETHERLANDS_EREDIVISIE(88L, "soccer_netherlands_eredivisie", 74),
    PORTUGAL_PRIMEIRA_LIGA(94L, "soccer_portugal_primeira_liga", 73),
    TURKEY_SUPER_LIG(203L, "soccer_turkey_super_league", 72),
    BELGIUM_FIRST_DIV(144L, "soccer_belgium_first_div", 70),
    SAUDI_PRO_LEAGUE(307L, "soccer_saudi_arabia_pro_league", 69),
    BRAZIL_SERIE_A(71L, "soccer_brazil_campeonato", 80);

    private final Long apiFootballLeagueId;
    private final String oddsSportKey;
    private final int priority;

    MajorLeague(Long apiFootballLeagueId, String oddsSportKey, int priority) {
        this.apiFootballLeagueId = apiFootballLeagueId;
        this.oddsSportKey = oddsSportKey;
        this.priority = priority;
    }

    public Long getApiFootballLeagueId() { return apiFootballLeagueId; }
    public String getOddsSportKey() { return oddsSportKey; }
    public int getPriority() { return priority; }

    private static final Map<Long, MajorLeague> BY_ID = Arrays.stream(values())
            .collect(HashMap::new, (m, v) -> m.put(v.apiFootballLeagueId, v), HashMap::putAll);

    public static Optional<MajorLeague> byLeagueId(Long leagueId) {
        return Optional.ofNullable(BY_ID.get(leagueId));
    }
}
