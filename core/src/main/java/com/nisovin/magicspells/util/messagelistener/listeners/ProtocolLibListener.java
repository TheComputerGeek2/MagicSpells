package com.nisovin.magicspells.util.messagelistener.listeners;

import java.util.*;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.messagelistener.MessageListener;

public class ProtocolLibListener implements MessageListener {

	private PacketListener instance;
	private Map<UUID, RecordedData> listening;

	public ProtocolLibListener() {
		instance = new PacketListener();
		ProtocolLibrary.getProtocolManager().addPacketListener(instance);
		listening = Collections.synchronizedMap(new HashMap<>());
	}

	@Override
	public void addPlayer(Player player, ListenerData listenerData) {
		listening.put(player.getUniqueId(), new RecordedData(listenerData));
	}

	@Override
	public void removePlayer(Player player) {
		RecordedData recordedData = listening.remove(player.getUniqueId());
		if (recordedData == null) return;

		String variable = recordedData.listenerData.storeChatOutput();
		String message = recordedData.recorded.toString();
		MagicSpells.getVariableManager().set(variable, player.getName(), message);
	}

	@Override
	public void turnOff() {
		ProtocolLibrary.getProtocolManager().removePacketListener(instance);
		instance = null;
		listening = null;
	}

	private class PacketListener extends PacketAdapter {

		PacketListener() {
			super(MagicSpells.getInstance(), Server.SYSTEM_CHAT, Server.DISGUISED_CHAT);
		}

		@Override
		public void onPacketSending(PacketEvent event) {
			PacketContainer container = event.getPacket();
			Player player = event.getPlayer();

			// Ignore SystemChat actionbar.
			if (container.getType() == Server.SYSTEM_CHAT && container.getBooleans().read(0)) return;

			RecordedData recordedData = listening.get(player.getUniqueId());
			if (recordedData == null) return;
			ListenerData listenerData = recordedData.listenerData;

			event.setCancelled(listenerData.blockChatOutput());

			if (listenerData.storeChatOutput() == null) return;

			WrappedChatComponent chat = container.getChatComponents().read(0);
			Component component = AdventureComponentConverter.fromWrapper(chat);

			StringBuilder recorded = recordedData.recorded;
			if (!recorded.isEmpty()) recorded.append("\\n");
			recorded.append(Util.getPlainString(component));
		}

	}

	private static final class RecordedData {

		private final ListenerData listenerData;
		private final StringBuilder recorded = new StringBuilder();

		private RecordedData(ListenerData listenerData) {
			this.listenerData = listenerData;
		}

	}

}
