package com.nisovin.magicjutsus.util.prompt;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.MagicJutsus;

public class MagicPromptResponder {

	boolean saveToVariable;
	String variableName;
	
	public MagicPromptResponder(ConfigurationSection section) {
		variableName = section.getString("variable-name", null);
		saveToVariable = MagicJutsus.getVariableManager().getVariable(variableName) != null;
	}
	
	public Prompt acceptValidatedInput(ConversationContext paramConversationContext, String paramString) {
		String playerName = null;
		Conversable who = ConversationContextUtil.getConversable(paramConversationContext.getAllSessionData());
		if (who instanceof Player) playerName = ((Player)who).getName();
		
		if (saveToVariable) MagicJutsus.getVariableManager().set(variableName, playerName, paramString);
		return Prompt.END_OF_CONVERSATION;
	}
	
}
