package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.BaronData;
import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.model.Dummy;
import com.hongtg.baronsim.model.Heimer;
import com.hongtg.baronsim.model.Turret;
import com.hongtg.baronsim.data.HeimerData;
import com.hongtg.baronsim.model.BaronStats;

import java.util.ArrayList;
import java.util.List;

import com.hongtg.baronsim.calc.DamageCalculator;

public class Simulator {
    public SimulatorResult run(SimulationInput input) {

        BaronData baronData = DataLoader.loadBaron();
        HeimerData heimerData = DataLoader.loadHeimer();

        BaronStats baron = baronData.toScaler().statsAt(input.baronLevel());

        // 1.CombatState에 바론 체력 받기 + Heimer 생성
        double heimerHp = heimerData.stats().health().valueAt(input.heimerLevel());
        double heimerArmor = heimerData.stats().armor().valueAt(input.heimerLevel());
        Heimer heimer = new Heimer(heimerHp, heimerArmor);

        // Q 데이터 읽어오기
        HeimerData.TurretData turret = heimerData.turret();

        double shotDamage = turret.shotDamageAtRank(input.qLevel());
        double shotAP = turret.shotApRatio();
        double shotInterval = turret.attackIntervalSeconds();

        double baseQDamage = shotDamage + shotAP * input.ap();
        double finalQDamage = DamageCalculator.reducedDamage(baseQDamage, baron.magicResist(), input.percentPen(),
                input.flatPen());

        double turretHp = heimerData.turret().healthAt(input.heimerLevel(), input.ap());
        double turretArmor = heimerData.turret().armorAt(input.heimerLevel());

        // 현재 터렛키트의 상태 생성
        TurretKits turretKits = new TurretKits((input.currentKitCount()), heimerData.turret().maxKits(), heimerData.turret().kitRechargeSeconds());

        // 설정한 터렛의 개수만큼 새로운 터렛들을 리스트에 추가
        List<Turret> turrets = new ArrayList<>();
        for (int i = 0; i < input.turretCount(); i++) {
            Turret t = new Turret(turretHp, turretArmor);
            turrets.add(t);
        }

        // Target Mode 정하기
        TankingStrategy strategy;
        if (input.hasDummy()) {
            strategy = new DummyTankingStrategy(new Dummy(0));
        } else {
            strategy = new NoDummyTankingStrategy(turrets, heimer);
        }

        TurretSpec turretSpec = new TurretSpec(turretHp, turretArmor, finalQDamage, shotInterval, turret.cooldownSeconds(), turret.maxTurrets());

        // 입력한 전략에 맞는 state 생성
        CombatState state = new CombatState(baron.health(), strategy, heimer, baronData.gaze().damageReduction(), turretKits, turrets, turretSpec);
        
        if(!turretKits.isFull()) {
            turretKits.setRecharging(true);
            state.schedule(new KitRechargeEvent(input.restKitChargeSecond()));
        }
        
        if (turretKits.hasKit() && input.turretCount() != turret.maxTurrets()){
            // 첫 포탑 설치 예약
            state.schedule(new TurretPlaceEvent(0, turretSpec));
        }

        // 이미 깔려있던 포탑들 공격 시작
        for (Turret t : turrets) {
            state.schedule(new TurretAttackEvent(0, finalQDamage, shotInterval, t));
        }

        // W 배웠으면 첫번째 로켓과 나머지 4개의 로켓 데미지를 계산 후 0초에 첫 사용 예약
        if (input.wLevel() > 0) {
            HeimerData.RocketsData rocket = heimerData.rockets();

            int extraRockets = rocket.rocketCount() - 1;
            double rocketDamage = rocket.firstRocketDamageAtRank(input.wLevel())
                    + extraRockets * rocket.extraRocketDamageAtRank(input.wLevel());
            double rocketAP = rocket.firstRocketApRatio() + extraRockets * rocket.extraRocketApRatio();
            double rocketCool = rocket.cooldownAtRank(input.wLevel());

            double baseWDamage = rocketDamage + rocketAP * input.ap();
            double finalWDamage = DamageCalculator.reducedDamage(baseWDamage, baron.magicResist(), input.percentPen(),
                    input.flatPen());

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

                double baseRWDamage = rFirstRocketDamage + rFirstRocketAP * input.ap()
                        + (r2to5RocketDamage + r2to5RocketAP * input.ap()) * rRocket2to5Count
                        + (rExtraRocketDamage + rExtraRocketAP * input.ap()) * rRocketExtraCount;
                double finalRWDamage = DamageCalculator.reducedDamage(baseRWDamage, baron.magicResist(),
                        input.percentPen(), input.flatPen());

                rw = new UltimateSpec(finalRWDamage, heimerData.upgrade().cooldownAtRank(input.rLevel()));
            }
            state.schedule(new RocketsEvent(0, finalWDamage, rocketCool, rw));
        }

        // E 배웠으면 최종 피해 계산 후 0초에 첫 사용 예약
        if (input.eLevel() > 0) {
            HeimerData.GrenadeData grenade = heimerData.grenade();

            double grenadeDamage = grenade.damageAtRank(input.eLevel());
            double grenadeAP = grenade.apRatio();
            double grenadeCool = grenade.cooldownSeconds();

            double baseEDamage = grenadeDamage + grenadeAP * input.ap();
            double finalEDamage = DamageCalculator.reducedDamage(baseEDamage, baron.magicResist(), input.percentPen(),
                    input.flatPen());

            UltimateSpec re = null;
            if (input.ultimate() == UltimateChoice.RE) {
                HeimerData.ReGrenadeData rGrenade = heimerData.reGrenade();

                int rGrenadeHits = rGrenade.maxHitsOnSingleMonster();
                double rGrenadeDamage = rGrenade.damageAtRank(input.rLevel());
                double rGrenadeAP = rGrenade.apRatio();

                double baseREDamage = (rGrenadeDamage + rGrenadeAP * input.ap()) * rGrenadeHits;
                double finalREDamage = DamageCalculator.reducedDamage(baseREDamage, baron.magicResist(),
                        input.percentPen(), input.flatPen());

                re = new UltimateSpec(finalREDamage, heimerData.upgrade().cooldownAtRank(input.rLevel()));
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

                double baseRqShotDamage = rturretShotDamage + rturretShotAp * input.ap();
                double baseRqBeamDamage = rturretBeamDamage + rturretBeamAp * input.ap();
                double finalRqShotDamage = DamageCalculator.reducedDamage(baseRqShotDamage, baron.magicResist(),
                        input.percentPen(), input.flatPen());
                double finalRqBeamDamage = DamageCalculator.reducedDamage(baseRqBeamDamage, baron.magicResist(),
                        input.percentPen(), input.flatPen());

                // 쿨이 돌 때마다 RQ 설치예약
                state.schedule(new RQPlaceEvent(0, finalRqShotDamage, finalRqBeamDamage, rturret,
                        heimerData.upgrade().cooldownAtRank(input.rLevel())));
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

        return new SimulatorResult(outcome, killSecond, state.damageBySource(), state.gazeLossBySource());
    }
}
