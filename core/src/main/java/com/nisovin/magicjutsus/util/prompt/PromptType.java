package com.nisovin.magicjutsus.util.prompt;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.Prompt;

public enum PromptType {

	REGEX_PROMPT("regex") {

		@Override
		public Prompt constructPrompt(ConfigurationSection section) {
			return MagicRegexPrompt.fromConfigSection(section);
		}
		
	},
	
	FIXED_SET_PROMOT("fixed-set") {

		@Override
		public Prompt constructPrompt(ConfigurationSection section) {
			return MagicFixedSetPrompt.fromConfigSection(section);
		}
		
	},
	
	ENUM_SET_PROMPT("enum") {

		@Override
		public Prompt constructPrompt(ConfigurationSection section) {
			return MagicEnumSetPrompt.fromConfigSection(section);
		}
		
		@Override
		public void unload() {
			MagicEnumSetPrompt.unload();
		}
		
	}
	
	;
	
	private String[] labels;
	
	private static boolean initialized = false;
	private static Map<String, PromptType> nameMap = null;
	
	PromptType(String... names) {
		labels = names;
	}
	
	public abstract Prompt constructPrompt(ConfigurationSection section);
	
	public void unload() {
		// instances may override
	}
	
	public static void unloadDestructPromptData() {
		for (PromptType type: PromptType.values()) {
			type.unload();
		}
	}
	
	private static void initialize() {
		if (initialized) return;
		nameMap = new HashMap<>();
		for (PromptType type: PromptType.values()) {
			for (String name: type.labels) {
				nameMap.put(name.toLowerCase(), type);
			}
		}
		
		initialized = true;
	}
	
	public static PromptType getPromptType(String label) {
		if (!initialized) initialize();
		return nameMap.get(label.toLowerCase());
	}
	
}
