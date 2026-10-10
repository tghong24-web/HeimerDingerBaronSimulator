package com.hongtg.baronsim.sim;

public enum DamageSource {
    TURRET_SHOT("포탑 평타"),
    ROCKETS("W 로켓"),
    GRENADE("E 수류탄"),
    RQ_TURRET_SHOT("RQ 포탑 평타"),
    RQ_TURRET_BEAM("RQ 포탑 광선"),
    RW_ROCKETS("RW 로켓"),
    RE_GRENADE("RE 수류탄");

    private final String displayName;

    DamageSource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }    
}
