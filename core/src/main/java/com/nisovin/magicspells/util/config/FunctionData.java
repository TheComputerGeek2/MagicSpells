package com.nisovin.magicspells.util.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;

import de.slikey.exp4j.Expression;
import de.slikey.exp4j.ValidationResult;
import de.slikey.exp4j.ExpressionBuilder;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import me.clip.placeholderapi.PlaceholderAPI;

import org.apache.commons.numbers.core.Precision;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.variables.variabletypes.PlayerStringVariable;

public class FunctionData<T extends Number> implements ConfigData<T> {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(?:((var|castervar|targetvar):(\\w+)(?::(\\d+))?)|(playervar:([a-zA-Z0-9_]{3,16}):(\\w+)(?::(\\d+))?)|(arg:(\\d+):(" + RegexUtil.DOUBLE_PATTERN + "))|((papi|casterpapi|targetpapi):([^%]+))|(playerpapi:([a-zA-Z0-9_]{3,16}):([^%]+)))%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private final Map<String, ConfigData<Double>> variables;
	private final Function<Double, T> converter;
	private final Expression expression;
	private final ConfigData<T> dataDef;
	private final T def;

	public FunctionData(@NotNull Expression expression, @NotNull Map<String, ConfigData<Double>> variables, @NotNull Function<Double, T> converter, @NotNull T def) {
		this.expression = expression;
		this.variables = variables;
		this.converter = converter;
		this.dataDef = null;
		this.def = def;
	}

	public FunctionData(@NotNull Expression expression, @NotNull Map<String, ConfigData<Double>> variables, @NotNull Function<Double, T> converter, @NotNull ConfigData<T> def) {
		this.expression = expression;
		this.variables = variables;
		this.converter = converter;
		this.dataDef = def;
		this.def = null;
	}

	@Nullable
	public static <T extends Number> FunctionData<T> build(@Nullable String expressionString, @NotNull Function<Double, T> converter, @NotNull T def) {
		return build(expressionString, converter, def, false);
	}

	@Nullable
	public static <T extends Number> FunctionData<T> build(@Nullable String expressionString, @NotNull Function<Double, T> converter, @NotNull T def, boolean silent) {
		Map<String, ConfigData<Double>> variables = new HashMap<>();

		Expression expression = buildExpression(expressionString, variables, silent);
		if (expression == null) return null;

		return new FunctionData<>(expression, variables, converter, def);
	}

	@Nullable
	public static <T extends Number> FunctionData<T> build(@Nullable String expressionString, @NotNull Function<Double, T> converter, @NotNull ConfigData<T> def) {
		return build(expressionString, converter, def, false);
	}

	@Nullable
	public static <T extends Number> FunctionData<T> build(@Nullable String expressionString, @NotNull Function<Double, T> converter, @NotNull ConfigData<T> def, boolean silent) {
		Map<String, ConfigData<Double>> variables = new HashMap<>();

		Expression expression = buildExpression(expressionString, variables, silent);
		if (expression == null) return null;

		return new FunctionData<>(expression, variables, converter, def);
	}

	@Nullable
	public static Expression buildExpression(@Nullable String expressionString, @NotNull Map<String, ConfigData<Double>> variables, boolean silent) {
		if (expressionString == null || expressionString.isEmpty()) return null;

		Matcher matcher = PLACEHOLDER_PATTERN.matcher(expressionString);
		StringBuilder builder = new StringBuilder();
		int count = 0;

		while (matcher.find()) {
			String variable = "var" + count;
			count++;

			ConfigData<Double> data = createData(matcher);
			variables.put(variable, data);

			matcher.appendReplacement(builder, variable);
		}
		matcher.appendTail(builder);

		Expression expression;
		try {
			expression = new ExpressionBuilder(builder.toString())
				.variables(variables.keySet())
				.variable("power")
				.build();

			ValidationResult result = expression.validate(false);
			if (!result.isValid()) {
				if (!silent)
					MagicSpells.error("Invalid expression '" + expressionString + "': [" + String.join(", ", result.getErrors()) + "]");
				return null;
			}

			return expression;
		} catch (IllegalArgumentException e) {
			if (!silent) {
				MagicSpells.error("Invalid expression '" + expressionString + "'.");
				e.printStackTrace();
			}

			return null;
		}
	}

	private static ConfigData<Double> createData(Matcher matcher) {
		if (matcher.group(1) != null) {
			String owner = matcher.group(2);
			String variable = matcher.group(3);
			String placesString = matcher.group(4);

			int places = -1;
			if (placesString != null) {
				try {
					places = Integer.parseInt(placesString);
				} catch (NumberFormatException e) {
					return (caster, target, power, args) -> 0d;
				}
			}

			return owner.equalsIgnoreCase("targetvar") ?
				new TargetVariableData(variable, places) :
				new CasterVariableData(variable, places);
		}

		if (matcher.group(5) != null) {
			String player = matcher.group(6);
			String variable = matcher.group(7);
			String placesString = matcher.group(8);

			int places = -1;
			if (placesString != null) {
				try {
					places = Integer.parseInt(placesString);
				} catch (NumberFormatException e) {
					return (caster, target, power, args) -> 0d;
				}
			}

			return new PlayerVariableData(variable, player, places);
		}

		if (matcher.group(9) != null) {
			String def = matcher.group(11);

			int index;
			try {
				index = Integer.parseInt(matcher.group(10));
			} catch (NumberFormatException e) {
				return (caster, target, power, args) -> 0d;
			}
			if (index == 0) return (caster, target, power, args) -> 0d;

			return new ArgumentData(index - 1, def);
		}

		if (matcher.group(12) != null) {
			String owner = matcher.group(13);
			String papiPlaceholder = '%' + matcher.group(14) + '%';

			return owner.equalsIgnoreCase("targetpapi") ?
				new TargetPAPIData(papiPlaceholder) :
				new CasterPAPIData(papiPlaceholder);
		}

		if (matcher.group(15) != null) {
			String player = matcher.group(16);
			String papiPlaceholder = '%' + matcher.group(17) + '%';

			return new PlayerPAPIData(papiPlaceholder, player);
		}

		return (caster, target, power, args) -> 0d;
	}

