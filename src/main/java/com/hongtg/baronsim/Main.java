package com.hongtg.baronsim;

import com.hongtg.baronsim.ui.BaronSimApp;

import javafx.application.Application;

/**
 * 프로그램 진입점. JavaFX 창(BaronSimApp)을 띄운다 (9-1단계).
 *
 * Application을 상속한 클래스를 바로 main 클래스로 쓰면, 모듈 설정 없이 실행할 때
 * "JavaFX runtime components are missing" 오류가 난다. 그래서 상속하지 않은 이 클래스에서 launch를 부른다.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(BaronSimApp.class, args);
    }
}
