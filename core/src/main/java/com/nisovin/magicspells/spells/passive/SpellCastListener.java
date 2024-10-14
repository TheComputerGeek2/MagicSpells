package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of internal spell names to accept
@Name("spellcast")
public class SpellCastListener extends PassiveListener {

	private SpellFilter filter;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		filter = SpellFilter.fromLegacyString(var);
	}

	@OverridePriority
	@EventHandler
	public void onSpellCast(SpellCastEvent event) {
		if (event.getSpellCastState() != SpellCastState.NORMAL) return;
		if (!isCancelStateOk(event.isCancelled())) return;

		LivingEntity caster = event.getCaster();
		if (!canTrigger(caster)) return;

		Spell spell = event.getSpell();
		if (spell.equals(passiveSpell)) return;
		if (filter != null && !filter.check(spell)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
