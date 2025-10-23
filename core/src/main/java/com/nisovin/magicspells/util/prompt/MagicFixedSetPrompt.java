package com.nisovin.magicspells.util.prompt;

import java.util.List;
import java.util.ArrayList;

import com.nisovin.magicspells.util.Util;

import org.jetbrains.annotations.NotNull;

import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.configuration.ConfigurationSection;

@SuppressWarnings("removal")
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
	@NotNull
	public String getPromptText(@NotNull ConversationContext context) {
		return promptText;
	}

	@Override
	protected Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
		return responder.acceptValidatedInput(context, input);
	}
	
	public static MagicFixedSetPrompt fromConfigSection(ConfigurationSection section) {
		// Get the options
		List<String> options = section.getStringList("options");
		if (options.isEmpty()) return null;

		MagicFixedSetPrompt ret = new MagicFixedSetPrompt(options);
		ret.responder = new MagicPromptResponder(section);
		ret.promptText = Util.colorize(section.getString("prompt-text", ""));
		return ret;
	}
	
}
