package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of internal spell names to accept
public class SpellTargetedListener extends PassiveListener {

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
	public void onSpellTarget(SpellTargetEvent event) {
		LivingEntity target = event.getTarget();
		if (!hasSpell(target)) return;
		if (!canTrigger(target)) return;

		if (!spellNames.isEmpty() && !spellNames.contains(event.getSpell().getInternalName())) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(target, event.getCaster());
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
