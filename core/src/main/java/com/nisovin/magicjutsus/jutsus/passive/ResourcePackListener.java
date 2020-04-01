package com.nisovin.magicjutsus.jutsus.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.EnumWrappers.ResourcePackStatus;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.PassiveJutsu;

// Trigger variable should be set to one of the following
// loaded
// declined
// failed
// accepted
public class ResourcePackListener extends PassiveListener {

	PacketListener listener;
	
	List<PassiveJutsu> jutsusLoaded = new ArrayList<>();
	List<PassiveJutsu> jutsusDeclined = new ArrayList<>();
	List<PassiveJutsu> jutsusFailed = new ArrayList<>();
	List<PassiveJutsu> jutsusAccepted = new ArrayList<>();
	
	@Override
	public void registerJutsu(PassiveJutsu jutsu, PassiveTrigger trigger, String var) {
		addPacketListener();
		if (var.equalsIgnoreCase("loaded")) {
			jutsusLoaded.add(jutsu);
		} else if (var.equalsIgnoreCase("declined")) {
			jutsusDeclined.add(jutsu);
		} else if (var.equalsIgnoreCase("failed")) {
			jutsusFailed.add(jutsu);
		} else if (var.equalsIgnoreCase("accepted")) {
			jutsusAccepted.add(jutsu);
		}
	}
	
	void addPacketListener() {
		if (listener == null) {						
			listener = new PacketAdapter(MagicJutsus.plugin, PacketType.Play.Client.RESOURCE_PACK_STATUS) {
				@Override
				public void onPacketReceiving(PacketEvent event) {
					Player player = event.getPlayer();
					ResourcePackStatus status = event.getPacket().getResourcePackStatus().read(0);
					if (status == ResourcePackStatus.SUCCESSFULLY_LOADED) {
						activate(player, jutsusLoaded);
					} else if (status == ResourcePackStatus.DECLINED) {
						activate(player, jutsusDeclined);
					} else if (status == ResourcePackStatus.FAILED_DOWNLOAD) {
						activate(player, jutsusFailed);
					} else if (status == ResourcePackStatus.ACCEPTED) {
						activate(player, jutsusAccepted);
					}
				}
			};
			ProtocolLibrary.getProtocolManager().addPacketListener(listener);
		}
	}
	
	void activate(Player player, List<PassiveJutsu> jutsus) {
		for (PassiveJutsu jutsu : jutsus) {
			jutsu.activate(player);
		}
	}

	@Override
	public void turnOff() {
		ProtocolLibrary.getProtocolManager().removePacketListener(listener);
		jutsusLoaded.clear();
		jutsusDeclined.clear();
		jutsusFailed.clear();
		jutsusAccepted.clear();
	}

}
