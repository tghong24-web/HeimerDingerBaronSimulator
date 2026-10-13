package com.hongtg.baronsim.model;

public class Dummy implements Target {
    private int hitCount;
    private double totalDamage;
    private final double armor;
    private final double magicResist;

    public Dummy(double armor, double mr) {
        this.armor = armor;
        this.magicResist = mr;
    }

    @Override 
    public double armor() {
        return armor;
    }

    @Override 
    public double magicResist() {
        return magicResist;
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
