package com.vielo.smartbet.football;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OddsApiResponse {
    private String id;
    private String sport_key;
    private String commence_time;
    private String home_team;
    private String away_team;
    private List<Bookmaker> bookmakers;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSport_key() { return sport_key; }
    public void setSport_key(String sport_key) { this.sport_key = sport_key; }
    public String getCommence_time() { return commence_time; }
    public void setCommence_time(String commence_time) { this.commence_time = commence_time; }
    public String getHome_team() { return home_team; }
    public void setHome_team(String home_team) { this.home_team = home_team; }
    public String getAway_team() { return away_team; }
    public void setAway_team(String away_team) { this.away_team = away_team; }
    public List<Bookmaker> getBookmakers() { return bookmakers; }
    public void setBookmakers(List<Bookmaker> bookmakers) { this.bookmakers = bookmakers; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bookmaker {
        private String key;
        private String title;
        private List<Market> markets;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<Market> getMarkets() { return markets; }
        public void setMarkets(List<Market> markets) { this.markets = markets; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Market {
        private String key;
        private List<Outcome> outcomes;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public List<Outcome> getOutcomes() { return outcomes; }
        public void setOutcomes(List<Outcome> outcomes) { this.outcomes = outcomes; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Outcome {
        private String name;
        private Double price;
        private Double point;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Double getPoint() { return point; }
        public void setPoint(Double point) { this.point = point; }
    }
}
