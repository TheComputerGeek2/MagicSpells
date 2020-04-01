package com.nisovin.magicjutsus.util;

import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.ConversationContext;

public class MagicConversationPrefix implements ConversationPrefix {

	private String prefix;
	
	public MagicConversationPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	@Override
	public String getPrefix(ConversationContext paramConversationContext) {
		return prefix;
	}

}
