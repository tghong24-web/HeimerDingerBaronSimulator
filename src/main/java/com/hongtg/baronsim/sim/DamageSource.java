package com.hongtg.baronsim.sim;

public enum DamageSource {
    TURRET_SHOT("포탑 평타"),
    ROCKETS("W 로켓"),
    GRENADE("E 수류탄");

    private final String displayName;

    DamageSource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }    
}
