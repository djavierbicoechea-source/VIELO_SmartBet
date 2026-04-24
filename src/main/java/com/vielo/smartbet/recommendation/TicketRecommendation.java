package com.vielo.smartbet.recommendation;

import java.util.List;

public class TicketRecommendation {
    private List<TicketLine> lines;
    private Double totalOdd;
    private Double stake;
    private Double potentialProfit;
    private String note;

    public TicketRecommendation() {}

    public TicketRecommendation(List<TicketLine> lines, Double totalOdd, Double stake, Double potentialProfit, String note) {
        this.lines = lines;
        this.totalOdd = totalOdd;
        this.stake = stake;
        this.potentialProfit = potentialProfit;
        this.note = note;
    }

    public static Builder builder() { return new Builder(); }

    public List<TicketLine> getLines() { return lines; }
    public void setLines(List<TicketLine> lines) { this.lines = lines; }
    public Double getTotalOdd() { return totalOdd; }
    public void setTotalOdd(Double totalOdd) { this.totalOdd = totalOdd; }
    public Double getStake() { return stake; }
    public void setStake(Double stake) { this.stake = stake; }
    public Double getPotentialProfit() { return potentialProfit; }
    public void setPotentialProfit(Double potentialProfit) { this.potentialProfit = potentialProfit; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public static class Builder {
        private List<TicketLine> lines;
        private Double totalOdd;
        private Double stake;
        private Double potentialProfit;
        private String note;
        public Builder lines(List<TicketLine> lines) { this.lines = lines; return this; }
        public Builder totalOdd(Double totalOdd) { this.totalOdd = totalOdd; return this; }
        public Builder stake(Double stake) { this.stake = stake; return this; }
        public Builder potentialProfit(Double potentialProfit) { this.potentialProfit = potentialProfit; return this; }
        public Builder note(String note) { this.note = note; return this; }
        public TicketRecommendation build() { return new TicketRecommendation(lines, totalOdd, stake, potentialProfit, note); }
    }
}
