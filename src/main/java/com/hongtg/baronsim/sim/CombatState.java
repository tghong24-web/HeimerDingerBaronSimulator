package com.hongtg.baronsim.sim;

import java.util.Map;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Target;

public class CombatState {
    private double baronHealth;
    private final Map<DamageSource, Double> damageBySource = new EnumMap<>(DamageSource.class);
    private final Map<DamageSource, Double> gazeLossBySource = new EnumMap<>(DamageSource.class);
    private double rReadyTime = 0;
    private final TankingStrategy strategy;
    private final Heimer heimer;
    private Target gazeTarget;
    private final double gazeReduction;
    private record Scheduled(Event event, long seq) { }
    private long nextSeq = 0;
    private final PriorityQueue<Scheduled> queue;

    public CombatState(double baronStartHealth, TankingStrategy strategy, Heimer heimer, double gazeReduction){
        this.baronHealth = baronStartHealth;
        this.strategy = strategy;
        this.heimer = heimer;
        this.gazeReduction = gazeReduction;
        queue = new PriorityQueue<>(Comparator.comparingDouble((Scheduled s) -> s.event().time()).thenComparingLong(Scheduled::seq));
    }

    public void setGazeTarget(Target target) {
        gazeTarget = target;
    }

    public void schedule(Event event) {
        queue.add(new Scheduled(event, nextSeq++));
    }

    public Event nextEvent() {
        return queue.poll().event();
    }

    public boolean hasEvents() {
        return !queue.isEmpty();
    }

    public void damageBaron(DamageSource source, double amount, Target attacker) {
        // 남작의 시선(Baron Gaze)이 적용 중이라면 딜감소 gazeReduction(0.5)만큼
        boolean gazed = gazeTarget != null && (gazeTarget == attacker || gazeTarget instanceof Heimer);
        double afterGazedAmount = gazed ? amount * (1 - gazeReduction) : amount;
        double realWithOutGaze  = Math.min(baronHealth, amount);
        // 남은 바론 체력보다 더 많은 데미지가 들어갔을 때를 방지해주는 Math.min
        double realDamage       = Math.min(baronHealth, afterGazedAmount);
        double gazeLoss         = realWithOutGaze - realDamage;  
        baronHealth -= realDamage;
        damageBySource.merge(source, realDamage, Double::sum);
        if (gazeLoss > 0) {
            gazeLossBySource.merge(source, gazeLoss, Double::sum);
        }
    }
    
    public Map<DamageSource, Double> damageBySource() {
        return Map.copyOf(damageBySource);
    }
    
    public Map<DamageSource, Double> gazeLossBySource() {
        return Map.copyOf(gazeLossBySource);
    }

    public Heimer heimer() {
        return heimer;
    }
    
    public double baronHealth() {
        return baronHealth;
    }

    public boolean isBaronDead() {
        return baronHealth <= 0;
    }

    public TankingStrategy tankingStrategy() {
        return strategy;
    }

    public boolean isRReady(double time) {
        return time >= rReadyTime;
    }

    public void useR(double time, double cooldown) {
        rReadyTime = time + cooldown;
    }

}
