package com.hongtg.baronsim.model;

public class Dummy implements Target {
    private int hitCount;
    private double totalDamage;
    private final double armor;

    public Dummy(double armor) {
        this.armor = armor;
    }

    @Override 
    public double armor() {
        return armor;
    }

    public int hitCount() {
        return hitCount;
    }

    public double totalDamage() {
        return totalDamage;
    }

    @Override 
    public void takeDamage(double amount) {
        hitCount += 1;
        totalDamage += amount;
    }
}
