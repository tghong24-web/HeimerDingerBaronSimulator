package com.hongtg.baronsim.sim;

import java.util.Comparator;
import java.util.PriorityQueue;

public class CombatState {
    private double baronHealth;
    private final PriorityQueue<Event> queue;

    public CombatState(double baronStartHealth){
        this.baronHealth = baronStartHealth;
        queue = new PriorityQueue<>(Comparator.comparingDouble(Event::time));
    }

    public void schedule(Event event) {
        queue.add(event);
    }

    public Event nextEvent() {
        return queue.poll();
    }

    public boolean hasEvents() {
        return !queue.isEmpty();
    }

    public void damageBaron(double amount) {
        baronHealth -= amount;
    }

    public double baronHealth() {
        return baronHealth;
    }

    public boolean isBaronDead() {
        return baronHealth <= 0;
    }
}
