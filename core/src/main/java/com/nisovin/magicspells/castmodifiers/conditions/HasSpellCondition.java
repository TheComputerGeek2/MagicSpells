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
        spell = MagicSpells.getSpellByInternalName(var);
        return spell != null;
    }

    @Override
    public boolean check(LivingEntity caster) {
        return hasSpell(caster);
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target) {
        return hasSpell(target);
    }

    @Override
    public boolean check(LivingEntity caster, Location location) {
        return false;
    }

    private boolean hasSpell(LivingEntity target) {
        return target instanceof Player pl && MagicSpells.getSpellbook(pl).hasSpell(spell);
    }

}
