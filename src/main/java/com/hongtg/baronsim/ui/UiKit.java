package com.hongtg.baronsim.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * 화면 곳곳에서 쓰는 작은 부품 모음. 정적 메서드만 있다 (C++의 namespace 함수 모음과 같은 용도).
 * 모양은 app.css의 클래스 이름으로 정한다.
 */
final class UiKit {

    private UiKit() {
    }

    /** 제목이 있는 패널 (시안의 남색 카드) */
    static VBox card(String title, Node... children) {
        VBox box = new VBox(10);
        box.getStyleClass().add("card");
        if (title != null) {
            box.getChildren().add(sectionTitle(title));
        }
        box.getChildren().addAll(children);
        return box;
    }

    static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    static Label styled(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    /** 위에 이름, 아래에 입력칸 */
    static VBox field(String label, Node control) {
        VBox box = new VBox(4, new Label(label), control);
        if (control instanceof javafx.scene.layout.Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    /** 위에 아이콘 + 이름, 아래에 입력칸 */
    static VBox field(Node icon, String label, Node control) {
        HBox head = new HBox(6, icon, new Label(label));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(4, head, control);
        if (control instanceof javafx.scene.layout.Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    /** 정수 입력칸. 직접 쳐서 넣은 값도 포커스가 빠지면 반영된다 */
    static Spinner<Integer> intSpinner(int min, int max, int initial) {
        Spinner<Integer> s = new Spinner<>(min, max, initial);
        commitOnFocusLost(s);
        return s;
    }

    static Spinner<Double> doubleSpinner(double min, double max, double initial, double step) {
        Spinner<Double> s = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step));
        commitOnFocusLost(s);
        return s;
    }

    private static <T> void commitOnFocusLost(Spinner<T> s) {
        s.setEditable(true);
        s.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                try {
                    s.increment(0);   // 입력한 글자를 값으로 확정
                } catch (RuntimeException e) {
                    s.getEditor().setText(String.valueOf(s.getValue()));   // 숫자가 아니면 원래 값으로
                }
            }
        });
    }

    /**
     * 여러 개 중 하나만 고르는 버튼 줄 (시안의 RQ/RW/RE, 앞/뒤). 각 버튼의 userData에 값을 넣는다.
     * 이미 고른 버튼을 다시 눌러도 선택이 풀리지 않게 막는다.
     */
    static HBox segmented(ToggleGroup group, Object[] values, String[] labels, int selectedIndex) {
        HBox row = new HBox();
        row.getStyleClass().add("segmented");
        for (int i = 0; i < values.length; i++) {
            ToggleButton b = new ToggleButton(labels[i]);
            b.setUserData(values[i]);
            b.setToggleGroup(group);
            b.getStyleClass().add("seg");
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            b.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                if (b.isSelected()) {
                    e.consume();
                }
            });
            row.getChildren().add(b);
        }
        group.selectToggle((Toggle) row.getChildren().get(selectedIndex));
        return row;
    }

    /**
     * 아이콘 자리. 그림이 오기 전에는 글자(Q, W …)를 보여 주고, setImage로 그림이 오면 그림을 덮는다.
     */
    static final class IconBox extends StackPane {
        private final ImageView view = new ImageView();

        IconBox(double size, String placeholder) {
            getStyleClass().add("icon-box");
            setMinSize(size, size);
            setPrefSize(size, size);
            setMaxSize(size, size);
            Label l = new Label(placeholder);
            l.getStyleClass().add("icon-placeholder");
            view.setFitWidth(size - 2);
            view.setFitHeight(size - 2);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            getChildren().addAll(l, view);
        }

        void setImage(Image image) {
            view.setImage(image);
        }
    }
}
