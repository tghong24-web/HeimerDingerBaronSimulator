package com.hongtg.baronsim.sim;

import java.util.ArrayList;
import java.util.Map;
import java.util.EnumMap;
import java.util.List;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.hongtg.baronsim.calc.CooldownCalculator;
import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Target;
import com.hongtg.baronsim.model.Turret;

public class CombatState {
    private double baronHealth;
    private final double baronStartHealth;
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
    private final TurretKits turretKits;
    private final List<Turret> turrets;
    private double qReadyTime = 0;
    private final TurretSpec turretSpec;
    private Turret rQTurret = null;
    private final List<BurnSpec> burnDamage;
    private final Map<DamageSource, Double> burnUntil = new EnumMap<>(DamageSource.class);;
    private final Map<DamageSource, Boolean> burning = new EnumMap<>(DamageSource.class);;
    private final LowHealthBonus lowHealthBonus;
    private double activeDamageAmp;
    private final ActiveEffect activeEffect;
    private double now = 0;   // 지금 처리 중인 이벤트 시각 (nextEvent가 갱신)
    private final List<ReplayEvent> timeline = new ArrayList<>();   // 9-2: 전투 기록

    public CombatState(double baronStartHealth, TankingStrategy strategy, Heimer heimer, double gazeReduction, TurretKits turretKits, List<Turret> turrets, TurretSpec turretSpec, List<BurnSpec> burnDamage, LowHealthBonus lowHealthBonus, ActiveEffect activeEffect){
        this.baronHealth = baronStartHealth;
        this.baronStartHealth = baronStartHealth;
        this.strategy = strategy;
        this.heimer = heimer;
        this.gazeReduction = gazeReduction;
        this.turretKits = turretKits;
        this.turrets = turrets;
        this.turretSpec = turretSpec;
        this.burnDamage = burnDamage;
        this.lowHealthBonus = lowHealthBonus;
        this.activeEffect = activeEffect;
        this.activeDamageAmp = activeEffect.damageAmp();
        queue = new PriorityQueue<>(Comparator.comparingDouble((Scheduled s) -> s.event().time()).thenComparingLong(Scheduled::seq));
    }

    public void setGazeTarget(Target target) {
        gazeTarget = target;
    }

    public void schedule(Event event) {
        queue.add(new Scheduled(event, nextSeq++));
    }

    public Event nextEvent() {
        Event e = queue.poll().event();
        now = e.time();
        return e;
    }

    public boolean hasEvents() {
        return !queue.isEmpty();
    }

    public void setRQ(Turret turret) {
        rQTurret = turret;
    }

    public void damageBaron(DamageSource source, double amount, Target attacker) {
        // 남작의 시선(Baron Gaze)이 적용 중이라면 딜감소 gazeReduction(0.5)만큼
        boolean gazed = gazeTarget != null && (gazeTarget == attacker || gazeTarget instanceof Heimer);
        
        double withoutGaze = 1.0;
        if (baronHealth <= baronStartHealth * lowHealthBonus.threhold()) 
            withoutGaze += lowHealthBonus.bonus();
        withoutGaze += activeDamageAmp;
        double modifier = withoutGaze;
        if (gazed) modifier -= gazeReduction;
        
        double realWithOutGaze   = Math.min(baronHealth, amount * withoutGaze);
        double finalAmount       = Math.min(baronHealth, amount * modifier);
        
        // 남은 바론 체력보다 더 많은 데미지가 들어갔을 때를 방지해주는 Math.min
        double realDamage       = Math.min(baronHealth, finalAmount);
        double gazeLoss         = realWithOutGaze - realDamage;  
        baronHealth -= realDamage;
        record(ReplayEvent.Kind.HIT, attacker, source, realDamage, baronHealth / baronStartHealth);
        damageBySource.merge(source, realDamage, Double::sum);
        if (gazeLoss > 0) {
            gazeLossBySource.merge(source, gazeLoss, Double::sum);
        }
    }

