package com.nisovin.magicspells.util.prompt;

import java.util.regex.Pattern;

import com.nisovin.magicspells.util.Util;

import org.jetbrains.annotations.NotNull;

import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.RegexPrompt;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.ConversationContext;

@SuppressWarnings("removal")
public class MagicRegexPrompt extends RegexPrompt {
	
	private String promptText;
	
	private MagicPromptResponder responder;
	
	public MagicRegexPrompt(String pattern) {
		super(pattern);
	}
	
	public MagicRegexPrompt(Pattern pattern) {
		super(pattern);
	}

	@Override
	@NotNull
	public String getPromptText(@NotNull ConversationContext paramConversationContext) {
		return promptText;
	}

	@Override
	protected Prompt acceptValidatedInput(@NotNull ConversationContext paramConversationContext, @NotNull String paramString) {
		return responder.acceptValidatedInput(paramConversationContext, paramString);
	}
	
	
	public static MagicRegexPrompt fromConfigSection(ConfigurationSection section) {
		// Handle the regex
		String regexp = section.getString("regexp", null);
		if (regexp == null || regexp.isEmpty()) return null;

		MagicRegexPrompt ret = new MagicRegexPrompt(regexp);
		ret.responder = new MagicPromptResponder(section);
		ret.promptText = Util.colorize(section.getString("prompt-text", ""));
		return ret;
	}

}
