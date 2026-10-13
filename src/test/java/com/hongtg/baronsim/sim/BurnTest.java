package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.calc.DamageCalculator;
import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.data.ItemData;
import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * 화상 테스트 (8-3단계).
 * 스킬 피해마다 3초로 갱신, 0.5초마다 틱 (0초에 걸리면 0.5 ~ 3.0초에 6틱).
 * 공격자는 하이머: Gaze가 하이머에게 있을 때만 절반, 포탑에 있을 때는 그대로.
 */
class BurnTest {

    private static final ItemData ITEMS = DataLoader.loadItems();

    private final Heimer heimer = new Heimer(2000, 50, 30);

    private CombatState state(List<BurnSpec> burns) {
        return new CombatState(1_000_000, new DummyTankingStrategy(new Dummy(0, 0)), heimer, 0.5,
                new TurretKits(0, 3, 20), new ArrayList<>(), new TurretSpec(500, 30, 30, 0, 1.0, 1.0, 3), burns, new LowHealthBonus(0, 0), new ActiveEffect(0, 0, 1));
    }

    private void runUntil(CombatState s, double timeLimit) {
        while (s.hasEvents()) {
            Event e = s.nextEvent();
            if (e.time() > timeLimit) {
                s.schedule(e);
                break;
            }
            e.apply(s);
        }
    }

    private double dealt(CombatState s, DamageSource source) {
        return s.damageBySource().getOrDefault(source, 0.0);
    }

    // ── 틱과 갱신 ──

    @Test
    void oneHitBurnsSixTicks() {
        CombatState s = state(List.of(new BurnSpec(DamageSource.FATED_ASHES_BURN, 10)));
        s.refreshBurns(0);

        runUntil(s, 2.9);
        assertEquals(50, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);   // 0.5 ~ 2.5초 5틱

        runUntil(s, 10);
        assertEquals(60, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);   // 3.0초 틱까지 6틱
    }

    @Test
    void hitsAtSameTimeDoNotDoubleTicks() {
        // 포탑 3개가 같은 시각에 쏴도 틱은 한 줄만 돈다
        CombatState s = state(List.of(new BurnSpec(DamageSource.FATED_ASHES_BURN, 10)));
        s.refreshBurns(0);
        s.refreshBurns(0);
        s.refreshBurns(0);

        runUntil(s, 10);
        assertEquals(60, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);
    }

    @Test
    void hitWhileBurningExtendsDuration() {
        // 0초 걸림, 2초에 다시 맞음 → 5초까지 → 0.5 ~ 5.0초 10틱
        CombatState s = state(List.of(new BurnSpec(DamageSource.FATED_ASHES_BURN, 10)));
        s.refreshBurns(0);
        runUntil(s, 2.0);
        s.refreshBurns(2.0);

        runUntil(s, 10);
        assertEquals(100, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);
    }

    @Test
    void burnRestartsAfterItEnds() {
        // 0초 걸림 → 3.0초까지 6틱 → 3.5초 틱에서 꺼짐 → 4초에 다시 맞음 → 4.5 ~ 7.0초 6틱
        CombatState s = state(List.of(new BurnSpec(DamageSource.FATED_ASHES_BURN, 10)));
        s.refreshBurns(0);
        runUntil(s, 3.9);
        assertEquals(60, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);

        s.refreshBurns(4.0);
        runUntil(s, 10);
        assertEquals(120, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);
    }

    @Test
    void threeBurnsTickSeparately() {
        CombatState s = state(List.of(
                new BurnSpec(DamageSource.FATED_ASHES_BURN, 10),
                new BurnSpec(DamageSource.BLACKFIRE_BURN, 20),
                new BurnSpec(DamageSource.LIANDRY_BURN, 30)));
        s.refreshBurns(0);

        runUntil(s, 10);
        assertEquals(60, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);
        assertEquals(120, dealt(s, DamageSource.BLACKFIRE_BURN), 1e-9);
        assertEquals(180, dealt(s, DamageSource.LIANDRY_BURN), 1e-9);
    }

    @Test
    void noBurnItemsNoBurn() {
        CombatState s = state(List.of());
        s.refreshBurns(0);

        assertEquals(false, s.hasEvents());
    }

    // ── Baron's Gaze ──

    @Test
    void gazeOnHeimerHalvesBurn() {
        CombatState s = state(List.of(new BurnSpec(DamageSource.FATED_ASHES_BURN, 10)));
        s.setGazeTarget(heimer);
        s.refreshBurns(0);

        runUntil(s, 10);
        assertEquals(30, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);
    }

