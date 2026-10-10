package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.HeimerData.RqTurretAttack;
import com.hongtg.baronsim.data.HeimerData.RqTurretData;

public record RQTurretEvent(
        double time,
        int attackIndex,
        double shotDamage,
        double beamDamage,
        RqTurretData rqData)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 1. 바론 체력 깎기(일반 평타면 SHOT, 광선이면 BEAM)
        switch (rqData.attackAt(attackIndex)) {
            case SHOT -> state.damageBaron(DamageSource.RQ_TURRET_SHOT, shotDamage);
            case BEAM -> state.damageBaron(DamageSource.RQ_TURRET_BEAM, beamDamage);
        }

        // 2. 다음 공격 예약: 공격 8회까지, 새 RQTurretEvent를 만들어 state.schedule(...)
        if (attackIndex + 1 < rqData.attackCount()) {
            RQTurretEvent nextAttack = new RQTurretEvent(time + rqData.attackIntervalSeconds(), attackIndex + 1, shotDamage, beamDamage, rqData);
            state.schedule(nextAttack);
        }
    }
}