package com.hongtg.baronsim.sim;

import com.hongtg.baronsim.data.BaronData;
import com.hongtg.baronsim.data.DataLoader;
import com.hongtg.baronsim.data.HeimerData;
import com.hongtg.baronsim.model.BaronStats;
import com.hongtg.baronsim.calc.DamageCalculator;

public class Simulator {
    public SimulatorResult run(int baronLevel, int qRank, double ap,
            double percentPen, double flatPen,
            int turretCount, double timeLimit) {
        BaronData baronData = DataLoader.loadBaron();
        HeimerData heimerData = DataLoader.loadHeimer();

        BaronStats baron = baronData.toScaler().statsAt(baronLevel);

        HeimerData.TurretData turret = heimerData.turret();

        double shotDamage = turret.shotDamageAtRank(qRank);
        double shotAP = turret.shotApRatio();
        double shotInterval = turret.attackIntervalSeconds();

        double baseDamage = shotDamage + shotAP * ap;
        double finalDamage = DamageCalculator.magicDamage(baseDamage, baron.magicResist(), percentPen, flatPen);
        
        // 1.CombatState에 바론 체력 받기
        CombatState state = new CombatState(baron.health());
        
        // 2.turret 개수만큼 첫번째 사격 등록
        for (int i = 0; i < turretCount; i++) {
            state.schedule(new TurretAttackEvent(0, i, finalDamage, shotInterval));
        }

        // 3.마지막 처리 시각을 담을 변수
        double killSecond = 0;

        // 4.바론이 죽을 때까지 이벤트 하나씩 꺼내기
        while (state.hasEvents() && !state.isBaronDead()) {
            Event e = state.nextEvent();
            if (e.time() > timeLimit) break;
            killSecond = e.time();

            e.apply(state);
        }

       return new SimulatorResult(state.isBaronDead(), killSecond);
    }
}
