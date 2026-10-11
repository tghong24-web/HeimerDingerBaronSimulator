package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.HeimerData.RqTurretData;

public record RQPlaceEvent(
        double time,
        double shotDamage,
        double beamDamage,
        RqTurretData rqData,
        double cooldown) implements Event {

    @Override
    public void apply(CombatState state) {
        state.schedule(new RQTurretEvent(time, 0, shotDamage, beamDamage, rqData));
        state.useR(time, cooldown);
        state.schedule(new RQPlaceEvent(time + cooldown, shotDamage, beamDamage, rqData, cooldown));
    }
}
