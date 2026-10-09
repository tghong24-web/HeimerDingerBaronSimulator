package com.hongtg.baronsim.sim;

public interface Event {
    // 이 이벤트가 일어나는 시각(초)을 돌려주는 메서드
    double time();

    // 이 이벤트를 실행하는 메서드 (CombatState를 받음)
    // CombatState란? 
    // 바론의 피 상황, 이벤트 큐, 이벤트를 큐에 넣는 schedule 메서드 등이 들어가있는 클래스
    void apply(CombatState state);
} 
