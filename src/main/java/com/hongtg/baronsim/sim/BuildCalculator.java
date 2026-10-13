package com.hongtg.baronsim.sim;

import java.util.HashSet;
import java.util.Set;

import com.hongtg.baronsim.data.ItemData;

public class BuildCalculator {
    public static FinalStats calculate(ItemData data, Build build, RoleQuest quest){
        double ap = 0, health = 0, armor = 0, mr = 0, haste = 0, flatPen = 0, mana = 0;
        double perRemain = 1.0;
        double amp = 0;
        int bootsCount = 0, nonBootsCount = 0;
        int gloryMax = 0;
        double gloryApPerStack = 0;
        Set<String> legendaries = new HashSet<>();
        Set<String> uniqueGroups = new HashSet<>();

        for (String id: build.items()) {
            ItemData.Item item = data.item(id);

            // 1. 완성템은 중복 불가
            if (item.legendary() && !legendaries.add(id)) {
                throw new IllegalArgumentException("완성템은 중복되면 안됩니다. ");
            }
            // 2. 같은 고유 그룹 2개 불가
            if (item.uniqueGroup() != null && !uniqueGroups.add(item.uniqueGroup())) {
                throw new IllegalArgumentException("같은 고유효과를 가진 아이템은 중복되면 안됩니다. ");
            }
            // 3. MID 가 아니면 신발 강화 불가
            if (id.equals("spellslinger") && quest != RoleQuest.MID){
                throw new IllegalArgumentException("MID 퀘스트가 아니면 해당 아이템은 장착 불가합니다. ");
            }
            // 4. 신발이면 신발 카운트 +
            if (item.boots()) {
                bootsCount++;
            } else {    // 아니면 일반 아이템 +
                nonBootsCount++;
            }
            // 5. 아이템 기본 능력치들 합계
            ap      += item.ap();
            health  += item.health();
            armor   += item.armor();
            mr      += item.magicResist();
            haste   += item.abilityHaste();
            flatPen += item.flatMagicPen();
            mana    += item.mana();
            perRemain *= (1 - item.percentMagicPen());
            amp     += item.apAmp();
            if(item.maxStacks() > 0) {
                gloryMax        = item.maxStacks();
                gloryApPerStack = item.apPerStack();
            }
        }

        // 6. 아이템 전체 개수 검사(BOT 퀘스트 유무에 따라)
        if (bootsCount > 1) {
            throw new IllegalArgumentException("신발은 1개까지 가능합니다: " + bootsCount);
        }
        if (quest == RoleQuest.BOT) {
            if (nonBootsCount > 6) {
                throw new IllegalArgumentException("아이템 개수는 6개 이하이어야 합니다: " + nonBootsCount);
            }
        } else {
            if (bootsCount + nonBootsCount > 6) {
                throw new IllegalArgumentException("아이템 개수는 6개 이하이어야 합니다: " + bootsCount + nonBootsCount);
            }
        }

        if (build.gloryStacks() < 0 || build.gloryStacks() > gloryMax) {
            throw new IllegalArgumentException("영광 중첩은 0~" + gloryMax + "이어야 합니다: " + build.gloryStacks());
        }
        ap += build.gloryStacks() * gloryApPerStack;

        ap      += build.bonusAp();
        haste   += build.bonusAbilityHaste();
        flatPen += build.bonusFlatPen();
        perRemain *= (1 - build.bonusPerPen());
        ap = ap * (1 + amp);
    
        return new FinalStats(ap, flatPen, 1 - perRemain, haste, health, armor, mr, mana);
    }
}
