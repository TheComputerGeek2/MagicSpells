package com.nisovin.magicspells.util;

import java.util.Map;

import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.prompt.PromptType;

public class ConfigReaderUtil {

	@SuppressWarnings("removal")
	public static Prompt readPrompt(ConfigurationSection section) {
		return readPrompt(section, Prompt.END_OF_CONVERSATION);
	}

	@SuppressWarnings("removal")
	public static Prompt readPrompt(ConfigurationSection section, Prompt defaultPrompt) {
		if (section == null) return defaultPrompt;
		String type = section.getString("prompt-type");
		PromptType promptType = PromptType.getPromptType(type);
		if (promptType == null) return defaultPrompt;
		return promptType.constructPrompt(section);
	}

	// prefix accepts a string and defaults to null
	// local-echo accepts a boolean and defaults to true
	// first-prompt accepts a configuration section in prompt format
	// timeout-seconds accepts an integer and defaults to 30
	// escape-sequence accepts a string and defaults to null
	@SuppressWarnings("removal")
	public static ConversationFactory readConversationFactory(ConfigurationSection section) {
		ConversationFactory ret = new ConversationFactory(MagicSpells.plugin);

		// Handle the prefix
		String prefix = section.getString("prefix", null);
		if (prefix != null && !prefix.isEmpty()) ret = ret.withPrefix(new MagicConversationPrefix(Util.colorize(prefix)));

		// Handle local echo
		boolean localEcho = section.getBoolean("local-echo", true);
		ret = ret.withLocalEcho(localEcho);

		// Handle first prompt loading
		Prompt firstPrompt = readPrompt(section.getConfigurationSection("first-prompt"));
		ret = ret.withFirstPrompt(firstPrompt);

		// Handle timeout
		int timeoutSeconds = section.getInt("timeout-seconds", 30);
		ret = ret.withTimeout(timeoutSeconds);

		// Handle escape sequence
		String escapeSequence = section.getString("escape-sequence", null);
		if (escapeSequence != null && !escapeSequence.isEmpty()) ret = ret.withEscapeSequence("");

		// Return
		return ret;
	}

	public static ConfigurationSection mapToSection(Map<?, ?> map) {
		ConfigurationSection section = new MemoryConfiguration();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = String.valueOf(entry.getKey());
			if (entry.getValue() instanceof Map<?, ?> inner) section.createSection(key, inner);
			else section.set(key, entry.getValue());
		}
		return section;
	}

}
