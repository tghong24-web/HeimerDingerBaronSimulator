package com.hongtg.baronsim.sim;

public class TurretKits {
    private int kits;
    private final int maxKits;
    private final double rechargeSeconds;
    private boolean charging;

    public TurretKits(int kits, int maxKits, double rechargeSeconds) {
        this.kits = kits;
        this.maxKits = maxKits;
        this.rechargeSeconds = rechargeSeconds;
    }

    public boolean hasKit() {
        return kits > 0;
    }
    public boolean isFull() {
        return kits >= maxKits;
    }
    public void addKit() {
        kits++;
    }
    public void useKit() {
        kits--;
    }

    public boolean isRecharging() {
        return charging;
    }
    public void setRecharging(boolean value) {
        charging = value;
    }

    public int kits() {
        return kits;
    }

    public double rechargeSeconds() {
        return rechargeSeconds;
    }
}
