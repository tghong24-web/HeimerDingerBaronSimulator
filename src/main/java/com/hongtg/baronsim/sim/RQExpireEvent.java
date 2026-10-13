package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Turret;

public record RQExpireEvent(
        double time,
        Turret rqTurret)
        implements Event {

    @Override
    public void apply(CombatState state) {
        if(state.rQTurret() == rqTurret){
            if (!rqTurret.isDead()) {   // 이미 부서졌으면 BaronAttackEvent가 기록했다
                state.record(ReplayEvent.Kind.RQ_GONE, rqTurret, null, 0, 0);
            }
            state.setRQ(null);
        }
    }
}
