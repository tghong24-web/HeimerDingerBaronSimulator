package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Turret;

public record TurretPlaceEvent(
        double time,
        TurretSpec spec)
        implements Event {

    @Override
    public void apply(CombatState state) {
        if (state.heimer().isDead()) {
            return;
        }

        // 죽은 자리(slot) 찾기
        int slot = -1;
        for (int i = 0; i < state.turrets().size(); i++) {
            if (state.turrets().get(i).isDead()){
                slot = i;
                break;
            }
        }

        Turret newTurret = new Turret(spec.health(), spec.armor());
        
        if (slot != -1) {
            state.turrets().set(slot, newTurret);
        } else if(state.turrets().size() < spec.maxTurrets()){
            state.turrets().add(newTurret);
        } else {
            return;
        }

        state.schedule(new TurretAttackEvent(time, spec.shotDamage(), spec.shotInterval(), newTurret));
    }
}