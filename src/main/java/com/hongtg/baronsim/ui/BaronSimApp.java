package com.hongtg.baronsim.ui;

import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.data.ItemData;
import com.hongtg.baronsim.sim.SimulationInput;
import com.hongtg.baronsim.sim.Simulator;
import com.hongtg.baronsim.sim.SimulatorResult;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * 메인 창 (9-1단계, 시안 A안).
 *   왼쪽: InputPanel (스크롤) | 가운데: BuildPanel + 체력 그래프/리플레이 | 오른쪽: ResultPanel (스크롤)
 * "시뮬레이션 실행"을 누르면 입력 → SimulationInput → Simulator.run → 세 화면에 결과를 뿌린다.
 * 잘못된 입력은 SimulationInput·Simulator가 던지는 예외 메시지를 위쪽에 빨간 글씨로 보여 준다.
 *
 * Main.main이 Application.launch(BaronSimApp.class)로 띄운다.
 */
public class BaronSimApp extends Application {

    private final ItemData items = DataLoader.loadItems();
    private final Simulator simulator = new Simulator();

    private InputPanel input;
    private BuildPanel build;
    private ResultPanel result;
    private HealthChart chart;
    private ReplayView replay;
    private final Label error = UiKit.styled("", "error");
    private final ToggleGroup view = new ToggleGroup();

    @Override
    public void start(Stage stage) {
        input = new InputPanel();
        build = new BuildPanel(items, () -> input.questProperty().get());
        result = new ResultPanel();
        chart = new HealthChart();
        replay = new ReplayView(DataLoader.loadReplayLayout());

        build.setOnChange(this::clearError);
        input.questProperty().addListener((o, a, b) -> build.refreshStats());
        build.refreshStats();

        // 리플레이를 보는 동안 사용 횟수를 재생 시각에 맞춘다
        replay.timeProperty().addListener((o, a, t) -> {
            if (replay.isVisible()) result.showCountsAt(t.doubleValue());
        });

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app");
        root.setTop(header());
        root.setCenter(body());

        Scene scene = new Scene(root, 1440, 900);
        scene.getStylesheets().add(BaronSimApp.class.getResource("app.css").toExternalForm());
        stage.setTitle("바론 처치 시뮬레이터 — 하이머딩거");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();

        runSimulation();   // 기본값으로 한 번 돌려서 빈 화면이 안 보이게
    }

    private HBox header() {
        Label title = UiKit.styled("바론 처치 시뮬레이터", "app-title");
        Label chip = UiKit.styled("하이머딩거", "chip");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button run = new Button("시뮬레이션 실행");
        run.getStyleClass().add("run-button");
        run.setDefaultButton(true);   // Enter로도 실행
        run.setOnAction(e -> runSimulation());
        error.setWrapText(true);
        error.setMaxWidth(560);
        HBox h = new HBox(14, title, chip, grow, error, run);
        h.getStyleClass().add("header");
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private HBox body() {
        ScrollPane left = scroll(input, 300);

        // 가운데: 빌드 + (체력 그래프 | 리플레이)
        HBox tabs = UiKit.segmented(view, new Object[] { "chart", "replay" }, new String[] { "체력 그래프", "리플레이 ▶" }, 0);
        tabs.setMaxWidth(260);
        chart.getStyleClass().add("card");
        replay.getStyleClass().add("card");
        replay.setVisible(false);
        StackPane views = new StackPane(chart, replay);
        VBox.setVgrow(views, Priority.ALWAYS);
        view.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) return;
            boolean showReplay = "replay".equals(b.getUserData());
            chart.setVisible(!showReplay);
            replay.setVisible(showReplay);
            if (showReplay) {
                result.showCountsAt(replay.timeProperty().get());
            } else {
                replay.pause();
                result.showCountsAt(Double.MAX_VALUE);
            }
        });
        VBox center = new VBox(12, build, tabs, views);
        HBox.setHgrow(center, Priority.ALWAYS);
        center.setMinWidth(480);

        ScrollPane right = scroll(result, 300);

        HBox body = new HBox(16, left, center, right);
        body.getStyleClass().add("body");
        return body;
    }

    private static ScrollPane scroll(Region content, double width) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setMinWidth(width);
        sp.setPrefWidth(width);
        sp.setMaxWidth(width);
        return sp;
    }

    private void runSimulation() {
        try {
            SimulationInput in = input.toInput(build.toBuild());
            SimulatorResult r = simulator.run(in);
            boolean shadowflame = in.build().items().contains("shadowflame");

            result.show(r, String.format("바론 %d레벨 · 포탑 %d · %s",
                    in.baronLevel(), in.turretCount(), in.hasDummy() ? "병풍 있음" : "병풍 없음"));
            chart.show(r, shadowflame);
            replay.load(r, in.hasDummy(), in.rqPosition(), shadowflame);
            if (replay.isVisible()) result.showCountsAt(0);
            error.setText("");
        } catch (IllegalArgumentException | IllegalStateException e) {
            error.setText(e.getMessage());
        }
    }

    private void clearError() {
        error.setText("");
    }
}
