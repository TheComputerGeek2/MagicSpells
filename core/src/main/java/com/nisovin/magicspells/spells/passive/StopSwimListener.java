package com.nisovin.magicspells.spells.passive;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityToggleSwimEvent;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

@Name("stopswim")
public class StopSwimListener extends PassiveListener {

	@Override
	public void initialize(@NotNull String var) {
	}

	@OverridePriority
	@EventHandler
	public void onSwim(EntityToggleSwimEvent event) {
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (event.isSwimming()) return;
		if (!canTrigger(caster)) return;
		passiveSpell.activate(caster);
	}

}
