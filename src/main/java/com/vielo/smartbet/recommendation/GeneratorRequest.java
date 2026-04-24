package com.vielo.smartbet.recommendation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GeneratorRequest {
    @NotNull @Min(1)
    private Double budget;

    @NotNull @Min(1)
    private Double targetProfit;

    @NotNull
    private RiskLevel risk = RiskLevel.MEDIUM;

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
    public Double getTargetProfit() { return targetProfit; }
    public void setTargetProfit(Double targetProfit) { this.targetProfit = targetProfit; }
    public RiskLevel getRisk() { return risk; }
    public void setRisk(RiskLevel risk) { this.risk = risk; }
}
