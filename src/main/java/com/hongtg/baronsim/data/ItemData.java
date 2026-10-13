package com.hongtg.baronsim.data;

import java.util.List;
import java.util.Objects;

/**
 * items.json을 읽은 결과. 아이템 기본 스탯만 담는다 (8-2단계).
 * JSON에 없는 스탯은 0이다 (Gson은 빠진 숫자 필드를 0으로 둔다).
 */
public record ItemData(String patch, List<Item> items) {

    /**
     * 아이템 하나의 기본 스탯.
     * percentMagicPen, apAmp는 비율이다 (0.40 = 40%).
     * apAmp는 라바돈처럼 "총 주문력이 N% 증가"하는 효과다.
     */
    public record Item(
            String id,
            String name,
            int cost,
            boolean boots,
            double ap,
            double health,
            double mana,
            double armor,
            double magicResist,
            double abilityHaste,
            double flatMagicPen,
            double percentMagicPen,
            double apAmp) {
    }

    /** id로 아이템을 찾는다. 없으면 IllegalArgumentException. */
    public Item item(String id) {
        for (Item it : items) {
            if (it.id().equals(id)) {
                return it;
            }
        }
        throw new IllegalArgumentException("items.json에 없는 아이템입니다: " + id);
    }

    /** JSON을 읽은 뒤 빠진 값이 없는지 확인한다. DataLoader가 부른다. */
    void validate() {
        Objects.requireNonNull(items, "items.json에 items가 없습니다");
        for (Item it : items) {
            Objects.requireNonNull(it.id(), "items.json에 id가 없는 아이템이 있습니다");
            Objects.requireNonNull(it.name(), "items.json의 " + it.id() + "에 name이 없습니다");
        }
    }
}
