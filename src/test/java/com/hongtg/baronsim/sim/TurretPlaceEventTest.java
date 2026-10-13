package com.hongtg.baronsim.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;

/**
 * 포탑 설치 테스트 (6단계 ③).
 * 죽은 자리 → 그 자리에, 빈 자리 → 맨 뒤에, 꽉 찼으면 설치 안 함.
 * 키트가 없으면 설치 안 하고, 설치하면 키트를 쓰고 충전 중이 아니면 충전을 시작한다.
 * 진행 중인 충전은 리셋되지 않는다. Q 쿨 동안은 못 깔고, 깔고 나면 Q가 돌아올 때 다음 설치를 시도한다.
 */
class TurretPlaceEventTest {

    private static final TurretSpec SPEC = new TurretSpec(500, 30, 0, 50, 1.0, 1.0, 3);

    private CombatState newState(TurretKits kits, List<Turret> turrets, Heimer heimer) {
        return new CombatState(1_000_000, new DummyTankingStrategy(new Dummy(0, 0)),
                heimer, 0.5, kits, turrets, SPEC, new ArrayList<>(), new LowHealthBonus(0, 0), new ActiveEffect(0, 0, 1));
    }

    private CombatState newState(TurretKits kits, List<Turret> turrets) {
        return newState(kits, turrets, new Heimer(2000, 50, 0));
    }

    /** 예약된 이벤트를 timeLimit초까지 돌린다. 넘어간 이벤트는 다시 넣어 두어 이어서 돌릴 수 있게 한다. */
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

    @Test
    void placesAtBackAndUsesKit() {
        TurretKits kits = new TurretKits(3, 3, 20);
        List<Turret> turrets = new ArrayList<>();
        CombatState state = newState(kits, turrets);

        new TurretPlaceEvent(0, SPEC).apply(state);

        assertEquals(1, turrets.size());
        assertEquals(500, turrets.get(0).health(), 1e-9);
        assertEquals(30, turrets.get(0).armor(), 1e-9);
        assertEquals(2, kits.kits());
        assertTrue(kits.isRecharging());
    }

    @Test
    void noKitNoPlacement() {
        TurretKits kits = new TurretKits(0, 3, 20);
        List<Turret> turrets = new ArrayList<>();
        CombatState state = newState(kits, turrets);

        new TurretPlaceEvent(0, SPEC).apply(state);

        assertEquals(0, turrets.size());
        assertEquals(0, kits.kits());
    }

    @Test
    void fullSlotsKeepKit() {
        TurretKits kits = new TurretKits(2, 3, 20);
        List<Turret> turrets = new ArrayList<>(List.of(
                new Turret(500, 30, 0), new Turret(500, 30, 0), new Turret(500, 30, 0)));
        CombatState state = newState(kits, turrets);

        new TurretPlaceEvent(0, SPEC).apply(state);

        assertEquals(3, turrets.size());
        assertEquals(2, kits.kits());
        assertFalse(kits.isRecharging());
        assertFalse(state.hasEvents());
    }

    @Test
    void replacesDeadTurretInItsSlot() {
        TurretKits kits = new TurretKits(1, 3, 20);
        Turret first = new Turret(500, 30, 0);
        Turret dead = new Turret(500, 30, 0);
        Turret third = new Turret(500, 30, 0);
        dead.takeDamage(10_000);
        List<Turret> turrets = new ArrayList<>(List.of(first, dead, third));
        CombatState state = newState(kits, turrets);

        new TurretPlaceEvent(0, SPEC).apply(state);

        assertEquals(3, turrets.size());
        assertSame(first, turrets.get(0));
        assertSame(third, turrets.get(2));
        assertFalse(turrets.get(1).isDead());
        assertEquals(0, kits.kits());
    }

    @Test
    void deadHeimerDoesNotPlace() {
        TurretKits kits = new TurretKits(3, 3, 20);
        List<Turret> turrets = new ArrayList<>();
        Heimer heimer = new Heimer(100, 0, 0);
        heimer.takeDamage(10_000);
        CombatState state = newState(kits, turrets, heimer);

        new TurretPlaceEvent(0, SPEC).apply(state);

        assertEquals(0, turrets.size());
        assertEquals(3, kits.kits());
    }

