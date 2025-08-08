package com.nisovin.magicspells.util.config;

import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.*;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.entity.EntityType;
import org.bukkit.block.data.BlockData;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.handlers.PotionEffectHandler;

public class ConfigDataUtil {

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			int value = config.getInt(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> function = FunctionData.build(config.getString(path), Double::intValue);
			if (function == null) return data -> null;

			return function;
		}

		return data -> null;
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, int def) {
		if (config.isInt(path)) {
			int value = config.getInt(path, def);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> function = FunctionData.build(config.getString(path), Double::intValue, def);
			if (function == null) return data -> def;

			return function;
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Integer> getInteger(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Integer> def) {
		if (config.isInt(path)) {
			int value = config.getInt(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Integer> function = FunctionData.build(config.getString(path), Double::intValue, def);
			if (function == null) return def;

			return function;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> function = FunctionData.build(config.getString(path), Double::longValue);
			if (function == null) return data -> null;

			return function;
		}

		return data -> null;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, long def) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path, def);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> function = FunctionData.build(config.getString(path), Double::longValue, def);
			if (function == null) return data -> def;

			return function;
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Long> getLong(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Long> def) {
		if (config.isInt(path) || config.isLong(path)) {
			long value = config.getLong(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Long> function = FunctionData.build(config.getString(path), Double::longValue, def);
			if (function == null) return def;

			return function;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> function = FunctionData.build(config.getString(path), Double::shortValue);
			if (function == null) return data -> null;

			return function;
		}

		return data -> null;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, short def) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path, def);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> function = FunctionData.build(config.getString(path), Double::shortValue, def);
			if (function == null) return data -> def;

			return function;
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Short> getShort(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Short> def) {
		if (config.isInt(path)) {
			short value = (short) config.getInt(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Short> function = FunctionData.build(config.getString(path), Double::shortValue, def);
			if (function == null) return def;

			return function;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> function = FunctionData.build(config.getString(path), Double::byteValue);
			if (function == null) return data -> null;

			return function;
		}

		return data -> null;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, byte def) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> function = FunctionData.build(config.getString(path), Double::byteValue, def);
			if (function == null) return data -> def;

			return function;
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Byte> getByte(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Byte> def) {
		if (config.isInt(path)) {
			byte value = (byte) config.getInt(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Byte> function = FunctionData.build(config.getString(path), Double::byteValue, def);
			if (function == null) return def;

			return function;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> function = FunctionData.build(config.getString(path), Function.identity());
			if (function == null) return data -> null;

			return function;
		}

		return data -> null;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, double def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path, def);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> function = FunctionData.build(config.getString(path), Function.identity(), def);
			if (function == null) return data -> def;

			return function;
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Double> getDouble(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Double> def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			double value = config.getDouble(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Double> function = FunctionData.build(config.getString(path), Function.identity(), def);
			if (function == null) return def;

			return function;
		}

		return def;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> function = FunctionData.build(config.getString(path), Double::floatValue);
			if (function == null) return data -> null;

			return function;
		}

		return data -> null;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, float def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path, def);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> function = FunctionData.build(config.getString(path), Double::floatValue, def);
			if (function == null) return data -> def;

			return function;
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Float> getFloat(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Float> def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			return data -> value;
		}

		if (config.isString(path)) {
			FunctionData<Float> function = FunctionData.build(config.getString(path), Double::floatValue, def);
			if (function == null) return def;

			return function;
		}

		return def;
	}

	@NotNull
	public static ConfigData<String> getString(@NotNull ConfigurationSection config, @NotNull String path, @Nullable String def) {
		String value = config.getString(path, def);
		if (value == null) return data -> null;

		return getString(value);
	}

	@NotNull
	public static ConfigData<String> getString(@Nullable String value) {
		if (value == null) return data -> null;

		StringData stringData = new StringData(value);
		if (stringData.isConstant()) return data -> value;

		List<ConfigData<String>> values = stringData.getValues();
		List<String> fragments = stringData.getFragments();
		if (values.size() == 1 && fragments.size() == 2 && fragments.get(0).isEmpty() && fragments.get(1).isEmpty())
			return values.get(0);

		return stringData;
	}

	@NotNull
	public static ConfigData<Component> getComponent(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Component def) {
		ConfigData<String> supplier = getString(config, path, null);
		if (supplier.isConstant()) {
			String value = supplier.get();
			if (value == null) return data -> def;

			Component component = Util.getMiniMessage(value);
			return data -> component;
		}

		return new ConfigData<>() {

			@Override
			public Component get(@NotNull SpellData data) {
				String value = supplier.get(data);
				if (value == null) return def;

				return Util.getMiniMessage(value);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<Component> getComponent(@NotNull String value) {
		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) {
			String val = supplier.get();
			if (val == null) return data -> null;

			Component component = Util.getMiniMessage(value);
			return data -> component;
		}

		return new ConfigData<>() {

			@Override
			public Component get(@NotNull SpellData data) {
				String value = supplier.get(data);
				if (value == null) return null;

				return Util.getMiniMessage(value);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return data -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			if (supplier.isConstant()) {
				Boolean val = parseBoolean(supplier.get());
				return data -> val;
			}

			return (VariableConfigData<Boolean>) data -> parseBoolean(supplier.get(data));
		}

		return data -> null;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, boolean def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return data -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			if (supplier.isConstant()) {
				boolean val = parseBoolean(supplier.get(), def);
				return data -> val;
			}

			return (VariableConfigData<Boolean>) data -> parseBoolean(supplier.get(data), def);
		}

		return data -> def;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull ConfigurationSection config, @NotNull String path, ConfigData<Boolean> def) {
		if (config.isBoolean(path)) {
			boolean val = config.getBoolean(path);
			return data -> val;
		}

		if (config.isString(path)) {
			ConfigData<String> supplier = getString(config, path, null);
			if (supplier.isConstant()) {
				Boolean val = parseBoolean(supplier.get());
				if (val == null) return def;

				return data -> val;
			}

			return (VariableConfigData<Boolean>) data -> {
				Boolean val = parseBoolean(supplier.get(data));
				return val == null ? def.get(data) : val;
			};
		}

		return def;
	}

	public static ConfigData<Boolean> getBoolean(@NotNull String value, boolean def) {
		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) {
			boolean val = parseBoolean(value, def);
			return data -> val;
		}

		return (VariableConfigData<Boolean>) data -> parseBoolean(supplier.get(data), def);
	}

	@Nullable
	private static Boolean parseBoolean(@Nullable String value) {
		if (value == null) return null;

		return switch (value.toLowerCase()) {
			case "true" -> true;
			case "false" -> false;
			default -> null;
		};
	}

	private static boolean parseBoolean(@Nullable String value, boolean def) {
		if (value == null) return def;

		return switch (value.toLowerCase()) {
			case "true" -> true;
			case "false" -> false;
			default -> def;
		};
	}

	@NotNull
	public static <T extends Enum<T>> ConfigData<T> getEnum(@NotNull ConfigurationSection config,
															@NotNull String path,
															@NotNull Class<T> type,
															@Nullable T def) {
		return getEnum(config.getString(path), type, def);
	}

	@NotNull
	public static <T extends Enum<T>> ConfigData<T> getEnum(@Nullable String value,
	                                                        @NotNull Class<T> type,
	                                                        @Nullable T def) {
		if (value == null) return data -> def;

		try {
			T val = Enum.valueOf(type, value.toUpperCase());
			return data -> val;
		} catch (IllegalArgumentException e) {
			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) return data -> def;

			return new ConfigData<>() {

				@Override
				public T get(@NotNull SpellData data) {
					String val = supplier.get(data);
					if (val == null) return def;

					try {
						return Enum.valueOf(type, val.toUpperCase());
					} catch (IllegalArgumentException ex) {
						return def;
					}
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}
	}

	public static ConfigData<Material> getMaterial(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Material def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		Material val = Util.getMaterial(value);
		if (val != null) return data -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return new ConfigData<>() {

			@Override
			public Material get(@NotNull SpellData data) {
				String val = supplier.get(data);
				if (val == null) return def;

				Material material = Util.getMaterial(val);
				return material == null ? def : material;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<PotionEffectType> getPotionEffectType(@NotNull ConfigurationSection config, @NotNull String path, @Nullable PotionEffectType def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		PotionEffectType type = PotionEffectHandler.getPotionEffectType(value);
		if (type != null) return data -> type;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return new ConfigData<>() {

			@Override
			public PotionEffectType get(@NotNull SpellData data) {
				String val = supplier.get(data);
				if (val == null) return def;

				PotionEffectType type = PotionEffectHandler.getPotionEffectType(val);
				return type == null ? def : type;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<Particle> getParticle(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Particle def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		Particle val = ParticleUtil.getParticle(value);
		if (val != null) return data -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return new ConfigData<>() {

			@Override
			public Particle get(@NotNull SpellData data) {
				String val = supplier.get(data);
				if (val == null) return def;

				Particle particle = ParticleUtil.getParticle(val);
				return particle == null ? def : particle;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	public static ConfigData<TargetBooleanState> getTargetBooleanState(@NotNull ConfigurationSection config, @NotNull String path, @Nullable TargetBooleanState def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		TargetBooleanState val = TargetBooleanState.getByName(value);
		if (val != null) return data -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return new ConfigData<>() {

			@Override
			public TargetBooleanState get(@NotNull SpellData data) {
				String val = supplier.get(data);
				if (val == null) return def;

				TargetBooleanState state = TargetBooleanState.getByName(val);
				return state == null ? def : state;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	public static ConfigData<EntityType> getEntityType(@NotNull ConfigurationSection config, @NotNull String path, @Nullable EntityType def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		EntityType val = MobUtil.getEntityType(value);
		if (val != null) return data -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return new ConfigData<>() {

			@Override
			public EntityType get(@NotNull SpellData data) {
				String val = supplier.get(data);
				if (val == null) return def;

				EntityType type = MobUtil.getEntityType(val);
				return type == null ? def : type;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<BlockData> getBlockData(@NotNull ConfigurationSection config, @NotNull String path, @Nullable BlockData def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		try {
			BlockData val = Bukkit.createBlockData(value.trim().toLowerCase());
			return data -> val;
		} catch (IllegalArgumentException e) {
			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) return data -> def;

			return new ConfigData<>() {

				@Override
				public BlockData get(@NotNull SpellData data) {
					String val = supplier.get(data);
					if (val == null) return def;

					try {
						return Bukkit.createBlockData(val.trim().toLowerCase());
					} catch (IllegalArgumentException e) {
						return def;
					}
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}
	}

	public static <T extends Keyed> ConfigData<T> getRegistryEntry(@NotNull ConfigurationSection config, @NotNull String path, @NotNull Registry<T> registry, @Nullable T def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		NamespacedKey key = NamespacedKey.fromString(value);
		if (key != null) {
			T val = registry.get(key);
			if (val != null) return data -> val;
		}

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return new ConfigData<>() {

			@Override
			public T get(@NotNull SpellData data) {
				String val = supplier.get(data);
				if (val == null) return def;

				NamespacedKey key = NamespacedKey.fromString(val);
				if (key == null) return def;

				T entry = registry.get(key);
				return entry == null ? def : entry;
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	public static ConfigData<NamedTextColor> getNamedTextColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable NamedTextColor def) {
		String value = config.getString(path);
		if (value == null) return data -> def;

		NamedTextColor val = NamedTextColor.NAMES.value(value.toLowerCase());
		if (val != null) return data -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return (VariableConfigData<NamedTextColor>) data -> {
			String string = supplier.get(data);
			if (string == null) return def;

			NamedTextColor color = NamedTextColor.NAMES.value(string.toLowerCase());
			return color == null ? def : color;
		};
	}

	public static ConfigData<NamespacedKey> getNamespacedKey(@NotNull ConfigurationSection config, @NotNull String path, @Nullable NamespacedKey def) {
		String value = config.getString(path);
		if (value == null) return data -> def;
		return getNamespacedKey(value, def);
	}

	public static ConfigData<NamespacedKey> getNamespacedKey(@NotNull String value, @Nullable NamespacedKey def) {
		NamespacedKey val = NamespacedKey.fromString(value.toLowerCase());
		if (val != null) return data -> val;

		ConfigData<String> supplier = getString(value);
		if (supplier.isConstant()) return data -> def;

		return (VariableConfigData<NamespacedKey>) data -> {
			String string = supplier.get(data);
			if (string == null) return def;

			NamespacedKey key = NamespacedKey.fromString(string.toLowerCase());
			return key == null ? def : key;
		};
	}

	public static ConfigData<Angle> getAngle(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Angle def) {
		if (config.isInt(path) || config.isLong(path) || config.isDouble(path)) {
			float value = (float) config.getDouble(path);
			Angle angle = new Angle(value, false);

			return data -> angle;
		}

		if (config.isString(path)) {
			String string = config.getString(path);

			boolean relative = string.startsWith("~");
			if (relative) {
				string = string.substring(1);

				try {
					float value = string.isEmpty() ? 0 : Float.parseFloat(string);
					Angle angle = new Angle(value, true);

					return data -> angle;
				} catch (NumberFormatException ignored) {
				}
			}

			FunctionData<Float> function = FunctionData.build(string, Double::floatValue);
			if (function == null) return data -> def;

			return data -> new Angle(function.get(data), relative);
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<Vector> getVector(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Vector def) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) return data -> null;
				return data -> def.clone();
			}

			String[] vec = value.split(",");
			if (vec.length != 3) {
				if (def == null) return data -> null;
				return data -> def.clone();
			}

			try {
				Vector vector = new Vector(Double.parseDouble(vec[0]), Double.parseDouble(vec[1]), Double.parseDouble(vec[2]));
				return data -> vector.clone();
			} catch (NumberFormatException e) {
				if (def == null) return data -> null;
				return data -> def.clone();
			}
		}

		ConfigData<Double> x;
		ConfigData<Double> y;
		ConfigData<Double> z;

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) {
				if (def == null) return data -> null;
				return data -> def.clone();
			}

			x = getDouble(section, "x", 0);
			y = getDouble(section, "y", 0);
			z = getDouble(section, "z", 0);
		} else if (config.isList(path)) {
			List<?> value = config.getList(path);
			if (value == null || value.size() != 3) {
				if (def == null) return data -> null;
				return data -> def.clone();
			}

			Object xObj = value.get(0);
			Object yObj = value.get(1);
			Object zObj = value.get(2);

			if (xObj instanceof Number number) {
				double val = number.doubleValue();
				x = data -> val;
			} else if (xObj instanceof String string) {
				x = FunctionData.build(string, Function.identity());
			} else x = null;

			if (yObj instanceof Number number) {
				double val = number.doubleValue();
				y = data -> val;
			} else if (yObj instanceof String string) {
				y = FunctionData.build(string, Function.identity());
			} else y = null;

			if (zObj instanceof Number number) {
				double val = number.doubleValue();
				z = data -> val;
			} else if (zObj instanceof String string) {
				z = FunctionData.build(string, Function.identity());
			} else z = null;

			if (x == null || y == null || z == null) {
				if (def == null) return data -> null;
				return data -> def.clone();
			}
		} else {
			if (def == null) return data -> null;
			return data -> def.clone();
		}

		if (x.isConstant() && y.isConstant() && z.isConstant()) {
			Vector vector = new Vector(x.get(), y.get(), z.get());
			return data -> vector.clone();
		}

		return data -> new Vector(x.get(data), y.get(data), z.get(data));
	}

	@NotNull
	public static ConfigData<EulerAngle> getEulerAngle(@NotNull ConfigurationSection config, @NotNull String path, @Nullable EulerAngle def) {
		if (config.isString(path)) {
			String value = config.getString(path);
			if (value == null) {
				if (def == null) return data -> null;
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			}

			String[] ang = value.split(",");
			if (ang.length != 3) {
				if (def == null) return data -> null;
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			}

			try {
				EulerAngle angle = new EulerAngle(Double.parseDouble(ang[0]), Double.parseDouble(ang[1]), Double.parseDouble(ang[2]));
				return data -> new EulerAngle(angle.getX(), angle.getY(), angle.getZ());
			} catch (NumberFormatException e) {
				if (def == null) return data -> null;
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			}
		}

		ConfigData<Double> x;
		ConfigData<Double> y;
		ConfigData<Double> z;

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) {
				if (def == null) return data -> null;
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			}

			x = getDouble(section, "x", 0);
			y = getDouble(section, "y", 0);
			z = getDouble(section, "z", 0);
		} else if (config.isList(path)) {
			List<?> value = config.getList(path);
			if (value == null || value.size() != 3) {
				if (def == null) return data -> null;
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			}

			Object xObj = value.get(0);
			Object yObj = value.get(1);
			Object zObj = value.get(2);

			if (xObj instanceof Number number) {
				double val = number.doubleValue();
				x = data -> val;
			} else if (xObj instanceof String string) {
				x = FunctionData.build(string, Function.identity());
			} else x = null;

			if (yObj instanceof Number number) {
				double val = number.doubleValue();
				y = data -> val;
			} else if (yObj instanceof String string) {
				y = FunctionData.build(string, Function.identity());
			} else y = null;

			if (zObj instanceof Number number) {
				double val = number.doubleValue();
				z = data -> val;
			} else if (zObj instanceof String string) {
				z = FunctionData.build(string, Function.identity());
			} else z = null;

			if (x == null || y == null || z == null) {
				if (def == null) return data -> null;
				return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
			}
		} else {
			if (def == null) return data -> null;
			return data -> new EulerAngle(def.getX(), def.getY(), def.getZ());
		}

		if (x.isConstant() && y.isConstant() && z.isConstant()) {
			EulerAngle angle = new EulerAngle(x.get(), y.get(), z.get());
			return data -> new EulerAngle(angle.getX(), angle.getY(), angle.getZ());
		}

		return data -> new EulerAngle(x.get(data), y.get(data), z.get(data));
	}

	public static ConfigData<Color> getColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		if (config.isInt(path) || config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return data -> def;

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				Color color = ColorUtil.getColorFromHexString(value, false);
				if (color == null) return data -> def;

				return data -> color;
			}

			return new ConfigData<>() {

				@Override
				public Color get(@NotNull SpellData data) {
					Color color = ColorUtil.getColorFromHexString(supplier.get(data), false);
					return color == null ? def : color;
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return data -> def;

			ConfigData<Integer> red = getInteger(section, "red", 0);
			ConfigData<Integer> green = getInteger(section, "green", 0);
			ConfigData<Integer> blue = getInteger(section, "blue", 0);

			if (red.isConstant() && green.isConstant() && blue.isConstant()) {
				Integer r = red.get();
				Integer g = green.get();
				Integer b = blue.get();
				if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
					return data -> def;

				Color c = Color.fromRGB(r, g, b);
				return data -> c;
			}

			return new ConfigData<>() {

				@Override
				public Color get(@NotNull SpellData data) {
					Integer r = red.get(data);
					Integer g = green.get(data);
					Integer b = blue.get(data);
					if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
						return def;

					return Color.fromRGB(r, g, b);
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		return data -> def;
	}

	public static ConfigData<Color> getARGBColor(@NotNull ConfigurationSection config, @NotNull String path, @Nullable Color def) {
		if (config.isInt(path) || config.isString(path)) {
			String value = config.getString(path);
			if (value == null) return data -> def;

			ConfigData<String> supplier = getString(value);
			if (supplier.isConstant()) {
				Color color = ColorUtil.getColorFromARGHexString(value, false);
				if (color == null) return data -> def;

				return data -> color;
			}

			return new ConfigData<>() {

				@Override
				public Color get(@NotNull SpellData data) {
					Color color = ColorUtil.getColorFromARGHexString(supplier.get(data), false);
					return color == null ? def : color;
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		if (config.isConfigurationSection(path)) {
			ConfigurationSection section = config.getConfigurationSection(path);
			if (section == null) return data -> def;

			ConfigData<Integer> alpha = getInteger(section, "alpha", 255);
			ConfigData<Integer> red = getInteger(section, "red", 0);
			ConfigData<Integer> green = getInteger(section, "green", 0);
			ConfigData<Integer> blue = getInteger(section, "blue", 0);

			if (alpha.isConstant() && red.isConstant() && green.isConstant() && blue.isConstant()) {
				Integer a = alpha.get();
				Integer r = red.get();
				Integer g = green.get();
				Integer b = blue.get();
				if (a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
					return data -> def;

				Color c = Color.fromARGB(a, r, g, b);
				return data -> c;
			}

			return new ConfigData<>() {

				@Override
				public Color get(@NotNull SpellData data) {
					Integer a = alpha.get(data);
					Integer r = red.get(data);
					Integer g = green.get(data);
					Integer b = blue.get(data);
					if (a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
						return def;

					return Color.fromARGB(a, r, g, b);
				}

				@Override
				public boolean isConstant() {
					return false;
				}

			};
		}

		return data -> def;
	}

	@NotNull
	public static ConfigData<DustOptions> getDustOptions(@NotNull ConfigurationSection config,
														 @NotNull String colorPath,
														 @NotNull String sizePath,
														 @Nullable DustOptions def) {
		ConfigData<Color> color = getColor(config, colorPath, def == null ? null : def.getColor());
		ConfigData<Float> size = def == null ? getFloat(config, sizePath) : getFloat(config, sizePath, def.getSize());

		if (color.isConstant() && size.isConstant()) {
			Color c = color.get();
			if (c == null) return data -> def;

			Float s = size.get();
			if (s == null) return data -> def;

			DustOptions options = new DustOptions(c, s);
			return data -> options;
		}

		return new ConfigData<>() {

			@Override
			public DustOptions get(@NotNull SpellData data) {
				Color c = color.get(data);
				if (c == null) return def;

				Float s = size.get(data);
				if (s == null) return def;

				return new DustOptions(c, s);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

	@NotNull
	public static ConfigData<DustTransition> getDustTransition(@NotNull ConfigurationSection config,
															   @NotNull String colorPath,
															   @NotNull String toColorPath,
															   @NotNull String sizePath,
															   @Nullable DustTransition def) {
		ConfigData<Color> color = getColor(config, colorPath, def == null ? null : def.getColor());
		ConfigData<Color> toColor = getColor(config, toColorPath, def == null ? null : def.getToColor());
		ConfigData<Float> size = def == null ? getFloat(config, sizePath) : getFloat(config, sizePath, def.getSize());

		if (color.isConstant() && toColor.isConstant() && size.isConstant()) {
			Color c = color.get();
			if (c == null) return data -> def;

			Color tc = toColor.get();
			if (tc == null) return data -> def;

			Float s = size.get();
			if (s == null) return data -> def;

			DustTransition transition = new DustTransition(c, tc, s);
			return data -> transition;
		}

		return new ConfigData<>() {

			@Override
			public DustTransition get(@NotNull SpellData data) {
				Color c = color.get(data);
				if (c == null) return def;

				Color tc = toColor.get(data);
				if (tc == null) return def;

				Float s = size.get(data);
				if (s == null) return def;

				return new DustTransition(c, tc, s);
			}

			@Override
			public boolean isConstant() {
				return false;
			}

		};
	}

}