    // ── 9-2: 전투 기록 ──

    /** 기록 한 줄 추가. 시각은 지금 처리 중인 이벤트 시각, 누가/몇 번 칸인지는 대상 객체로 판단한다. */
    public void record(ReplayEvent.Kind kind, Target who, DamageSource source, double amount, double healthRatio) {
        timeline.add(new ReplayEvent(now, kind, actorOf(who), slotOf(who), source, amount, healthRatio));
    }

    /** 바론 평타 기록: 맞은 뒤 체력 비율, 그리고 죽었으면 사망 기록까지. BaronAttackEvent가 부른다. */
    public void recordBaronAttack(Target target, double damage) {
        double ratio = 1.0;
        if (target instanceof Turret t) {
            ratio = t.health() / t.maxHealth();
        } else if (target instanceof Heimer h) {
            ratio = h.health() / h.maxHealth();
        }
        record(ReplayEvent.Kind.BARON_ATTACK, target, null, damage, ratio);

        if (target instanceof Turret t && t.isDead()) {
            ReplayEvent.Kind died = actorOf(t) == ReplayEvent.Actor.RQ ? ReplayEvent.Kind.RQ_GONE : ReplayEvent.Kind.TURRET_DIED;
            record(died, t, null, 0, 0);
        } else if (target instanceof Heimer h && h.isDead()) {
            record(ReplayEvent.Kind.HEIMER_DIED, h, null, 0, 0);
        }
    }

    private ReplayEvent.Actor actorOf(Target who) {
        if (who instanceof Heimer) return ReplayEvent.Actor.HEIMER;
        if (who instanceof Dummy) return ReplayEvent.Actor.DUMMY;
        return slotOf(who) >= 0 ? ReplayEvent.Actor.TURRET : ReplayEvent.Actor.RQ;
    }

    /** turrets 리스트에서 몇 번째 칸인지. Turret은 equals를 안 바꿨으므로 indexOf는 같은 객체(주소)를 찾는다. 없으면 -1 */
    private int slotOf(Target who) {
        return (who instanceof Turret) ? turrets.indexOf(who) : -1;
    }

    public List<ReplayEvent> timeline() {
        return List.copyOf(timeline);
    }

    public void refreshBurns(double time) {
        for (BurnSpec spec : burnDamage) {
            burnUntil.put(spec.source(), time + 3);

            if (!burning.getOrDefault(spec.source(), false)){
                burning.put(spec.source(), true);        
                schedule(new BurnTickEvent(time + 0.5, spec));
            }
        }    
    }

    public double basicReadyTime(double start, double cooldown){
        return CooldownCalculator.readyTime(start, cooldown, activeEffect.durationSeconds(), activeEffect.cooldownRate());
    }

    public void endActive() {
        activeDamageAmp = 0;
    }

    public double baronStartHealth(){
        return baronStartHealth;
    }

    public void stopBurning(DamageSource source) {
        burning.put(source, false);
    }
    
    public double burnUntil(DamageSource source) {
        return burnUntil.getOrDefault(source, 0.0);
    }
    
    public List<Turret> turrets() {
        return turrets;
    }

    public TurretKits turretKits() {
        return turretKits;
    }
    
    public TurretSpec turretSpec() {
        return turretSpec;
    }

    public Turret rQTurret() {
        return rQTurret;
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

    public double qReadyTime() {
        return qReadyTime;
    }

    public boolean isQReady(double time) {
        return time >= qReadyTime;
    }

    public void useQ(double time, double cooldown) {
        qReadyTime = basicReadyTime(time, cooldown);   // 실체화 장비: Q 사이 대기도 기본 스킬 대기로 가속
    }

    public boolean isRReady(double time) {
        return time >= rReadyTime;
    }

    public void useR(double time, double cooldown) {
        rReadyTime = time + cooldown;
    }

}
