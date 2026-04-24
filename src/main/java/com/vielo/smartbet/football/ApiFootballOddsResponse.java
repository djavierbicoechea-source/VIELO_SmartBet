package com.vielo.smartbet.football;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFootballOddsResponse {
    private List<OddsItem> response;

    public List<OddsItem> getResponse() { return response; }
    public void setResponse(List<OddsItem> response) { this.response = response; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OddsItem {
        private List<Bookmaker> bookmakers;
        public List<Bookmaker> getBookmakers() { return bookmakers; }
        public void setBookmakers(List<Bookmaker> bookmakers) { this.bookmakers = bookmakers; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bookmaker {
        private String name;
        private List<Bet> bets;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Bet> getBets() { return bets; }
        public void setBets(List<Bet> bets) { this.bets = bets; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bet {
        private String name;
        private List<Value> values;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Value> getValues() { return values; }
        public void setValues(List<Value> values) { this.values = values; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        private String value;
        private String odd;
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getOdd() { return odd; }
        public void setOdd(String odd) { this.odd = odd; }
    }
}
