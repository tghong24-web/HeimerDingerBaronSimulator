package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Target;
import com.hongtg.baronsim.model.Turret;

import java.util.List;

import com.hongtg.baronsim.model.Heimer;

public class NoDummyTankingStrategy implements TankingStrategy{
    private final List<Turret> turrets;
    private final Heimer heimer;

    public NoDummyTankingStrategy(List<Turret> turrets, Heimer heimer) {
        this.turrets = turrets;
        this.heimer = heimer;
    }

    @Override 
    public Target currentTarget(CombatState state) {
        for (Turret t : turrets) {
            if (!t.isDead()) {
                return t;
            }
        }
        return heimer;
    }
}
