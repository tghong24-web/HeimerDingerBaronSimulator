package com.hongtg.baronsim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.sim.DamageSource;
import com.hongtg.baronsim.sim.ReplayEvent;
import com.hongtg.baronsim.sim.ReplayEvent.Actor;
import com.hongtg.baronsim.sim.ReplayEvent.Kind;

/**
 * 리플레이 장면 계산 테스트 (9-4단계). 그리기 없이 "t초에 무엇이 살아 있고 체력이 얼마인지"만 본다.
 */
class ReplayFrameTest {

    private static ReplayEvent ev(double t, Kind kind, Actor actor, int slot, DamageSource src, double amount, double ratio) {
        return new ReplayEvent(t, kind, actor, slot, src, amount, ratio);
    }

    /** 포탑 2개로 시작 → 0.5초 RQ → 1초 포탑 평타 → 1.5초 바론이 0번 포탑 반피 → 2초 0번 파괴 → 3초 0번 재설치
     *  → 4초 RQ 끝 → 5초 하이머 30% → 6초 하이머 사망 */
    private final List<ReplayEvent> timeline = List.of(
            ev(0, Kind.TURRET_READY, Actor.TURRET, 0, null, 0, 1),
            ev(0, Kind.TURRET_READY, Actor.TURRET, 1, null, 0, 1),
            ev(0.5, Kind.RQ_PLACED, Actor.RQ, -1, null, 0, 1),
            ev(1, Kind.HIT, Actor.TURRET, 1, DamageSource.TURRET_SHOT, 70, 0.9),
            ev(1.5, Kind.BARON_ATTACK, Actor.TURRET, 0, null, 300, 0.5),
            ev(2, Kind.BARON_ATTACK, Actor.TURRET, 0, null, 300, 0),
            ev(2, Kind.TURRET_DIED, Actor.TURRET, 0, null, 0, 0),
            ev(3, Kind.TURRET_PLACED, Actor.TURRET, 0, null, 0, 1),
            ev(4, Kind.RQ_GONE, Actor.RQ, -1, null, 0, 0),
            ev(5, Kind.BARON_ATTACK, Actor.HEIMER, -1, null, 500, 0.3),
            ev(6, Kind.BARON_ATTACK, Actor.HEIMER, -1, null, 500, 0),
            ev(6, Kind.HEIMER_DIED, Actor.HEIMER, -1, null, 0, 0));

    @Test
    void startState() {
        ReplayFrame f = ReplayFrame.at(timeline, 0);
        assertEquals(1, f.baronRatio, 1e-12);
        assertEquals(1, f.turretRatio[0], 1e-12);
        assertEquals(1, f.turretRatio[1], 1e-12);
        assertEquals(-1, f.turretRatio[2], 1e-12);   // 빈 칸
        assertEquals(-1, f.rqRatio, 1e-12);          // RQ는 0.5초부터
    }

    @Test
    void baronHealthFollowsLastHit() {
        assertEquals(1, ReplayFrame.at(timeline, 0.99).baronRatio, 1e-12);
        assertEquals(0.9, ReplayFrame.at(timeline, 1).baronRatio, 1e-12);
        assertEquals(0.9, ReplayFrame.at(timeline, 10).baronRatio, 1e-12);
    }

    @Test
    void turretDiesAndComesBack() {
        assertEquals(0.5, ReplayFrame.at(timeline, 1.7).turretRatio[0], 1e-12);
        assertEquals(-1, ReplayFrame.at(timeline, 2.5).turretRatio[0], 1e-12);
        assertEquals(1, ReplayFrame.at(timeline, 3).turretRatio[0], 1e-12);
    }

    @Test
    void rqLifetime() {
        assertEquals(1, ReplayFrame.at(timeline, 0.5).rqRatio, 1e-12);
        assertEquals(1, ReplayFrame.at(timeline, 3.9).rqRatio, 1e-12);
        assertEquals(-1, ReplayFrame.at(timeline, 4).rqRatio, 1e-12);
    }

    @Test
    void heimerDeath() {
        ReplayFrame before = ReplayFrame.at(timeline, 5.5);
        assertEquals(0.3, before.heimerRatio, 1e-12);
        assertFalse(before.heimerDead);
        ReplayFrame after = ReplayFrame.at(timeline, 6);
        assertTrue(after.heimerDead);
        assertEquals(0, after.heimerRatio, 1e-12);
    }

    @Test
    void recentEventsOnlyNearNow() {
        // 공격선은 0.25초, 피해 숫자는 0.8초 동안만
        assertEquals(1, ReplayFrame.at(timeline, 1.2).recentHits.size());
        assertEquals(0, ReplayFrame.at(timeline, 1.9).recentHits.size());
        assertEquals(1, ReplayFrame.at(timeline, 1.6).recentBaronAttacks.size());
        assertEquals(0, ReplayFrame.at(timeline, 1.8).recentBaronAttacks.size());
    }
}
