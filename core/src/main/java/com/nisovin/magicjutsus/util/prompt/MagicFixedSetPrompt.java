package com.nisovin.magicjutsus.util.prompt;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;

public class MagicFixedSetPrompt extends FixedSetPrompt {

	private String promptText;
	
	private MagicPromptResponder responder;
	
	public MagicFixedSetPrompt(List<String> options) {
		super();
		super.fixedSet = new ArrayList<>(options);
	}
	
	public MagicFixedSetPrompt(String... options) {
		super(options);
	}
	
	@Override
	public String getPromptText(ConversationContext context) {
		return promptText;
	}

	@Override
	protected Prompt acceptValidatedInput(ConversationContext context, String input) {
		return responder.acceptValidatedInput(context, input);
	}
	
	public static MagicFixedSetPrompt fromConfigSection(ConfigurationSection section) {
		// Get the options
		List<String> options = section.getStringList("options");
		if (options == null || options.isEmpty()) return null;
		MagicFixedSetPrompt ret = new MagicFixedSetPrompt(options);
		
		ret.responder = new MagicPromptResponder(section);
		
		String promptText = section.getString("prompt-text", "");
		ret.promptText = promptText;
		
		return ret;
	}
	
}
