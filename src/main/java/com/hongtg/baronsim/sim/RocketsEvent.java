package com.hongtg.baronsim.sim;

// W: 마법공학 초소형 로켓 데미지 이벤트
public record RocketsEvent(double time, double damage, double coolDown)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 1. 바론 체력 깎기
        state.damageBaron(DamageSource.ROCKETS, damage);
        
        // 2. 다음 공격 예약: 새 RocketsEvent를 만들어 state.schedule(...)
        RocketsEvent nextAttack = new RocketsEvent(time + coolDown, damage, coolDown);
        state.schedule(nextAttack);
    }
}