package com.hongtg.baronsim.ui;

import java.util.List;

import com.hongtg.baronsim.data.ReplayLayout;
import com.hongtg.baronsim.data.ReplayLayout.Spot;
import com.hongtg.baronsim.sim.DamageSource;
import com.hongtg.baronsim.sim.ReplayEvent;
import com.hongtg.baronsim.sim.ReplayEvent.Actor;
import com.hongtg.baronsim.sim.RqTurretPosition;
import com.hongtg.baronsim.sim.SimulatorResult;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * 리플레이 화면 (9-4단계, 시안 A-2).
 * 둥지 그림 위에 바론·포탑·RQ 터렛·하이머·병풍을 놓고, 전투 기록대로 공격선과 피해 숫자를 그린다.
 * 0.5×/1×/2×/4× 배속 재생, 위치 슬라이더. 자리는 replay.json에서 읽는다.
 *
 * AnimationTimer는 화면이 새로 그려질 때마다(보통 초당 60번) handle을 부르는 JavaFX 타이머다.
 * C++ 게임 루프의 "while (running) { update(dt); render(); }"에서 한 바퀴에 해당한다.
 */
final class ReplayView extends VBox {

    private static final Color BG = Color.web("#06121A");
    private static final Color GOLD = Color.web("#C8AA6E");
    private static final Color TEAL = Color.web("#0AC8B9");
    private static final Color PURPLE = Color.web("#A86BE0");
    private static final Color ORANGE = Color.web("#E8823A");
    private static final Color RED = Color.web("#E84057");
    private static final Color TEXT = Color.web("#F0E6D2");
    private static final Color MUTED = Color.web("#A09B8C");
    private static final Color BLUE = Color.web("#5B8DEF");

    private final ReplayLayout layout;
    private final Canvas canvas = new Canvas();
    private final StackPane canvasHolder = new StackPane(canvas);

    private final Region hpFill = new Region();
    private final Label hpLabel = UiKit.styled("100%", "value");
    private final Button playButton = new Button("▶ 재생");
    private final ToggleGroup speedGroup = new ToggleGroup();
    private final Slider slider = new Slider(0, 1, 0);
    private final Label timeLabel = UiKit.styled("0.00 / 0.00초", "value");

    private final DoubleProperty time = new SimpleDoubleProperty(0);

    private SimulatorResult result;
    private List<ReplayEvent> timeline = List.of();
    private double end = 0;
    private boolean playing;
    private boolean dirty = true;
    private boolean updatingSlider;
    private long lastNanos;

    private boolean hasDummy;
    private boolean hasShadowflame;
    private RqTurretPosition rqPosition = RqTurretPosition.FRONT;

