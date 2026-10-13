package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.BaronData;
import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;
import com.hongtg.baronsim.data.HeimerData;
import com.hongtg.baronsim.data.ItemData;
import com.hongtg.baronsim.model.BaronStats;

import java.util.ArrayList;
import java.util.List;

import com.hongtg.baronsim.calc.CooldownCalculator;
import com.hongtg.baronsim.calc.DamageCalculator;

public class Simulator {
    public SimulatorResult run(SimulationInput input) {
        ItemData itemData = DataLoader.loadItems();
        FinalStats stats = BuildCalculator.calculate(itemData, input.build(), input.quest());

        BaronData baronData = DataLoader.loadBaron();
        HeimerData heimerData = DataLoader.loadHeimer();

        BaronStats baron = baronData.toScaler().statsAt(input.baronLevel());

        // 1.CombatState에 바론 체력 받기 + Heimer 생성
        double heimerHp = heimerData.stats().health().valueAt(input.heimerLevel()) + stats.health();
        double heimerArmor = heimerData.stats().armor().valueAt(input.heimerLevel()) + stats.armor();
        double heimerMr = heimerData.stats().magicResist().valueAt(input.heimerLevel()) + stats.magicResist();
        Heimer heimer = new Heimer(heimerHp, heimerArmor, heimerMr);
        
        double rCooldown = 0;
        if(input.rLevel() > 0){
            rCooldown = CooldownCalculator.reducedCooldown(heimerData.upgrade().cooldownAtRank(input.rLevel()), stats.abilityHaste());
        }

        // Q 데이터 읽어오기
        HeimerData.TurretData turret = heimerData.turret();

        double shotDamage = turret.shotDamageAtRank(input.qLevel());
        double shotAP = turret.shotApRatio();
        double shotInterval = turret.attackIntervalSeconds();

        double baseQDamage = shotDamage + shotAP * stats.ap();
        double finalQDamage = DamageCalculator.reducedDamage(baseQDamage, baron.magicResist(), stats.perPen(),
                stats.flatPen());

        double turretHp = heimerData.turret().healthAt(input.heimerLevel(), stats.ap());
        double turretArmor = heimerData.turret().armorAt(input.heimerLevel());
        double turretMr = heimerData.turret().magicResistAt(input.heimerLevel());

        // 현재 터렛키트의 상태 생성
        TurretKits turretKits = new TurretKits((input.currentKitCount()), heimerData.turret().maxKits(), CooldownCalculator.reducedCooldown(heimerData.turret().kitRechargeSeconds(), stats.abilityHaste()));
        
        if(turretKits.rechargeSeconds() < input.restKitChargeSecond()){
            throw new IllegalArgumentException("남은 포탑 충전 대기시간은 "+ turretKits.rechargeSeconds() + "이하여야합니다");
        }

        // 설정한 터렛의 개수만큼 새로운 터렛들을 리스트에 추가
        List<Turret> turrets = new ArrayList<>();
        for (int i = 0; i < input.turretCount(); i++) {
            Turret t = new Turret(turretHp, turretArmor, turretMr);
            turrets.add(t);
        }

        // Target Mode 정하기
        TankingStrategy strategy;
        if (input.hasDummy()) {
            strategy = new DummyTankingStrategy(new Dummy(0, 0));
        } else {
            strategy = new NoDummyTankingStrategy(turrets, heimer, input.rqPosition());
        }

        TurretSpec turretSpec = new TurretSpec(turretHp, turretArmor, turretMr, finalQDamage, shotInterval, CooldownCalculator.reducedCooldown(turret.cooldownSeconds(), stats.abilityHaste()), turret.maxTurrets());

        // 아이템 고유 효과 확인(도트딜, 그림자불꽃)
        LowHealthBonus lowHealthBonus = new LowHealthBonus(0, 0);
        List<BurnSpec> burns = new ArrayList<>();
        double activeDamageAmp = 0;
        double activeDuration = 0;
        double activeCooldownRate = 1;

        for (String id: input.build().items()){
            ItemData.Item item = itemData.item(id);

            // 그림자불꽃인지
            if (item.lowHealthDamageBonus() > 0) {
                lowHealthBonus = new LowHealthBonus(item.lowHealthThreshold(), item.lowHealthDamageBonus());
            }
            if (item.hasActive()) {
                activeDamageAmp = item.activeDamageAmp() + item.activeDamageAmpPerMana() * stats.mana();
                activeDuration = item.activeDurationSeconds();
                activeCooldownRate = item.activeCooldownRate();
            }
            // 도트딜있는 템인지
            if (!item.hasBurn()) {
                continue;
            }

            double baseBurn = item.burnPerTick() + item.burnApRatioPerTick() * stats.ap();
            double finalBurn = DamageCalculator.reducedDamage(baseBurn, baron.magicResist(), stats.perPen(), stats.flatPen());
        
            DamageSource source = switch (id) {
                case "fatedashes" -> DamageSource.FATED_ASHES_BURN;
                case "blackfire" -> DamageSource.BLACKFIRE_BURN;
                case "liandrys" -> DamageSource.LIANDRY_BURN;
                default -> throw new IllegalStateException("화상 출처가 없는 아이템: " + id);
            };

            burns.add(new BurnSpec(source, finalBurn));
        }

        // 입력한 전략에 맞는 state 생성
        CombatState state = new CombatState(baron.health(), strategy, heimer, baronData.gaze().damageReduction(), turretKits, turrets, turretSpec, burns, lowHealthBonus, new ActiveEffect(activeDamageAmp, activeDuration, activeCooldownRate));
        if (activeDuration > 0) {
            state.schedule(new ActiveEndEvent(activeDuration));
        }

        if(!turretKits.isFull()) {
            turretKits.setRecharging(true);
            state.schedule(new KitRechargeEvent(state.basicReadyTime(0, input.restKitChargeSecond())));
        }
        
        if (turretKits.hasKit() && input.turretCount() != turret.maxTurrets()){
            // 첫 포탑 설치 예약
            state.schedule(new TurretPlaceEvent(0, turretSpec));
        }

        // 이미 깔려있던 포탑들 공격 시작
        for (Turret t : turrets) {
            state.record(ReplayEvent.Kind.TURRET_READY, t, null, 0, 1);   // 9-2: 리플레이에 처음부터 보이게
            state.schedule(new TurretAttackEvent(0, finalQDamage, shotInterval, t));
        }

        // RW/RE 한 방 피해 (체력 그래프 막타 기준선). 막타 구간은 그림자불꽃 구간이라고 가정한다
        double ultimateOneShot = 0;

        // W 배웠으면 첫번째 로켓과 나머지 4개의 로켓 데미지를 계산 후 0초에 첫 사용 예약
        if (input.wLevel() > 0) {
            HeimerData.RocketsData rocket = heimerData.rockets();

            int extraRockets = rocket.rocketCount() - 1;
            double rocketDamage = rocket.firstRocketDamageAtRank(input.wLevel())
                    + extraRockets * rocket.extraRocketDamageAtRank(input.wLevel());
            double rocketAP = rocket.firstRocketApRatio() + extraRockets * rocket.extraRocketApRatio();
            double rocketCool = CooldownCalculator.reducedCooldown(rocket.cooldownAtRank(input.wLevel()), stats.abilityHaste());

            double baseWDamage = rocketDamage + rocketAP * stats.ap();
            double finalWDamage = DamageCalculator.reducedDamage(baseWDamage, baron.magicResist(), stats.perPen(),
                    stats.flatPen());

            UltimateSpec rw = null;
            if (input.ultimate() == UltimateChoice.RW) {
                HeimerData.RwRocketsData rRocket = heimerData.rwRockets();

                int rRocket2to5Count = 4;
                // 첫발 (1개) + 2~5번째 (4개) 를 뺀 나머지 로켓 수
                int rRocketExtraCount = rRocket.rocketCount() - 5;

                double rFirstRocketDamage = rRocket.firstRocketDamageAtRank(input.rLevel());
                double rFirstRocketAP = rRocket.firstRocketApRatio();
                double r2to5RocketDamage = rRocket.rockets2to5DamageAtRank(input.rLevel());
                double r2to5RocketAP = rRocket.rockets2to5ApRatio();
                double rExtraRocketDamage = rRocket.laterRocketDamageAtRank(input.rLevel());
                double rExtraRocketAP = rRocket.laterRocketApRatio();

                double baseRWDamage = rFirstRocketDamage + rFirstRocketAP * stats.ap()
                        + (r2to5RocketDamage + r2to5RocketAP * stats.ap()) * rRocket2to5Count
                        + (rExtraRocketDamage + rExtraRocketAP * stats.ap()) * rRocketExtraCount;
                double finalRWDamage = DamageCalculator.reducedDamage(baseRWDamage, baron.magicResist(),
                        stats.perPen(), stats.flatPen());

                rw = new UltimateSpec(finalRWDamage, rCooldown);
                ultimateOneShot = finalRWDamage * (1 + lowHealthBonus.bonus());
            }
            state.schedule(new RocketsEvent(0, finalWDamage, rocketCool, rw));
        }

        // E 배웠으면 최종 피해 계산 후 0초에 첫 사용 예약
        if (input.eLevel() > 0) {
            HeimerData.GrenadeData grenade = heimerData.grenade();

            double grenadeDamage = grenade.damageAtRank(input.eLevel());
            double grenadeAP = grenade.apRatio();
            double grenadeCool = CooldownCalculator.reducedCooldown(grenade.cooldownSeconds(), stats.abilityHaste());

            double baseEDamage = grenadeDamage + grenadeAP * stats.ap();
            double finalEDamage = DamageCalculator.reducedDamage(baseEDamage, baron.magicResist(), stats.perPen(),
                    stats.flatPen());

            UltimateSpec re = null;
            if (input.ultimate() == UltimateChoice.RE) {
                HeimerData.ReGrenadeData rGrenade = heimerData.reGrenade();

                int rGrenadeHits = rGrenade.maxHitsOnSingleMonster();
                double rGrenadeDamage = rGrenade.damageAtRank(input.rLevel());
                double rGrenadeAP = rGrenade.apRatio();

                double baseREDamage = (rGrenadeDamage + rGrenadeAP * stats.ap()) * rGrenadeHits;
                double finalREDamage = DamageCalculator.reducedDamage(baseREDamage, baron.magicResist(),
                        stats.perPen(), stats.flatPen());

                re = new UltimateSpec(finalREDamage, rCooldown);
                ultimateOneShot = finalREDamage * (1 + lowHealthBonus.bonus());
            }

            state.schedule(new GrenadeEvent(0, finalEDamage, grenadeCool, re));
        }

        // RQ 선택했으면 포탑하나 깔기
        if (input.rLevel() > 0) {
            if (input.ultimate() == UltimateChoice.RQ) {
                HeimerData.RqTurretData rturret = heimerData.rqTurret();

                double rturretShotDamage = rturret.shotDamageAtRank(input.rLevel());
                double rturretBeamDamage = rturret.beamDamageAtRank(input.rLevel());
                double rturretShotAp = rturret.shotApRatio();
                double rturretBeamAp = rturret.beamApRatio();

                double baseRqShotDamage = rturretShotDamage + rturretShotAp * stats.ap();
                double baseRqBeamDamage = rturretBeamDamage + rturretBeamAp * stats.ap();
                double finalRqShotDamage = DamageCalculator.reducedDamage(baseRqShotDamage, baron.magicResist(),
                        stats.perPen(), stats.flatPen());
                double finalRqBeamDamage = DamageCalculator.reducedDamage(baseRqBeamDamage, baron.magicResist(),
                        stats.perPen(), stats.flatPen());

                double rturretHp = rturret.healthAt(input.heimerLevel(), stats.ap());
                double rturretArmor = rturret.armorAt(input.heimerLevel());
                double rturretMr = rturret.magicResistAt(input.heimerLevel());

                // 쿨이 돌 때마다 RQ 설치예약
                state.schedule(new RQPlaceEvent(0, rturretHp, rturretArmor, rturretMr, finalRqShotDamage, finalRqBeamDamage, rturret,
                        rCooldown));
            }
        }

        double baronDamage = baron.attackDamage();
        double baronAttackInterval = 1 / baron.attackSpeed();

        state.schedule(new BaronAttackEvent(0, baronDamage, baronAttackInterval));

        // 3.마지막 처리 시각을 담을 변수
        double killSecond = 0;

        // 4.바론이 죽을 때까지 이벤트 하나씩 꺼내기
        while (state.hasEvents() && !state.isBaronDead() && !heimer.isDead()) {
            Event e = state.nextEvent();
            if (e.time() > input.timeLimit()) {
                break;
            }
            killSecond = e.time();
            e.apply(state);
        }

        // 루프가 왜 끝났는지 여기서 한 번에 판단
        Outcome outcome;
        if (state.isBaronDead()) {
            outcome = Outcome.KILLED;
        } else if (heimer.isDead()) {
            outcome = Outcome.HEIMER_DEAD;
        } else {
            outcome = Outcome.TIME_LIMIT;
        }

        return new SimulatorResult(outcome, killSecond, state.damageBySource(), state.gazeLossBySource(),
                baron.health(), ultimateOneShot, state.timeline());
    }
}
