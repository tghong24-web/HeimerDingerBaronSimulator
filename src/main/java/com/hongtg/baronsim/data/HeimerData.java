package com.hongtg.baronsim.data;

import com.hongtg.baronsim.calc.StatGrowth;

import java.util.Objects;

/**
 * heimerdinger.json의 내용을 담는 record.
 * 스킬 레벨별 수치는 배열로 저장되어 있고, 배열 인덱스는 0부터지만
 * 스킬 레벨(rank)은 1부터라서 접근은 반드시 ...AtRank 메서드를 거친다.
 */
public record HeimerData(
        String patch,
        String source,
        ChampionStats stats,
        TurretData turret,
        ApexTurretData apexTurret,
        RocketsData rockets,
        RocketSwarmData rocketSwarm,
        GrenadeData grenade,
        LightningGrenadeData lightningGrenade,
        UpgradeData upgrade) {

    /** 레벨에 따라 바뀌는 값의 최소(1레벨)와 최대(18레벨). 사이 공식은 아직 미확인. */
    public record Range(double min, double max) {}

    /** 하이머딩거 기본 스탯. 성장은 바론과 같은 StatGrowth 공식을 쓴다. */
    public record ChampionStats(
            StatGrowth health,
            StatGrowth mana,
            StatGrowth attackDamage,
            StatGrowth armor,
            StatGrowth magicResist,
            double baseAttackSpeed,
            double attackRange) {}

    /** Q: H-28G 진화 포탑. */
    public record TurretData(
            double manaCost,
            double cooldownSeconds,
            int maxTurrets,
            int maxKits,
            double kitRechargeSeconds,
            double castTimeSeconds,
            double[] shotDamage,
            double shotApRatio,
            double attackSpeed,
            double attackRange,
            double[] beamDamage,
            double beamApRatio,
            double beamChargePerSecond,
            Range health,
            Range healthApRatio,
            Range armor,
            Range magicResist) {

        /** Q 레벨(1~5)에 해당하는 평타 기본 피해. */
        public double shotDamageAtRank(int qRank) {
            return valueAtRank(shotDamage, qRank, "Q");
        }

        /** Q 레벨(1~5)에 해당하는 광선 기본 피해. */
        public double beamDamageAtRank(int qRank) {
            return valueAtRank(beamDamage, qRank, "Q");
        }

        /** 공격 사이 간격(초). 공격 속도 1.75면 약 0.571초. */
        public double attackIntervalSeconds() {
            return 1.0 / attackSpeed;
        }
    }

    /** R+Q: H-28Q 강화 포탑. */
    public record ApexTurretData(
            double durationSeconds,
            double[] shotDamage,
            double shotApRatio,
            double attackSpeed,
            double[] beamDamage,
            double beamApRatio,
            double beamChargePerSecond,
            double beamChargePerShot,
            Range health,
            double healthApRatio) {

        /** R 레벨(1~3)에 해당하는 평타 기본 피해. */
        public double shotDamageAtRank(int rRank) {
            return valueAtRank(shotDamage, rRank, "R");
        }

        /** R 레벨(1~3)에 해당하는 광선 기본 피해. */
        public double beamDamageAtRank(int rRank) {
            return valueAtRank(beamDamage, rRank, "R");
        }

        public double attackIntervalSeconds() {
            return 1.0 / attackSpeed;
        }
    }

    /** W: 마이크로 로켓. 첫 발과 나머지 발의 피해가 다르다. */
    public record RocketsData(
            double[] manaCost,
            double[] cooldownSeconds,
            double castTimeSeconds,
            int rocketCount,
            double[] firstRocketDamage,
            double firstRocketApRatio,
            double[] extraRocketDamage,
            double extraRocketApRatio) {

        public double manaCostAtRank(int wRank) {
            return valueAtRank(manaCost, wRank, "W");
        }

        public double cooldownAtRank(int wRank) {
            return valueAtRank(cooldownSeconds, wRank, "W");
        }

        public double firstRocketDamageAtRank(int wRank) {
            return valueAtRank(firstRocketDamage, wRank, "W");
        }

        /** 두 번째 발부터 한 발당 기본 피해 (챔피언·몬스터 기준). */
        public double extraRocketDamageAtRank(int wRank) {
            return valueAtRank(extraRocketDamage, wRank, "W");
        }
    }

    /** R+W: 로켓 세례. 1발 / 2~5발 / 6발 이후 피해가 각각 다르다. */
    public record RocketSwarmData(
            int rocketCount,
            double[] firstRocketDamage,
            double firstRocketApRatio,
            double[] rockets2to5Damage,
            double rockets2to5ApRatio,
            double[] laterRocketDamage,
            double laterRocketApRatio) {

        public double firstRocketDamageAtRank(int rRank) {
            return valueAtRank(firstRocketDamage, rRank, "R");
        }

        public double rockets2to5DamageAtRank(int rRank) {
            return valueAtRank(rockets2to5Damage, rRank, "R");
        }

        public double laterRocketDamageAtRank(int rRank) {
            return valueAtRank(laterRocketDamage, rRank, "R");
        }
    }

    /** E: 전자 폭풍 수류탄. */
    public record GrenadeData(
            double manaCost,
            double cooldownSeconds,
            double castTimeSeconds,
            double projectileSpeed,
            double[] damage,
            double apRatio) {

        public double damageAtRank(int eRank) {
            return valueAtRank(damage, eRank, "E");
        }
    }

    /** R+E: 번개 수류탄. 몬스터에는 튕길 때마다 피해가 들어가고, 단일 대상에는 최대 maxHitsOnSingleMonster번 맞는다. */
    public record LightningGrenadeData(int bounces, int maxHitsOnSingleMonster, double[] damage, double apRatio) {

        public double damageAtRank(int rRank) {
            return valueAtRank(damage, rRank, "R");
        }
    }

    /** R: 업그레이드!!! */
    public record UpgradeData(double manaCost, double[] cooldownSeconds) {

        public double cooldownAtRank(int rRank) {
            return valueAtRank(cooldownSeconds, rRank, "R");
        }
    }

    public HeimerData {
        Objects.requireNonNull(stats, "heimerdinger.json에 stats가 없습니다");
        Objects.requireNonNull(turret, "heimerdinger.json에 turret이 없습니다");
        Objects.requireNonNull(apexTurret, "heimerdinger.json에 apexTurret이 없습니다");
        Objects.requireNonNull(rockets, "heimerdinger.json에 rockets가 없습니다");
        Objects.requireNonNull(rocketSwarm, "heimerdinger.json에 rocketSwarm이 없습니다");
        Objects.requireNonNull(grenade, "heimerdinger.json에 grenade가 없습니다");
        Objects.requireNonNull(lightningGrenade, "heimerdinger.json에 lightningGrenade가 없습니다");
        Objects.requireNonNull(upgrade, "heimerdinger.json에 upgrade가 없습니다");
        Objects.requireNonNull(stats.health(), "heimerdinger.json에 stats.health가 없습니다");
        Objects.requireNonNull(turret.shotDamage(), "heimerdinger.json에 turret.shotDamage가 없습니다");
        Objects.requireNonNull(turret.beamDamage(), "heimerdinger.json에 turret.beamDamage가 없습니다");
        Objects.requireNonNull(apexTurret.shotDamage(), "heimerdinger.json에 apexTurret.shotDamage가 없습니다");
        Objects.requireNonNull(apexTurret.beamDamage(), "heimerdinger.json에 apexTurret.beamDamage가 없습니다");
        Objects.requireNonNull(rockets.firstRocketDamage(), "heimerdinger.json에 rockets.firstRocketDamage가 없습니다");
        Objects.requireNonNull(rockets.extraRocketDamage(), "heimerdinger.json에 rockets.extraRocketDamage가 없습니다");
        Objects.requireNonNull(grenade.damage(), "heimerdinger.json에 grenade.damage가 없습니다");
        if (turret.attackSpeed() <= 0 || apexTurret.attackSpeed() <= 0) {
            throw new IllegalArgumentException("heimerdinger.json의 공격 속도는 0보다 커야 합니다");
        }
        if (lightningGrenade.maxHitsOnSingleMonster() < 1) {
            throw new IllegalArgumentException("heimerdinger.json의 lightningGrenade.maxHitsOnSingleMonster는 1 이상이어야 합니다");
        }
        if (rockets.rocketCount() < 1 || rocketSwarm.rocketCount() < 5) {
            throw new IllegalArgumentException("heimerdinger.json의 로켓 개수가 잘못됐습니다");
        }
    }

    /** 1부터 시작하는 스킬 레벨로 배열 값을 꺼낸다. 범위를 벗어나면 바로 예외. */
    private static double valueAtRank(double[] values, int rank, String skill) {
        if (values == null) {
            throw new IllegalStateException(skill + " 수치가 heimerdinger.json에 없습니다");
        }
        if (rank < 1 || rank > values.length) {
            throw new IllegalArgumentException(
                    skill + " 레벨은 1~" + values.length + "이어야 합니다: " + rank);
        }
        return values[rank - 1];
    }
}
