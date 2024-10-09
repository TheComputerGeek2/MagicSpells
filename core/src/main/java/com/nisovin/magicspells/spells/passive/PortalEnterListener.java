package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

import com.nisovin.magicspells.util.Name;

import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.events.PortalEnterEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("portalenter")
public class PortalEnterListener extends PassiveListener {

	private SpellFilter filter;

	@Override
	public void initialize(@NotNull String var) {
		if (var.isEmpty()) return;
		filter = SpellFilter.fromLegacyString(var);
	}

	@EventHandler
	public void onEnter(PortalEnterEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;

		LivingEntity entity = event.getEntity();
		if (!canTrigger(entity)) return;

		if (filter != null && !filter.check(event.getPortalSpell())) return;

		boolean casted = passiveSpell.activate(entity);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
