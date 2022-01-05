package com.nisovin.magicspells.spells.passive;

import java.util.Set;
import java.util.EnumSet;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

import static org.bukkit.event.entity.EntityTargetEvent.TargetReason;

public class EntityTargetListener extends PassiveListener {

	private final Set<TargetReason> targetReasons = EnumSet.noneOf(TargetReason.class);

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.split("\\|");
		for (String s : split) {
			try {
				TargetReason reason = TargetReason.valueOf(s.trim().toUpperCase());
				targetReasons.add(reason);
			} catch (IllegalArgumentException e) {
				MagicSpells.error("Invalid target reason'" + s + "' in entitytarget trigger on passive spell '" + passiveSpell.getInternalName() + "'.");
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if (!isCancelStateOk(event.isCancelled())) return;
		if (!(event.getEntity() instanceof LivingEntity caster)) return;
		if (!(event.getTarget() instanceof LivingEntity target)) return;

		if (!targetReasons.isEmpty() && !targetReasons.contains(event.getReason())) return;
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		boolean casted = passiveSpell.activate(caster, target);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
