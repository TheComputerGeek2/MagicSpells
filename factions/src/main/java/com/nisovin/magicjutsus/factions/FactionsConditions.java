package com.nisovin.magicjutsus.factions;

import java.util.HashMap;
import java.util.Map;

import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.factions.conditions.HasFactionCondition;
import com.nisovin.magicjutsus.factions.conditions.PowerEqualsCondition;
import com.nisovin.magicjutsus.factions.conditions.PowerGreaterThanCondition;
import com.nisovin.magicjutsus.factions.conditions.PowerLessThanCondition;

public class FactionsConditions {
	
	public static Map<String, Class<? extends Condition>> conditions;
	private static final String ADDON_KEY = "factions";
	
	private static String makeKey(String basicName) {
		return (ADDON_KEY + ':' + basicName).toLowerCase();
	}
	
	static {
		conditions = new HashMap<>();
		conditions.put(makeKey("powerlessthan"), PowerLessThanCondition.class);
		conditions.put(makeKey("powergreaterthan"), PowerGreaterThanCondition.class);
		conditions.put(makeKey("powerequals"), PowerEqualsCondition.class);
		conditions.put(makeKey("hasfaction"), HasFactionCondition.class);
	}
	
}
