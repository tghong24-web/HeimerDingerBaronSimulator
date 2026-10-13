package com.hongtg.baronsim.ui;

import com.hongtg.baronsim.sim.ReplayEvent;
import com.hongtg.baronsim.sim.SimulatorResult;

import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 바론 체력 그래프 (9-3단계). x = 시간(초), y = 남은 체력(%).
 * - 체력 곡선: 전투 기록(HIT)의 "맞은 뒤 체력 비율"을 이은 선
 * - RW/RE 기준선: 한 방 피해 높이의 가로 점선. 곡선이 이 선 아래로 내려오는 첫 시각 = 막타 가능 시각
 * - 그림자불꽃 40% 선: 그림자불꽃을 들었을 때만
 * 선 모양은 app.css의 hp-line, baseline-line, shadow-line 클래스로 정한다.
 */
final class HealthChart extends VBox {

    private final NumberAxis xAxis = new NumberAxis();
    private final NumberAxis yAxis = new NumberAxis(0, 100, 25);
    private final LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
    private final Label finishHint = UiKit.styled("", "finish-hint");
    private final HBox legend = new HBox(16);

    HealthChart() {
        setSpacing(6);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setHorizontalZeroLineVisible(false);
        chart.setVerticalZeroLineVisible(false);
        xAxis.setLabel("시간 (초)");
        xAxis.setAutoRanging(false);
        xAxis.setForceZeroInRange(true);
        yAxis.setLabel("바론 체력 (%)");
        VBox.setVgrow(chart, Priority.ALWAYS);
        chart.setMinHeight(260);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox head = new HBox(12, UiKit.sectionTitle("바론 체력"), finishHint, grow, legend);
        head.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(head, chart);
    }

    void show(SimulatorResult r, boolean hasShadowflame) {
        chart.getData().clear();
        legend.getChildren().clear();

        double end = Math.max(r.killTime(), 1);
        double tick = end <= 30 ? 5 : end <= 120 ? 10 : 20;   // 눈금이 너무 빽빽하지 않게
        xAxis.setTickUnit(tick);
        xAxis.setUpperBound(Math.ceil(end / tick) * tick);   // 눈금 단위로 올림

        // 체력 곡선: 같은 시각에 여러 번 맞으면 마지막 값만 쓴다 (점 수 줄이기)
        XYChart.Series<Number, Number> hp = new XYChart.Series<>();
        hp.getData().add(new XYChart.Data<>(0, 100));
        double lastTime = -1;
        for (ReplayEvent ev : r.timeline()) {
            if (ev.kind() != ReplayEvent.Kind.HIT) continue;
            XYChart.Data<Number, Number> point = new XYChart.Data<>(ev.time(), ev.healthRatio() * 100);
            if (ev.time() == lastTime) {
                hp.getData().set(hp.getData().size() - 1, point);
            } else {
                hp.getData().add(point);
            }
            lastTime = ev.time();
        }
        chart.getData().add(hp);
        hp.getNode().getStyleClass().add("hp-line");
        legend.getChildren().add(legendItem("체력", "legend-hp"));

        // RW/RE 막타 기준선
        finishHint.setText("");
        if (r.ultimateOneShot() > 0) {
            double y = Math.min(100, r.ultimateOneShot() / r.baronStartHealth() * 100);
            chart.getData().add(flatLine(y, xAxis.getUpperBound(), "baseline-line"));
            legend.getChildren().add(legendItem("궁 한 방", "legend-baseline"));
            double at = firstTimeBelow(r, y / 100);
            if (at >= 0) {
                finishHint.setText(String.format("궁 막타 가능: %.1f초부터", at));
            }
        }

        // 그림자불꽃 40%
        if (hasShadowflame) {
            chart.getData().add(flatLine(40, xAxis.getUpperBound(), "shadow-line"));
            legend.getChildren().add(legendItem("그림자불꽃 40%", "legend-shadow"));
        }
    }

    private static XYChart.Series<Number, Number> flatLine(double y, double xEnd, String styleClass) {
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.getData().add(new XYChart.Data<>(0, y));
        s.getData().add(new XYChart.Data<>(xEnd, y));
        s.nodeProperty().addListener((o, a, node) -> {
            if (node != null) node.getStyleClass().add(styleClass);
        });
        return s;
    }

    /** 체력 비율이 처음으로 ratio 이하가 되는 시각. 없으면 -1 */
    private static double firstTimeBelow(SimulatorResult r, double ratio) {
        for (ReplayEvent ev : r.timeline()) {
            if (ev.kind() == ReplayEvent.Kind.HIT && ev.healthRatio() <= ratio) {
                return ev.time();
            }
        }
        return -1;
    }

    private static HBox legendItem(String text, String swatchClass) {
        Region swatch = new Region();
        swatch.getStyleClass().addAll("legend-swatch", swatchClass);
        HBox h = new HBox(6, swatch, new Label(text));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
