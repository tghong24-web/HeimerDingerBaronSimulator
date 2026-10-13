package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.data.ItemData;

/**
 * 아이템 스탯 합산 테스트 (8-2단계).
 * 기대값은 items.json 툴팁 수치로 손계산했다.
 *   최종 AP = (아이템 AP + 영광 중첩 AP + 추가 AP) × (1 + 증폭)
 *   % 관통 = 1 − Π(1 − p), 고정 관통은 합
 */
class BuildCalculatorTest {

    private static final ItemData DATA = DataLoader.loadItems();

    private FinalStats calc(RoleQuest quest, int gloryStacks, String... items) {
        return BuildCalculator.calculate(DATA, new Build(List.of(items), 0, 0, 0, 0, gloryStacks), quest);
    }

    private FinalStats calc(String... items) {
        return calc(RoleQuest.NONE, 0, items);
    }

    // ── 합산 ──

    @Test
    void emptyBuildIsAllZero() {
        FinalStats s = calc();
        assertEquals(0, s.ap(), 1e-9);
        assertEquals(0, s.flatPen(), 1e-9);
        assertEquals(0, s.perPen(), 1e-9);
        assertEquals(0, s.abilityHaste(), 1e-9);
    }

    @Test
    void defensiveStats() {
        // 존야 + 밴시 + 리안드리
        FinalStats s = calc("zhonyas", "banshees", "liandrys");
        assertEquals(105 + 105 + 60, s.ap(), 1e-9);
        assertEquals(50, s.armor(), 1e-9);
        assertEquals(40, s.magicResist(), 1e-9);
        assertEquals(300, s.health(), 1e-9);
    }

    @Test
    void abilityHasteAdds() {
        // 어둠불꽃 20 + 사라진 양피지 10 + 추가 5
        FinalStats s = BuildCalculator.calculate(DATA,
                new Build(List.of("blackfire", "lostchapter"), 0, 5, 0, 0, 0), RoleQuest.NONE);
        assertEquals(35, s.abilityHaste(), 1e-9);
    }

    @Test
    void rabadonAmplifiesAllAp() {
        // (130 + 65 + 추가 10) × 1.3 = 266.5
        FinalStats s = BuildCalculator.calculate(DATA,
                new Build(List.of("rabadons", "largerod"), 10, 0, 0, 0, 0), RoleQuest.NONE);
        assertEquals(266.5, s.ap(), 1e-9);
    }

    @Test
    void blackfireAmpAddsToRabadon() {
        // 증폭은 더한다: (130 + 80) × (1 + 0.30 + 0.04) = 281.4 (곱하면 283.92)
        assertEquals(281.4, calc("rabadons", "blackfire").ap(), 1e-9);
    }

    @Test
    void blackfireAmpMatchesInGameMeasurement() {
        // 2026-10-13 실측: 라바돈 + 어둠불꽃 + 룬 파편 AP 18
        //   불길 전 228 × 1.30 = 296.4 → 화면 296, 불길 후 228 × 1.34 = 305.52 → 화면 306 (곱이면 308)
        FinalStats s = BuildCalculator.calculate(DATA,
                new Build(List.of("rabadons", "blackfire"), 18, 0, 0, 0, 0), RoleQuest.NONE);
        assertEquals(305.52, s.ap(), 1e-9);
        assertEquals(306, Math.round(s.ap()));
    }

    @Test
    void blackfireAloneAmplifiesFourPercent() {
        // (80 + 추가 220) × 1.04 = 312
        FinalStats s = BuildCalculator.calculate(DATA,
                new Build(List.of("blackfire"), 220, 0, 0, 0, 0), RoleQuest.NONE);
        assertEquals(312, s.ap(), 1e-9);
    }

    @Test
    void flatPenAddsUp() {
        // 그림자불꽃 15 + 마법사의 신발 12 = 27, % 관통은 공허 40%
        FinalStats s = calc("voidstaff", "shadowflame", "sorcs");
        assertEquals(27, s.flatPen(), 1e-9);
        assertEquals(0.40, s.perPen(), 1e-9);
    }

