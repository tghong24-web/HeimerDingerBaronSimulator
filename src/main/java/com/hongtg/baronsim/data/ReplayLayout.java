package com.hongtg.baronsim.data;

import java.util.List;
import java.util.Objects;

/**
 * replay.json을 읽은 결과 (9-4단계). 리플레이 화면에서 바론·포탑·하이머·병풍을 그릴 자리.
 * 좌표는 둥지 그림 기준 0~1 비율이고, size는 그림 너비 대비 지름 비율이다.
 * turrets는 포탑 칸 번호(0, 1, 2) 순서. rqFront/rqBack은 RQ 터렛 위치(앞/뒤) 입력에 따라 고른다.
 */
public record ReplayLayout(
        double aspectRatio,
        Spot baron,
        List<Spot> turrets,
        Spot rqFront,
        Spot rqBack,
        Spot heimer,
        Spot dummy) {

    public record Spot(double x, double y, double size) {
    }

    /** JSON을 읽은 뒤 빠진 값이 없는지 확인한다. DataLoader가 부른다. */
    void validate() {
        Objects.requireNonNull(baron, "replay.json에 baron이 없습니다");
        Objects.requireNonNull(turrets, "replay.json에 turrets가 없습니다");
        Objects.requireNonNull(rqFront, "replay.json에 rqFront가 없습니다");
        Objects.requireNonNull(rqBack, "replay.json에 rqBack이 없습니다");
        Objects.requireNonNull(heimer, "replay.json에 heimer가 없습니다");
        Objects.requireNonNull(dummy, "replay.json에 dummy가 없습니다");
        if (turrets.size() < 3) {
            throw new IllegalStateException("replay.json의 turrets는 3칸이어야 합니다: " + turrets.size());
        }
        if (aspectRatio <= 0) {
            throw new IllegalStateException("replay.json의 aspectRatio는 0보다 커야 합니다: " + aspectRatio);
        }
    }
}
