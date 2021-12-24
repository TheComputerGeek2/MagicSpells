package com.nisovin.magicspells.util.config;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.Pair;

import de.slikey.exp4j.Expression;
import de.slikey.exp4j.ValidationResult;
import de.slikey.exp4j.ExpressionBuilder;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.managers.VariableManager;

public abstract class FunctionData<T> implements ConfigData<T> {

	private static final Pattern ARGUMENT_PATTERN = Pattern.compile("%arg:(\\d+):(" + RegexUtil.DOUBLE_PATTERN + ")%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("%(var|castervar|targetvar):(\\w+)%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private final Expression expression;
	private final boolean targeted;

	protected final ConfigData<T> dataDef;
	protected final T def;

	public FunctionData(@NotNull Expression expression, @NotNull T def, boolean targeted) {
		this.expression = expression;
		this.targeted = targeted;
		this.dataDef = null;
		this.def = def;
	}

	public FunctionData(@NotNull Expression expression, @NotNull ConfigData<T> def, boolean targeted) {
		this.expression = expression;
		this.targeted = targeted;
		this.dataDef = def;
		this.def = null;
	}

	public static Pair<Expression, Boolean> buildExpression(String expressionString) {
		return buildExpression(expressionString, false);
	}

	public static Pair<Expression, Boolean> buildExpression(String expressionString, boolean silent) {
		if (expressionString.isEmpty()) return null;

		Set<String> variables = new HashSet<>();
		variables.add("power");

		Matcher matcher = VARIABLE_PATTERN.matcher(expressionString);
		StringBuilder builder = new StringBuilder();
		boolean targeted = false;

		while (matcher.find()) {
			String variable;
			if (matcher.group(1).equals("targetvar")) {
				variable = "target." + matcher.group(2);
				targeted = true;
			} else variable = matcher.group(2);

			variables.add(variable);
			matcher.appendReplacement(builder, variable);
		}
		matcher.appendTail(builder);

		matcher = ARGUMENT_PATTERN.matcher(builder.toString());
		builder = new StringBuilder();
		while (matcher.find()) {
			String variable = "arg." + matcher.group(1) + "." + matcher.group(2);

			variables.add(variable);
			matcher.appendReplacement(builder, variable);
		}
		matcher.appendTail(builder);

		Expression expression;
		try {
			expression = new ExpressionBuilder(builder.toString())
				.variables(variables)
				.build();

			ValidationResult result = expression.validate(false);
			if (!result.isValid()) {
				if (!silent) MagicSpells.error("Invalid equation '" + expressionString + "': [" + String.join(", ", result.getErrors()) + "]");
				return null;
			}

			return new Pair<>(expression, targeted);
		} catch (IllegalArgumentException e) {
			if (!silent) {
				MagicSpells.error("Invalid expression '" + expressionString + "'.");
				e.printStackTrace();
			}

			return null;
		}
	}

	protected Double getValue(LivingEntity caster, LivingEntity target, float power, String[] args) {
		Set<String> variables = expression.getVariableNames();

		Player playerCaster = caster instanceof Player ? (Player) caster : null;
		Player playerTarget = target instanceof Player ? (Player) target : null;

		VariableManager variableManager = MagicSpells.getVariableManager();

		for (String variable : variables) {
			if (variable.startsWith("arg.")) {
				String[] split = variable.split("\\.");

				int index = Integer.parseInt(split[1]) - 1;
				double value;

				if (args != null && index < args.length) {
					try {
						value = Double.parseDouble(args[index]);
					} catch (NumberFormatException ignored) {
						value = Double.parseDouble(split[2]);
					}
				} else value = Double.parseDouble(split[2]);

				expression.setVariable(variable, value);
			} else if (variable.startsWith("target."))
				expression.setVariable(variable, variableManager.getValue(variable.substring(7), playerTarget));
			else
				expression.setVariable(variable, variableManager.getValue(variable, playerCaster));
		}

		expression.setVariable("power", power);

		try {
			return expression.evaluate();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	@Override
	public boolean isTargeted() {
		return targeted;
	}

	public static class DoubleData extends FunctionData<Double> {

		public DoubleData(@NotNull Expression expression, @NotNull Double def, boolean targeted) {
			super(expression, def, targeted);
		}

		public DoubleData(@NotNull Expression expression, @NotNull ConfigData<Double> def, boolean targeted) {
			super(expression, def, targeted);
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			Double ret = getValue(caster, target, power, args);

			if (ret == null) {
				if (dataDef != null) ret = dataDef.get(caster, target, power, args);
				else ret = def;
			}

			return ret;
		}

	}

	public static class FloatData extends FunctionData<Float> {

		public FloatData(@NotNull Expression expression, @NotNull Float def, boolean targeted) {
			super(expression, def, targeted);
		}

		public FloatData(@NotNull Expression expression, @NotNull ConfigData<Float> def, boolean targeted) {
			super(expression, def, targeted);
		}

		@Override
		public Float get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			Double value = getValue(caster, target, power, args);
			Float ret;

			if (value != null) ret = value.floatValue();
			else if (dataDef != null) ret = dataDef.get(caster, target, power, args);
			else ret = def;

			return ret;
		}

	}

	public static class IntegerData extends FunctionData<Integer> {

		public IntegerData(@NotNull Expression expression, @NotNull Integer def, boolean targeted) {
			super(expression, def, targeted);
		}

		public IntegerData(@NotNull Expression expression, @NotNull ConfigData<Integer> def, boolean targeted) {
			super(expression, def, targeted);
		}

		@Override
		public Integer get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			Double value = getValue(caster, target, power, args);
			Integer ret;

			if (value != null) ret = value.intValue();
			else if (dataDef != null) ret = dataDef.get(caster, target, power, args);
			else ret = def;

			return ret;
		}

	}

}
