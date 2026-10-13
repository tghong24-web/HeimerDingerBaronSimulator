package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.calc.DamageCalculator;
import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * 그림자불꽃과 피해 증감 합산 테스트 (8-3단계).
 * 바론 체력이 최대 체력의 40% 이하면 모든 피해 +20%. Gaze 감소(−50%)와는 더한다: 1 + 0.2 − 0.5 = 0.7.
 */
class ShadowflameTest {

    private final Heimer heimer = new Heimer(2000, 50, 30);
    private static final LowHealthBonus SHADOWFLAME = new LowHealthBonus(0.40, 0.20);

    private CombatState state(double baronHealth, LowHealthBonus bonus) {
        return new CombatState(baronHealth, new DummyTankingStrategy(new Dummy(0, 0)), heimer, 0.5,
                new TurretKits(0, 3, 20), new ArrayList<>(), new TurretSpec(500, 30, 30, 0, 1.0, 1.0, 3),
                new ArrayList<>(), bonus, new ActiveEffect(0, 0, 1));
    }

    private double dealt(CombatState s, DamageSource source) {
        return s.damageBySource().getOrDefault(source, 0.0);
    }

    private double loss(CombatState s, DamageSource source) {
        return s.gazeLossBySource().getOrDefault(source, 0.0);
    }

    @Test
    void noBonusAboveFortyPercent() {
        CombatState s = state(1000, SHADOWFLAME);
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);   // 체력 100% → 그대로

        assertEquals(100, dealt(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void bonusAtOrBelowFortyPercent() {
        CombatState s = state(1000, SHADOWFLAME);
        s.damageBaron(DamageSource.TURRET_SHOT, 600, heimer);   // 1000 → 400 (정확히 40%)
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);       // 40% 이하 → 120

        assertEquals(120, dealt(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void justAboveFortyPercentHasNoBonus() {
        CombatState s = state(1000, SHADOWFLAME);
        s.damageBaron(DamageSource.TURRET_SHOT, 599, heimer);   // 401 → 40.1%
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);

        assertEquals(100, dealt(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void noShadowflameNoBonus() {
        CombatState s = state(1000, new LowHealthBonus(0, 0));
        s.damageBaron(DamageSource.TURRET_SHOT, 900, heimer);
        s.damageBaron(DamageSource.ROCKETS, 50, heimer);

        assertEquals(50, dealt(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void appliesToTurretsAndBurnsToo() {
        CombatState s = state(1000, SHADOWFLAME);
        s.damageBaron(DamageSource.GRENADE, 700, heimer);   // 300 남음
        Turret t = new Turret(500, 30, 30);
        s.damageBaron(DamageSource.TURRET_SHOT, 10, t);
        s.damageBaron(DamageSource.LIANDRY_BURN, 10, heimer);

        assertEquals(12, dealt(s, DamageSource.TURRET_SHOT), 1e-9);
        assertEquals(12, dealt(s, DamageSource.LIANDRY_BURN), 1e-9);
    }

    @Test
    void gazeAndShadowflameAdd() {
        // 1 + 0.2 − 0.5 = 0.7 (곱이면 0.6) / Gaze가 없었다면 1.2 → 손해 0.5
        CombatState s = state(1000, SHADOWFLAME);
        s.damageBaron(DamageSource.GRENADE, 700, heimer);   // 300 남음
        s.setGazeTarget(heimer);
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);

        assertEquals(70, dealt(s, DamageSource.ROCKETS), 1e-9);
        assertEquals(50, loss(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void gazeLossIsCappedByRemainingHealth() {
        // 남은 체력 100: Gaze 없었다면 min(100, 120) = 100, 실제 min(100, 70) = 70 → 손해 30
        CombatState s = state(1000, SHADOWFLAME);
        s.damageBaron(DamageSource.GRENADE, 900, heimer);
        s.setGazeTarget(heimer);
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);

        assertEquals(70, dealt(s, DamageSource.ROCKETS), 1e-9);
        assertEquals(30, loss(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void measuredFatedAshesWithGazeAndShadowflame() {
        // 2026-10-13 실측 (연습 모드 바론 마저 67, 화면은 소수점 버림), 운명의 재 틱 10
        //   그림자불꽃만 (고정 관통 15 → 마저 52): 4.61 → 4
        //   + 공허의 지팡이 (마저 67 × 0.6 − 15 = 25.2): 5.59 → 5
        double tick1 = DamageCalculator.reducedDamage(10, 67, 0, 15);
        double tick2 = DamageCalculator.reducedDamage(10, 67, 0.40, 15);
        CombatState s1 = state(1000, SHADOWFLAME);
        CombatState s2 = state(1000, SHADOWFLAME);
        for (CombatState s : List.of(s1, s2)) {
            s.damageBaron(DamageSource.GRENADE, 700, heimer);
            s.setGazeTarget(heimer);
        }
        s1.damageBaron(DamageSource.FATED_ASHES_BURN, tick1, heimer);
        s2.damageBaron(DamageSource.FATED_ASHES_BURN, tick2, heimer);

        assertEquals(4, Math.floor(dealt(s1, DamageSource.FATED_ASHES_BURN)), 1e-9);
        assertEquals(5, Math.floor(dealt(s2, DamageSource.FATED_ASHES_BURN)), 1e-9);
    }

    // ── Simulator (바론 15레벨, 하이머 13레벨, Q5, 포탑 3개, 병풍 있음, Q만) ──

    private SimulatorResult run(Build build) {
        return new Simulator().run(new SimulationInput(15, 13, 5, 0, 0, 0, UltimateChoice.NONE, build,
                3, 0, 20, RqTurretPosition.FRONT, true, 300, RoleQuest.NONE));
    }

    @Test
    void shadowflameInSimulation() {
        // 그림자불꽃 + 추가 190 = AP 300, 고정 관통 15 → 바론 마저 70.06, 포탑 한 발 75.27
        // 바론 체력 40% 이하부터 한 발 × 1.2 (83발) → 43.43초 처치
        // 같은 관통을 추가 스탯으로만 주면(그림자불꽃 없음) 46.86초 (Python으로 따로 계산)
        SimulatorResult withSf = run(new Build(List.of("shadowflame"), 190, 0, 0, 0, 0));
        SimulatorResult noSf = run(new Build(List.of(), 300, 0, 15, 0, 0));

        assertEquals(43.428571, withSf.killTime(), 1e-5);
        assertEquals(46.857143, noSf.killTime(), 1e-5);
    }
}
