package com.hongtg.baronsim.sim;

public record BurnTickEvent(
        double time,
        BurnSpec spec)
        implements Event {

    @Override
    public void apply(CombatState state) {
        if (time > state.burnUntil(spec.source())) {
            state.stopBurning(spec.source());
            return;
        }
        state.damageBaron(spec.source(), spec.damagePerTick(), state.heimer());
        state.schedule(new BurnTickEvent(time + 0.5, spec));

    }
}
