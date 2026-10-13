package com.hongtg.baronsim.ui;

import java.util.ArrayList;
import java.util.List;

import com.hongtg.baronsim.sim.ReplayEvent;
import com.hongtg.baronsim.sim.ReplayEvent.Actor;

/**
 * 리플레이 한 장면 (9-4단계): 전투 기록을 t초까지 다시 훑어서 "지금 무엇이 살아 있고 체력이 얼마인지"를 만든다.
 * JavaFX를 쓰지 않는 순수 계산이라 JUnit으로 검사할 수 있다. 그리기는 ReplayView가 한다.
 *
 * 기록이 수천 줄이어도 한 번 훑는 데 1ms가 안 걸리므로, 매 프레임 처음부터 다시 계산한다
 * (되감기·슬라이더 이동을 따로 처리할 필요가 없어진다).
 */
final class ReplayFrame {

    /** 공격선을 보여 줄 시간(초). 이보다 오래된 공격은 그리지 않는다 */
    static final double FLASH_SECONDS = 0.25;
    /** 피해 숫자가 떠오르는 시간(초) */
    static final double NUMBER_SECONDS = 0.8;

    static final int SLOTS = 3;

    double baronRatio = 1;
    /** 포탑 칸별 체력 비율. 없거나 부서졌으면 -1 */
    final double[] turretRatio = { -1, -1, -1 };
    /** RQ 터렛 체력 비율. 없으면 -1 */
    double rqRatio = -1;
    double heimerRatio = 1;
    boolean heimerDead;
    /** 막 들어간 공격 (공격선, 피해 숫자용) */
    final List<ReplayEvent> recentHits = new ArrayList<>();
    /** 막 들어온 바론 공격 */
    final List<ReplayEvent> recentBaronAttacks = new ArrayList<>();

    static ReplayFrame at(List<ReplayEvent> timeline, double t) {
        ReplayFrame f = new ReplayFrame();
        for (ReplayEvent ev : timeline) {
            if (ev.time() > t) break;   // 기록은 시간순
            double age = t - ev.time();
            switch (ev.kind()) {
                case HIT -> {
                    f.baronRatio = ev.healthRatio();
                    if (age <= NUMBER_SECONDS) f.recentHits.add(ev);
                }
                case BARON_ATTACK -> {
                    if (ev.actor() == Actor.TURRET && inSlot(ev.slot())) f.turretRatio[ev.slot()] = ev.healthRatio();
                    if (ev.actor() == Actor.RQ) f.rqRatio = ev.healthRatio();
                    if (ev.actor() == Actor.HEIMER) f.heimerRatio = ev.healthRatio();
                    if (age <= FLASH_SECONDS) f.recentBaronAttacks.add(ev);
                }
                case TURRET_READY, TURRET_PLACED -> {
                    if (inSlot(ev.slot())) f.turretRatio[ev.slot()] = 1;
                }
                case TURRET_DIED -> {
                    if (inSlot(ev.slot())) f.turretRatio[ev.slot()] = -1;
                }
                case RQ_PLACED -> f.rqRatio = 1;
                case RQ_GONE -> f.rqRatio = -1;
                case HEIMER_DIED -> {
                    f.heimerDead = true;
                    f.heimerRatio = 0;
                }
            }
        }
        return f;
    }

    private static boolean inSlot(int slot) {
        return slot >= 0 && slot < SLOTS;
    }
}