    @Test
    void percentPenMultiplies() {
        // 공허 40% + 주문투척자 8% → 1 − 0.6 × 0.92 = 0.448 (게임 표시 45%)
        FinalStats s = calc(RoleQuest.MID, 0, "voidstaff", "spellslinger");
        assertEquals(0.448, s.perPen(), 1e-9);
        assertEquals(20, s.flatPen(), 1e-9);
    }

    @Test
    void bonusPercentPenAlsoMultiplies() {
        // 공허 40% + 추가 10% → 1 − 0.6 × 0.9 = 0.46
        FinalStats s = BuildCalculator.calculate(DATA,
                new Build(List.of("voidstaff"), 0, 0, 3, 0.10, 0), RoleQuest.NONE);
        assertEquals(0.46, s.perPen(), 1e-9);
        assertEquals(3, s.flatPen(), 1e-9);
    }

    @Test
    void componentsCanBeDuplicated() {
        // 쓸데없이 큰 지팡이 2개 = 130
        assertEquals(130, calc("largerod", "largerod").ap(), 1e-9);
    }

    // ── 영광 중첩 ──

    @Test
    void darkSealStacks() {
        // 15 + 7 × 4 = 43, 체력 50
        FinalStats s = calc(RoleQuest.NONE, 7, "darkseal");
        assertEquals(43, s.ap(), 1e-9);
        assertEquals(50, s.health(), 1e-9);
    }

    @Test
    void mejaiFullStacks() {
        // 20 + 25 × 5 = 145
        assertEquals(145, calc(RoleQuest.NONE, 25, "mejais").ap(), 1e-9);
    }

    @Test
    void gloryStacksAreAmplifiedByRabadon() {
        // (15 + 28 + 130) × 1.3 = 224.9
        assertEquals(224.9, calc(RoleQuest.NONE, 7, "darkseal", "rabadons").ap(), 1e-9);
    }

    @Test
    void gloryStacksOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.NONE, 11, "darkseal"));
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.NONE, 26, "mejais"));
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.NONE, -1, "darkseal"));
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.NONE, 1, "rabadons"),
                "영광 아이템 없이 중첩을 넣으면 거부");
    }

    // ── 조합 규칙 ──

    @Test
    void sameLegendaryTwiceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calc("rabadons", "rabadons"));
    }

    @Test
    void sameUniqueGroupIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calc("voidstaff", "blightingjewel"));   // 역병
        assertThrows(IllegalArgumentException.class, () -> calc("darkseal", "mejais"));            // 영광
        assertThrows(IllegalArgumentException.class, () -> calc("zhonyas", "seekers"));            // 경직
    }

    @Test
    void spellslingerNeedsMidQuest() {
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.NONE, 0, "spellslinger"));
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.BOT, 0, "spellslinger"));
        calc(RoleQuest.MID, 0, "spellslinger");
    }

    @Test
    void onlyOneBoots() {
        assertThrows(IllegalArgumentException.class, () -> calc("sorcs", "boots"));
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.BOT, 0, "sorcs", "boots"));
    }

    @Test
    void sixSlotsIncludingBoots() {
        // 아이템 5 + 신발 = 6 통과, 아이템 6 + 신발 = 7 거부
        calc("zhonyas", "rabadons", "voidstaff", "liandrys", "banshees", "sorcs");
        assertThrows(IllegalArgumentException.class,
                () -> calc("zhonyas", "rabadons", "voidstaff", "liandrys", "banshees", "shadowflame", "sorcs"));
        assertThrows(IllegalArgumentException.class,
                () -> calc("largerod", "largerod", "largerod", "largerod", "largerod", "largerod", "largerod"));
    }

    @Test
    void botQuestSeparatesBootsSlot() {
        // 원딜 퀘스트: 아이템 6 + 신발 1 통과, 아이템 7 거부
        FinalStats s = calc(RoleQuest.BOT, 0,
                "zhonyas", "rabadons", "voidstaff", "liandrys", "banshees", "shadowflame", "sorcs");
        assertEquals((105 + 130 + 95 + 60 + 105 + 110) * 1.3, s.ap(), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> calc(RoleQuest.BOT, 0,
                "largerod", "largerod", "largerod", "largerod", "largerod", "largerod", "largerod"));
    }

    @Test
    void unknownItemIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calc("ludens"));
    }
}
