package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class HasScoreboardTagCondition extends Condition {

    private String var;
    private String tag;
    private boolean isVar;
    private Set<String> enttags = new HashSet<>();

    @Override
    public boolean initialize(String var) {
        if (var == null || var.isEmpty()) return false;
        if (var.contains("%var:")) isVar = true;
        this.var = var;
        tag = var;
        return true;
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        enttags = livingEntity.getScoreboardTags();
        if (!(livingEntity instanceof Player)) return enttags.contains(tag);
        if (isVar) tag = MagicSpells.doVariableReplacements((Player) livingEntity, var);
        return enttags.contains(tag);
    }

    @Override
    public boolean check(LivingEntity livingEntity, LivingEntity target) {
        enttags = target.getScoreboardTags();
        if (!(target instanceof Player)) return enttags.contains(tag);
        if (isVar) tag = MagicSpells.doVariableReplacements((Player) target, var);
        return enttags.contains(tag);
    }

    @Override
    public boolean check(LivingEntity livingEntity, Location location) {
        return false;
    }

}
