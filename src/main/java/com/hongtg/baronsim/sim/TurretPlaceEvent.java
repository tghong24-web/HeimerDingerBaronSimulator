package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Turret;

public record TurretPlaceEvent(
        double time,
        TurretSpec spec)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 딩거 죽었으면 X
        if (state.heimer().isDead()) {
            return;
        }
        // 남은 Kit 없으면 X
        if (!state.turretKits().hasKit()) {
            return;
        }
        // Q 쿨이 안 돌았으면 X
        if (!state.isQReady(time)) {
            state.schedule(new TurretPlaceEvent(state.qReadyTime(), spec));
            return;
        }

        // 죽은 자리(slot) 찾기
        int slot = -1;
        for (int i = 0; i < state.turrets().size(); i++) {
            if (state.turrets().get(i).isDead()) {
                slot = i;
                break;
            }
        }

        Turret newTurret = new Turret(spec.health(), spec.armor(), spec.magicResist());

        if (slot != -1) {
            state.turrets().set(slot, newTurret);
        } else if (state.turrets().size() < spec.maxTurrets()) {
            state.turrets().add(newTurret);
        } else {
            return;
        }

        state.turretKits().useKit();
        state.useQ(time, spec.qCooldown());

        if (!state.turretKits().isRecharging()) {
            state.turretKits().setRecharging(true);
            state.schedule(new KitRechargeEvent(time + state.turretKits().rechargeSeconds()));
        }

        state.schedule(new TurretPlaceEvent(time + spec.qCooldown(), spec));

        state.schedule(new TurretAttackEvent(time, spec.shotDamage(), spec.shotInterval(), newTurret));
    }
}