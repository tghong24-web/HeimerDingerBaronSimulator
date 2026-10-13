package com.hongtg.baronsim.sim;

// W: 마법공학 초소형 로켓 데미지 이벤트
public record RocketsEvent(double time, double damage, double coolDown, UltimateSpec rw)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 1. 바론 체력 깎기
        if (rw != null && state.isRReady(time)) {   //RW를 선택했고, R쿨이 돌았다면
            state.damageBaron(DamageSource.RW_ROCKETS, rw.damage(), state.heimer());
            state.refreshBurns(time);
            state.useR(time, rw.cooldown());
        }
        else {  //기본적으론 일반 W
            state.damageBaron(DamageSource.ROCKETS, damage, state.heimer());
            state.refreshBurns(time);
        }        
        
        // 2. 다음 공격 예약: 새 RocketsEvent를 만들어 state.schedule(...)
        RocketsEvent nextAttack = new RocketsEvent(state.basicReadyTime(time, coolDown), damage, coolDown, rw);
        state.schedule(nextAttack);

    }
}