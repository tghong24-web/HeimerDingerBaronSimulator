package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.sim.ReplayEvent.Actor;
import com.hongtg.baronsim.sim.ReplayEvent.Kind;

/**
 * 전투 기록(ReplayEvent)과 사용 횟수 테스트 (9-2단계).
 * 공통: 바론 15레벨, 하이머 13레벨, Q5, AP 300.
 * 숫자 기대값은 기존 테스트에서 이미 검증된 값을 쓴다 (W 한 번 312.8715, Q만 쓸 때 포탑 269발 처치).
 */
class ReplayTimelineTest {

    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int w, int e, int r, UltimateChoice ult, List<String> items,
            int turrets, int kits, double rest, boolean dummy) {
        return simulator.run(new SimulationInput(15, 13, 5, w, e, r, ult, new Build(items, 300, 0, 0, 0, 0),
                turrets, kits, rest, RqTurretPosition.FRONT, dummy, 300, RoleQuest.NONE));
    }

    private List<ReplayEvent> of(SimulatorResult result, Kind kind) {
        return result.timeline().stream().filter(ev -> ev.kind() == kind).toList();
    }

    // ── 기본 성질 ──

    @Test
    void initialTurretsAreRecordedFirst() {
        SimulatorResult result = run(5, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 20, true);
        List<ReplayEvent> t = result.timeline();

        for (int i = 0; i < 3; i++) {
            assertEquals(Kind.TURRET_READY, t.get(i).kind());
            assertEquals(Actor.TURRET, t.get(i).actor());
            assertEquals(i, t.get(i).slot());
            assertEquals(0, t.get(i).time(), 1e-9);
        }
    }

    @Test
    void timelineIsSortedByTime() {
        SimulatorResult result = run(5, 5, 3, UltimateChoice.RQ, List.of("liandrys"), 1, 2, 10, false);
        double prev = 0;
        for (ReplayEvent ev : result.timeline()) {
            assertTrue(ev.time() >= prev, "시간이 거꾸로 감: " + ev);
            prev = ev.time();
        }
    }

    @Test
    void hitsAddUpToDamageBySource() {
        SimulatorResult result = run(5, 5, 3, UltimateChoice.RW, List.of("liandrys", "shadowflame"), 3, 0, 20, true);
        Map<DamageSource, Double> sum = new EnumMap<>(DamageSource.class);
        for (ReplayEvent ev : of(result, Kind.HIT)) {
            sum.merge(ev.source(), ev.amount(), Double::sum);
        }
        for (DamageSource s : result.damageBySource().keySet()) {
            assertEquals(result.damageBySource().get(s), sum.get(s), 1e-6, s.name());
        }
    }

    @Test
    void baronHealthCurveGoesDownToZero() {
        SimulatorResult result = run(5, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 20, true);
        List<ReplayEvent> hits = of(result, Kind.HIT);

        double prev = 1.0;
        for (ReplayEvent ev : hits) {
            assertTrue(ev.healthRatio() <= prev + 1e-12);
            prev = ev.healthRatio();
        }
        ReplayEvent last = hits.get(hits.size() - 1);
        assertEquals(0, last.healthRatio(), 1e-12);
        assertEquals(result.killTime(), last.time(), 1e-9);
        assertEquals(44.57, result.killTime(), 0.01);   // AbilityHasteTest.zeroHasteIsUnchanged와 같은 조건
    }

    // ── 사용 횟수 ──

    @Test
    void rocketsCount() {
        // W 쿨 7초: 0, 7, …, 42초 → 7번, 44.57초 처치
        SimulatorResult result = run(5, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 20, true);
        assertEquals(7, result.skillCounts().w());
        assertEquals(7 * 312.8715, result.damageBySource().get(DamageSource.ROCKETS), 0.01);
        assertEquals(0, result.skillCounts().e());
        assertEquals(0, result.skillCounts().q());
    }

    @Test
    void turretPlacementCountsAsQ() {
        // TurretPlacementSimulatorTest와 같은 조건: 포탑 1 + 키트 2 → 0초 1번 칸, 1초 2번 칸에 설치. 269발에 처치
        SimulatorResult result = run(0, 0, 0, UltimateChoice.NONE, List.of(), 1, 2, 20, true);
        List<ReplayEvent> placed = of(result, Kind.TURRET_PLACED);

        assertEquals(2, result.skillCounts().q());
        assertEquals(1, placed.get(0).slot());
        assertEquals(0, placed.get(0).time(), 1e-9);
        assertEquals(2, placed.get(1).slot());
        assertEquals(1, placed.get(1).time(), 1e-9);
        assertEquals(269, result.skillCounts().turretShots());
    }

    @Test
    void countsUntilTime() {
        SimulatorResult result = run(5, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 20, true);
        assertEquals(1, SkillCounts.of(result.timeline(), 0).w());
        assertEquals(1, SkillCounts.of(result.timeline(), 6.99).w());
        assertEquals(2, SkillCounts.of(result.timeline(), 7).w());
    }

    @Test
    void rqPlacementCountsAsR() {
        SimulatorResult result = run(0, 0, 3, UltimateChoice.RQ, List.of(), 3, 0, 20, true);
        List<ReplayEvent> rq = of(result, Kind.RQ_PLACED);

        assertEquals(0, rq.get(0).time(), 1e-9);
        assertEquals(Actor.RQ, rq.get(0).actor());
        assertEquals(rq.size(), result.skillCounts().r());
        long rqHits = of(result, Kind.HIT).stream().filter(ev -> ev.actor() == Actor.RQ).count();
        assertEquals(rqHits, result.skillCounts().rqAttacks());
        assertTrue(rqHits > 0);
    }

    // ── 바론 공격과 사망 ──

    @Test
    void dummyTakesEveryBaronAttack() {
        SimulatorResult result = run(5, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 20, true);
        List<ReplayEvent> attacks = of(result, Kind.BARON_ATTACK);
        assertTrue(attacks.size() > 0);
        for (ReplayEvent ev : attacks) {
            assertEquals(Actor.DUMMY, ev.actor());
            assertEquals(1.0, ev.healthRatio(), 1e-12);
        }
    }

    @Test
    void turretDeathIsRecordedAfterLastHit() {
        // 병풍 없음: 바론이 포탑을 부순다. 부서진 칸에는 키트가 차면 다시 설치된다
        SimulatorResult result = run(0, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 19, false);
        List<ReplayEvent> t = result.timeline();
        List<ReplayEvent> died = of(result, Kind.TURRET_DIED);
        assertTrue(died.size() > 0);

        for (ReplayEvent d : died) {
            int i = t.indexOf(d);
            ReplayEvent before = t.get(i - 1);
            assertEquals(Kind.BARON_ATTACK, before.kind());
            assertEquals(Actor.TURRET, before.actor());
            assertEquals(d.slot(), before.slot());
            assertEquals(0, before.healthRatio(), 1e-12);
        }
        // 19초에 키트가 차서 부서진 칸에 다시 설치
        ReplayEvent firstPlaced = of(result, Kind.TURRET_PLACED).get(0);
        assertEquals(19, firstPlaced.time(), 1e-9);
        assertTrue(died.stream().anyMatch(d -> d.slot() == firstPlaced.slot() && d.time() <= 19));
    }

    @Test
    void heimerDeathIsLastEvent() {
        // 포탑 1개, 키트 없음, 병풍 없음 → 포탑이 부서지면 하이머가 맞다가 죽는다
        SimulatorResult result = run(0, 0, 0, UltimateChoice.NONE, List.of(), 1, 0, 20, false);
        assertEquals(Outcome.HEIMER_DEAD, result.outcome());
        List<ReplayEvent> t = result.timeline();
        assertEquals(Kind.HEIMER_DIED, t.get(t.size() - 1).kind());
        assertEquals(Actor.HEIMER, t.get(t.size() - 2).actor());
        assertEquals(0, t.get(t.size() - 2).healthRatio(), 1e-12);
    }

    // ── 막타 기준선 ──

    @Test
    void ultimateOneShotIsRwHit() {
        // 첫 RW는 0초, 체력 100%라 증감이 없다 → 기록된 피해 = 한 방 피해
        SimulatorResult result = run(5, 0, 3, UltimateChoice.RW, List.of(), 3, 0, 20, true);
        ReplayEvent firstRw = of(result, Kind.HIT).stream()
                .filter(ev -> ev.source() == DamageSource.RW_ROCKETS).findFirst().orElseThrow();
        assertEquals(firstRw.amount(), result.ultimateOneShot(), 1e-9);
    }

    @Test
    void ultimateOneShotAssumesShadowflame() {
        SimulatorResult result = run(5, 0, 3, UltimateChoice.RW, List.of("shadowflame"), 3, 0, 20, true);
        ReplayEvent firstRw = of(result, Kind.HIT).stream()
                .filter(ev -> ev.source() == DamageSource.RW_ROCKETS).findFirst().orElseThrow();
        assertEquals(firstRw.amount() * 1.2, result.ultimateOneShot(), 1e-9);
    }

    @Test
    void noUltimateMeansNoBaseline() {
        assertEquals(0, run(5, 0, 3, UltimateChoice.RQ, List.of(), 3, 0, 20, true).ultimateOneShot(), 1e-12);
        assertEquals(0, run(5, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 20, true).ultimateOneShot(), 1e-12);
    }

    @Test
    void totals() {
        SimulatorResult result = run(5, 0, 0, UltimateChoice.NONE, List.of(), 3, 0, 20, true);
        assertEquals(result.baronStartHealth(), result.totalDamage(), 1e-6);   // 처치했으면 총 피해 = 바론 체력
    }
}
