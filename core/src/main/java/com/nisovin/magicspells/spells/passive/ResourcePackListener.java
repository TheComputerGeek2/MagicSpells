package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Trigger variable should be set to one of the following
// loaded,successfully_loaded
// declined
// failed,failed_download
// accepted
public class ResourcePackListener extends PassiveListener {

	private final EnumSet<Status> packStatus = EnumSet.noneOf(Status.class);
	
	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;
		switch (var.toLowerCase()) {
			case "successfully_loaded":
			case "loaded":
				packStatus.add(Status.SUCCESSFULLY_LOADED);
				break;
			case "declined":
				packStatus.add(Status.DECLINED);
				break;
			case "failed_download":
			case "failed":
				packStatus.add(Status.FAILED_DOWNLOAD);
				break;
			case "accepted":
				packStatus.add(Status.ACCEPTED);
				break;
		}
	}

	@OverridePriority
	@EventHandler
	public void onPlayerResourcePack(PlayerResourcePackStatusEvent event) {
		if (!hasSpell(event.getPlayer())) return;
		if (!packStatus.isEmpty() && !packStatus.contains(event.getStatus())) return;
		passiveSpell.activate(event.getPlayer());
	}

}