    @Test
    void gazeOnTurretDoesNotReduceBurn() {
        CombatState s = state(List.of(new BurnSpec(DamageSource.FATED_ASHES_BURN, 10)));
        s.setGazeTarget(new Turret(500, 30, 30));
        s.refreshBurns(0);

        runUntil(s, 10);
        assertEquals(60, dealt(s, DamageSource.FATED_ASHES_BURN), 1e-9);
    }

    // ── 실측값 재현 (2026-10-13, 연습 모드 바론 마저 67, 화면은 소수점 버림) ──

    private double tick(String id, double ap, double percentPen) {
        ItemData.Item item = ITEMS.item(id);
        double base = item.burnPerTick() + item.burnApRatioPerTick() * ap;
        return DamageCalculator.reducedDamage(base, 67, percentPen, 0);
    }

    @Test
    void measuredFatedAshes() {
        assertEquals(5, Math.floor(tick("fatedashes", 0, 0)), 1e-9);         // 5.99
        assertEquals(2, Math.floor(tick("fatedashes", 0, 0) * 0.5), 1e-9);   // Gaze 2.99
    }

    @Test
    void measuredBlackfire() {
        // AP 127에서 3초(6틱) 합 76, Gaze 시 38
        assertEquals(76, Math.floor(6 * tick("blackfire", 127, 0)), 1e-9);
        assertEquals(38, Math.floor(6 * tick("blackfire", 127, 0) * 0.5), 1e-9);
    }

    @Test
    void measuredLiandry() {
        assertEquals(11, Math.floor(tick("liandrys", 102, 0)), 1e-9);        // AP와 무관
        assertEquals(14, Math.floor(tick("liandrys", 102, 0.40)), 1e-9);     // 공허의 지팡이
        assertEquals(5, Math.floor(tick("liandrys", 102, 0) * 0.5), 1e-9);   // Gaze
    }

    // ── Simulator (바론 15레벨, 하이머 13레벨, Q5, 포탑 3개, 병풍 있음) ──

    private SimulatorResult run(Build build) {
        return new Simulator().run(new SimulationInput(15, 13, 5, 0, 0, 0, UltimateChoice.NONE, build,
                3, 0, 10, RqTurretPosition.FRONT, true, 300, RoleQuest.NONE));   // 어둠불꽃 가속 20 때문에 남은 충전 10초
    }

    @Test
    void fatedAshesInSimulation() {
        // AP 300 (운명의 재 30 + 추가 270). 화상 없으면 50.86초
        // 틱당 10 × 100/185.06 = 5.4037, 0.5 ~ 49.0초 98틱 → 49.14초 처치 (Python으로 따로 계산)
        SimulatorResult r = run(new Build(List.of("fatedashes"), 270, 0, 0, 0, 0));

        assertEquals(49.142857, r.killTime(), 1e-5);
        assertEquals(98 * 5.403653, r.damageBySource().get(DamageSource.FATED_ASHES_BURN), 1e-3);
    }

    @Test
    void allThreeBurnsInSimulation() {
        // 운명의 재 + 어둠불꽃 + 리안드리 + 추가 130 → (300) × 1.04 = AP 312
        // 틱당: 재 10, 어둠불꽃 20 + 3.12, 리안드리 20 (× 0.5404) → 42.86초 처치, 각 85틱
        SimulatorResult r = run(new Build(List.of("fatedashes", "blackfire", "liandrys"), 130, 0, 0, 0, 0));

        assertEquals(42.857143, r.killTime(), 1e-5);
        assertEquals(459.3105, r.damageBySource().get(DamageSource.FATED_ASHES_BURN), 1e-3);
        assertEquals(1061.9259, r.damageBySource().get(DamageSource.BLACKFIRE_BURN), 1e-3);
        assertEquals(918.621, r.damageBySource().get(DamageSource.LIANDRY_BURN), 1e-3);
    }

    @Test
    void burnUsesMagicPenetration() {
        // 리안드리 + 공허의 지팡이 + 추가 145 = AP 300, % 관통 40% → 바론 마저 85.06 × 0.6 = 51.04
        // 리안드리 틱당 20 × 100/151.04 = 13.2419, 0.5 ~ 38.5초 77틱 → 38.86초 처치 (Python으로 따로 계산)
        SimulatorResult r = run(new Build(List.of("liandrys", "voidstaff"), 145, 0, 0, 0, 0));

        assertEquals(38.857143, r.killTime(), 1e-5);
        assertEquals(77 * 13.241876, r.damageBySource().get(DamageSource.LIANDRY_BURN), 1e-3);
    }
}
