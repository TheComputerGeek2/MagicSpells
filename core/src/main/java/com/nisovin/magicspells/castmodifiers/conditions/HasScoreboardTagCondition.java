package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class HasScoreboardTagCondition extends Condition {

    private String tag;

    @Override
    public boolean initialize(String var) {
        if (var == null || var.isEmpty()) return false;
        tag = var;
        return true;
    }

    @Override
    public boolean check(LivingEntity caster) {
        return checkTags(caster, caster);
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target) {
        return checkTags(caster, target);
    }

    @Override
    public boolean check(LivingEntity livingEntity, Location location) {
        return false;
    }

    // TODO: Add functionality to check both caster and target variables
    private boolean checkTags(LivingEntity caster, LivingEntity target) {
        String localTag = tag;
        if (localTag.contains("%")) localTag = MagicSpells.doVariableReplacements((Player) caster, localTag);
        return target.getScoreboardTags().contains(localTag);
    }

}
