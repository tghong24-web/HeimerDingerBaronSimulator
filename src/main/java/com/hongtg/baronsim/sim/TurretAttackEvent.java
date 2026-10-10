package com.hongtg.baronsim.sim;

public record TurretAttackEvent(double time, int turretIndex, double damage, double interval)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 1. 바론 체력 깎기
        state.damageBaron(DamageSource.TURRET_SHOT, damage);
        
        // 2. 다음 공격 예약: 새 TurretAttackEvent를 만들어 state.schedule(...)
        TurretAttackEvent nextAttack = new TurretAttackEvent(time + interval, turretIndex, damage, interval);
        state.schedule(nextAttack);
    }
}