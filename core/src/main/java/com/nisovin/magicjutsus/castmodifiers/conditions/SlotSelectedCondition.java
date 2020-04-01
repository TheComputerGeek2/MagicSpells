package com.nisovin.magicjutsus.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SlotSelectedCondition extends OperatorCondition {

    private int slot;

    @Override
    public boolean setVar(String var) {
        if(var == null || var.isEmpty()) return false;
        try {
            slot = Integer.parseInt(var);
            return true;
        }
        catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        if(!(livingEntity instanceof Player)) return false;
        int theirSlot = ((Player) livingEntity).getInventory().getHeldItemSlot();
        if(equals) return theirSlot == slot;
        else if(moreThan) return theirSlot > slot;
        else if(lessThan) return theirSlot < slot;
        return false;
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target) {
        return check(target);
    }

    @Override
    public boolean check(LivingEntity caster, Location location) {
        return false;
    }
}
