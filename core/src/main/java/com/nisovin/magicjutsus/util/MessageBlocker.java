package com.nisovin.magicjutsus.util;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.MagicJutsus;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketAdapter;

public class MessageBlocker {

	private Set<String> blocking = Collections.synchronizedSet(new HashSet<>());

	private PacketListener packetListener;
	
	public MessageBlocker() {
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		packetListener = new PacketListener();
		protocolManager.addPacketListener(packetListener);
	}
	
	public void addPlayer(Player player) {
		blocking.add(player.getName());
	}
	
	public void removePlayer(Player player) {
		blocking.remove(player.getName());
	}
	
	public void turnOff() {
		if (packetListener == null) return;
		ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
		protocolManager.removePacketListener(packetListener);
		packetListener = null;
	}
	
	private class PacketListener extends PacketAdapter {
		
		PacketListener() {
			super(MagicJutsus.plugin, PacketType.Play.Server.CHAT);
		}
		
		@Override
		public void onPacketSending(PacketEvent event) {
			if (!blocking.contains(event.getPlayer().getName())) return;
			event.setCancelled(true);
		}
		
	}
	
}
