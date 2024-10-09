package com.nisovin.magicspells.spells.passive;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellSelectionChangedEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("spellselect")
public class SpellSelectListener extends PassiveListener {

	private SpellFilter filter;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		filter = SpellFilter.fromLegacyString(var);
	}

	@OverridePriority
	@EventHandler
	public void onSpellSelect(SpellSelectionChangedEvent event) {
		if (!(event.getCaster() instanceof Player caster)) return;
		if (!canTrigger(caster)) return;
		if (filter != null && !filter.check(event.getSpell())) return;
		passiveSpell.activate(caster);
	}

}
