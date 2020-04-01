package com.nisovin.magicjutsus.util.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.Prompt;

import com.nisovin.magicjutsus.MagicJutsus;

public class MagicEnumSetPrompt extends FixedSetPrompt {

	private static Map<String, List<String>> enumToNames = null;
	private static boolean initialized = false;
	
	private String promptText;
	
	private MagicPromptResponder responder;
	
	public MagicEnumSetPrompt(List<String> options) {
		super();
		super.fixedSet = new ArrayList<>(options);
	}
	
	public MagicEnumSetPrompt(String... options) {
		super(options);
	}
	
	private static void initializeEnumToNameMap() {
		if (initialized) return;
		if (enumToNames == null) enumToNames = new ConcurrentHashMap<>();
		initialized = true;
	}
	
	public static void unload() {
		if (!initialized) return;
		enumToNames.clear();
		initialized = false;
	}
	
	@Override
	public String getPromptText(ConversationContext context) {
		return promptText;
	}

	@Override
	protected Prompt acceptValidatedInput(ConversationContext context, String input) {
		return responder.acceptValidatedInput(context, input);
	}
	
	public static MagicEnumSetPrompt fromConfigSection(ConfigurationSection section) {
		// Get the options
		String enumClassName = section.getString("enum-class");
		Class<? extends Enum<?>> enumClass;
		try {
			enumClass = (Class<? extends Enum<?>>) Class.forName(enumClassName);
			if (!enumClass.isEnum()) throw new ClassCastException(enumClass.getName() + " is not an enum!");
		} catch (ClassNotFoundException e) {
			MagicJutsus.error("Error trying to produce MagicEnumSetPrompt");
			e.printStackTrace();
			return null;
		}
		
		List<String> parsedValues = getEnumValues(enumClass);
		
		MagicEnumSetPrompt ret = new MagicEnumSetPrompt(parsedValues);
		
		ret.responder = new MagicPromptResponder(section);
		
		String promptText = section.getString("prompt-text", "");
		ret.promptText = promptText;
		
		return ret;
	}
	
	private static List<String> getEnumValues(Class<? extends Enum<?>> clazz) {
		initializeEnumToNameMap();
		
		if (!enumToNames.containsKey(clazz.getName())) {
			Enum<?>[] values =  clazz.getEnumConstants();
			if (values == null || values.length == 0) {
				enumToNames.put(clazz.getName(), new ArrayList<>());
			} else {
				List<String> parsedValues = new ArrayList<>();
				for (Enum<?> e: values) {
					parsedValues.add(e.name());
				}
				enumToNames.put(clazz.getName(), parsedValues);
			}
		}
		
		return new ArrayList<>(enumToNames.get(clazz.getName()));
	}
	
}
