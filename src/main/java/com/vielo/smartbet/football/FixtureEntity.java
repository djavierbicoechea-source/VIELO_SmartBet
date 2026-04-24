package com.vielo.smartbet.football;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "fixtures")
public class FixtureEntity {
    @Id
    private Long id;

    private Long leagueId;
    private String leagueName;
    private String leagueCountry;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime kickoff;
    private String statusShort;

    public FixtureEntity() {}

    public FixtureEntity(Long id, Long leagueId, String leagueName, String leagueCountry, String homeTeam, String awayTeam, LocalDateTime kickoff, String statusShort) {
        this.id = id;
        this.leagueId = leagueId;
        this.leagueName = leagueName;
        this.leagueCountry = leagueCountry;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoff = kickoff;
        this.statusShort = statusShort;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLeagueId() { return leagueId; }
    public void setLeagueId(Long leagueId) { this.leagueId = leagueId; }
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    public String getLeagueCountry() { return leagueCountry; }
    public void setLeagueCountry(String leagueCountry) { this.leagueCountry = leagueCountry; }
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    public LocalDateTime getKickoff() { return kickoff; }
    public void setKickoff(LocalDateTime kickoff) { this.kickoff = kickoff; }
    public String getStatusShort() { return statusShort; }
    public void setStatusShort(String statusShort) { this.statusShort = statusShort; }

    public static class Builder {
        private Long id;
        private Long leagueId;
        private String leagueName;
        private String leagueCountry;
        private String homeTeam;
        private String awayTeam;
        private LocalDateTime kickoff;
        private String statusShort;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder leagueId(Long leagueId) { this.leagueId = leagueId; return this; }
        public Builder leagueName(String leagueName) { this.leagueName = leagueName; return this; }
        public Builder leagueCountry(String leagueCountry) { this.leagueCountry = leagueCountry; return this; }
        public Builder homeTeam(String homeTeam) { this.homeTeam = homeTeam; return this; }
        public Builder awayTeam(String awayTeam) { this.awayTeam = awayTeam; return this; }
        public Builder kickoff(LocalDateTime kickoff) { this.kickoff = kickoff; return this; }
        public Builder statusShort(String statusShort) { this.statusShort = statusShort; return this; }
        public FixtureEntity build() { return new FixtureEntity(id, leagueId, leagueName, leagueCountry, homeTeam, awayTeam, kickoff, statusShort); }
    }
}