    private final Image pit = Icons.resource("replay_pit.png", 1920);
    private final Image baronToken = Icons.resource("baron_token.png", 512);
    private Image turretIcon;
    private Image rqIcon;
    private Image heimerIcon;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double dt = lastNanos == 0 ? 0 : Math.min(0.1, (now - lastNanos) / 1e9);
            lastNanos = now;
            if (playing) {
                setTime(Math.min(end, time.get() + dt * speed()));
                if (time.get() >= end) {
                    playing = false;
                    updatePlayButton();
                }
            }
            if (dirty) {
                draw();
                dirty = false;
            }
        }
    };

    ReplayView(ReplayLayout layout) {
        this.layout = layout;
        setSpacing(10);

        // 바론 체력바
        hpFill.getStyleClass().add("hp-fill");
        hpFill.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane track = new StackPane(hpFill);
        track.getStyleClass().add("hp-track");
        track.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(track, Priority.ALWAYS);
        track.widthProperty().addListener((o, a, b) -> dirty = true);
        Label baronName = UiKit.styled("내셔 남작", "baron-name");
        hpLabel.setMinWidth(48);
        hpLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox hpRow = new HBox(12, baronName, track, hpLabel);
        hpRow.setAlignment(Pos.CENTER_LEFT);

        // 캔버스는 칸 크기에 맞춰 16:10을 유지하며 늘었다 줄었다 한다
        canvasHolder.getStyleClass().add("replay-stage");
        canvasHolder.setMinSize(0, 0);
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);
        canvasHolder.widthProperty().addListener((o, a, b) -> resizeCanvas());
        canvasHolder.heightProperty().addListener((o, a, b) -> resizeCanvas());

        // 조작 줄
        playButton.getStyleClass().add("play-button");
        playButton.setOnAction(e -> togglePlay());
        HBox speeds = UiKit.segmented(speedGroup, new Object[] { 0.5, 1.0, 2.0, 4.0 },
                new String[] { "0.5×", "1×", "2×", "4×" }, 1);
        speeds.setMinWidth(200);
        speeds.setMaxWidth(200);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.valueProperty().addListener((o, a, v) -> {
            if (!updatingSlider) setTime(v.doubleValue());
        });
        timeLabel.setMinWidth(120);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox controls = new HBox(12, playButton, speeds, slider, timeLabel);
        controls.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(hpRow, canvasHolder, controls);

        Icons.spell('Q', img -> { turretIcon = img; dirty = true; });
        Icons.spell('R', img -> { rqIcon = img; dirty = true; });
        Icons.champion(img -> { heimerIcon = img; dirty = true; });

        timer.start();
    }

    /** 재생 위치(초). ResultPanel이 사용 횟수를 맞추려고 듣는다 */
    DoubleProperty timeProperty() {
        return time;
    }

    void load(SimulatorResult r, boolean hasDummy, RqTurretPosition rqPosition, boolean hasShadowflame) {
        this.result = r;
        this.timeline = r.timeline();
        this.end = r.killTime();
        this.hasDummy = hasDummy;
        this.rqPosition = rqPosition;
        this.hasShadowflame = hasShadowflame;
        playing = false;
        slider.setMax(Math.max(end, 0.01));
        setTime(0);
        updatePlayButton();
    }

    void pause() {
        playing = false;
        updatePlayButton();
    }

    // ── 재생 조작 ──

    private void togglePlay() {
        if (result == null) return;
        if (!playing && time.get() >= end) {
            setTime(0);   // 끝까지 봤으면 처음부터
        }
        playing = !playing;
        updatePlayButton();
    }

    private void updatePlayButton() {
        if (playing) {
            playButton.setText("❚❚ 일시정지");
        } else if (result != null && time.get() >= end) {
            playButton.setText("↺ 다시 보기");
        } else {
            playButton.setText("▶ 재생");
        }
    }

    private double speed() {
        return speedGroup.getSelectedToggle() == null ? 1 : (Double) speedGroup.getSelectedToggle().getUserData();
    }

    private void setTime(double t) {
        time.set(t);
        updatingSlider = true;
        slider.setValue(t);
        updatingSlider = false;
        timeLabel.setText(String.format("%.2f / %.2f초", t, end));
        if (!playing) updatePlayButton();
        dirty = true;
    }

    private void resizeCanvas() {
        double w = canvasHolder.getWidth();
        double h = canvasHolder.getHeight();
        if (w <= 0 || h <= 0) return;
        double cw = Math.min(w, h * layout.aspectRatio());
        canvas.setWidth(Math.floor(cw));
        canvas.setHeight(Math.floor(cw / layout.aspectRatio()));
        dirty = true;
    }

    // ── 그리기 ──

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        ReplayFrame f = ReplayFrame.at(timeline, time.get());
        boolean low = hasShadowflame && f.baronRatio <= 0.4;
        updateHpBar(f.baronRatio, low);

        drawBackground(g, w, h);
        drawAttacks(g, f, w, h);
        if (hasDummy) drawDummy(g, w, h);
        drawTurrets(g, f, w, h);
        drawRq(g, f, w, h);
        drawHeimer(g, f, w, h);
        drawBaron(g, f, w, h, low);
        drawNumbers(g, f, w, h);
    }

    private void updateHpBar(double ratio, boolean low) {
        Region track = (Region) hpFill.getParent();
        hpFill.setPrefWidth(Math.max(0, track.getWidth() - 2) * ratio);
        hpFill.setStyle("-fx-background-color: " + (low ? "#E8823A" : "#A86BE0") + ";");
        hpLabel.setText(String.format("%.0f%%", ratio * 100));
    }

    private void drawBackground(GraphicsContext g, double w, double h) {
        if (pit != null && !pit.isError()) {
            g.drawImage(pit, 0, 0, w, h);
            return;
        }
        // 그림이 없을 때: 시안처럼 타원 두 개로 둥지를 그린다
        g.setFill(BG);
        g.fillRect(0, 0, w, h);
        g.setFill(Color.web("#0D1C1B"));
        g.setStroke(Color.web("#22332C"));
        g.setLineWidth(w * 0.015);
        g.fillOval(w * 0.05, h * 0.05, w * 0.90, h * 0.90);
        g.strokeOval(w * 0.05, h * 0.05, w * 0.90, h * 0.90);
        g.setFill(Color.web("#102320"));
        g.setStroke(Color.web("#463714"));
        g.setLineWidth(1.5);
        g.setLineDashes(4, 6);
        g.fillOval(w * 0.12, h * 0.15, w * 0.76, h * 0.72);
        g.strokeOval(w * 0.12, h * 0.15, w * 0.76, h * 0.72);
        g.setLineDashes();
        g.setFill(Color.web("#0D1C1B"));
        g.fillRect(w * 0.42, h * 0.90, w * 0.16, h * 0.10);
    }

    private void drawAttacks(GraphicsContext g, ReplayFrame f, double w, double h) {
        double bx = layout.baron().x() * w, by = layout.baron().y() * h;
        g.setLineCap(StrokeLineCap.ROUND);

        for (ReplayEvent ev : f.recentHits) {
            double age = time.get() - ev.time();
            if (age > ReplayFrame.FLASH_SECONDS * 2) continue;
            double alpha = 1 - age / (ReplayFrame.FLASH_SECONDS * 2);
            Color c = Color.web(ResultPanel.COLORS.getOrDefault(ev.source(), "#A09B8C"), alpha);
            Spot from = spotOf(ev.actor(), ev.slot());
            if (from == null) continue;
            double fx = from.x() * w, fy = from.y() * h;

            switch (ev.source()) {
                case ROCKETS, RW_ROCKETS -> {
                    // 로켓: 하이머에서 바론으로 휘어 날아가는 선 여러 개
                    g.setStroke(c);
                    g.setLineWidth(ev.source() == DamageSource.RW_ROCKETS ? 4 : 2.5);
                    int n = ev.source() == DamageSource.RW_ROCKETS ? 7 : 3;
                    for (int i = 0; i < n; i++) {
                        double bend = (i - (n - 1) / 2.0) * w * 0.07;
                        g.beginPath();
                        g.moveTo(fx, fy);
                        g.quadraticCurveTo((fx + bx) / 2 + bend, (fy + by) / 2, bx + bend * 0.2, by + h * 0.04);
                        g.stroke();
                    }
                }
                case GRENADE, RE_GRENADE -> {
                    // 수류탄: 바론 위에서 터지는 원
                    g.setStroke(c);
                    g.setLineWidth(3);
                    double r = w * (0.05 + age * 0.15);
                    g.strokeOval(bx - r, by - r * 0.6, r * 2, r * 1.2);
                }
                case FATED_ASHES_BURN, BLACKFIRE_BURN, LIANDRY_BURN -> {
                    // 화상은 선 대신 바론 아래 작은 불꽃 점
                    g.setFill(c);
                    double r = w * 0.008;
                    double ox = (ev.source().ordinal() - 8) * w * 0.025;
                    g.fillOval(bx + ox - r, by + layout.baron().size() * w * 0.32 - r, r * 2, r * 2);
                }
                default -> {
                    // 포탑·RQ 평타와 광선: 직선
                    g.setStroke(c);
                    g.setLineWidth(ev.source() == DamageSource.RQ_TURRET_BEAM ? 7 : 2);
                    g.strokeLine(fx, fy, bx, by + h * 0.04);
                }
            }
        }

        // 바론 공격: 빨간 선
        for (ReplayEvent ev : f.recentBaronAttacks) {
            Spot to = spotOf(ev.actor(), ev.slot());
            if (to == null) continue;
            double alpha = 1 - (time.get() - ev.time()) / ReplayFrame.FLASH_SECONDS;
            g.setStroke(Color.color(RED.getRed(), RED.getGreen(), RED.getBlue(), Math.max(0, alpha)));
            g.setLineWidth(5);
            g.strokeLine(bx, by, to.x() * w, to.y() * h);
        }
    }

    private void drawDummy(GraphicsContext g, double w, double h) {
        Spot s = layout.dummy();
        double r = s.size() * w / 2;
        double x = s.x() * w, y = s.y() * h;
        g.setFill(Color.web("#0A1428"));
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setStroke(BLUE);
        g.setLineWidth(2);
        g.strokeOval(x - r, y - r, r * 2, r * 2);
        label(g, "병풍", x, y, BLUE, r * 0.7);
    }

    private void drawTurrets(GraphicsContext g, ReplayFrame f, double w, double h) {
        for (int i = 0; i < ReplayFrame.SLOTS; i++) {
            Spot s = layout.turrets().get(i);
            double size = s.size() * w;
            double x = s.x() * w, y = s.y() * h;
            if (f.turretRatio[i] < 0) {
                // 빈 자리: 흐린 점선 칸
                g.setStroke(Color.web("#463714"));
                g.setLineWidth(1);
                g.setLineDashes(3, 4);
                g.strokeRect(x - size / 2, y - size / 2, size, size);
                g.setLineDashes();
                continue;
            }
            tokenSquare(g, turretIcon, x, y, size, GOLD, "Q");
            hpBar(g, x, y + size / 2 + 4, size, f.turretRatio[i], GOLD);
            label(g, "포탑 " + (i + 1), x, y + size / 2 + 16, GOLD, Math.max(10, size * 0.24));
        }
    }

    private void drawRq(GraphicsContext g, ReplayFrame f, double w, double h) {
        if (f.rqRatio < 0) return;
        Spot s = rqSpot();
        double size = s.size() * w;
        double x = s.x() * w, y = s.y() * h;
        tokenSquare(g, rqIcon, x, y, size, TEAL, "R");
        hpBar(g, x, y + size / 2 + 4, size, f.rqRatio, TEAL);
        label(g, "RQ 터렛", x, y + size / 2 + 16, TEAL, Math.max(10, size * 0.2));
    }

    private void drawHeimer(GraphicsContext g, ReplayFrame f, double w, double h) {
        Spot s = layout.heimer();
        double size = s.size() * w;
        double x = s.x() * w, y = s.y() * h;
        tokenCircle(g, heimerIcon, x, y, size, f.heimerDead ? MUTED : TEXT, "딩거");
        if (f.heimerDead) {
            g.setFill(Color.color(0, 0, 0, 0.6));
            g.fillOval(x - size / 2, y - size / 2, size, size);
            label(g, "사망", x, y, RED, size * 0.3);
        }
        if (!hasDummy) {
            hpBar(g, x, y + size / 2 + 4, size, f.heimerRatio, Color.web("#3FD0C9"));
        }
    }

    private void drawBaron(GraphicsContext g, ReplayFrame f, double w, double h, boolean low) {
        Spot s = layout.baron();
        double size = s.size() * w;
        double x = s.x() * w, y = s.y() * h;
        Color ring = low ? ORANGE : PURPLE;
        if (baronToken != null && !baronToken.isError()) {
            tokenCircle(g, baronToken, x, y, size, ring, "");
        } else {
            g.setFill(Color.web("#24123A"));
            g.fillOval(x - size * 0.6, y - size * 0.38, size * 1.2, size * 0.76);
            g.setStroke(ring);
            g.setLineWidth(3);
            g.strokeOval(x - size * 0.6, y - size * 0.38, size * 1.2, size * 0.76);
            label(g, "바론", x, y, TEXT, size * 0.16);
        }
        if (f.baronRatio <= 0) {
            g.setFill(Color.color(0, 0, 0, 0.55));
            g.fillOval(x - size * 0.6, y - size * 0.45, size * 1.2, size * 0.9);
            label(g, "처치!", x, y, TEAL, size * 0.2);
        }
    }

    /** 맞은 피해 숫자: 바론 위로 떠오르며 사라진다. 화상은 너무 잦아서 뺀다 */
    private void drawNumbers(GraphicsContext g, ReplayFrame f, double w, double h) {
        double bx = layout.baron().x() * w;
        double by = layout.baron().y() * h - layout.baron().size() * w * 0.35;
        for (ReplayEvent ev : f.recentHits) {
            DamageSource src = ev.source();
            if (src == DamageSource.FATED_ASHES_BURN || src == DamageSource.BLACKFIRE_BURN || src == DamageSource.LIANDRY_BURN) {
                continue;
            }
            double age = time.get() - ev.time();
            double alpha = Math.max(0, 1 - age / ReplayFrame.NUMBER_SECONDS);
            boolean big = src == DamageSource.ROCKETS || src == DamageSource.RW_ROCKETS
                    || src == DamageSource.GRENADE || src == DamageSource.RE_GRENADE || src == DamageSource.RQ_TURRET_BEAM;
            double ox = ((ev.time() * 7919) % 1 - 0.5) * w * 0.14;   // 같은 시각 숫자가 겹치지 않게 살짝 흩뿌림
            g.setFont(Font.font(null, FontWeight.BOLD, big ? w * 0.03 : w * 0.017));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.setFill(Color.web(ResultPanel.COLORS.getOrDefault(src, "#F0E6D2"), alpha));
            g.fillText(String.format("%.0f", ev.amount()), bx + ox, by - age * h * 0.12);
        }
    }

    // ── 그리기 도우미 ──

    private Spot rqSpot() {
        return rqPosition == RqTurretPosition.FRONT ? layout.rqFront() : layout.rqBack();
    }

    private Spot spotOf(Actor actor, int slot) {
        return switch (actor) {
            case TURRET -> slot >= 0 && slot < layout.turrets().size() ? layout.turrets().get(slot) : null;
            case RQ -> rqSpot();
            case HEIMER -> layout.heimer();
            case DUMMY -> layout.dummy();
        };
    }

    private static void tokenSquare(GraphicsContext g, Image img, double x, double y, double size, Color border, String fallback) {
        g.setFill(Color.web("#1E2328"));
        g.fillRect(x - size / 2, y - size / 2, size, size);
        if (img != null && !img.isError()) {
            g.drawImage(img, x - size / 2, y - size / 2, size, size);
        } else {
            label(g, fallback, x, y, border, size * 0.45);
        }
        g.setStroke(border);
        g.setLineWidth(2);
        g.strokeRect(x - size / 2, y - size / 2, size, size);
    }

    private static void tokenCircle(GraphicsContext g, Image img, double x, double y, double size, Color border, String fallback) {
        g.setFill(Color.web("#0A1428"));
        g.fillOval(x - size / 2, y - size / 2, size, size);
        if (img != null && !img.isError()) {
            g.save();
            g.beginPath();
            g.arc(x, y, size / 2, size / 2, 0, 360);
            g.closePath();
            g.clip();
            g.drawImage(img, x - size / 2, y - size / 2, size, size);
            g.restore();
        } else {
            label(g, fallback, x, y, border, size * 0.3);
        }
        g.setStroke(border);
        g.setLineWidth(2.5);
        g.strokeArc(x - size / 2, y - size / 2, size, size, 0, 360, ArcType.OPEN);
    }

    private static void hpBar(GraphicsContext g, double cx, double top, double width, double ratio, Color color) {
        double hgt = 4;
        g.setFill(Color.web("#1E2328"));
        g.fillRect(cx - width / 2, top, width, hgt);
        g.setFill(color);
        g.fillRect(cx - width / 2, top, width * Math.max(0, Math.min(1, ratio)), hgt);
    }

    private static void label(GraphicsContext g, String text, double x, double y, Color color, double size) {
        g.setFont(Font.font(null, FontWeight.BOLD, Math.max(9, size)));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(color);
        g.fillText(text, x, y);
    }
}
