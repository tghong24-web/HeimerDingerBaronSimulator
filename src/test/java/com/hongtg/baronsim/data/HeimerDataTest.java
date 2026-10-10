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
    void rqTurretByRank() {
        assertEquals(120, heimer.rqTurret().shotDamageAtRank(3), 0.0001);
        assertEquals(180, heimer.rqTurret().beamDamageAtRank(3), 0.0001);
    }

    @Test
    void rejectsInvalidRank() {
        assertThrows(IllegalArgumentException.class, () -> heimer.turret().shotDamageAtRank(0));
        assertThrows(IllegalArgumentException.class, () -> heimer.turret().shotDamageAtRank(6));
        assertThrows(IllegalArgumentException.class, () -> heimer.rwRockets().firstRocketDamageAtRank(4));
    }

    /** 위키의 W 단일 대상 최대 피해: 90/135/180/225/270 (+103% AP) */
    @Test
    void rocketsAddUpToWikiMaximum() {
        HeimerData.RocketsData w = heimer.rockets();
        int extra = w.rocketCount() - 1;

        assertEquals(90, w.firstRocketDamageAtRank(1) + extra * w.extraRocketDamageAtRank(1), 0.0001);
        assertEquals(270, w.firstRocketDamageAtRank(5) + extra * w.extraRocketDamageAtRank(5), 0.0001);
        assertEquals(1.03, w.firstRocketApRatio() + extra * w.extraRocketApRatio(), 0.0001);
    }

    /** 위키의 R+W 단일 대상 최대 피해: 503/697.5/892 (+183% AP) */
    @Test
    void rwRocketsAddsUpToWikiMaximum() {
        HeimerData.RwRocketsData rw = heimer.rwRockets();
        int later = rw.rocketCount() - 5;

        assertEquals(503, rw.firstRocketDamageAtRank(1) + 4 * rw.rockets2to5DamageAtRank(1)
                + later * rw.laterRocketDamageAtRank(1), 0.0001);
        assertEquals(697.5, rw.firstRocketDamageAtRank(2) + 4 * rw.rockets2to5DamageAtRank(2)
                + later * rw.laterRocketDamageAtRank(2), 0.0001);
        assertEquals(892, rw.firstRocketDamageAtRank(3) + 4 * rw.rockets2to5DamageAtRank(3)
                + later * rw.laterRocketDamageAtRank(3), 0.0001);
        assertEquals(1.83, rw.firstRocketApRatio() + 4 * rw.rockets2to5ApRatio()
                + later * rw.laterRocketApRatio(), 0.0001);
    }

    @Test
    void grenadeAndUpgrade() {
        assertEquals(220, heimer.grenade().damageAtRank(5), 0.0001);
        assertEquals(0.60, heimer.grenade().apRatio(), 0.0001);
        assertEquals(11, heimer.grenade().cooldownSeconds(), 0.0001);
        assertEquals(70, heimer.upgrade().cooldownAtRank(3), 0.0001);
    }

    /** 실측: 평타 5 → 광선 1 → 평타 2 */
    @Test
    void rqTurretAttackPatternMatchesMeasurement() {
        HeimerData.RqTurretData rq = heimer.rqTurret();

        assertEquals(8, rq.attackCount());
        for (int i = 0; i < 5; i++) {
            assertEquals(HeimerData.RqTurretAttack.SHOT, rq.attackAt(i));
        }
        assertEquals(HeimerData.RqTurretAttack.BEAM, rq.attackAt(5));
        assertEquals(HeimerData.RqTurretAttack.SHOT, rq.attackAt(6));
        assertEquals(HeimerData.RqTurretAttack.SHOT, rq.attackAt(7));
        assertThrows(IllegalArgumentException.class, () -> rq.attackAt(8));
    }

    @Test
    void reGrenadeHitsSingleMonsterTwice() {
        assertEquals(300, heimer.reGrenade().damageAtRank(3), 0.0001);
        assertEquals(2, heimer.reGrenade().maxHitsOnSingleMonster());
    }

    /** 하이머 기본 스탯도 바론과 같은 StatGrowth 공식을 쓴다. 18레벨은 기본값 + 성장값 × 17. */
    @Test
    void championStatsUseStatGrowth() {
        assertEquals(558, heimer.stats().health().valueAt(1), 0.0001);
        assertEquals(558 + 105 * 17, heimer.stats().health().valueAt(18), 0.0001);
    }
}
