package com.hongtg.baronsim.calc;

/*
 * 하이머딩거가 바론에게 준 딜을 입력하면,
 * 마저(magicRegist), 퍼마관(percentPen), 고정마관(flatPen)
 * 에 따라 마지막 최종 데미지를 계산해주는 클래스
*/

public class DamageCalculator {
    public static double magicDamage(double originDamage, double magicRegist, double percentPen, double flatPen) {
        double rate;    //마저에 따른 피해 감소 배율
        double cal_MR;  //마관에 따른 감소된 마저

        // MG가 0 이상이면, 1.퍼마관 -> 2.고정마관 적용
        if (magicRegist >= 0) {
           cal_MR = Math.max(0,(magicRegist) * (1 - percentPen) - flatPen);
        } // 음수면 그대로
        else cal_MR = magicRegist;
        
        // 계산된 마저(양수, 음수)에 따른 최종 감소 비율
        if (cal_MR >= 0){
            rate = 100 / (100 + cal_MR); 
        }
        else {
            rate = 2 - 100 / (100 - cal_MR);
        }

        return originDamage * rate;
        
    }
}
