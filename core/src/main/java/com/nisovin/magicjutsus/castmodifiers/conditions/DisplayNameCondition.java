package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.castmodifiers.Condition;

public class DisplayNameCondition extends Condition {

    private String displayName;

    @Override
    public boolean setVar(String var) {
        if (var == null || var.isEmpty()) return false;
        displayName = ChatColor.translateAlternateColorCodes('&', var);
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
