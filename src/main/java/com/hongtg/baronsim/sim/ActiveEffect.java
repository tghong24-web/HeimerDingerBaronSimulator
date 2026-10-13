package com.hongtg.baronsim.sim;

// 실체화 장비가 없을 때는 (0, 0, 1)
public record ActiveEffect(
    double damageAmp,
    double durationSeconds,
    double cooldownRate)
{ }
