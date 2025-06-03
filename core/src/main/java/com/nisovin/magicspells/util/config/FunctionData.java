package com.nisovin.magicspells.util.config;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.clip.placeholderapi.PlaceholderAPI;

import org.apache.commons.numbers.core.Precision;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.parser.ParseException;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.variables.variabletypes.GlobalVariable;
import com.nisovin.magicspells.util.config.expression.ExpressionDictionary;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.variables.variabletypes.PlayerStringVariable;

public class FunctionData<T extends Number> implements ConfigData<T> {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(?:" +
		"(?<var>(?<varOwner>var|castervar|targetvar):(?<varName>\\w+)(?::(?<varPrecision>\\d+))?)|" +
		"(?<pVar>playervar:(?<pVarUser>[^:]+):(?<pVarName>\\w+)(?::(?<pVarPrecision>\\d+))?)|" +
		"(?<arg>arg:(?<argValue>\\d+):(?<argDefault>" + RegexUtil.DOUBLE_PATTERN + "))|" +
		"(?<papi>(?<papiOwner>papi|casterpapi|targetpapi):(?<papiValue>[^%]+))|" +
		"(?<playerPapi>playerpapi:(?<playerPapiUser>[^:]+):(?<playerPapiValue>[^%]+))" +
		")%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

	private final Map<String, ConfigData<Double>> variables;
	private final Function<Double, T> converter;
	private final Expression expression;
	private final ConfigData<T> dataDef;
	private final T def;

	public FunctionData(@NotNull Expression expression, @NotNull Map<String, ConfigData<Double>> variables, @NotNull Function<Double, T> converter) {
		this.expression = expression;
		this.variables = variables;
		this.converter = converter;
		this.dataDef = null;
		this.def = null;
	}

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
	public static <T extends Number> FunctionData<T> build(@Nullable String expressionString, @NotNull Function<Double, T> converter) {
		return build(expressionString, converter, false);
	}

	@Nullable
	public static <T extends Number> FunctionData<T> build(@Nullable String expressionString, @NotNull Function<Double, T> converter, boolean silent) {
		Map<String, ConfigData<Double>> variables = new HashMap<>();

		Expression expression = buildExpression(expressionString, variables, silent);
		if (expression == null) return null;

		return new FunctionData<>(expression, variables, converter);
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

		ExpressionDictionary dictionary = MagicSpells.getExpressionDictionary();
		Expression expression = new Expression(builder.toString(), dictionary.getExpressionConfiguration()).withValues(dictionary.getConstants());
		try {
			expression.validate();
			return expression;
		} catch (ParseException e) {
			if (!silent) {
				MagicSpells.error("Invalid expression '" + expressionString + "'.");
				e.printStackTrace();
			}

			return null;
		}
	}

	private static ConfigData<Double> createData(Matcher matcher) {
		if (matcher.group("var") != null) {
			String owner = matcher.group("varOwner");
			String variable = matcher.group("varName");
			String placesString = matcher.group("varPrecision");

			int places = -1;
			if (placesString != null) {
				try {
					places = Integer.parseInt(placesString);
				} catch (NumberFormatException e) {
					return data -> 0d;
				}
			}

			return switch (owner.toLowerCase()) {
				case "castervar" -> new CasterVariableData(variable, places);
				case "targetvar" -> new TargetVariableData(variable, places);
				default -> new DefaultVariableData(variable, places);
			};
		}

		if (matcher.group("pVar") != null) {
			String player = matcher.group("pVarUser");
			String variable = matcher.group("pVarName");
			String placesString = matcher.group("pVarPrecision");

			int places = -1;
			if (placesString != null) {
				try {
					places = Integer.parseInt(placesString);
				} catch (NumberFormatException e) {
					return data -> 0d;
				}
			}

			return new PlayerVariableData(variable, player, places);
		}

		if (matcher.group("arg") != null) {
			String def = matcher.group("argDefault");

			int index;
			try {
				index = Integer.parseInt(matcher.group("argValue"));
			} catch (NumberFormatException e) {
				return data -> 0d;
			}
			if (index == 0) return data -> 0d;

			return new ArgumentData(index - 1, def);
		}

		if (matcher.group("papi") != null) {
			String owner = matcher.group("papiOwner");
			String papiPlaceholder = '%' + matcher.group("papiValue") + '%';

			return switch (owner.toLowerCase()) {
				case "casterpapi" -> new CasterPAPIData(papiPlaceholder);
				case "targetpapi" -> new TargetPAPIData(papiPlaceholder);
				default -> new DefaultPAPIData(papiPlaceholder);
			};
		}

		if (matcher.group("playerPapi") != null) {
			String player = matcher.group("playerPapiUser");
			String papiPlaceholder = '%' + matcher.group("playerPapiValue") + '%';

			return new PlayerPAPIData(papiPlaceholder, player);
		}

		return data -> 0d;
	}

	@Override
	public T get(@NotNull SpellData data) {
		for (Map.Entry<String, ConfigData<Double>> entry : variables.entrySet())
			expression.with(entry.getKey(), entry.getValue().get(data));

		expression.with("power", data.power());

		try {
			return converter.apply(expression.evaluate().getNumberValue().doubleValue());
		} catch (Exception e) {
			return dataDef != null ? dataDef.get(data) : def;
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
		public Double get(@NotNull SpellData data) {
			if (data.args() != null && data.args().length > index) {
				try {
					return Double.parseDouble(data.args()[index]);
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

	public static class DefaultVariableData implements ConfigData<Double> {

		private final String variable;
		private final int places;

		public DefaultVariableData(String variable, int places) {
			this.variable = variable;
			this.places = places;
		}

		@Override
		public Double get(@NotNull SpellData data) {
			Variable var = MagicSpells.getVariableManager().getVariable(variable);
			if (var == null) return 0d;

			String player = data.recipient() instanceof Player p ? p.getName() : null;
			if (player == null && !(var instanceof GlobalVariable) && !(var instanceof GlobalStringVariable))
				return 0d;

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

	public static class CasterVariableData implements ConfigData<Double> {

		private final String variable;
		private final int places;

		public CasterVariableData(String variable, int places) {
			this.variable = variable;
			this.places = places;
		}

		@Override
		public Double get(@NotNull SpellData data) {
			Variable var = MagicSpells.getVariableManager().getVariable(variable);
			if (var == null) return 0d;

			String player = data.caster() instanceof Player p ? p.getName() : null;
			if (player == null && !(var instanceof GlobalVariable) && !(var instanceof GlobalStringVariable))
				return 0d;

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
		public Double get(@NotNull SpellData data) {
			Variable var = MagicSpells.getVariableManager().getVariable(variable);
			if (var == null) return 0d;

			String player = data.target() instanceof Player p ? p.getName() : null;
			if (player == null && !(var instanceof GlobalVariable) && !(var instanceof GlobalStringVariable))
				return 0d;

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
		public Double get(@NotNull SpellData data) {
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

	public static class DefaultPAPIData implements ConfigData<Double> {

		private final String placeholder;

		public DefaultPAPIData(String placeholder) {
			this.placeholder = placeholder;
		}

		@Override
		public Double get(@NotNull SpellData data) {
			if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return 0d;

			String value = PlaceholderAPI.setPlaceholders(data.recipient() instanceof Player p ? p : null, placeholder);

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

	public static class CasterPAPIData implements ConfigData<Double> {

		private final String placeholder;

		public CasterPAPIData(String placeholder) {
			this.placeholder = placeholder;
		}

		@Override
		public Double get(@NotNull SpellData data) {
			if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
				return 0d;

			String value = PlaceholderAPI.setPlaceholders(data.caster() instanceof Player p ? p : null, placeholder);

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
		public Double get(@NotNull SpellData data) {
			if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
				return 0d;

			String value = PlaceholderAPI.setPlaceholders(data.target() instanceof Player p ? p : null, placeholder);

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
		public Double get(@NotNull SpellData data) {
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
