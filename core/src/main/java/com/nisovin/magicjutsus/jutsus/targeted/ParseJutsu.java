package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class ParseJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private int op;

	private String parseTo;
	private String operation;
	private String firstVariable;
	private String expectedValue;
	private String secondVariable;
	private String parseToVariable;
	private String variableToParse;

	public ParseJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		parseTo = getConfigString("parse-to", "");
		operation = getConfigString("operation", "normal");
		firstVariable = getConfigString("first-variable", "");
		expectedValue = getConfigString("expected-value", "");
		secondVariable = getConfigString("second-variable", "");
		parseToVariable = getConfigString("parse-to-variable", "");
		variableToParse = getConfigString("variable-to-parse", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		op = 0;
		if (operation.contains("translate") || operation.contains("normal")) {
			if (expectedValue.isEmpty()) {
				MagicJutsus.error("ParseJutsu '" + internalName + "' has an invalid expected-value defined!");
				return;
			}
			if (parseToVariable.isEmpty()) {
				MagicJutsus.error("ParseJutsu '" + internalName + "' has an invalid parse-to-variable defined!");
				return;
			}
			if (variableToParse.isEmpty() || MagicJutsus.getVariableManager().getVariable(variableToParse) == null) {
				MagicJutsus.error("ParseJutsu '" + internalName + "' has an invalid variable-to-parse defined!");
				return;
			}
			op = 1;
		}

		if (operation.contains("difference")) {
			if (firstVariable.isEmpty() || MagicJutsus.getVariableManager().getVariable(firstVariable) == null) {
				MagicJutsus.error("ParseJutsu '" + internalName + "' has an invalid first-variable defined!");
				return;
			}
			if (secondVariable.isEmpty() || MagicJutsus.getVariableManager().getVariable(secondVariable) == null) {
				MagicJutsus.error("ParseJutsu '" + internalName + "' has an invalid second-variable defined!");
				return;
			}
			op = 2;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(livingEntity);

			parse(target);
			playJutsuEffects(livingEntity, target);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		playJutsuEffects(caster, target);
		parse(target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playJutsuEffects(EffectPosition.TARGET, target);
		parse(target);
		return true;
	}

	private void parse(LivingEntity target) {
		if (!(target instanceof Player)) return;
		if (op == 0) return;

		if (op == 1) {
			String receivedValue = MagicJutsus.getVariableManager().getStringValue(variableToParse, (Player) target);
			if (!receivedValue.equalsIgnoreCase(expectedValue) && !expectedValue.contains("any")) return;
			MagicJutsus.getVariableManager().set(parseToVariable, (Player) target, parseTo);
		} else if (op == 2) {
			double primary = MagicJutsus.getVariableManager().getValue(firstVariable, (Player) target);
			double secondary = MagicJutsus.getVariableManager().getValue(secondVariable, (Player) target);
			double diff = Math.abs(primary - secondary);
			MagicJutsus.getVariableManager().set(parseToVariable, (Player) target, diff);
		}
	}

}
