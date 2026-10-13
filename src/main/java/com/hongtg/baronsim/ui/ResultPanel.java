package com.hongtg.baronsim.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.hongtg.baronsim.sim.DamageSource;
import com.hongtg.baronsim.sim.SimulatorResult;
import com.hongtg.baronsim.sim.SkillCounts;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 오른쪽 결과 패널 (시안 A안): 처치 결과 카드, 딜 비중(한 줄 누적 막대 + 범례), 스킬 사용 횟수, Gaze 손해.
 * 리플레이를 보는 동안에는 showCountsAt으로 사용 횟수가 재생 시각에 맞춰 바뀐다.
 */
final class ResultPanel extends VBox {

    /** 피해 출처별 색. 그래프·리플레이와 같은 색을 쓴다 */
    static final Map<DamageSource, String> COLORS = new EnumMap<>(DamageSource.class);
    static {
        COLORS.put(DamageSource.TURRET_SHOT, "#C8AA6E");
        COLORS.put(DamageSource.ROCKETS, "#F2994A");
        COLORS.put(DamageSource.GRENADE, "#5B8DEF");
        COLORS.put(DamageSource.RQ_TURRET_SHOT, "#0AC8B9");
        COLORS.put(DamageSource.RQ_TURRET_BEAM, "#7FE6DC");
        COLORS.put(DamageSource.RW_ROCKETS, "#FFD08A");
        COLORS.put(DamageSource.RE_GRENADE, "#9DB8FF");
        COLORS.put(DamageSource.FATED_ASHES_BURN, "#B5562B");
        COLORS.put(DamageSource.BLACKFIRE_BURN, "#A86BE0");
        COLORS.put(DamageSource.LIANDRY_BURN, "#E84057");
    }

    private final Label status = UiKit.styled("", "status");
    private final Label time = UiKit.styled("-", "big-time");
    private final Label condition = UiKit.styled("", "hint");

    private final HBox shareBar = new HBox();
    private final FlowPane legend = new FlowPane(14, 6);

    private final Label[] castValues = new Label[4];
    private final UiKit.IconBox[] castIcons = {
        new UiKit.IconBox(40, "Q"), new UiKit.IconBox(40, "W"), new UiKit.IconBox(40, "E"), new UiKit.IconBox(40, "R")
    };
    private final Label turretShots = UiKit.styled("-", "value");
    private final Label rqAttacks = UiKit.styled("-", "value");
    private final Label burnTicks = UiKit.styled("-", "value");
    private final Label countsTitle = UiKit.sectionTitle("스킬 사용 횟수");

    private final Label totalDamage = UiKit.styled("-", "value");
    private final Label gazeLoss = UiKit.styled("-", "loss");

    private SimulatorResult result;

    ResultPanel() {
        getStyleClass().add("side");
        setSpacing(12);

        VBox resultCard = new VBox(4, status, time, condition);
        resultCard.getStyleClass().addAll("card", "card-gold");
        resultCard.setAlignment(Pos.CENTER);

        shareBar.getStyleClass().add("share-bar");
        VBox shareCard = UiKit.card("딜 비중", shareBar, legend);

        GridPane casts = new GridPane();
        casts.setHgap(8);
        casts.setVgap(4);
        String[] names = { "포탑 설치", "로켓", "수류탄", "궁극기" };
        for (int i = 0; i < 4; i++) {
            castValues[i] = UiKit.styled("-", "count");
            VBox cell = new VBox(4, castIcons[i], UiKit.styled(names[i], "hint"), castValues[i]);
            cell.setAlignment(Pos.CENTER);
            casts.add(cell, i, 0);
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            casts.getColumnConstraints().add(c);
            Icons.spell("QWER".charAt(i), castIcons[i]::setImage);
        }
        VBox extra = new VBox(6, row("포탑 평타", turretShots), row("RQ 터렛 공격", rqAttacks), row("화상 틱", burnTicks));
        VBox countsCard = new VBox(10, countsTitle, casts, extra);
        countsCard.getStyleClass().add("card");

        VBox totalsCard = UiKit.card(null, row("바론에게 준 피해", totalDamage), row("Gaze 손해", gazeLoss));

        getChildren().addAll(resultCard, shareCard, countsCard, totalsCard);
    }

    /** 새 결과 표시 */
    void show(SimulatorResult r, String conditionText) {
        this.result = r;
        status.getStyleClass().removeAll("status-ok", "status-bad", "status-warn");
        switch (r.outcome()) {
            case KILLED -> {
                status.setText("처치 성공");
                status.getStyleClass().add("status-ok");
            }
            case HEIMER_DEAD -> {
                status.setText("하이머 사망");
                status.getStyleClass().add("status-bad");
            }
            case TIME_LIMIT -> {
                status.setText("시간 초과");
                status.getStyleClass().add("status-warn");
            }
        }
        time.setText(String.format("%.2f초", r.killTime()));
        condition.setText(conditionText);

        showShare(r);
        showCountsAt(Double.MAX_VALUE);
        totalDamage.setText(String.format("%,.0f", r.totalDamage()));
        gazeLoss.setText(r.totalGazeLoss() > 0 ? String.format("−%,.0f", r.totalGazeLoss()) : "0");
    }

    /** t초까지의 사용 횟수. 끝까지면 Double.MAX_VALUE */
    void showCountsAt(double t) {
        if (result == null) return;
        SkillCounts c = SkillCounts.of(result.timeline(), t);
        int[] v = { c.q(), c.w(), c.e(), c.r() };
        for (int i = 0; i < 4; i++) {
            castValues[i].setText(v[i] + "회");
        }
        turretShots.setText(c.turretShots() + "발");
        rqAttacks.setText(c.rqAttacks() + "회");
        burnTicks.setText(c.burnTicks() + "틱");
        countsTitle.setText(t == Double.MAX_VALUE ? "스킬 사용 횟수" : String.format("스킬 사용 횟수 (%.1f초까지)", t));
    }

    private void showShare(SimulatorResult r) {
        shareBar.getChildren().clear();
        legend.getChildren().clear();
        double total = r.totalDamage();
        if (total <= 0) return;

        // 큰 것부터
        List<Map.Entry<DamageSource, Double>> entries = new ArrayList<>(r.damageBySource().entrySet());
        entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        for (Map.Entry<DamageSource, Double> e : entries) {
            double frac = e.getValue() / total;
            if (frac <= 0) continue;
            String color = COLORS.getOrDefault(e.getKey(), "#A09B8C");

            Region piece = new Region();
            piece.setStyle("-fx-background-color: " + color + ";");
            piece.setMinWidth(0);
            piece.prefWidthProperty().bind(shareBar.widthProperty().subtract(2).multiply(frac));
            piece.setMaxHeight(Double.MAX_VALUE);
            Tooltip.install(piece, new Tooltip(String.format("%s %,.0f (%.1f%%)", e.getKey().displayName(), e.getValue(), frac * 100)));
            shareBar.getChildren().add(piece);

            Region swatch = new Region();
            swatch.setStyle("-fx-background-color: " + color + ";");
            swatch.setMinSize(10, 10);
            swatch.setMaxSize(10, 10);
            Label name = UiKit.styled(e.getKey().displayName(), "value-plain");
            Label pct = new Label(String.format("%.0f%%", frac * 100));
            HBox item = new HBox(6, swatch, name, pct);
            item.setAlignment(Pos.CENTER_LEFT);
            legend.getChildren().add(item);
        }
    }

    private static HBox row(String name, Label value) {
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox h = new HBox(new Label(name), grow, value);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
