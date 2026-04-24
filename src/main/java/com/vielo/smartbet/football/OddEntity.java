package com.vielo.smartbet.football;

import jakarta.persistence.*;

@Entity
@Table(name = "odds", indexes = {@Index(name="idx_odds_fixture", columnList="fixtureId")})
public class OddEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fixtureId;
    private String bookmaker;
    private String market;
    private String outcome;
    private Double odd;

    public OddEntity() {}

    public OddEntity(Long id, Long fixtureId, String bookmaker, String market, String outcome, Double odd) {
        this.id = id;
        this.fixtureId = fixtureId;
        this.bookmaker = bookmaker;
        this.market = market;
        this.outcome = outcome;
        this.odd = odd;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFixtureId() { return fixtureId; }
    public void setFixtureId(Long fixtureId) { this.fixtureId = fixtureId; }
    public String getBookmaker() { return bookmaker; }
    public void setBookmaker(String bookmaker) { this.bookmaker = bookmaker; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Double getOdd() { return odd; }
    public void setOdd(Double odd) { this.odd = odd; }

    public static class Builder {
        private Long id;
        private Long fixtureId;
        private String bookmaker;
        private String market;
        private String outcome;
        private Double odd;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder fixtureId(Long fixtureId) { this.fixtureId = fixtureId; return this; }
        public Builder bookmaker(String bookmaker) { this.bookmaker = bookmaker; return this; }
        public Builder market(String market) { this.market = market; return this; }
        public Builder outcome(String outcome) { this.outcome = outcome; return this; }
        public Builder odd(Double odd) { this.odd = odd; return this; }
        public OddEntity build() { return new OddEntity(id, fixtureId, bookmaker, market, outcome, odd); }
    }
}