    @Test
    void usedKitComesBackAfterRecharge() {
        // 포탑 2개 + 키트 3개(가득) → 0초에 설치(키트 2) → 1초 설치 시도는 자리가 꽉 차서 무시 → 20초에 키트 3
        TurretKits kits = new TurretKits(3, 3, 20);
        List<Turret> turrets = new ArrayList<>(List.of(new Turret(500, 30, 0), new Turret(500, 30, 0)));
        CombatState state = newState(kits, turrets);
        state.schedule(new TurretPlaceEvent(0, SPEC));

        runUntil(state, 19.9);
        assertEquals(3, turrets.size());
        assertEquals(2, kits.kits());

        runUntil(state, 20.0);
        assertEquals(3, kits.kits());
        assertFalse(kits.isRecharging());
    }

    @Test
    void placingDoesNotResetOngoingRecharge() {
        // 포탑 2개, 키트 2개, 남은 충전 5초 → 1초에 설치(키트 1) → 5초에 2개, 25초에 3개 (21초로 밀리지 않음)
        TurretKits kits = new TurretKits(2, 3, 20);
        List<Turret> turrets = new ArrayList<>(List.of(new Turret(500, 30, 0), new Turret(500, 30, 0)));
        CombatState state = newState(kits, turrets);
        kits.setRecharging(true);
        state.schedule(new KitRechargeEvent(5));
        state.schedule(new TurretPlaceEvent(1, SPEC));

        runUntil(state, 4.9);
        assertEquals(1, kits.kits());

        runUntil(state, 5.0);
        assertEquals(2, kits.kits());

        runUntil(state, 24.9);
        assertEquals(2, kits.kits());

        runUntil(state, 25.0);
        assertEquals(3, kits.kits());
    }

    @Test
    void placedTurretStartsShooting() {
        // 키트 1개 → 0초 설치 → 0, 1, 2, 3초에 사격 = 4발
        TurretKits kits = new TurretKits(1, 3, 20);
        CombatState state = newState(kits, new ArrayList<>());
        state.schedule(new TurretPlaceEvent(0, SPEC));

        runUntil(state, 3.5);

        assertEquals(4 * 50, state.damageBySource().get(DamageSource.TURRET_SHOT), 1e-9);
    }

    @Test
    void placesOneTurretPerQCooldown() {
        // 포탑 0개, 키트 3개 → 0초, 1초, 2초에 하나씩
        TurretKits kits = new TurretKits(3, 3, 20);
        List<Turret> turrets = new ArrayList<>();
        CombatState state = newState(kits, turrets);
        state.schedule(new TurretPlaceEvent(0, SPEC));

        runUntil(state, 0.0);
        assertEquals(1, turrets.size());

        runUntil(state, 0.9);
        assertEquals(1, turrets.size());

        runUntil(state, 1.0);
        assertEquals(2, turrets.size());

        runUntil(state, 2.0);
        assertEquals(3, turrets.size());
        assertEquals(0, kits.kits());
    }

    @Test
    void sameTimeEventsWaitForQ() {
        // 같은 0초에 설치 이벤트 2개 → Q 쿨 때문에 0초엔 1개만, 1초에 2개째
        TurretKits kits = new TurretKits(3, 3, 20);
        List<Turret> turrets = new ArrayList<>();
        CombatState state = newState(kits, turrets);
        state.schedule(new TurretPlaceEvent(0, SPEC));
        state.schedule(new TurretPlaceEvent(0, SPEC));

        runUntil(state, 0.5);
        assertEquals(1, turrets.size());
        assertEquals(2, kits.kits());

        runUntil(state, 1.0);
        assertEquals(2, turrets.size());
    }

    @Test
    void oneTurretTwoKitsFillsUp() {
        // 포탑 1개 + 키트 2개 → 0초, 1초에 설치해 3개, 키트 0 → 충전은 0초에 시작했으니 20초에 키트 1
        TurretKits kits = new TurretKits(2, 3, 20);
        List<Turret> turrets = new ArrayList<>(List.of(new Turret(500, 30, 0)));
        CombatState state = newState(kits, turrets);
        state.schedule(new TurretPlaceEvent(0, SPEC));

        runUntil(state, 1.0);
        assertEquals(3, turrets.size());
        assertEquals(0, kits.kits());
        assertTrue(kits.isRecharging());

        runUntil(state, 19.9);
        assertEquals(0, kits.kits());

        runUntil(state, 20.0);
        assertEquals(1, kits.kits());
        assertEquals(3, turrets.size());
    }
}
