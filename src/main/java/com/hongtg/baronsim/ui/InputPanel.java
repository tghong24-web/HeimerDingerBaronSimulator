package com.hongtg.baronsim.ui;

import com.hongtg.baronsim.sim.Build;
import com.hongtg.baronsim.sim.RoleQuest;
import com.hongtg.baronsim.sim.RqTurretPosition;
import com.hongtg.baronsim.sim.SimulationInput;
import com.hongtg.baronsim.sim.UltimateChoice;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * 왼쪽 입력 패널 (시안 A안): 레벨, 스킬, 포탑, 탱킹·퀘스트.
 * 빌드(아이템·추가 스탯)는 BuildPanel이 맡고, toInput에서 둘을 합쳐 SimulationInput을 만든다.
 * 잘못된 값 검사는 SimulationInput과 Simulator가 이미 하므로 여기서는 하지 않는다 (예외 메시지를 화면에 띄운다).
 */
final class InputPanel extends VBox {

    /** 시뮬레이션 제한 시간(초). 바론을 못 잡는 조합도 끝나도록 넉넉히 */
    private static final double TIME_LIMIT = 300;

    private final Spinner<Integer> baronLevel = UiKit.intSpinner(1, 18, 15);
    private final Spinner<Integer> heimerLevel = UiKit.intSpinner(1, 20, 13);
    private final Spinner<Integer> q = UiKit.intSpinner(1, 5, 5);
    private final Spinner<Integer> w = UiKit.intSpinner(0, 5, 5);
    private final Spinner<Integer> e = UiKit.intSpinner(0, 5, 0);
    private final Spinner<Integer> r = UiKit.intSpinner(0, 3, 0);
    private final ToggleGroup ultimate = new ToggleGroup();
    private final Spinner<Integer> turretCount = UiKit.intSpinner(0, 3, 3);
    private final Spinner<Integer> kitCount = UiKit.intSpinner(0, 3, 0);
    private final Spinner<Double> restKit = UiKit.doubleSpinner(0, 20, 20, 0.5);
    private final CheckBox dummy = new CheckBox("병풍 있음");
    private final ToggleGroup rqPosition = new ToggleGroup();
    private final ComboBox<RoleQuest> quest = new ComboBox<>();

    private final UiKit.IconBox baronIcon = new UiKit.IconBox(22, "");
    private final UiKit.IconBox heimerIcon = new UiKit.IconBox(22, "");
    private final UiKit.IconBox[] skillIcons = {
        new UiKit.IconBox(40, "Q"), new UiKit.IconBox(40, "W"), new UiKit.IconBox(40, "E"), new UiKit.IconBox(40, "R")
    };

    InputPanel() {
        getStyleClass().add("side");
        setSpacing(12);
        dummy.setSelected(true);

        quest.getItems().addAll(RoleQuest.values());
        quest.setValue(RoleQuest.NONE);
        quest.setMaxWidth(Double.MAX_VALUE);
        quest.setConverter(new StringConverter<>() {
            @Override
            public String toString(RoleQuest rq) {
                if (rq == null) return "";
                return switch (rq) {
                    case NONE -> "없음";
                    case TOP -> "탑 (19~20레벨)";
                    case MID -> "미드 (주문투척자의 신발)";
                    case BOT -> "원딜 (신발 칸 따로)";
                };
            }

            @Override
            public RoleQuest fromString(String s) {
                return quest.getValue();
            }
        });

        getChildren().addAll(levelCard(), skillCard(), turretCard(), tankCard());
        loadIcons();
    }

    // ── 카드 ──

    private VBox levelCard() {
        GridPane g = twoColumns();
        g.add(UiKit.field(baronIcon, "바론", baronLevel), 0, 0);
        g.add(UiKit.field(heimerIcon, "하이머", heimerLevel), 1, 0);
        return UiKit.card("레벨", g);
    }

    private VBox skillCard() {
        GridPane g = new GridPane();
        g.setHgap(8);
        Spinner<?>[] spinners = { q, w, e, r };
        for (int i = 0; i < 4; i++) {
            VBox cell = new VBox(4, skillIcons[i], spinners[i]);
            cell.setAlignment(Pos.CENTER);
            spinners[i].setMaxWidth(Double.MAX_VALUE);
            spinners[i].getStyleClass().add("compact");
            g.add(cell, i, 0);
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(25);
            g.getColumnConstraints().add(c);
        }
        VBox ult = UiKit.field("궁극기 강화",
                UiKit.segmented(ultimate, UltimateChoice.values(), new String[] { "없음", "RQ", "RW", "RE" }, 0));
        return UiKit.card("스킬", g, ult);
    }

    private VBox turretCard() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.add(UiKit.field("설치됨", turretCount), 0, 0);
        g.add(UiKit.field("키트", kitCount), 1, 0);
        g.add(UiKit.field("충전(초)", restKit), 2, 0);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100 / 3.0);
            g.getColumnConstraints().add(c);
        }
        Label hint = UiKit.styled("충전(초): 다음 키트가 찰 때까지 남은 시간", "hint");
        hint.setWrapText(true);
        return UiKit.card("포탑", g, hint);
    }

    private VBox tankCard() {
        VBox rq = UiKit.field("RQ 터렛 위치",
                UiKit.segmented(rqPosition, RqTurretPosition.values(), new String[] { "앞", "뒤" }, 0));
        return UiKit.card("탱킹 · 퀘스트", dummy, rq, UiKit.field("역할군 퀘스트", quest));
    }

    private static GridPane twoColumns() {
        GridPane g = new GridPane();
        g.setHgap(10);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(50);
            c.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    private void loadIcons() {
        for (int i = 0; i < 4; i++) {
            UiKit.IconBox box = skillIcons[i];
            Icons.spell("QWER".charAt(i), box::setImage);
        }
        Icons.champion(heimerIcon::setImage);
        Image baron = Icons.resource("baron_token.png", 64);
        if (baron != null) {
            baronIcon.setImage(baron);
        }
    }

    // ── 값 꺼내기 ──

    /** 입력 패널 값 + 빌드로 시뮬레이션 입력을 만든다. 값이 잘못되면 IllegalArgumentException */
    SimulationInput toInput(Build build) {
        return new SimulationInput(
                baronLevel.getValue(), heimerLevel.getValue(),
                q.getValue(), w.getValue(), e.getValue(), r.getValue(),
                ultimate(), build,
                turretCount.getValue(), kitCount.getValue(), restKit.getValue(),
                (RqTurretPosition) rqPosition.getSelectedToggle().getUserData(),
                dummy.isSelected(), TIME_LIMIT, quest.getValue());
    }

    UltimateChoice ultimate() {
        return (UltimateChoice) ultimate.getSelectedToggle().getUserData();
    }

    boolean hasDummy() {
        return dummy.isSelected();
    }

    ReadOnlyObjectProperty<RoleQuest> questProperty() {
        return quest.valueProperty();
    }

    int baronLevel() {
        return baronLevel.getValue();
    }

    int turretCount() {
        return turretCount.getValue();
    }
}
