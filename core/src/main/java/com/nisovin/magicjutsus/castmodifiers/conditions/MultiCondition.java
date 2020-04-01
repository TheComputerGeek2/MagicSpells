package com.nisovin.magicjutsus.castmodifiers.conditions;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.castmodifiers.Modifier;
import com.nisovin.magicjutsus.events.ManaChangeEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.castmodifiers.Condition;
import com.nisovin.magicjutsus.castmodifiers.IModifier;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;
import com.nisovin.magicjutsus.events.MagicJutsusGenericPlayerEvent;

/*
 * Just a heads up that for the modifier actions inside this, I recommend that you use
 * stop rather than denied most of the time, because the denied action will actually cancel
 * the event being processed whereas the stop action will just say that this specific check
 * counts as a fail.
 * 
 * in the general config, you can define a set of modifiers like this
 * 
 * general:
 *     modifiers:
 *         modifier_name:
 *             checks:
 *                 - condition condition_var action action_var
 *                 - condition condition_var action action_var
 *             pass-condition: a string value that can be one of the following ANY, ALL, XOR
 * 
 * You can also define some in the jutsu*.yml files as follows
 * 
 * modifiers:
 *     modifier_name:
 *         checks:
 *             - condition condition_var action action_var
 *             - condition condition_var action action_var
 *         pass-condition: a string value that can be one of the following ANY, ALL, XOR
 *
 * to reference the modifier collection, you just slip this into your modifiers listed on a jutsu
 * - collection <modifier_name> action action_var
 * where <modifier_name> is the name that you assigned to the modifier collection as shown above
 */

public class MultiCondition extends Condition implements IModifier {
	
	private String configPrefix = "general.modifiers.";
	private List<Modifier> modifiers;
	
	//the condition on which this condition as a whole may pass
	private PassCondition passCondition = PassCondition.ALL;
	
	@Override
	public boolean setVar(String var) {
		configPrefix += var;
		MagicConfig config = MagicJutsus.plugin.getMagicConfig();
		if (!(config.contains(configPrefix) && config.isSection(configPrefix))) return false;
		
		List<String> modifierStrings = config.getStringList(configPrefix + ".checks", null);
		if (modifierStrings == null) return false;
		
		String passConditionString = config.getString(configPrefix + ".pass-condition", "ALL").toUpperCase();
		try {
			passCondition = PassCondition.valueOf(passConditionString);
		} catch (IllegalArgumentException badPassCondition) {
			MagicJutsus.error("Invalid value for \"pass-condition\" of \"" + passConditionString + "\".");
			// To preserve old behavior, just default it to "ALL"
			MagicJutsus.error("Defaulting pass-condition to \"ALL\"");
			passCondition = PassCondition.ALL;
		}
		
		modifiers = new ArrayList<>();
		for (String modString : modifierStrings) {
			Modifier m = Modifier.factory(modString);
			if (m != null) modifiers.add(m);
			else MagicJutsus.error("Problem in reading predefined modifier: \"" + modString + "\" from \"" + var + '\"');
		}
		
		if (modifiers == null || modifiers.isEmpty()) {
			MagicJutsus.error("Could not load any modifier checks for predefined modifier \"" + var + '\"');
			return false;
		}
		
		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return check(livingEntity, livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		int pass = 0;
		int fail = 0;
		for (Modifier m : modifiers) {
			boolean check = m.check(target);
			if (check) pass++;
			else fail++;
			if (!passCondition.shouldContinue(pass, fail)) return passCondition.hasPassed(pass, fail);
		}
		return passCondition.hasPassed(pass, fail);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	@Override
	public boolean apply(JutsuCastEvent event) {
		int pass = 0;
		int fail = 0;
		for (Modifier m : modifiers) {
			boolean check = m.apply(event);
			if (check) pass++;
			else fail++;

			if (!passCondition.shouldContinue(pass, fail)) return passCondition.hasPassed(pass, fail);
		}
		return passCondition.hasPassed(pass, fail);
	}

	@Override
	public boolean apply(ManaChangeEvent event) {
		int pass = 0;
		int fail = 0;
		for (Modifier m : modifiers) {
			boolean check = m.apply(event);
			if (check) pass++;
			else fail++;

			if (!passCondition.shouldContinue(pass, fail)) return passCondition.hasPassed(pass, fail);
		}
		return passCondition.hasPassed(pass, fail);
	}

	@Override
	public boolean apply(JutsuTargetEvent event) {
		int pass = 0;
		int fail = 0;
		for (Modifier m : modifiers) {
			boolean check = m.apply(event);
			if (check) pass++;
			else fail++;

			if (!passCondition.shouldContinue(pass, fail)) return passCondition.hasPassed(pass, fail);
		}
		return passCondition.hasPassed(pass, fail);
	}

	@Override
	public boolean apply(JutsuTargetLocationEvent event) {
		int pass = 0;
		int fail = 0;
		for (Modifier m : modifiers) {
			boolean check = m.apply(event);
			if (check) pass++;
			else fail++;

			if (!passCondition.shouldContinue(pass, fail)) return passCondition.hasPassed(pass, fail);
		}
		return passCondition.hasPassed(pass, fail);
	}

	@Override
	public boolean apply(MagicJutsusGenericPlayerEvent event) {
		int pass = 0;
		int fail = 0;
		for (Modifier m : modifiers) {
			boolean check = m.apply(event);
			if (check) pass++;
			else fail++;

			if (!passCondition.shouldContinue(pass, fail)) return passCondition.hasPassed(pass, fail);
		}
		return passCondition.hasPassed(pass, fail);
	}
	
	public enum PassCondition {
		
		ALL{

			@Override
			public boolean hasPassed(int passes, int fails) {
				return fails == 0;
			}

			@Override
			public boolean shouldContinue(int passes, int fails) {
				return fails == 0;
			}
			
		},
		
		ANY {
			@Override
			public boolean hasPassed(int passes, int fails) {
				return passes > 0;
			}

			@Override
			public boolean shouldContinue(int passes, int fails) {
				return passes == 0;
			}
		},
		
		XOR {

			@Override
			public boolean hasPassed(int passes, int fails) {
				return passes == 1;
			}

			@Override
			public boolean shouldContinue(int passes, int fails) {
				return passes <= 1;
			}
			
		}
		
		;
		
		PassCondition() {
			
		}
		
		public abstract boolean hasPassed(int passes, int fails);
		public abstract boolean shouldContinue(int passes, int fails);
		
	}
	
}
