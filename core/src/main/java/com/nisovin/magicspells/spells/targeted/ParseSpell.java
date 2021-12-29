package com.nisovin.magicspells.spells.targeted;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ParseSpell extends TargetedSpell implements TargetedEntitySpell {

	private final String operation;
	private Pattern regexPattern;

	private final String operationName;
	private final String parseTo;
	private final String firstVariable;
	private final String expectedValue;
	private final String secondVariable;
	private final String parseToVariable;
	private final String variableToParse;

	public ParseSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		operationName = getConfigString("operation", "normal");
		operation = operationName.toLowerCase();

		parseTo = getConfigString("parse-to", "");
		expectedValue = getConfigString("expected-value", "");
		parseToVariable = getConfigString("parse-to-variable", "");
		variableToParse = getConfigString("variable-to-parse", "");
		firstVariable = getConfigString("first-variable", "");
		secondVariable = getConfigString("second-variable", "");
	}

	@Override
	public void initializeVariables() {
		super.initializeVariables();
		// Check if the related spell properties were defined properly.
		switch (operation) {
			case "translate", "normal", "append" -> {
				if (expectedValue.isEmpty()) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid expected-value defined!");
				}
				if (parseToVariable.isEmpty()) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid parse-to-variable defined!");
				}
				if (variableToParse.isEmpty() || MagicSpells.getVariableManager().getVariable(variableToParse) == null) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid variable-to-parse defined!");
				}
			}
			case "difference" -> {
				if (firstVariable.isEmpty() || MagicSpells.getVariableManager().getVariable(firstVariable) == null) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid first-variable defined!");
				}
				if (secondVariable.isEmpty() || MagicSpells.getVariableManager().getVariable(secondVariable) == null) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid second-variable defined!");
				}
			}
			case "regex", "regexp" -> {
				if (parseTo.isEmpty()) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid parse-to defined!");
				}
				regexPattern = Pattern.compile(expectedValue);
				if (parseToVariable.isEmpty()) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid parse-to-variable defined!");
				}
				if (variableToParse.isEmpty() || MagicSpells.getVariableManager().getVariable(variableToParse) == null) {
					MagicSpells.error("ParseSpell '" + internalName + "' has an invalid variable-to-parse defined!");
				}
			}
			default -> MagicSpells.error("ParseSpell '" + internalName + "' has invalid operation defined: " + operationName);
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power, args);
			if (targetInfo == null) return noTarget(caster);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(caster);

			parse(target);
			playSpellEffects(caster, target);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		playSpellEffects(caster, target);
		parse(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		parse(target);
		return true;
	}

	private void parse(LivingEntity target) {
		if (!(target instanceof Player)) return;
		String receivedValue;
		switch (operation) {
			case "translate", "normal" -> {
				receivedValue = MagicSpells.getVariableManager().getStringValue(variableToParse, (Player) target);
				if (!receivedValue.equalsIgnoreCase(expectedValue) && !expectedValue.contains("any")) return;
				MagicSpells.getVariableManager().set(parseToVariable, (Player) target, parseTo);
			}
			case "difference" -> {
				double primary = MagicSpells.getVariableManager().getValue(firstVariable, (Player) target);
				double secondary = MagicSpells.getVariableManager().getValue(secondVariable, (Player) target);
				double diff = Math.abs(primary - secondary);
				MagicSpells.getVariableManager().set(parseToVariable, (Player) target, diff);
			}
			case "append" -> {
				receivedValue = MagicSpells.getVariableManager().getStringValue(variableToParse, (Player) target);
				if (!receivedValue.equalsIgnoreCase(expectedValue) && !expectedValue.contains("any")) return;
				receivedValue += parseTo;
				receivedValue = MagicSpells.getVariableManager().getStringValue(parseToVariable, (Player) target) + receivedValue;
				MagicSpells.getVariableManager().set(parseToVariable, (Player) target, receivedValue);
			}
			case "regex", "regexp" -> {
				receivedValue = MagicSpells.getVariableManager().getStringValue(variableToParse, (Player) target);
				Matcher matcher = regexPattern.matcher(receivedValue);
				StringBuilder found = new StringBuilder();
				while (matcher.find()) {
					if (!parseTo.isEmpty()) {
						// If there is replacement text, replace and exit.
						found = new StringBuilder(receivedValue.replaceAll(regexPattern.pattern(), parseTo));
						break;
					}
					// Otherwise, collect found text.
					found.append(matcher.group());
				}
				MagicSpells.getVariableManager().set(parseToVariable, (Player) target, found.toString());
			}
		}
	}
}
