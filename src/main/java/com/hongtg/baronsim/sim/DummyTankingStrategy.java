package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Target;
import com.hongtg.baronsim.model.Dummy;

public class DummyTankingStrategy implements TankingStrategy{
    private final Dummy dummy;

    public DummyTankingStrategy(Dummy dummy) {
        this.dummy = dummy;
    }

    @Override 
    public Target currentTarget(CombatState state) {
        return dummy;
    }
}
