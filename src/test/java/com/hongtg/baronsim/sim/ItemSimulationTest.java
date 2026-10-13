package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 아이템·역할군 퀘스트를 넣은 Simulator 테스트 (8-2단계).
 * 아이템 스탯이 "추가 스탯"으로 직접 넣은 것과 똑같이 시뮬레이션에 반영되는지 비교한다.
 * 공통 조건: 바론 15레벨, Q5, 병풍 있음, 포탑 3개.
 */
class ItemSimulationTest {

    private final Simulator simulator = new Simulator();

    private SimulatorResult run(int heimerLevel, int wLevel, Build build, RoleQuest quest, boolean hasDummy,
            double restKitSeconds) {
        return simulator.run(new SimulationInput(15, heimerLevel, 5, wLevel, 0, 0, UltimateChoice.NONE, build,
                3, 0, restKitSeconds, RqTurretPosition.FRONT, hasDummy, 300, quest));
    }

    private SimulatorResult run(Build build) {
        return run(13, 5, build, RoleQuest.NONE, true, 20);
    }

    // ── 아이템 스탯 = 같은 양의 추가 스탯 ──

    @Test
    void itemApWorksLikeBonusAp() {
        // 쓸데없이 큰 지팡이 4개(260) + 증폭의 고서 2개(40) = AP 300 → 추가 AP 300과 같은 결과
        SimulatorResult items = run(new Build(
                List.of("largerod", "largerod", "largerod", "largerod", "amptome", "amptome"), 0, 0, 0, 0, 0));
        SimulatorResult bonus = run(new Build(List.of(), 300, 0, 0, 0, 0));

        assertEquals(bonus.killTime(), items.killTime(), 1e-9);
        assertEquals(44.57, items.killTime(), 0.01);   // SkillSimulationTest: 포탑 3 + W5, AP 300
    }

    @Test
    void itemPenetrationWorksLikeBonusPenetration() {
        // 공허의 지팡이(AP 95, % 관통 40%) + 마법사의 신발(고정 12) = 추가 AP 95, 40%, 12
        SimulatorResult items = run(new Build(List.of("voidstaff", "sorcs"), 0, 0, 0, 0, 0));
        SimulatorResult bonus = run(new Build(List.of(), 95, 0, 12, 0.40, 0));

        assertEquals(bonus.killTime(), items.killTime(), 1e-9);
        assertEquals(bonus.damageBySource().get(DamageSource.ROCKETS),
                items.damageBySource().get(DamageSource.ROCKETS), 1e-9);
    }

    @Test
    void blackfireHasteAndAmpWorkLikeBonusStats() {
        // 어둠불꽃 횃불(AP 80, 가속 20, 주문력 4% 증폭) + 추가 AP 220 → (300) × 1.04 = AP 312, 가속 20
        // 가속 20이면 키트 충전이 20 × 100/120 ≈ 16.7초라 남은 충전 시간은 그 이하로 넣는다
        SimulatorResult items = run(13, 5, new Build(List.of("blackfire"), 220, 0, 0, 0, 0), RoleQuest.NONE, true, 10);
        SimulatorResult bonus = run(13, 5, new Build(List.of(), 312, 20, 0, 0, 0), RoleQuest.NONE, true, 10);

        assertEquals(bonus.killTime(), items.killTime(), 1e-9);
    }

    @Test
    void rabadonAmplifiesSkillDamage() {
        // 라바돈(130) + 추가 100 → (230) × 1.3 = 299 → AP 299와 같은 결과
        SimulatorResult items = run(new Build(List.of("rabadons"), 100, 0, 0, 0, 0));
        SimulatorResult bonus = run(new Build(List.of(), 299, 0, 0, 0, 0));

        assertEquals(bonus.killTime(), items.killTime(), 1e-9);
        assertEquals(bonus.damageBySource().get(DamageSource.ROCKETS),
                items.damageBySource().get(DamageSource.ROCKETS), 1e-6);
    }

    // ── 하이머 체력 (병풍 없음) ──

    @Test
    void itemHealthKeepsHeimerAliveLonger() {
        // NoDummyTankingTest와 같은 조건(하이머 18레벨, AP 300, 남은 충전 19초): 아이템 없이 48.0초 사망 (11방)
        // 리안드리 체력 +300 → 2643 / 231.25 → 12방 → 49.6초 사망. AP는 리안드리 60 + 추가 240 = 300으로 맞춘다
        SimulatorResult noItem = run(18, 0, new Build(List.of(), 300, 0, 0, 0, 0), RoleQuest.NONE, false, 19);
        SimulatorResult liandry = run(18, 0, new Build(List.of("liandrys"), 240, 0, 0, 0, 0), RoleQuest.NONE, false, 19);

        assertEquals(Outcome.HEIMER_DEAD, noItem.outcome());
        assertEquals(48.0, noItem.killTime(), 0.001);
        assertEquals(Outcome.HEIMER_DEAD, liandry.outcome());
        assertEquals(49.6, liandry.killTime(), 0.001);
    }

    // ── 역할군 퀘스트 ──

    @Test
    void levelNineteenNeedsTopQuest() {
        Build b = new Build(List.of(), 300, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> run(19, 0, b, RoleQuest.NONE, true, 20));
        assertThrows(IllegalArgumentException.class, () -> run(20, 0, b, RoleQuest.MID, true, 20));
        run(19, 0, b, RoleQuest.TOP, true, 20);
        run(20, 0, b, RoleQuest.TOP, true, 20);
    }

    @Test
    void spellslingerThroughSimulatorNeedsMidQuest() {
        Build b = new Build(List.of("spellslinger"), 300, 0, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> run(13, 5, b, RoleQuest.NONE, true, 20));
        run(13, 5, b, RoleQuest.MID, true, 20);
    }

    @Test
    void invalidBuildIsRejectedBySimulator() {
        assertThrows(IllegalArgumentException.class,
                () -> run(new Build(List.of("rabadons", "rabadons"), 0, 0, 0, 0, 0)));
    }

    @Test
    void nullBuildOrQuestIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SimulationInput(15, 13, 5, 0, 0, 0,
                UltimateChoice.NONE, null, 3, 0, 20, RqTurretPosition.FRONT, true, 300, RoleQuest.NONE));
        assertThrows(IllegalArgumentException.class, () -> new SimulationInput(15, 13, 5, 0, 0, 0,
                UltimateChoice.NONE, new Build(List.of(), 0, 0, 0, 0, 0), 3, 0, 20, RqTurretPosition.FRONT, true,
                300, null));
    }
}