	@Override
	public T get(LivingEntity caster, LivingEntity target, float power, String[] args) {
		for (Map.Entry<String, ConfigData<Double>> entry : variables.entrySet())
			expression.setVariable(entry.getKey(), entry.getValue().get(caster, target, power, args));

		expression.setVariable("power", power);

		try {
			return converter.apply(expression.evaluate());
		} catch (Exception e) {
			return dataDef != null ? dataDef.get(caster, target, power, args) : def;
		}
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	public static class ArgumentData implements ConfigData<Double> {

		private final double def;
		private final int index;

		public ArgumentData(int index, String def) {
			this.index = index;

			double d;
			try {
				d = Double.parseDouble(def);
			} catch (NumberFormatException e) {
				d = 0;
			}

			this.def = d;
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			if (args != null && args.length > index) {
				try {
					return Double.parseDouble(args[index]);
				} catch (NumberFormatException e) {
					return def;
				}
			} else return def;
		}

		@Override
		public boolean isConstant() {
			return false;
		}

	}

	public static class CasterVariableData implements ConfigData<Double> {

		private final String variable;
		private final int places;

		public CasterVariableData(String variable, int places) {
			this.variable = variable;
			this.places = places;
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			if (!(caster instanceof Player player)) return 0d;

			Variable var = MagicSpells.getVariableManager().getVariable(variable);
			if (var == null) return 0d;

			double value;
			if (var instanceof PlayerStringVariable || var instanceof GlobalStringVariable) {
				try {
					value = Double.parseDouble(var.getStringValue(player));
				} catch (NumberFormatException e) {
					return 0d;
				}
			} else value = var.getValue(player);

			return places >= 0 ? Precision.round(value, places) : value;
		}

		@Override
		public boolean isConstant() {
			return false;
		}

	}

	public static class TargetVariableData implements ConfigData<Double> {

		private final String variable;
		private final int places;

		public TargetVariableData(String variable, int places) {
			this.variable = variable;
			this.places = places;
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			if (!(target instanceof Player player)) return 0d;

			Variable var = MagicSpells.getVariableManager().getVariable(variable);
			if (var == null) return 0d;

			double value;
			if (var instanceof PlayerStringVariable || var instanceof GlobalStringVariable) {
				try {
					value = Double.parseDouble(var.getStringValue(player));
				} catch (NumberFormatException e) {
					return 0d;
				}
			} else value = var.getValue(player);

			return places >= 0 ? Precision.round(value, places) : value;
		}

		@Override
		public boolean isConstant() {
			return false;
		}

	}

	public static class PlayerVariableData implements ConfigData<Double> {

		private final String variable;
		private final String player;
		private final int places;

		public PlayerVariableData(String variable, String player, int places) {
			this.variable = variable;
			this.player = player;
			this.places = places;
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			Variable var = MagicSpells.getVariableManager().getVariable(variable);
			if (var == null) return 0d;

			double value;
			if (var instanceof PlayerStringVariable || var instanceof GlobalStringVariable) {
				try {
					value = Double.parseDouble(var.getStringValue(player));
				} catch (NumberFormatException e) {
					return 0d;
				}
			} else value = var.getValue(player);

			return places >= 0 ? Precision.round(value, places) : value;
		}

		@Override
		public boolean isConstant() {
			return false;
		}

	}

	public static class CasterPAPIData implements ConfigData<Double> {

		private final String papiPlaceholder;

		public CasterPAPIData(String papiPlaceholder) {
			this.papiPlaceholder = papiPlaceholder;
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") || !(caster instanceof Player player))
				return 0d;

			String value = PlaceholderAPI.setPlaceholders(player, papiPlaceholder);

			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return 0d;
			}
		}

		@Override
		public boolean isConstant() {
			return false;
		}

	}

	public static class TargetPAPIData implements ConfigData<Double> {

		private final String placeholder;

		public TargetPAPIData(String placeholder) {
			this.placeholder = placeholder;
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") || !(target instanceof Player player))
				return 0d;

			String value = PlaceholderAPI.setPlaceholders(player, placeholder);

			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return 0d;
			}
		}

		@Override
		public boolean isConstant() {
			return false;
		}

	}

	public static class PlayerPAPIData implements ConfigData<Double> {

		private final String placeholder;
		private final String player;

		public PlayerPAPIData(String placeholder, String player) {
			this.placeholder = placeholder;
			this.player = player;
		}

		@Override
		public Double get(LivingEntity caster, LivingEntity target, float power, String[] args) {
			if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return 0d;

			String value = PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(player), placeholder);

			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return 0d;
			}
		}

		@Override
		public boolean isConstant() {
			return false;
		}

	}

}
