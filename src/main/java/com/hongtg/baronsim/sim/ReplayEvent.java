package com.hongtg.baronsim.sim;

/**
 * 전투 기록 한 줄 (9-2단계). 체력 그래프, 사용 횟수, 리플레이가 모두 이 기록에서 나온다.
 * C++로 치면 std::vector에 push_back하는 로그 구조체.
 *
 * kind별 의미:
 *   HIT           actor가 바론을 때렸다. source = 피해 출처, amount = 실제로 들어간 피해,
 *                 healthRatio = 맞은 뒤 바론 체력 비율 (0~1)
 *   BARON_ATTACK  바론이 actor를 때렸다. amount = 피해, healthRatio = 맞은 뒤 대상 체력 비율 (병풍은 1)
 *   TURRET_READY  전투 전에 이미 깔려 있던 포탑 (0초)
 *   TURRET_PLACED Q로 포탑을 설치했다
 *   TURRET_DIED   포탑이 부서졌다
 *   RQ_PLACED     RQ 터렛을 설치했다
 *   RQ_GONE       RQ 터렛이 사라졌다 (부서짐 또는 지속시간 끝)
 *   HEIMER_DIED   하이머가 죽었다
 *
 * slot: 포탑이면 turrets 리스트의 칸 번호(0부터), 아니면 -1. 재설치된 포탑은 죽은 칸을 그대로 쓴다.
 * source: HIT일 때만 있고 나머지는 null.
 */
public record ReplayEvent(
        double time,
        Kind kind,
        Actor actor,
        int slot,
        DamageSource source,
        double amount,
        double healthRatio) {

    public enum Kind {
        HIT, BARON_ATTACK, TURRET_READY, TURRET_PLACED, TURRET_DIED, RQ_PLACED, RQ_GONE, HEIMER_DIED
    }

    /** 누가 (HIT이면 공격자, BARON_ATTACK이면 맞은 쪽) */
    public enum Actor {
        HEIMER, TURRET, RQ, DUMMY
    }
}
