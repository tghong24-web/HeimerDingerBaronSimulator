package com.hongtg.baronsim.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(10, data.items().size());
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
    void unknownIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> data.item("ludens"));
    }
}
