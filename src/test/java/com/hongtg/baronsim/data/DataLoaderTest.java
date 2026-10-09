package com.hongtg.baronsim.data;

import com.hongtg.baronsim.model.BaronStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataLoaderTest {

    @Test
    void loadsBaronJson() {
        BaronData baron = DataLoader.loadBaron();

        assertEquals(11, baron.minLevel());
        assertEquals(18, baron.maxLevel());
        assertEquals(0.5, baron.gaze().damageReduction(), 0.0001);
        assertEquals(100, baron.voidCorruption().maxStacks());
    }

    @Test
    void scalerFromJsonMatchesWikiValues() {
        BaronStats level11 = DataLoader.loadBaron().toScaler().statsAt(11);
        BaronStats level18 = DataLoader.loadBaron().toScaler().statsAt(18);

        assertEquals(17791.75, level11.health(), 0.01);
        assertEquals(19190, level18.health(), 0.01);
        assertEquals(515, level18.attackDamage(), 0.01);
    }
}
