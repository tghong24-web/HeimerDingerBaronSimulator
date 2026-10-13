package com.hongtg.baronsim.calc;

public class CooldownCalculator {
    public static double reducedCooldown(double cooldown, double abilityHaste){
        return cooldown * 100 / (100 + abilityHaste);
    }
    public static double readyTime(double start, double cooldown, double boostEnd, double rate){
        if (start >= boostEnd) {
            return start + cooldown;
        } else {
            double boostedAmount = (boostEnd - start) * rate;
            if (cooldown <= boostedAmount){
                return start + cooldown / rate;
            }
            return boostEnd + (cooldown - boostedAmount);

        }
    }
}
