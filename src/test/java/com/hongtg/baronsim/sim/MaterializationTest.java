package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.calc.CooldownCalculator;
import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;

/**
 * 실체화 장비 "마나 실현" 테스트 (8-3단계).
 * 0초에 한 번 써서 8초 동안 모든 피해 +(15% + 추가 마나 × 0.005%). 실체화 장비만 들면 마나 300 → +16.5%.
 * Gaze·그림자불꽃과 같은 합산 묶음으로 더한다 (미확인 가정, DESIGN.md).
 * 8초 동안 기본 스킬 대기(W·E·Q 사이·키트 충전, R 제외)가 1.3배 빨리 흐른다.
 */
class MaterializationTest {

    private final Heimer heimer = new Heimer(2000, 50, 30);

    private CombatState state(double activeDamageAmp) {
        return state(new ActiveEffect(activeDamageAmp, 8, 1.3));
    }

    private CombatState state(ActiveEffect active) {
        return new CombatState(1_000_000, new DummyTankingStrategy(new Dummy(0, 0)), heimer, 0.5,
                new TurretKits(0, 3, 20), new ArrayList<>(), new TurretSpec(500, 30, 30, 0, 1.0, 1.0, 3),
                new ArrayList<>(), new LowHealthBonus(0, 0), active);
    }

    private double dealt(CombatState s, DamageSource source) {
        return s.damageBySource().getOrDefault(source, 0.0);
    }

    @Test
    void ampWhileActive() {
        CombatState s = state(0.165);
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);

        assertEquals(116.5, dealt(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void ampEndsAfterEndEvent() {
        CombatState s = state(0.165);
        new ActiveEndEvent(8).apply(s);
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);

        assertEquals(100, dealt(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void ampAddsWithGaze() {
        // 1 + 0.165 − 0.5 = 0.665, Gaze가 없었다면 1.165 → 손해 50
        CombatState s = state(0.165);
        s.setGazeTarget(heimer);
        s.damageBaron(DamageSource.ROCKETS, 100, heimer);

        assertEquals(66.5, dealt(s, DamageSource.ROCKETS), 1e-9);
        assertEquals(50, s.gazeLossBySource().get(DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void finalStatsHasItemMana() {
        // 실체화 장비 300 + 사라진 양피지 300 = 600 → 증폭 0.15 + 600 × 0.00005 = 0.18
        FinalStats f = BuildCalculator.calculate(com.hongtg.baronsim.data.DataLoader.loadItems(),
                new Build(List.of("materialization", "lostchapter"), 0, 0, 0, 0, 0), RoleQuest.NONE);
        assertEquals(600, f.mana(), 1e-9);
    }

    private void runUntil(CombatState state, double timeLimit) {
        while (state.hasEvents()) {
            Event e = state.nextEvent();
            if (e.time() > timeLimit) {
                state.schedule(e);
                break;
            }
            e.apply(state);
        }
    }

    // ── 쿨 가속 공식 (CooldownCalculator.readyTime) ──

    @Test
    void readyTimeFullyInsideBoost() {
        // 0초에 쿨 7초: 8초 안에 7 / 1.3 = 5.3846초 만에 끝
        assertEquals(7 / 1.3, CooldownCalculator.readyTime(0, 7, 8, 1.3), 1e-9);
    }

    @Test
    void readyTimeCrossesBoostEnd() {
        // 5.3846초에 쿨 7초: 8초까지 (8 − 5.3846) × 1.3 = 3.4초분 → 남은 3.6초는 보통 속도 → 11.6초
        assertEquals(11.6, CooldownCalculator.readyTime(7 / 1.3, 7, 8, 1.3), 1e-9);
    }

    @Test
    void readyTimeAfterBoostIsPlain() {
        assertEquals(17, CooldownCalculator.readyTime(10, 7, 8, 1.3), 1e-9);
        assertEquals(8 + 7, CooldownCalculator.readyTime(8, 7, 8, 1.3), 1e-9);   // 끝나는 순간 시작하면 가속 없음
    }

    @Test
    void readyTimeWithoutActiveIsPlain() {
        // 실체화 장비가 없으면 ActiveEffect(0, 0, 1) → 그냥 start + cooldown
        assertEquals(12, CooldownCalculator.readyTime(5, 7, 0, 1), 1e-9);
        assertEquals(12, state(new ActiveEffect(0, 0, 1)).basicReadyTime(5, 7), 1e-9);
    }

    // ── 이벤트에 적용 ──

    @Test
    void rocketsCooldownAccelerated() {
        // W 쿨 7초: 0, 5.3846, 11.6, 18.6, 25.6초 → 26초까지 5번 (가속 없으면 0, 7, 14, 21초 4번)
        // 8초 전 2번만 +16.5% → 116.5 × 2 + 100 × 3 = 533
        CombatState s = state(0.165);
        s.schedule(new ActiveEndEvent(8));
        s.schedule(new RocketsEvent(0, 100, 7, null));

        runUntil(s, 25.5);
        assertEquals(433, dealt(s, DamageSource.ROCKETS), 1e-9);
        runUntil(s, 26);
        assertEquals(533, dealt(s, DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void qGapAccelerated() {
        // Q 사이 1초 → 실체화 중엔 1 / 1.3초
        CombatState s = state(0.165);
        s.useQ(0, 1);
        assertEquals(1 / 1.3, s.qReadyTime(), 1e-9);

        s.useQ(10, 1);
        assertEquals(11, s.qReadyTime(), 1e-9);
    }

    @Test
    void ultimateCooldownNotAccelerated() {
        // R은 가속 대상이 아님: 0초에 R 쿨 70초 → 69.9초엔 아직, 70초에 준비
        CombatState s = state(0.165);
        s.useR(0, 70);
        assertFalse(s.isRReady(69.9));
        assertTrue(s.isRReady(70));
    }
}
