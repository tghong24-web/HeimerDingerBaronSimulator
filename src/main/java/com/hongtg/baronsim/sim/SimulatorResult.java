package com.hongtg.baronsim.sim;

import java.util.Map;

public record SimulatorResult (Outcome outcome, double killTime, Map<DamageSource, Double> damageBySource) {
    public boolean killed() {
        return outcome == Outcome.KILLED;
    }
}
