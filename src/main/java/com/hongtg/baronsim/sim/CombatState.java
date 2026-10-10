package com.hongtg.baronsim.sim;

import java.util.Map;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.PriorityQueue;

public class CombatState {
    private double baronHealth;
    private final PriorityQueue<Event> queue;
    private final Map<DamageSource, Double> damageBySource = new EnumMap<>(DamageSource.class);

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

    public void damageBaron(DamageSource source, double amount) {
        // 남은 바론 체력보다 더 많은 데미지가 들어갔을 때를 방지해주는 Math.min
        double realDamage = Math.min(baronHealth, amount);  
        baronHealth -= realDamage;
        damageBySource.merge(source, realDamage, Double::sum);
    }
    
    public Map<DamageSource, Double> damageBySource() {
        return Map.copyOf(damageBySource);
    }

    public double baronHealth() {
        return baronHealth;
    }

    public boolean isBaronDead() {
        return baronHealth <= 0;
    }
}
