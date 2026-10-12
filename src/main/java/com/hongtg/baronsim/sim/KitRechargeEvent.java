package com.hongtg.baronsim.sim;

public record KitRechargeEvent(double time)
        implements Event {
        
    @Override 
    public void apply(CombatState state) {
        // 1.키트 하나 추가
        state.turretKits().addKit();
        // 2.만약 가득 찼으면 충전 중 끄기(false)
        if(state.turretKits().isFull()) {
            state.turretKits().setRecharging(false);
        }
        // 3. 아니면(키트 3개 보다 적으면) time + 충전시간(rechargeSeconds) 후에
        //    KitRechargeEvent 하나 예약
        else {
            KitRechargeEvent nextEvent = new KitRechargeEvent(time + state.turretKits().rechargeSeconds());
            state.schedule(nextEvent);
        }
    }
}
