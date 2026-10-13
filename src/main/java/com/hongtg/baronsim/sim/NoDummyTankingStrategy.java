package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Target;
import com.hongtg.baronsim.model.Turret;

import java.util.List;

import com.hongtg.baronsim.model.Heimer;

public class NoDummyTankingStrategy implements TankingStrategy{
    private final List<Turret> turrets;
    private final Heimer heimer;
    private final RqTurretPosition rqPosition;

    public NoDummyTankingStrategy(List<Turret> turrets, Heimer heimer, RqTurretPosition rqPosition) {
        this.turrets = turrets;
        this.heimer = heimer;
        this.rqPosition = rqPosition;
    }

    @Override 
    public Target currentTarget(CombatState state) {
        Turret rq = state.rQTurret();
        boolean rqAlive = rq != null && !rq.isDead();

        if (rqPosition == RqTurretPosition.FRONT) {
            if(rqAlive){
                return rq;
            }
        }

        for (Turret t : turrets) {
            if (!t.isDead()) {
                return t;
            }
        }
        
        if(rqAlive) {
            return rq;
        }
        return heimer;
    }
}
