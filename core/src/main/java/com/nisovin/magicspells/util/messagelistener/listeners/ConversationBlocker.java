package com.nisovin.magicspells.util.messagelistener.listeners;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.conversations.Conversation;

import com.nisovin.magicspells.util.messagelistener.MessageListener;
import com.nisovin.magicspells.util.messagelistener.MagicConversation;

@SuppressWarnings("removal")
public class ConversationBlocker implements MessageListener {

	private final Map<UUID, Conversation> conversations = new HashMap<>();

	@Override
	public void addPlayer(Player player, ListenerData data) {
		if (!data.blockChatOutput()) return;

		Conversation conversation = new MagicConversation(player, data.strBlockedOutput());
		conversation.begin();
		conversations.put(player.getUniqueId(), conversation);
	}

	@Override
	public void removePlayer(Player player) {
		Conversation conversation = conversations.remove(player.getUniqueId());
		if (conversation == null) return;
		conversation.abandon();
	}

	@Override
	public void turnOff() {
		conversations.values().forEach(Conversation::abandon);
		conversations.clear();
	}

}
