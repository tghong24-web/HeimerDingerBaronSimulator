package com.hongtg.baronsim.sim;


// E: CH-2 전자 폭풍 수류탄 데미지 이벤트
public record GrenadeEvent(double time, double damage, double coolDown, UltimateSpec re)
        implements Event {

    @Override
    public void apply(CombatState state) {
        // 1. 바론 체력 깎기
        if (re != null && state.isRReady(time)) {   //RE를 선택했고, R쿨이 돌았다면
            state.damageBaron(DamageSource.RE_GRENADE, re.damage(), state.heimer());
            state.refreshBurns(time);
            state.useR(time, re.cooldown());
        }
        else {  //기본적으론 일반 E
            state.damageBaron(DamageSource.GRENADE, damage, state.heimer());
            state.refreshBurns(time);
        }        
        
        // 2. 다음 공격 예약: 새 GrenadeEvent를 만들어 state.schedule(...)
        GrenadeEvent nextAttack = new GrenadeEvent(state.basicReadyTime(time, coolDown), damage, coolDown, re);
        state.schedule(nextAttack);

    }
}