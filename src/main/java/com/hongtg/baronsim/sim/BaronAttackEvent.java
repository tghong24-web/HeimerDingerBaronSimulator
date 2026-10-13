package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.model.Target;
import com.hongtg.baronsim.model.Turret;
import com.hongtg.baronsim.calc.DamageCalculator;

public record BaronAttackEvent(
    double time, 
    double baronDamage,
    double baronAttackInterval) 
    implements Event {

    @Override
    public void apply(CombatState state) {
        // 1. state에서 현재 바론의 Target을 받고, 
        // 해당 대상의 armor()를 토대로 데미지 계산후(finalDamage) Target의 피 깎기(takeDamage)
        Target target = state.tankingStrategy().currentTarget(state);        
        double finalDamage = DamageCalculator.reducedDamage(baronDamage, target.armor(), 0, 0);
        target.takeDamage(finalDamage);
        state.recordBaronAttack(target, finalDamage);
        // 방금 공격으로 포탑이 죽었으면 재설치 이벤트 실행
        if(target instanceof Turret t && t.isDead()) {
            state.schedule(new TurretPlaceEvent(time, state.turretSpec()));
        }
        state.setGazeTarget(target);

        // baronAttackSpeed 후 다음 공격 예약
        BaronAttackEvent nextAttack = new BaronAttackEvent(time + baronAttackInterval, baronDamage, baronAttackInterval);
        state.schedule(nextAttack);
    }
}