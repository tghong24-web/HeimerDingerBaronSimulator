package com.hongtg.baronsim.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * replay.json 읽기 테스트 (9-4단계). 자리 좌표는 0~1 비율이어야 한다.
 */
class ReplayLayoutTest {

    @Test
    void loadsAllSpotsInsideImage() {
        ReplayLayout layout = DataLoader.loadReplayLayout();
        assertEquals(1.6, layout.aspectRatio(), 1e-9);
        assertEquals(3, layout.turrets().size());

        for (ReplayLayout.Spot s : java.util.List.of(layout.baron(), layout.rqFront(), layout.rqBack(),
                layout.heimer(), layout.dummy(), layout.turrets().get(0), layout.turrets().get(1), layout.turrets().get(2))) {
            assertTrue(s.x() > 0 && s.x() < 1, "x: " + s);
            assertTrue(s.y() > 0 && s.y() < 1, "y: " + s);
            assertTrue(s.size() > 0 && s.size() < 0.5, "size: " + s);
        }
    }

    @Test
    void rqFrontIsCloserToBaronThanBack() {
        ReplayLayout layout = DataLoader.loadReplayLayout();
        double front = Math.abs(layout.rqFront().y() - layout.baron().y());
        double back = Math.abs(layout.rqBack().y() - layout.baron().y());
        assertTrue(front < back);
    }
}
