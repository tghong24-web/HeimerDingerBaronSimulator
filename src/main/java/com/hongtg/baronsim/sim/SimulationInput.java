package com.hongtg.baronsim.sim;

// Simulation을 돌릴 때, 입력하기 편하게 정보들 record 형태로 만들어놓기
public record SimulationInput(
    int baronLevel, 
    int qLevel,
    int wLevel,
    int eLevel,
    int rLevel,
    UltimateChoice ultimate,
    double ap,
    double percentPen,
    double flatPen,
    int turretCount,
    double timeLimit) {
    
    public SimulationInput {
        if (turretCount < 0 || turretCount > 3) {
            throw new IllegalArgumentException("포탑은 0~3개여야 합니다: " + turretCount);
        }
        if (timeLimit <= 0) {
            throw new IllegalArgumentException("제한 시간은 0보다 커야 합니다: " + timeLimit);
        }
        if (ap < 0) {
            throw new IllegalArgumentException("주문력은 양수이어야 합니다: " + ap);
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
        if (ultimate != UltimateChoice.NONE && rLevel == 0) {
            throw new IllegalArgumentException("R을 사용하려면 R레벨이 1 이상이어야 합니다: " + ultimate);
        }
        if (ultimate == UltimateChoice.RW && wLevel == 0) {
            throw new IllegalArgumentException("RW를 사용하려면 W레벨이 1 이상이어야 합니다: " + ultimate);
        }
    }
}
