package com.nisovin.magicspells.util.prompt;

import org.bukkit.entity.Player;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;

public class MagicPromptResponder {

	String variableName;
	
	public MagicPromptResponder(ConfigurationSection section) {
		variableName = section.getString("variable-name", null);
	}

	@SuppressWarnings("removal")
	public Prompt acceptValidatedInput(ConversationContext paramConversationContext, String paramString) {
		String playerName = null;
		Conversable who = paramConversationContext.getForWhom();
		if (who instanceof Player player) playerName = player.getName();

		// Try to save response to a variable.
		MagicSpells.getVariableManager().set(variableName, playerName, paramString);

		return Prompt.END_OF_CONVERSATION;
	}
	
}
