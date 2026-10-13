package com.hongtg.baronsim.sim;

public record TurretSpec(
    double health,
    double armor,
    double magicResist,
    double shotDamage,
    double shotInterval,
    double qCooldown,
    int maxTurrets) { }
