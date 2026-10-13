package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Turret;

public record RQExpireEvent(
        double time,
        Turret rqTurret)
        implements Event {

    @Override
    public void apply(CombatState state) {
        if(state.rQTurret() == rqTurret){
            state.setRQ(null);
        }
    }
}
