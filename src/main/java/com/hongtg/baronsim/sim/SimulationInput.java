package com.hongtg.baronsim.sim;

// Simulation을 돌릴 때, 입력하기 편하게 정보들 record 형태로 만들어놓기
public record SimulationInput(
        int baronLevel,
        int heimerLevel,
        int qLevel,
        int wLevel,
        int eLevel,
        int rLevel,
        UltimateChoice ultimate,
        Build build, // ap, abilityHaste, flatPen, PerPen이 담긴 Build
        int turretCount,
        int currentKitCount,
        double restKitChargeSecond,
        RqTurretPosition rqPosition,
        boolean hasDummy,
        double timeLimit,
        RoleQuest quest) {

    public SimulationInput {
        if (build == null) {
            throw new IllegalArgumentException("build 정보가 필요합니다. ");

        }
        if (quest == null) {
            throw new IllegalArgumentException("퀘스트를 선택해야합니다. ");

        }
        if (build.bonusAbilityHaste() < 0) {
            throw new IllegalArgumentException("스킬가속은 양수이어야합니다: " + build.bonusAbilityHaste());
        }
        if (turretCount < 0 || turretCount > 3) {
            throw new IllegalArgumentException("포탑은 0~3개여야 합니다: " + turretCount);
        }
        if (currentKitCount < 0 || currentKitCount > 3) {
            throw new IllegalArgumentException("포탑키트 개수는 0~3개여야 합니다: " + currentKitCount);
        }
        if (restKitChargeSecond < 0) {
            throw new IllegalArgumentException("키트 충전시간은 0초 이상이어야 합니다: " + restKitChargeSecond);
        }
        if (currentKitCount == 3) {
            restKitChargeSecond = 0;
        }
        if (heimerLevel < 1 || heimerLevel > 20) {
            throw new IllegalArgumentException("하이머딩거 레벨은 1~18이어야 합니다: " + heimerLevel);
        }
        if (heimerLevel > 18 && quest != RoleQuest.TOP) {
            throw new IllegalArgumentException("퀘스트가 TOP이어야 20레벨 까지 가능합니다: " + heimerLevel);
        }
        if (timeLimit <= 0) {
            throw new IllegalArgumentException("제한 시간은 0보다 커야 합니다: " + timeLimit);
        }
        if (build.bonusAp() < 0) {
            throw new IllegalArgumentException("주문력은 양수이어야 합니다: " + build.bonusAp());
        }
        if (qLevel < 1 || qLevel > 5) {
            throw new IllegalArgumentException("Q레벨은 1이상 5이하 이어야 합니다: " + qLevel);
        }
        if (eLevel < 0 || eLevel > 5) {
            throw new IllegalArgumentException("E레벨은 0이상 5이하 이어야 합니다: " + eLevel);
        }
        if (wLevel < 0 || wLevel > 5) {
            throw new IllegalArgumentException("W레벨은 0이상 5이하 이어야 합니다: " + wLevel);
        }
        if (rLevel < 0 || rLevel > 3) {
            throw new IllegalArgumentException("R레벨은 0이상 3이하 이어야 합니다: " + rLevel);
        }
        if (ultimate == null) {
            throw new IllegalArgumentException("R사용 유무 선택을 해야합니다");
        }
        if (ultimate != UltimateChoice.NONE && rLevel == 0) {
            throw new IllegalArgumentException("R을 사용하려면 R레벨이 1 이상이어야 합니다: " + ultimate);
        }
        if (ultimate == UltimateChoice.RW && wLevel == 0) {
            throw new IllegalArgumentException("RW를 사용하려면 W레벨이 1 이상이어야 합니다: " + ultimate);
        }
        if (rqPosition == null) {
            throw new IllegalArgumentException("RQ 터렛 위치를 정해야 합니다");
        }
    }
}
