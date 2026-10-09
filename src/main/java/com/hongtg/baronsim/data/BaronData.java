package com.hongtg.baronsim.data;

import com.hongtg.baronsim.calc.BaronScaler;
import com.hongtg.baronsim.calc.StatGrowth;

import java.util.Objects;

/**
 * baron.json의 내용을 그대로 담는 record. 필드 이름이 JSON 키와 같아야 Gson이 짝지어 준다.
 */
public record BaronData(
        String patch,
        int spawnTimeSeconds,
        int minLevel,
        int maxLevel,
        StatGrowth health,
        StatGrowth attackDamage,
        StatGrowth armor,
        StatGrowth magicResist,
        double attackSpeed,
        double healthRegen,
        Gaze gaze,
        VoidCorruption voidCorruption) {

    /** Baron's Gaze: 마지막으로 평타를 맞은 유닛에게서 받는 피해 감소. */
    public record Gaze(double damageReduction, double durationSeconds) {}

    /** Void Corruption: 중첩당 방어력, 마법 저항력 감소. */
    public record VoidCorruption(double resistReductionPerStack, int maxStacks, double durationSeconds) {}

    /**
     * JSON에 키가 빠졌거나 오타가 나면 Gson은 조용히 null을 넣는다.
     * 그런 값이 시뮬레이션까지 흘러가지 않도록 여기서 바로 멈춘다.
     */
    public BaronData {
        Objects.requireNonNull(patch, "baron.json에 patch가 없습니다");
        Objects.requireNonNull(health, "baron.json에 health가 없습니다");
        Objects.requireNonNull(attackDamage, "baron.json에 attackDamage가 없습니다");
        Objects.requireNonNull(armor, "baron.json에 armor가 없습니다");
        Objects.requireNonNull(magicResist, "baron.json에 magicResist가 없습니다");
        Objects.requireNonNull(gaze, "baron.json에 gaze가 없습니다");
        Objects.requireNonNull(voidCorruption, "baron.json에 voidCorruption이 없습니다");
        if (minLevel <= 0 || maxLevel < minLevel) {
            throw new IllegalArgumentException(
                    "baron.json의 레벨 범위가 잘못됐습니다: " + minLevel + "~" + maxLevel);
        }
    }

    /** 읽어 온 수치로 BaronScaler를 만든다. */
    public BaronScaler toScaler() {
        return new BaronScaler(health, attackDamage, attackSpeed, armor, magicResist, minLevel, maxLevel);
    }
}
