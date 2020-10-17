package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of teleport causes to accept
public class TeleportListener extends PassiveListener {

	private final EnumSet<TeleportCause> teleportCauses = EnumSet.noneOf(TeleportCause.class);
	
	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		String[] split = var.replace(" ", "").split(",");
		for (String s : split) {
			s = s.trim().replace("_", "");
			for (TeleportCause cause : TeleportCause.values()) {
				if (!cause.name().replace("_", "").equalsIgnoreCase(s)) continue;
				teleportCauses.add(cause);
				break;
			}
		}
	}
	
	@OverridePriority
	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (!teleportCauses.isEmpty() && !teleportCauses.contains(event.getCause())) return;
		Player player = event.getPlayer();
		if (!hasSpell(player)) return;

		if (!isCancelStateOk(event.isCancelled())) return;
		boolean casted = passiveSpell.activate(player);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
