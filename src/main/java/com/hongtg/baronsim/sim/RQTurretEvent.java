package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.HeimerData.RqTurretData;
import com.hongtg.baronsim.model.Turret;

public record RQTurretEvent(
        double time,
        int attackIndex,
        double shotDamage,
        double beamDamage,
        RqTurretData rqData,
        Turret rqTurret)
        implements Event {

    @Override
    public void apply(CombatState state) {
        if (rqTurret.isDead()) {
            return;
        }
        // 1. 바론 체력 깎기(일반 평타면 SHOT, 광선이면 BEAM)
        switch (rqData.attackAt(attackIndex)) {
            case SHOT -> state.damageBaron(DamageSource.RQ_TURRET_SHOT, shotDamage, rqTurret);
            case BEAM -> state.damageBaron(DamageSource.RQ_TURRET_BEAM, beamDamage, rqTurret);
        }   
        state.refreshBurns(time);

        // 2. 다음 공격 예약: 공격 8회까지, 새 RQTurretEvent를 만들어 state.schedule(...)
        if (attackIndex + 1 < rqData.attackCount()) {
            RQTurretEvent nextAttack = new RQTurretEvent(time + rqData.attackIntervalSeconds(), attackIndex + 1, shotDamage, beamDamage, rqData, rqTurret);
            state.schedule(nextAttack);
        }
    }
}