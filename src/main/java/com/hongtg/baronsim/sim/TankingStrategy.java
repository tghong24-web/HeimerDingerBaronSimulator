package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Target;

public interface TankingStrategy {
    Target currentTarget(CombatState state);
}
