package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;

import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.ConversationContext;

@SuppressWarnings("removal")
public class MagicConversationPrefix implements ConversationPrefix {

	private String prefix;
	
	public MagicConversationPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	@NotNull
	public String getPrefix(@NotNull ConversationContext paramConversationContext) {
		return prefix;
	}

}
