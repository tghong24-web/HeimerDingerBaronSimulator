package com.hongtg.baronsim.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.hongtg.baronsim.data.ItemData;
import com.hongtg.baronsim.sim.Build;
import com.hongtg.baronsim.sim.BuildCalculator;
import com.hongtg.baronsim.sim.FinalStats;
import com.hongtg.baronsim.sim.RoleQuest;

import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 가운데 위 빌드 패널 (시안 A안): 아이템 6칸 + 신발 칸, 추가 스탯, 영광 중첩, 최종 스탯(색 강조).
 * 칸을 누르면 아이템 목록이 뜬다. 조합 규칙(중복, 고유 그룹, 칸 수)은 BuildCalculator가 검사하고,
 * 어기면 스탯 줄에 빨간 글씨로 이유를 보여 준다.
 */
final class BuildPanel extends VBox {

    private static final int SLOTS = 6;

    private final ItemData items;
    private final Supplier<RoleQuest> quest;
    private final String[] slotIds = new String[SLOTS];
    private final Button[] slotButtons = new Button[SLOTS];
    private String bootsId;
    private final Button bootsButton = new Button("신발");

    private final Spinner<Double> bonusAp = UiKit.doubleSpinner(0, 2000, 18, 1);
    private final Spinner<Double> bonusHaste = UiKit.doubleSpinner(0, 500, 0, 1);
    private final Spinner<Double> bonusFlatPen = UiKit.doubleSpinner(0, 200, 0, 1);
    private final Spinner<Double> bonusPerPen = UiKit.doubleSpinner(0, 100, 0, 1);
    private final Spinner<Integer> glory = UiKit.intSpinner(0, 25, 0);

    private final Label apValue = UiKit.styled("-", "stat-ap");
    private final Label penValue = UiKit.styled("-", "stat-pen");
    private final Label hasteValue = UiKit.styled("-", "stat-haste");
    private final Label buildError = UiKit.styled("", "error");

    private Runnable onChange = () -> { };

    BuildPanel(ItemData items, Supplier<RoleQuest> quest) {
        this.items = items;
        this.quest = quest;
        getStyleClass().add("card");
        setSpacing(12);

        // 제목 + 스탯
        HBox stats = new HBox(16,
                labeled("주문력", apValue), labeled("마법 관통", penValue), labeled("스킬 가속", hasteValue));
        stats.setAlignment(Pos.CENTER_LEFT);
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox head = new HBox(12, UiKit.sectionTitle("빌드"), grow, stats);
        head.setAlignment(Pos.CENTER_LEFT);

        // 아이템 칸
        HBox slots = new HBox(8);
        slots.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < SLOTS; i++) {
            int index = i;
            Button b = slotButton();
            b.setOnAction(e -> showMenu(b, false, id -> {
                slotIds[index] = id;
                refreshSlot(b, id);
            }));
            slotButtons[i] = b;
            slots.getChildren().add(b);
        }
        Region divider = new Region();
        divider.getStyleClass().add("divider");
        bootsButton.getStyleClass().addAll("slot", "slot-boots");
        bootsButton.setOnAction(e -> showMenu(bootsButton, true, id -> {
            bootsId = id;
            refreshSlot(bootsButton, id);
        }));
        slots.getChildren().addAll(divider, bootsButton);

        // 추가 스탯 (룬·파편 등, 아이템 밖의 스탯)
        FlowPane bonus = new FlowPane(10, 8,
                small("추가 주문력", bonusAp), small("추가 가속", bonusHaste),
                small("고정 관통", bonusFlatPen), small("% 관통", bonusPerPen), small("영광 중첩", glory));
        Label bonusHint = UiKit.styled("추가 스탯: 룬·파편처럼 아이템 밖에서 오는 스탯 (기본값 18 = 적응형 파편 2개)", "hint");

        buildError.setWrapText(true);
        getChildren().addAll(head, slots, bonus, bonusHint, buildError);

