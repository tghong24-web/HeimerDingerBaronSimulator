package com.hongtg.baronsim.sim;

import java.util.List;

/**
 * 스킬 사용 횟수 (9-2단계). 전투 기록(ReplayEvent)에서 센다.
 *
 * q: Q로 포탑을 설치한 횟수 (전투 전에 깔려 있던 포탑은 빼고)
 * w: W 시전 (일반 W + RW)
 * e: E 시전 (일반 E + RE)
 * r: R 시전 (RQ 설치 + RW + RE)
 * turretShots: 일반 포탑 평타 수
 * rqAttacks: RQ 터렛 공격 수 (평타 + 광선)
 * burnTicks: 화상 틱 수 (세 아이템 합)
 */
public record SkillCounts(int q, int w, int e, int r, int turretShots, int rqAttacks, int burnTicks) {

    /** until초까지(포함)의 기록만 센다. 리플레이에서 시간에 따라 올라가는 숫자에 쓴다. */
    public static SkillCounts of(List<ReplayEvent> timeline, double until) {
        int q = 0, w = 0, e = 0, r = 0, shots = 0, rq = 0, burns = 0;
        for (ReplayEvent ev : timeline) {
            if (ev.time() > until) {
                break;   // 기록은 시간순이다
            }
            switch (ev.kind()) {
                case TURRET_PLACED -> q++;
                case RQ_PLACED -> r++;
                case HIT -> {
                    switch (ev.source()) {
                        case ROCKETS -> w++;
                        case RW_ROCKETS -> { w++; r++; }
                        case GRENADE -> e++;
                        case RE_GRENADE -> { e++; r++; }
                        case TURRET_SHOT -> shots++;
                        case RQ_TURRET_SHOT, RQ_TURRET_BEAM -> rq++;
                        case FATED_ASHES_BURN, BLACKFIRE_BURN, LIANDRY_BURN -> burns++;
                    }
                }
                default -> { }
            }
        }
        return new SkillCounts(q, w, e, r, shots, rq, burns);
    }
}
