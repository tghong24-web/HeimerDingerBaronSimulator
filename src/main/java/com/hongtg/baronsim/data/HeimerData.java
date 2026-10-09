package com.hongtg.baronsim.data;

import java.util.Objects;

/**
 * heimerdinger.json의 내용을 담는 record.
 * 스킬 레벨별 수치는 배열로 저장되어 있고, 배열 인덱스는 0부터지만
 * 스킬 레벨(rank)은 1부터라서 접근은 반드시 rank 메서드를 거친다.
 */
public record HeimerData(String patch, String source, TurretData turret, ApexTurretData apexTurret) {

    /** 레벨에 따라 바뀌는 값의 최소(1레벨)와 최대(18레벨). 사이 공식은 아직 미확인. */
    public record Range(double min, double max) {}

    /** H-28G 진화 포탑 (일반 Q). */
    public record TurretData(
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

    /** H-28Q 강화 포탑 (R+Q). */
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

    public HeimerData {
        Objects.requireNonNull(turret, "heimerdinger.json에 turret이 없습니다");
        Objects.requireNonNull(apexTurret, "heimerdinger.json에 apexTurret이 없습니다");
        Objects.requireNonNull(turret.shotDamage(), "heimerdinger.json에 turret.shotDamage가 없습니다");
        Objects.requireNonNull(turret.beamDamage(), "heimerdinger.json에 turret.beamDamage가 없습니다");
        Objects.requireNonNull(apexTurret.shotDamage(), "heimerdinger.json에 apexTurret.shotDamage가 없습니다");
        Objects.requireNonNull(apexTurret.beamDamage(), "heimerdinger.json에 apexTurret.beamDamage가 없습니다");
        if (turret.attackSpeed() <= 0 || apexTurret.attackSpeed() <= 0) {
            throw new IllegalArgumentException("heimerdinger.json의 공격 속도는 0보다 커야 합니다");
        }
    }

    /** 1부터 시작하는 스킬 레벨로 배열 값을 꺼낸다. 범위를 벗어나면 바로 예외. */
    private static double valueAtRank(double[] values, int rank, String skill) {
        if (rank < 1 || rank > values.length) {
            throw new IllegalArgumentException(
                    skill + " 레벨은 1~" + values.length + "이어야 합니다: " + rank);
        }
        return values[rank - 1];
    }
}
