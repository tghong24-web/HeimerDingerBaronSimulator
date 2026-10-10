package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.BaronData;
import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.data.HeimerData;
import com.hongtg.baronsim.model.BaronStats;
import com.hongtg.baronsim.calc.DamageCalculator;

public class Simulator {
    public SimulatorResult run(SimulationInput input) {

        BaronData baronData = DataLoader.loadBaron();
        HeimerData heimerData = DataLoader.loadHeimer();

        BaronStats baron = baronData.toScaler().statsAt(input.baronLevel());

        // 1.CombatState에 바론 체력 받기
        CombatState state = new CombatState(baron.health());

        // Q 데이터 읽어오기
        HeimerData.TurretData turret = heimerData.turret();

        double shotDamage = turret.shotDamageAtRank(input.qLevel());
        double shotAP = turret.shotApRatio();
        double shotInterval = turret.attackIntervalSeconds();

        double baseQDamage = shotDamage + shotAP * input.ap();
        double finalQDamage = DamageCalculator.magicDamage(baseQDamage, baron.magicResist(), input.percentPen(),
                input.flatPen());

        // Q: turret 개수만큼 첫번째 사격 등록
        for (int i = 0; i < input.turretCount(); i++) {
            state.schedule(new TurretAttackEvent(0, i, finalQDamage, shotInterval));
        }

        // W 배웠으면 첫번째 로켓과 나머지 4개의 로켓 데미지를 계산 후 0초에 첫 사용 예약
        if (input.wLevel() > 0) {
            HeimerData.RocketsData rocket = heimerData.rockets();
            
            int extraRockets = rocket.rocketCount() - 1;
            double rocketDamage = rocket.firstRocketDamageAtRank(input.wLevel()) + extraRockets * rocket.extraRocketDamageAtRank(input.wLevel());
            double rocketAP = rocket.firstRocketApRatio() + extraRockets * rocket.extraRocketApRatio();
            double rocketCool = rocket.cooldownAtRank(input.wLevel());

            double baseWDamage = rocketDamage + rocketAP * input.ap();
            double finalWDamage = DamageCalculator.magicDamage(baseWDamage, baron.magicResist(), input.percentPen(), input.flatPen());
            
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
                double finalRWDamage = DamageCalculator.magicDamage(baseRWDamage, baron.magicResist(), input.percentPen(), input.flatPen());

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
            double finalEDamage = DamageCalculator.magicDamage(baseEDamage, baron.magicResist(), input.percentPen(),
                    input.flatPen());

            UltimateSpec re = null;
            if (input.ultimate() == UltimateChoice.RE) {
                HeimerData.ReGrenadeData rGrenade = heimerData.reGrenade();

                int rGrenadeHits = rGrenade.maxHitsOnSingleMonster();
                double rGrenadeDamage = rGrenade.damageAtRank(input.rLevel());
                double rGrenadeAP = rGrenade.apRatio();

                double baseREDamage = (rGrenadeDamage + rGrenadeAP * input.ap()) * rGrenadeHits;
                double finalREDamage = DamageCalculator.magicDamage(baseREDamage, baron.magicResist(), input.percentPen(), input.flatPen());
                
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
                double finalRqShotDamage = DamageCalculator.magicDamage(baseRqShotDamage, baron.magicResist(), input.percentPen(), input.flatPen());
                double finalRqBeamDamage = DamageCalculator.magicDamage(baseRqBeamDamage, baron.magicResist(), input.percentPen(), input.flatPen());

                state.schedule(new RQTurretEvent(0, 0, finalRqShotDamage, finalRqBeamDamage, rturret));
            }
        }
    

        // 3.마지막 처리 시각을 담을 변수
        double killSecond = 0;

        // 4.바론이 죽을 때까지 이벤트 하나씩 꺼내기
        while (state.hasEvents() && !state.isBaronDead()) {
            Event e = state.nextEvent();
            if (e.time() > input.timeLimit())
                break;
            killSecond = e.time();

            e.apply(state);
        }

        return new SimulatorResult(state.isBaronDead(), killSecond, state.damageBySource());
    }
}
