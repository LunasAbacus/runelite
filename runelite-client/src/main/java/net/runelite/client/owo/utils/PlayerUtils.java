package net.runelite.client.owo.utils;

import net.runelite.api.Client;
import net.runelite.api.Skill;

public class PlayerUtils {
    public static boolean needsHealingByThreshold(final Client client, final int hpDropThreshold) {
        int hp = client.getBoostedSkillLevel(Skill.HITPOINTS); // current HP
        int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
        return hp <= maxHp - hpDropThreshold;
    }

    public static boolean needsHealingByPercent(final Client client, final double percent) {
        int hp = client.getBoostedSkillLevel(Skill.HITPOINTS); // current HP
        int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
        int hpThreshold = Math.toIntExact(Math.round(maxHp * percent));
        return hp <= hpThreshold;
    }
}
