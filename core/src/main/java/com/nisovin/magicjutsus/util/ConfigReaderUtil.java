package com.nisovin.magicjutsus.util;

import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.prompt.PromptType;

public class ConfigReaderUtil {

	public static MagicLocation readLocation(ConfigurationSection section, String path) {
		return readLocation(section, path, "world,0,0,0");
	}
	
	public static MagicLocation readLocation(ConfigurationSection section, String path, String defaultText) {
		String s = section.getString(path, defaultText);
		MagicLocation ret;
		try {
			String[] split = s.split(",");
			String world = split[0];
			double x = Double.parseDouble(split[1]);
			double y = Double.parseDouble(split[2]);
			double z = Double.parseDouble(split[3]);
			float yaw = 0;
			float pitch = 0;
			if (split.length > 4) yaw = Float.parseFloat(split[4]);
			if (split.length > 5) pitch = Float.parseFloat(split[5]);
			ret = new MagicLocation(world, x, y, z, yaw, pitch);
		} catch (Exception e) {
			return null;
		}
		return ret;
	}
	
	public static Prompt readPrompt(ConfigurationSection section) {
		return readPrompt(section, Prompt.END_OF_CONVERSATION);
	}
	
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
	public static ConversationFactory readConversationFactory(ConfigurationSection section) {
		ConversationFactory ret = new ConversationFactory(MagicJutsus.plugin);
		
		// Handle the prefix
		String prefix = section.getString("prefix", null);
		if (prefix != null && !prefix.isEmpty()) ret = ret.withPrefix(new MagicConversationPrefix(prefix));
		
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
	
}
