package com.hongtg.baronsim.model;

public interface Target {
    double armor();
    double magicResist();
    void takeDamage(double amount);
}
