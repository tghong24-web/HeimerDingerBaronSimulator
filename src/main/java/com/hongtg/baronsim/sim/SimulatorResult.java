package com.hongtg.baronsim.sim;

import java.util.Map;

public record SimulatorResult (boolean killed, double killTime, Map<DamageSource, Double> damageBySource) {}
