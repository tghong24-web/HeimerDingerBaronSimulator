package com.hongtg.baronsim.sim;


// E: CH-2 전자 폭풍 수류탄 데미지 이벤트
public record GrenadeEvent(double time, double damage, double coolDown)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 1. 바론 체력 깎기
        state.damageBaron(damage);
        
        // 2. 다음 공격 예약: 새 GrenadeEvent를 만들어 state.schedule(...)
        GrenadeEvent nextAttack = new GrenadeEvent(time + coolDown, damage, coolDown);
        state.schedule(nextAttack);
    }
}