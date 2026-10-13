package com.hongtg.baronsim.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.data.ItemData.Item;

/** items.json 로딩 테스트. 기대값은 26.20 상점 툴팁 (2026-10-13). */
class ItemDataTest {

    private final ItemData data = DataLoader.loadItems();

    @Test
    void loadsAllItems() {
        assertEquals("26.20", data.patch());
        assertEquals(21, data.items().size());
    }

    @Test
    void zhonyasAndBanshees() {
        Item z = data.item("zhonyas");
        assertEquals(105, z.ap(), 1e-9);
        assertEquals(50, z.armor(), 1e-9);
        assertEquals(0, z.magicResist(), 1e-9);

        Item b = data.item("banshees");
        assertEquals(105, b.ap(), 1e-9);
        assertEquals(40, b.magicResist(), 1e-9);
        assertEquals(0, b.armor(), 1e-9);
    }

    @Test
    void hasteAndManaItems() {
        Item blackfire = data.item("blackfire");
        assertEquals(80, blackfire.ap(), 1e-9);
        assertEquals(600, blackfire.mana(), 1e-9);
        assertEquals(20, blackfire.abilityHaste(), 1e-9);
        assertEquals(0.04, blackfire.apAmp(), 1e-9);   // 어둠불꽃: 바론이 불길에 걸리면 주문력 4%

        Item m = data.item("materialization");
        assertEquals(90, m.ap(), 1e-9);
        assertEquals(300, m.mana(), 1e-9);
        assertEquals(10, m.abilityHaste(), 1e-9);
    }

    @Test
    void liandrysHealth() {
        Item l = data.item("liandrys");
        assertEquals(60, l.ap(), 1e-9);
        assertEquals(300, l.health(), 1e-9);
    }

    @Test
    void penetrationItems() {
        assertEquals(0.40, data.item("voidstaff").percentMagicPen(), 1e-9);
        assertEquals(95, data.item("voidstaff").ap(), 1e-9);
        assertEquals(15, data.item("shadowflame").flatMagicPen(), 1e-9);
        assertEquals(110, data.item("shadowflame").ap(), 1e-9);
        assertEquals(12, data.item("sorcs").flatMagicPen(), 1e-9);
        assertEquals(20, data.item("spellslinger").flatMagicPen(), 1e-9);
        assertEquals(0.08, data.item("spellslinger").percentMagicPen(), 1e-9);
    }

    @Test
    void rabadonsAmp() {
        Item r = data.item("rabadons");
        assertEquals(130, r.ap(), 1e-9);
        assertEquals(0.30, r.apAmp(), 1e-9);
    }

    @Test
    void bootsCost() {
        // 주문투척자의 신발은 마법사의 신발에서 강화되는 신발이라 같은 1100으로 둔다
        assertEquals(1100, data.item("sorcs").cost());
        assertEquals(1100, data.item("spellslinger").cost());
    }

    @Test
    void basicBoots() {
        // 기본 신발: 이동 속도만 있어서 계산 스탯은 전부 0, 칸만 차지한다
        Item boots = data.item("boots");
        assertTrue(boots.boots());
        assertEquals(300, boots.cost());
        assertEquals(0, boots.ap(), 1e-9);
        assertEquals(0, boots.flatMagicPen(), 1e-9);
    }

    @Test
    void bootsFlag() {
        assertTrue(data.item("sorcs").boots());
        assertTrue(data.item("spellslinger").boots());
        assertFalse(data.item("rabadons").boots());
    }

    @Test
    void missingStatsAreZero() {
        Item sorcs = data.item("sorcs");
        assertEquals(0, sorcs.ap(), 1e-9);
        assertEquals(0, sorcs.apAmp(), 1e-9);
        assertEquals(0, sorcs.abilityHaste(), 1e-9);
    }

    @Test
    void components() {
        assertEquals(30, data.item("fatedashes").ap(), 1e-9);
        assertEquals(900, data.item("fatedashes").cost());
        assertEquals(45, data.item("alternator").ap(), 1e-9);
        assertEquals(25, data.item("blightingjewel").ap(), 1e-9);
        assertEquals(0.13, data.item("blightingjewel").percentMagicPen(), 1e-9);
        assertEquals(40, data.item("lostchapter").ap(), 1e-9);
        assertEquals(300, data.item("lostchapter").mana(), 1e-9);
        assertEquals(10, data.item("lostchapter").abilityHaste(), 1e-9);
        assertEquals(40, data.item("seekers").ap(), 1e-9);
        assertEquals(25, data.item("seekers").armor(), 1e-9);
        assertEquals(20, data.item("amptome").ap(), 1e-9);
        assertEquals(400, data.item("amptome").cost());
        assertEquals(45, data.item("blastingwand").ap(), 1e-9);
        assertEquals(65, data.item("largerod").ap(), 1e-9);
    }

    @Test
    void legendaryFlag() {
        assertTrue(data.item("rabadons").legendary());
        assertTrue(data.item("zhonyas").legendary());
        assertFalse(data.item("largerod").legendary());
        assertFalse(data.item("amptome").legendary());
        assertFalse(data.item("sorcs").legendary());
    }

    @Test
    void uniqueGroups() {
        // 같은 "고유" 그룹은 하나만 들 수 있다
        assertEquals("역병", data.item("voidstaff").uniqueGroup());
        assertEquals("역병", data.item("blightingjewel").uniqueGroup());
        assertEquals("영광", data.item("darkseal").uniqueGroup());
        assertEquals("영광", data.item("mejais").uniqueGroup());
        assertEquals("경직", data.item("seekers").uniqueGroup());
        assertEquals("경직", data.item("zhonyas").uniqueGroup());
        assertEquals("무효화", data.item("banshees").uniqueGroup());
        assertNull(data.item("rabadons").uniqueGroup());
    }

    @Test
    void gloryStacks() {
        Item seal = data.item("darkseal");
        assertEquals(15, seal.ap(), 1e-9);
        assertEquals(50, seal.health(), 1e-9);
        assertEquals(10, seal.maxStacks());
        assertEquals(4, seal.apPerStack(), 1e-9);

        Item mejai = data.item("mejais");
        assertEquals(20, mejai.ap(), 1e-9);
        assertEquals(100, mejai.health(), 1e-9);
        assertEquals(25, mejai.maxStacks());
        assertEquals(5, mejai.apPerStack(), 1e-9);

        assertEquals(0, data.item("rabadons").maxStacks());
    }

    @Test
    void unknownIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> data.item("ludens"));
    }
}
