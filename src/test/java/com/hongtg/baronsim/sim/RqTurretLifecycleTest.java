package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.data.HeimerData.RqTurretData;
import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * RQ 터렛 객체 테스트 (6단계 ④ 준비).
 * RQ 터렛은 체력·방어력이 있는 Turret 객체로 깔리고, 8초 뒤(또는 죽으면) 사라진다.
 * 공격 패턴: 평타 5 → 광선 1 → 평타 2, 1/1.05초 간격 (0초 ~ 6.67초).
 * 여기서는 평타 100, 광선 200으로 단순하게 둔다.
 */
class RqTurretLifecycleTest {

    private static final RqTurretData RQ = DataLoader.loadHeimer().rqTurret();
    private static final double SHOT = 100;
    private static final double BEAM = 200;

    private CombatState newState() {
        return new CombatState(1_000_000, new DummyTankingStrategy(new Dummy(0)), new Heimer(2000, 50), 0.5,
                new TurretKits(0, 3, 20), new ArrayList<>(), new TurretSpec(500, 30, 0, 1.0, 1.0, 3));
    }

    /** timeLimit초까지 돌린다. 넘어간 이벤트는 다시 넣어 두어 이어서 돌릴 수 있게 한다. */
    private void runUntil(CombatState state, double timeLimit) {
        while (state.hasEvents()) {
            Event e = state.nextEvent();
            if (e.time() > timeLimit) {
                state.schedule(e);
                break;
            }
            e.apply(state);
        }
    }

    private double rqDamage(CombatState state) {
        return state.damageBySource().getOrDefault(DamageSource.RQ_TURRET_SHOT, 0.0)
                + state.damageBySource().getOrDefault(DamageSource.RQ_TURRET_BEAM, 0.0);
    }

    @Test
    void placedRqTurretHasHealthAndArmor() {
        CombatState state = newState();
        state.schedule(new RQPlaceEvent(0, 1000, 50, SHOT, BEAM, RQ, 70));

        runUntil(state, 0);

        assertNotNull(state.rQTurret());
        assertEquals(1000, state.rQTurret().health(), 1e-9);
        assertEquals(50, state.rQTurret().armor(), 1e-9);
    }

    @Test
    void rqTurretDisappearsAfterEightSeconds() {
        CombatState state = newState();
        state.schedule(new RQPlaceEvent(0, 1000, 50, SHOT, BEAM, RQ, 70));

        runUntil(state, 7.9);
        assertNotNull(state.rQTurret());

        runUntil(state, 8.0);
        assertNull(state.rQTurret());
    }

    @Test
    void allEightAttacksLandBeforeExpiry() {
        CombatState state = newState();
        state.schedule(new RQPlaceEvent(0, 1000, 50, SHOT, BEAM, RQ, 70));

        runUntil(state, 10);

        assertEquals(7 * SHOT + BEAM, rqDamage(state), 1e-9);
    }

    @Test
    void deadRqTurretStopsAttacking() {
        // 0초 첫 평타 뒤에 죽으면 남은 7번은 안 나간다
        CombatState state = newState();
        state.schedule(new RQPlaceEvent(0, 1000, 50, SHOT, BEAM, RQ, 70));
        runUntil(state, 0);

        state.rQTurret().takeDamage(10_000);
        runUntil(state, 10);

        assertEquals(SHOT, rqDamage(state), 1e-9);
    }

    @Test
    void gazeOnRqTurretHalvesItsDamage() {
        // 0초 첫 평타는 정상, 그 뒤 바론이 RQ 터렛을 때렸다고 치면 남은 평타 6발 + 광선 1방이 절반
        CombatState state = newState();
        state.schedule(new RQPlaceEvent(0, 1000, 50, SHOT, BEAM, RQ, 70));
        runUntil(state, 0);

        state.setGazeTarget(state.rQTurret());
        runUntil(state, 10);

        assertEquals(SHOT + (6 * SHOT + BEAM) * 0.5, rqDamage(state), 1e-9);
        assertEquals((6 * SHOT + BEAM) * 0.5,
                state.gazeLossBySource().getOrDefault(DamageSource.RQ_TURRET_SHOT, 0.0)
                        + state.gazeLossBySource().getOrDefault(DamageSource.RQ_TURRET_BEAM, 0.0), 1e-9);
    }

    @Test
    void gazeOnOtherTurretDoesNotAffectRqTurret() {
        CombatState state = newState();
        state.schedule(new RQPlaceEvent(0, 1000, 50, SHOT, BEAM, RQ, 70));
        state.setGazeTarget(new Turret(500, 30));

        runUntil(state, 10);

        assertEquals(7 * SHOT + BEAM, rqDamage(state), 1e-9);
    }

    @Test
    void oldExpiryDoesNotRemoveNewRqTurret() {
        // R 쿨이 지속시간(8초)보다 짧다고 가정: 0초에 RQ1, 5초에 RQ2
        // 8초의 RQ1 만료 이벤트는 state의 RQ 터렛이 이미 RQ2라서 아무것도 지우지 않는다 (== 비교)
        CombatState state = newState();
        state.schedule(new RQPlaceEvent(0, 1000, 50, SHOT, BEAM, RQ, 5));

        runUntil(state, 0);
        Turret first = state.rQTurret();

        runUntil(state, 5);
        Turret second = state.rQTurret();
        assertFalse(first == second, "5초에 새 RQ 터렛이 깔린다");

        runUntil(state, 8.0);
        assertSame(second, state.rQTurret());
    }
}
