package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of internal spell names to accept
public class SpellCastedListener extends PassiveListener {

	private final Set<String> spellNames = new HashSet<>();
			
	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split(",");
		for (String s : split) {
			Spell sp = MagicSpells.getSpellByInternalName(s.trim());
			if (sp == null) continue;

			spellNames.add(sp.getInternalName());
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onSpellCast(SpellCastedEvent event) {
		LivingEntity caster = event.getCaster();
		if (event.getSpellCastState() != SpellCastState.NORMAL) return;
		if (event.getPostCastAction() == PostCastAction.ALREADY_HANDLED) return;
		if (!hasSpell(caster)) return;
		if (!canTrigger(caster)) return;

		Spell spell = event.getSpell();
		if (!spellNames.isEmpty() && !spellNames.contains(spell.getInternalName())) return;

		if (spell.equals(passiveSpell)) return;
		passiveSpell.activate(caster);
	}

}
