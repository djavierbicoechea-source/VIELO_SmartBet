package com.vielo.smartbet.recommendation;

public class TicketLine {
    private String match;
    private String market;
    private String pick;
    private Double odd;

    public TicketLine() {}

    public TicketLine(String match, String market, String pick, Double odd) {
        this.match = match;
        this.market = market;
        this.pick = pick;
        this.odd = odd;
    }

    public String getMatch() { return match; }
    public void setMatch(String match) { this.match = match; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public String getPick() { return pick; }
    public void setPick(String pick) { this.pick = pick; }
    public Double getOdd() { return odd; }
    public void setOdd(Double odd) { this.odd = odd; }
}
