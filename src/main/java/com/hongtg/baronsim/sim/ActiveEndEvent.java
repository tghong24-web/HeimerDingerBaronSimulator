package com.hongtg.baronsim.sim;

public record ActiveEndEvent(double time) implements Event{
    @Override
    public void apply(CombatState state) {
        state.endActive();
    }

} 
