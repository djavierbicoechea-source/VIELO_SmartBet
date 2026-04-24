package com.vielo.smartbet.football;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFootballFixturesResponse {
    private List<FixtureItem> response;

    public List<FixtureItem> getResponse() { return response; }
    public void setResponse(List<FixtureItem> response) { this.response = response; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FixtureItem {
        private Fixture fixture;
        private Teams teams;
        private League league;
        public Fixture getFixture() { return fixture; }
        public void setFixture(Fixture fixture) { this.fixture = fixture; }
        public Teams getTeams() { return teams; }
        public void setTeams(Teams teams) { this.teams = teams; }
        public League getLeague() { return league; }
        public void setLeague(League league) { this.league = league; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fixture {
        private Long id;
        private String date;
        private Status status;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        @JsonProperty("short")
        private String shortCode;
        public String getShort() { return shortCode; }
        public void setShort(String shortCode) { this.shortCode = shortCode; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Teams {
        private Team home;
        private Team away;
        public Team getHome() { return home; }
        public void setHome(Team home) { this.home = home; }
        public Team getAway() { return away; }
        public void setAway(Team away) { this.away = away; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private Long id;
        private String name;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private Long id;
        private String name;
        private String country;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }
}
