package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Turret;

public record TurretAttackEvent(
        double time,
        double damage,
        double interval,
        Turret turret)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 포탑 죽었으면, 발사 X, 예약도 X
        if (turret.isDead()) {
            return;
        }

        // 1. 바론 체력 깎기
        state.damageBaron(DamageSource.TURRET_SHOT, damage, turret);

        // 2. 다음 공격 예약: 새 TurretAttackEvent를 만들어 state.schedule(...)

        TurretAttackEvent nextAttack = new TurretAttackEvent(time + interval, damage, interval, turret);
        state.schedule(nextAttack);

    }
}