package com.hongtg.baronsim.calc;

import com.hongtg.baronsim.model.BaronStats;

public class BaronScaler {
    //필드: 스탯별 StatGrowth 4개와 공격 속도
    private final StatGrowth health;
    private final StatGrowth attackDamage;
    private final double attackSpeed;
    private final StatGrowth armor;
    private final StatGrowth magicResist;


    //생성자: 위 값들을 받아서 저장
    public BaronScaler(StatGrowth health, StatGrowth attackDamage, double attackSpeed, StatGrowth armor, StatGrowth magicResist) {
        this.health = health;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.armor = armor;
        this.magicResist = magicResist;
    }
    
    //메소드: 레벨을 입력하면 그에 맞는 바론의 스탯들 반환
    public BaronStats statsAt(int level) {
       return new BaronStats(
        health.valueAt(level),
        attackDamage.valueAt(level),
        attackSpeed,
        armor.valueAt(level),
        magicResist.valueAt(level));
    }
}
