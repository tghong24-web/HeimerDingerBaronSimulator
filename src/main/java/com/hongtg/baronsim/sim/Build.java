package com.hongtg.baronsim.sim;

import java.util.List;

public record Build(
        List<String> items,
        double bonusAp,
        double bonusAbilityHaste,
        double bonusFlatPen,
        double bonusPerPen,
        int gloryStacks) {

}
