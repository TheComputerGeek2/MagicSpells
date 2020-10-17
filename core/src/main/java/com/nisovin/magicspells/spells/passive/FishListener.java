package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerFishEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable is optional
// If not specified, it triggers in all forms
// The trigger variable may be a comma separated list containing any of the following
// ground, fish, fail, <entity type>
public class FishListener extends PassiveListener {

	private final EnumSet<EntityType> types = EnumSet.noneOf(EntityType.class);

	private final EnumSet<PlayerFishEvent.State> states = EnumSet.noneOf(PlayerFishEvent.State.class);
	
	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.replace(" ", "").toUpperCase().split(",");
		for (String s : split) {
			switch (s.toLowerCase()) {
				case "in_ground":
				case "ground":
					states.add(PlayerFishEvent.State.IN_GROUND);
					break;
				case "caught_fish":
				case "fish":
					states.add(PlayerFishEvent.State.CAUGHT_FISH);
					break;
				case "failed_attempt":
				case "failed":
				case "fail":
					states.add(PlayerFishEvent.State.FAILED_ATTEMPT);
					break;
				default:
					EntityType type = Util.getEntityType(s);
					if (type == null) return;
					types.add(type);
					break;
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onFish(PlayerFishEvent event) {
		PlayerFishEvent.State state = event.getState();
		Player player = event.getPlayer();
		if (!hasSpell(player)) return;

		Entity entity = event.getCaught();

		if (states.isEmpty()) {
			if (!isCancelStateOk(event.isCancelled())) return;
			boolean casted = passiveSpell.activate(player, entity instanceof LivingEntity ? (LivingEntity) entity : null);
			if (!cancelDefaultAction(casted)) return;
			event.setCancelled(true);

			return;
		}

		if (!states.contains(state) && types.isEmpty()) return;
		boolean casted;
		switch (state) {
			case IN_GROUND:
			case CAUGHT_FISH:
			case FAILED_ATTEMPT:
				if (!states.contains(state)) return;
				if (!isCancelStateOk(event.isCancelled())) return;
				casted = passiveSpell.activate(player, event.getHook().getLocation());
				if (!cancelDefaultAction(casted)) return;
				event.setCancelled(true);
				break;
			case CAUGHT_ENTITY:
				if (entity == null) return;
				if (!types.contains(entity.getType())) return;
				if (!isCancelStateOk(event.isCancelled())) return;
				casted = passiveSpell.activate(player, entity instanceof LivingEntity ? (LivingEntity) entity : null);
				if (!cancelDefaultAction(casted)) return;
				event.setCancelled(true);
				break;
		}
	}
	
}
