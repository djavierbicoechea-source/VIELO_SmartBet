package com.vielo.smartbet.prediction;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name="predictions", indexes = {@Index(name="idx_pred_date", columnList="forDate")})
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate forDate;
    private Long fixtureId;
    private String league;
    private String matchLabel;
    private String market;
    private String pick;
    private Double odd;
    private Double impliedProb;
    private Double score;

    @Enumerated(EnumType.STRING)
    private PredictionTier tier;

    public Prediction() {}

    public Prediction(Long id, LocalDate forDate, Long fixtureId, String league, String matchLabel, String market,
                      String pick, Double odd, Double impliedProb, Double score, PredictionTier tier) {
        this.id = id;
        this.forDate = forDate;
        this.fixtureId = fixtureId;
        this.league = league;
        this.matchLabel = matchLabel;
        this.market = market;
        this.pick = pick;
        this.odd = odd;
        this.impliedProb = impliedProb;
        this.score = score;
        this.tier = tier;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getForDate() { return forDate; }
    public void setForDate(LocalDate forDate) { this.forDate = forDate; }
    public Long getFixtureId() { return fixtureId; }
    public void setFixtureId(Long fixtureId) { this.fixtureId = fixtureId; }
    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }
    public String getMatchLabel() { return matchLabel; }
    public void setMatchLabel(String matchLabel) { this.matchLabel = matchLabel; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getPick() { return pick; }
    public void setPick(String pick) { this.pick = pick; }
    public Double getOdd() { return odd; }
    public void setOdd(Double odd) { this.odd = odd; }
    public Double getImpliedProb() { return impliedProb; }
    public void setImpliedProb(Double impliedProb) { this.impliedProb = impliedProb; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public PredictionTier getTier() { return tier; }
    public void setTier(PredictionTier tier) { this.tier = tier; }

    public PredictionTier getTierOrDefault() {
        return tier != null ? tier : PredictionTier.PREMIUM;
    }

    public boolean isFree() {
        return getTierOrDefault() == PredictionTier.FREE;
    }

    public boolean isPremium() {
        return getTierOrDefault() == PredictionTier.PREMIUM;
    }

    public String getTierLabel() {
        return getTierOrDefault().name();
    }

    public static class Builder {
        private Long id; private LocalDate forDate; private Long fixtureId; private String league; private String matchLabel;
        private String market; private String pick; private Double odd; private Double impliedProb; private Double score; private PredictionTier tier;
        public Builder id(Long id) { this.id = id; return this; }
        public Builder forDate(LocalDate forDate) { this.forDate = forDate; return this; }
        public Builder fixtureId(Long fixtureId) { this.fixtureId = fixtureId; return this; }
        public Builder league(String league) { this.league = league; return this; }
        public Builder matchLabel(String matchLabel) { this.matchLabel = matchLabel; return this; }
        public Builder market(String market) { this.market = market; return this; }
        public Builder pick(String pick) { this.pick = pick; return this; }
        public Builder odd(Double odd) { this.odd = odd; return this; }
        public Builder impliedProb(Double impliedProb) { this.impliedProb = impliedProb; return this; }
        public Builder score(Double score) { this.score = score; return this; }
        public Builder tier(PredictionTier tier) { this.tier = tier; return this; }
        public Prediction build() { return new Prediction(id, forDate, fixtureId, league, matchLabel, market, pick, odd, impliedProb, score, tier); }
    }
}
