package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class HasSpellCondition extends Condition {

    private Spell spell;

    @Override
    public boolean initialize(String var) {
        Spell s = MagicSpells.getSpellByInternalName(var);
        if (s == null) return false;
        if (!(s instanceof Spell)) return false;
        spell = s;
        return true;
    }

    @Override
    public boolean check(LivingEntity livingEntity) {
        if (livingEntity instanceof Player) return MagicSpells.getSpellbook((Player)livingEntity).hasSpell(spell);
        return false;
    }

    @Override
    public boolean check(LivingEntity livingEntity, LivingEntity target) {
        return check(target);
    }

    @Override
    public boolean check(LivingEntity livingEntity, Location location) {
        return false;
    }

}
