package com.hongtg.baronsim.calc;

public record StatGrowth(double base, double perLevel) {
    private static final double GROWTH_BASE = 0.7025;
    private static final double GROWTH_PER = 0.0175;
    // 해당 레벨에 해당하는 능력치 반환
    public double valueAt(int level) {
        return base + perLevel * (level - 1) * (GROWTH_BASE + GROWTH_PER * (level - 1));
    }  
} 
