package com.hongtg.baronsim.sim;

import java.util.Map;

public record SimulatorResult (Outcome outcome, double killTime, Map<DamageSource, Double> damageBySource, Map<DamageSource, Double> gazeLossBySource) {
    public boolean killed() {
        return outcome == Outcome.KILLED;
    }
}