        for (Spinner<?> s : List.of(bonusAp, bonusHaste, bonusFlatPen, bonusPerPen, glory)) {
            s.valueProperty().addListener((o, a, b) -> changed());
        }
    }

    void setOnChange(Runnable r) {
        onChange = r;
    }

    // ── 값 ──

    Build toBuild() {
        List<String> ids = new ArrayList<>();
        for (String id : slotIds) {
            if (id != null) ids.add(id);
        }
        if (bootsId != null) ids.add(bootsId);
        return new Build(ids, bonusAp.getValue(), bonusHaste.getValue(), bonusFlatPen.getValue(),
                bonusPerPen.getValue() / 100.0, glory.getValue());
    }

    boolean has(String itemId) {
        return toBuild().items().contains(itemId);
    }

    /** 스탯 줄 다시 계산. 조합이 잘못되면 이유를 보여 준다 */
    void refreshStats() {
        syncGloryLimit();
        try {
            FinalStats f = BuildCalculator.calculate(items, toBuild(), quest.get());
            apValue.setText(fmt(f.ap()));
            penValue.setText(fmt(f.flatPen()) + " · " + fmt(f.perPen() * 100) + "%");
            hasteValue.setText(fmt(f.abilityHaste()));
            buildError.setText("");
        } catch (IllegalArgumentException e) {
            apValue.setText("-");
            penValue.setText("-");
            hasteValue.setText("-");
            buildError.setText("조합 오류: " + e.getMessage());
        }
    }

    /** 영광 아이템(암흑의 인장 10, 메자이 25)이 있을 때만 중첩을 넣을 수 있게 상한을 맞춘다 */
    private void syncGloryLimit() {
        int max = 0;
        for (String id : toBuild().items()) {
            max = Math.max(max, items.item(id).maxStacks());
        }
        var factory = (javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory) glory.getValueFactory();
        factory.setMax(max);
        glory.setDisable(max == 0);
        if (glory.getValue() > max) {
            factory.setValue(max);
        }
    }

    // ── 칸과 메뉴 ──

    private Button slotButton() {
        Button b = new Button("+");
        b.getStyleClass().add("slot");
        return b;
    }

    /** 칸을 누르면 뜨는 아이템 목록. 신발 칸은 신발만, 아이템 칸은 신발 빼고 */
    private void showMenu(Button anchor, boolean boots, java.util.function.Consumer<String> pick) {
        ContextMenu menu = new ContextMenu();
        MenuItem clear = new MenuItem("(비우기)");
        clear.setOnAction(e -> {
            pick.accept(null);
            changed();
        });
        menu.getItems().addAll(clear, new SeparatorMenuItem());

        boolean legendaryDone = false;
        for (ItemData.Item it : sorted()) {
            if (it.boots() != boots) continue;
            if (it.id().equals("spellslinger") && quest.get() != RoleQuest.MID) continue;   // MID 퀘스트에서만
            if (!boots && !it.legendary() && !legendaryDone) {
                menu.getItems().add(new SeparatorMenuItem());   // 완성템과 하위 아이템 사이
                legendaryDone = true;
            }
            MenuItem m = new MenuItem(it.name() + "  (" + it.cost() + ")");
            ImageView icon = new ImageView();
            icon.setFitWidth(20);
            icon.setFitHeight(20);
            m.setGraphic(icon);
            Icons.item(it.name(), icon::setImage);
            m.setOnAction(e -> {
                pick.accept(it.id());
                changed();
            });
            menu.getItems().add(m);
        }
        menu.show(anchor, Side.BOTTOM, 0, 4);
    }

    /** 완성템 먼저, 그다음 하위 아이템. 각각 가격 높은 순 */
    private List<ItemData.Item> sorted() {
        List<ItemData.Item> list = new ArrayList<>(items.items());
        list.sort((a, b) -> {
            if (a.legendary() != b.legendary()) return a.legendary() ? -1 : 1;
            return Integer.compare(b.cost(), a.cost());
        });
        return list;
    }

    private void refreshSlot(Button b, String id) {
        b.getStyleClass().remove("slot-filled");
        b.setGraphic(null);
        b.setTooltip(null);
        if (id == null) {
            b.setText(b == bootsButton ? "신발" : "+");
            return;
        }
        ItemData.Item it = items.item(id);
        b.getStyleClass().add("slot-filled");
        b.setText(shortName(it.name()));
        b.setTooltip(new Tooltip(it.name()));
        Icons.item(it.name(), img -> {
            if (!id.equals(b == bootsButton ? bootsId : slotIdOf(b))) return;   // 그 사이 다른 아이템으로 바뀌었으면 무시
            ImageView v = new ImageView(img);
            v.setFitWidth(52);
            v.setFitHeight(52);
            b.setGraphic(v);
            b.setText("");
        });
    }

    private String slotIdOf(Button b) {
        for (int i = 0; i < SLOTS; i++) {
            if (slotButtons[i] == b) return slotIds[i];
        }
        return null;
    }

    private void changed() {
        refreshStats();
        onChange.run();
    }

    // ── 작은 도우미 ──

    private static String shortName(String name) {
        String s = name.replace("의 ", " ");
        return s.length() <= 6 ? s : s.substring(0, 6);
    }

    private static HBox labeled(String name, Label value) {
        HBox h = new HBox(6, new Label(name), value);
        h.setAlignment(Pos.BASELINE_LEFT);
        return h;
    }

    private static VBox small(String name, Spinner<?> s) {
        s.setPrefWidth(96);
        return new VBox(4, new Label(name), s);
    }

    static String fmt(double v) {
        return Math.abs(v - Math.rint(v)) < 1e-9 ? String.valueOf((long) Math.rint(v)) : String.format("%.1f", v);
    }
}
