package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.HeimerData.RqTurretData;
import com.hongtg.baronsim.model.Turret;

public record RQPlaceEvent(
        double time,
        double health,
        double armor,
        double magicResist,
        double shotDamage,
        double beamDamage,
        RqTurretData rqData,
        double cooldown) implements Event {

    @Override
    public void apply(CombatState state) {
        Turret rq = new Turret(health, armor, magicResist);
        state.setRQ(rq);
        state.record(ReplayEvent.Kind.RQ_PLACED, rq, null, 0, 1);

        state.schedule(new RQTurretEvent(time, 0, shotDamage, beamDamage, rqData, rq));
        state.useR(time, cooldown);
        state.schedule(new RQPlaceEvent(time + cooldown, health, armor, magicResist, shotDamage, beamDamage, rqData, cooldown));
        state.schedule(new RQExpireEvent(time + rqData.durationSeconds(), rq));
    }
}
