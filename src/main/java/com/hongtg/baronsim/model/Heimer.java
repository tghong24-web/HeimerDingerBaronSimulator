package com.hongtg.baronsim.model;

public class Heimer implements Target {
    private double health;
    private final double armor;
    private final double maxHealth;

    public Heimer(double health, double armor) {
        this.health = health;
        this.armor = armor;
        this.maxHealth = health;
    }

    @Override
    public void takeDamage(double amount) {
        health = Math.max(0, health - amount);
    }

    public double health() {
        return health;
    }

    @Override
    public double armor() {
        return armor;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public boolean isDead() {
        return health <= 0;
    }
}
