package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.util.Util;

import com.nisovin.magicspells.castmodifiers.Condition;

public class DisplayNameCondition extends Condition {

    private String displayName;

    @Override
    public boolean initialize(String var) {
        if (var == null || var.isEmpty()) return false;
        displayName = Util.colorize(var);
        return true;
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        return check(livingEntity, livingEntity);
    }

    @Override
    public boolean check(LivingEntity livingEntity, LivingEntity target) {
        if (!(target instanceof Player)) return false;
        return ((Player) target).getDisplayName().equalsIgnoreCase(displayName);
    }

    @Override
    public boolean check(LivingEntity livingEntity, Location location) {
        return false;
    }

}
