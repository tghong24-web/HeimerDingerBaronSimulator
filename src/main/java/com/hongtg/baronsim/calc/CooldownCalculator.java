package com.hongtg.baronsim.calc;

public class CooldownCalculator {
    public static double reducedCooldown(double coolDown, double abilityHaste){
        return coolDown * 100 / (100 + abilityHaste);
    }
}
