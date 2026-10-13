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
        if (!state.turretKits().hasKit()) {
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

        Turret newTurret = new Turret(spec.health(), spec.armor());

        if (slot != -1) {
            state.turrets().set(slot, newTurret);
        } else if (state.turrets().size() < spec.maxTurrets()) {
            state.turrets().add(newTurret);
        } else {
            return;
        }

        state.turretKits().useKit();
        if (!state.turretKits().isRecharging()) {
            state.turretKits().setRecharging(true);
            state.schedule(new KitRechargeEvent(time + state.turretKits().rechargeSeconds()));
        }

        state.schedule(new TurretAttackEvent(time, spec.shotDamage(), spec.shotInterval(), newTurret));
    }
}