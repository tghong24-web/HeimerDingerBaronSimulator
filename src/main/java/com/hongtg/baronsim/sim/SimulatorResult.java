package com.hongtg.baronsim.sim;

import java.util.List;
import java.util.Map;

/**
 * 시뮬레이션 결과.
 * baronStartHealth: 바론 최대 체력 (그래프 눈금용)
 * ultimateOneShot: RW/RE 한 방 피해 (그림자불꽃 보너스 포함 가정, 막타 기준선). RW/RE가 아니면 0
 * timeline: 전투 기록 (9-2단계). 체력 곡선, 사용 횟수, 리플레이가 여기서 나온다
 */
public record SimulatorResult (Outcome outcome, double killTime, Map<DamageSource, Double> damageBySource, Map<DamageSource, Double> gazeLossBySource,
        double baronStartHealth, double ultimateOneShot, List<ReplayEvent> timeline) {

    /** 전투 끝까지의 스킬 사용 횟수 */
    public SkillCounts skillCounts() {
        return SkillCounts.of(timeline, Double.MAX_VALUE);
    }

    /** 바론에게 들어간 총 피해 */
    public double totalDamage() {
        double sum = 0;
        for (double d : damageBySource.values()) sum += d;
        return sum;
    }

    /** Gaze 때문에 덜 들어간 총 피해 */
    public double totalGazeLoss() {
        double sum = 0;
        for (double d : gazeLossBySource.values()) sum += d;
        return sum;
    }

    public boolean killed() {
        return outcome == Outcome.KILLED;
    }
}
