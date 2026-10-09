package com.hongtg.baronsim.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeimerDataTest {

    private final HeimerData heimer = DataLoader.loadHeimer();

    @Test
    void turretShotDamageByRank() {
        assertEquals(7, heimer.turret().shotDamageAtRank(1), 0.0001);
        assertEquals(23, heimer.turret().shotDamageAtRank(5), 0.0001);
        assertEquals(0.35, heimer.turret().shotApRatio(), 0.0001);
    }

    @Test
    void turretAttackInterval() {
        assertEquals(1 / 1.75, heimer.turret().attackIntervalSeconds(), 0.0001);
    }

    @Test
    void apexTurretByRank() {
        assertEquals(120, heimer.apexTurret().shotDamageAtRank(3), 0.0001);
        assertEquals(180, heimer.apexTurret().beamDamageAtRank(3), 0.0001);
    }

    @Test
    void rejectsInvalidRank() {
        assertThrows(IllegalArgumentException.class, () -> heimer.turret().shotDamageAtRank(0));
        assertThrows(IllegalArgumentException.class, () -> heimer.turret().shotDamageAtRank(6));
    }
}
