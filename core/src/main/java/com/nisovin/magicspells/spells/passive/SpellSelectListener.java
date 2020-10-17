package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellSelectionChangedEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

public class SpellSelectListener extends PassiveListener {

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
	public void onSpellSelect(SpellSelectionChangedEvent event) {
		if (!(event.getCaster() instanceof Player)) return;
		Player player = (Player) event.getCaster();
		if (!hasSpell(player)) return;

		Spell spell = event.getSpell();
		if (!spellNames.isEmpty() && !spellNames.contains(spell.getInternalName())) return;

		passiveSpell.activate(player);
	}

}
