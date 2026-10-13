package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * RQ 터렛을 바론의 대상으로 (6단계 ⑤).
 * 병풍 없음에서 바론의 대상 순서:
 *   FRONT: RQ 터렛 → 일반 포탑(먼저 깐 순) → 하이머
 *   BACK : 일반 포탑(먼저 깐 순) → RQ 터렛 → 하이머
 * RQ 터렛이 없거나(null, 만료) 죽었으면 건너뛴다.
 */
class RqTurretTargetTest {

    private final Turret t1 = new Turret(775, 80, 0);
    private final Turret t2 = new Turret(775, 80, 0);
    private final Turret rq = new Turret(1000, 50, 0);
    private final Heimer heimer = new Heimer(2343, 90.4, 0);

    private CombatState state(List<Turret> turrets, RqTurretPosition position) {
        return new CombatState(1_000_000, new NoDummyTankingStrategy(turrets, heimer, position), heimer, 0.5,
                new TurretKits(0, 3, 20), turrets, new TurretSpec(775, 80, 0, 0, 1.0, 1.0, 3), new ArrayList<>());
    }

    private void kill(Turret t) {
        t.takeDamage(1_000_000);
    }

    // ── 대상 선택만 ──

    @Test
    void frontRqTurretComesBeforeTurrets() {
        CombatState s = state(new ArrayList<>(List.of(t1, t2)), RqTurretPosition.FRONT);
        s.setRQ(rq);

        assertSame(rq, s.tankingStrategy().currentTarget(s));
    }

    @Test
    void backRqTurretComesAfterTurrets() {
        CombatState s = state(new ArrayList<>(List.of(t1, t2)), RqTurretPosition.BACK);
        s.setRQ(rq);

        assertSame(t1, s.tankingStrategy().currentTarget(s));
        kill(t1);
        assertSame(t2, s.tankingStrategy().currentTarget(s));
        kill(t2);
        assertSame(rq, s.tankingStrategy().currentTarget(s), "포탑이 다 죽으면 하이머보다 RQ 터렛이 먼저");
    }

    @Test
    void deadRqTurretIsSkipped() {
        CombatState front = state(new ArrayList<>(List.of(t1)), RqTurretPosition.FRONT);
        front.setRQ(rq);
        kill(rq);
        assertSame(t1, front.tankingStrategy().currentTarget(front));

        kill(t1);
        assertSame(heimer, front.tankingStrategy().currentTarget(front));
    }

    @Test
    void noRqTurretMeansOldOrder() {
        // RQ 터렛이 없을 때(null)는 FRONT/BACK 상관없이 포탑 → 하이머
        CombatState s = state(new ArrayList<>(List.of(t1)), RqTurretPosition.FRONT);

        assertSame(t1, s.tankingStrategy().currentTarget(s));
        kill(t1);
        assertSame(heimer, s.tankingStrategy().currentTarget(s));
    }

    @Test
    void expiredRqTurretIsNotTargeted() {
        CombatState s = state(new ArrayList<>(), RqTurretPosition.FRONT);
        s.setRQ(rq);
        new RQExpireEvent(8, rq).apply(s);

        assertSame(heimer, s.tankingStrategy().currentTarget(s));
    }

    // ── 바론 평타와 함께 ──

    /** 바론 15레벨 평타 440.3, 1.6초마다. 0초부터 시작하도록 예약한다. */
    private void startBaron(CombatState s) {
        s.schedule(new BaronAttackEvent(0, 440.3, 1.6));
    }

    /** timeLimit초까지 돌린다. 넘어간 이벤트는 다시 넣어 두어 이어서 돌릴 수 있게 한다. */
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

    @Test
    void baronHitsFrontRqTurretUntilItDies() {
        // RQ 터렛 체력 1000, 방어력 50 → 한 방 440.3 * 100/150 ≈ 293.53 → 0, 1.6, 3.2, 4.8초 4방에 사망
        // 그다음 6.4초 평타부터 포탑1 (한 방 ≈ 244.61)
        CombatState s = state(new ArrayList<>(List.of(t1)), RqTurretPosition.FRONT);
        s.setRQ(rq);
        startBaron(s);

        runUntil(s, 3.3);
        assertEquals(1000 - 3 * 293.5333, rq.health(), 0.01);
        assertEquals(775, t1.health(), 1e-9);

        runUntil(s, 6.5);
        assertTrue(rq.isDead());
        assertEquals(775 - 244.6111, t1.health(), 0.01);
    }

    @Test
    void baronGazeMovesToRqTurret() {
        CombatState s = state(new ArrayList<>(List.of(t1)), RqTurretPosition.FRONT);
        s.setRQ(rq);
        startBaron(s);
        runUntil(s, 0.5);   // 0초 한 방이 RQ 터렛에

        // Gaze가 RQ 터렛에 걸렸으니 RQ 딜은 절반, 포탑1 딜은 그대로
        s.damageBaron(DamageSource.RQ_TURRET_SHOT, 100, rq);
        s.damageBaron(DamageSource.TURRET_SHOT, 100, t1);
        assertEquals(50, s.damageBySource().get(DamageSource.RQ_TURRET_SHOT), 1e-9);
        assertEquals(100, s.damageBySource().get(DamageSource.TURRET_SHOT), 1e-9);
    }

    @Test
    void backRqTurretTanksBeforeHeimer() {
        // 일반 포탑 없음, BACK이어도 하이머보다 RQ 터렛을 먼저 때린다
        CombatState s = state(new ArrayList<>(), RqTurretPosition.BACK);
        s.setRQ(rq);
        startBaron(s);

        runUntil(s, 0.5);

        assertEquals(1000 - 293.5333, rq.health(), 0.01);
        assertEquals(2343, heimer.health(), 1e-9);
    }
}
