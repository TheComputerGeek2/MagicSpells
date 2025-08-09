package com.nisovin.magicspells.util;

import java.io.File;
import java.io.FileOutputStream;

import java.net.URL;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Predicate;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.*;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.util.VoxelShape;
import org.bukkit.util.BoundingBox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import io.papermc.paper.block.fluid.FluidData;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Util {

	private static final Pattern WEIRD_HEX_PATTERN = Pattern.compile("[&§]x(([&§][0-9a-f]){6})", Pattern.CASE_INSENSITIVE);
	private static final Pattern COLOR_PATTERN = Pattern.compile("[&§]([0-9a-fk-or])", Pattern.CASE_INSENSITIVE);
	private static final Pattern HEX_PATTERN = Pattern.compile("[&§](#[0-9a-f]{6})", Pattern.CASE_INSENSITIVE);

	private static final MiniMessage STRICT_SERIALIZER = MiniMessage.builder().strict(true).build();

	public static Material getMaterial(String name) {
		return Material.matchMaterial(name);
	}

	// - <potionEffectType> (amplifier) (duration) (ambient) (particles) (icon)
	public static PotionEffect buildPotionEffect(String effectString) {
		String[] data = effectString.split(" ");
		PotionEffectType type = PotionEffectHandler.getPotionEffectType(data[0]);

		if (type == null) {
			MagicSpells.error('\'' + data[0] + "' could not be connected to a potion effect type");
			return null;
		}

		int amplifier = 0;
		if (data.length > 1) {
			try {
				amplifier = Integer.parseInt(data[1]);
			} catch (NumberFormatException ex) {
				DebugHandler.debugNumberFormat(ex);
			}
		}

		int duration = 600;
		if (data.length > 2) {
			try {
				duration = Integer.parseInt(data[2]);
			} catch (NumberFormatException ex) {
				DebugHandler.debugNumberFormat(ex);
			}
		}

		boolean ambient = data.length > 3 && (BooleanUtils.isYes(data[3]) || data[3].equalsIgnoreCase("ambient"));

		boolean particles = data.length > 4 && (BooleanUtils.isYes(data[4]) || data[4].equalsIgnoreCase("particles"));

		boolean icon = data.length > 5 && (BooleanUtils.isYes(data[5]) || data[5].equalsIgnoreCase("icon"));

		return new PotionEffect(type, duration, amplifier, ambient, particles, icon);
	}

	// - <potionEffectType> (duration)
	public static PotionEffect buildSuspiciousStewPotionEffect(String effectString) {
		String[] data = effectString.split(" ");
		PotionEffectType t = PotionEffectHandler.getPotionEffectType(data[0]);

		if (t == null) {
			MagicSpells.error('\'' + data[0] + "' could not be connected to a potion effect type");
			return null;
		}

		int duration = 600;
		if (data.length > 1) {
			try {
				duration = Integer.parseInt(data[1]);
			} catch (NumberFormatException ex) {
				DebugHandler.debugNumberFormat(ex);
			}
		}
		return new PotionEffect(t, duration, 0, true);
	}

	public static Color[] getColorsFromString(String str) {
		int[] colors = new int[] { 0xFF0000 };
		String[] args = str.replace(" ", "").split(",");
		if (args.length > 0) {
			colors = new int[args.length];
			for (int i = 0; i < colors.length; i++) {
				try {
					colors[i] = Integer.parseInt(args[i], 16);
				} catch (NumberFormatException e) {
					colors[i] = 0;
				}
			}
		}

		Color[] c = new Color[colors.length];
		for (int i = 0; i < colors.length; i++) {
			c[i] = Color.fromRGB(colors[i]);
		}
		return c;
	}

	@Nullable
	public static List<ConfigData<PotionEffect>> getPotionEffects(@Nullable List<?> potionEffectData, @Nullable String internalName) {
		return getPotionEffects(potionEffectData, internalName, false, false);
	}

	@Nullable
	public static List<ConfigData<PotionEffect>> getPotionEffects(@Nullable List<?> potionEffectData, @Nullable String internalName, boolean spellPowerAffectsDuration, boolean spellPowerAffectsStrength) {
		if (potionEffectData == null || potionEffectData.isEmpty()) return null;

		List<ConfigData<PotionEffect>> potionEffects = new ArrayList<>();

		for (Object potionEffectObj : potionEffectData) {
			if (potionEffectObj instanceof String potionEffectString) {
				String[] data = potionEffectString.split(" ");

				PotionEffectType type = PotionEffectHandler.getPotionEffectType(data[0]);
				if (type == null) {
					MagicSpells.error("Invalid potion effect string '" + potionEffectString + "'" + (internalName == null ? "" : " in spell '" + internalName + "'."));
					continue;
				}

				int duration = 0;
				if (data.length >= 2) {
					try {
						duration = Integer.parseInt(data[1]);
					} catch (NumberFormatException e) {
						MagicSpells.error("Invalid duration '" + duration + "' in potion effect string '" + potionEffectString + "'" + (internalName == null ? "" : " in spell '" + internalName + "'."));
						continue;
					}
				}

				int strength = 0;
				if (data.length >= 3) {
					try {
						strength = Integer.parseInt(data[2]);
					} catch (NumberFormatException e) {
						MagicSpells.error("Invalid strength '" + strength + "' in potion effect string '" + potionEffectString + "'" + (internalName == null ? "" : " in spell '" + internalName + "'."));
						continue;
					}
				}

				boolean particles = data.length < 4 || !Boolean.parseBoolean(data[3]);
				boolean ambient = data.length >= 5 && Boolean.parseBoolean(data[4]);
				boolean icon = data.length < 6 || Boolean.parseBoolean(data[5]);

				if (data.length > 6)
					MagicSpells.error("Trailing data found in potion effect string '" + potionEffectString + "'" + (internalName == null ? "" : " in spell '" + internalName + "'."));

				if (spellPowerAffectsDuration || spellPowerAffectsStrength) {
					int finalDuration = duration;
					int finalStrength = strength;

					ConfigData<PotionEffect> effect;
					if (!spellPowerAffectsStrength)
						effect = spellData -> new PotionEffect(type, Math.round(finalDuration * spellData.power()), finalStrength, ambient, particles, icon);
					else if (!spellPowerAffectsDuration)
						effect = spellData -> new PotionEffect(type, finalDuration, Math.round(finalStrength * spellData.power()), ambient, particles, icon);
					else
						effect = spellData -> new PotionEffect(type, Math.round(finalDuration * spellData.power()), Math.round(finalStrength * spellData.power()), ambient, particles, icon);

					potionEffects.add(effect);
				} else {
					PotionEffect effect = new PotionEffect(type, duration, strength, ambient, particles, icon);
					potionEffects.add(spellData -> effect);
				}
			} else if (potionEffectObj instanceof Map<?, ?> potionEffectMap) {
				ConfigurationSection section = ConfigReaderUtil.mapToSection(potionEffectMap);

				ConfigData<PotionEffectType> type = ConfigDataUtil.getPotionEffectType(section, "type", PotionEffectType.SPEED);
				ConfigData<Integer> duration = ConfigDataUtil.getInteger(section, "duration", 0);
				ConfigData<Integer> strength = ConfigDataUtil.getInteger(section, "strength", 0);
				ConfigData<Boolean> ambient = ConfigDataUtil.getBoolean(section, "ambient", false);
				ConfigData<Boolean> hidden = ConfigDataUtil.getBoolean(section, "hidden", false);
				ConfigData<Boolean> icon = ConfigDataUtil.getBoolean(section, "icon", true);

				ConfigData<PotionEffect> effect = spellData -> {
					int d = duration.get(spellData);
					if (spellPowerAffectsDuration) d = Math.round(d * spellData.power());

					int s = strength.get(spellData);
					if (spellPowerAffectsStrength) s = Math.round(s * spellData.power());

					return new PotionEffect(
						type.get(spellData),
						d,
						s,
						ambient.get(spellData),
						!hidden.get(spellData),
						icon.get(spellData)
					);
				};

				potionEffects.add(effect);
			}
		}

		return potionEffects.isEmpty() ? null : potionEffects;
	}

	public static double getYawOfVector(Vector vector) {
		return AccurateMath.toDegrees(AccurateMath.atan2(-vector.getX(), vector.getZ()));
	}

	public static void playHurtEffect(@NotNull LivingEntity receiver, @Nullable LivingEntity source) {
		float angle = 0;

		// Get horizontal directed angle between the receiver's direction and the source.
		if (source != null && !receiver.equals(source)) {
			Vector first = receiver.getLocation().getDirection();
			Vector second = source.getLocation().toVector().subtract(receiver.getLocation().toVector());
			double angrad = AccurateMath.atan2(second.getZ(), second.getX()) - AccurateMath.atan2(first.getZ(), first.getX());
			angle = (float) AccurateMath.toDegrees(angrad);
			if (angle < 0) angle += 360;
		}

		receiver.playHurtAnimation(angle);
		MagicSpells.getVolatileCodeHandler().playHurtSound(receiver);
	}

	public static String arrayJoin(String[] array, char with) {
		if (array == null || array.length == 0) return "";
		int len = array.length;
		StringBuilder sb = new StringBuilder(16 + len << 3);
		sb.append(array[0]);
		for (int i = 1; i < len; i++) {
			sb.append(with);
			sb.append(array[i]);
		}
		return sb.toString();
	}

	public static String[] splitParams(String string, int max) {
		String[] words = string.trim().split(" ");
		if (words.length <= 1) return words;

		char quote = ' ';
		List<String> list = new ArrayList<>();
		StringBuilder building = new StringBuilder();

		for (String word : words) {
			if (word.isEmpty()) continue;
			if (max > 0 && list.size() == max - 1) {
				if (!building.isEmpty()) building.append(" ");
				building.append(word);
				continue;
			}

			if (quote == ' ') {
				if (word.length() == 1 || (word.charAt(0) != '"' && word.charAt(0) != '\'')) {
					list.add(word);
					continue;
				}

				quote = word.charAt(0);

				if (quote == word.charAt(word.length() - 1)) {
					quote = ' ';
					list.add(word.substring(1, word.length() - 1));
					continue;
				}

				building = new StringBuilder(word.substring(1));
				continue;
			}

			if (word.charAt(word.length() - 1) == quote) {
				list.add(building.toString() + ' ' + word.substring(0, word.length() - 1));
				building = new StringBuilder();
				quote = ' ';
				continue;
			}
			building.append(' ').append(word);
		}

		if (!building.isEmpty()) list.add(building.toString());

		return list.toArray(new String[0]);
	}

	public static String[] splitParams(String string) {
		return splitParams(string, 0);
	}

	public static String[] splitParams(String[] split, int max) {
		return splitParams(arrayJoin(split, ' '), max);
	}

	public static String[] splitParams(String[] split) {
		return splitParams(arrayJoin(split, ' '), 0);
	}

	public static boolean removeFromInventory(Inventory inventory, SpellReagents.ReagentItem item) {
		MagicItemData itemData = item.getMagicItemData();
		if (itemData == null) return false;

		int amt = item.getAmount();
		MagicItemData magicData;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) continue;

			magicData = MagicItems.getMagicItemDataFromItemStack(items[i]);
			if (magicData == null || !itemData.matches(magicData)) continue;

			if (items[i].getAmount() > amt) {
				items[i].setAmount(items[i].getAmount() - amt);
				amt = 0;
				break;
			}

			if (items[i].getAmount() == amt) {
				items[i] = null;
				amt = 0;
				break;
			}

			amt -= items[i].getAmount();
			items[i] = null;
		}

		if (amt == 0) {
			inventory.setContents(items);
			return true;
		}

		return false;
	}

	public static boolean removeFromInventory(EntityEquipment entityEquipment, SpellReagents.ReagentItem item) {
		MagicItemData itemData = item.getMagicItemData();
		if (itemData == null) return false;

		int amt = item.getAmount();
		MagicItemData magicData;
		ItemStack[] armorContents = entityEquipment.getArmorContents();
		ItemStack[] items = new ItemStack[6];
		System.arraycopy(armorContents, 0, items, 0, 4);
		items[4] = entityEquipment.getItemInMainHand();
		items[5] = entityEquipment.getItemInOffHand();

		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) continue;

			magicData = MagicItems.getMagicItemDataFromItemStack(items[i]);
			if (magicData == null || !itemData.matches(magicData)) continue;

			if (items[i].getAmount() > amt) {
				items[i].setAmount(items[i].getAmount() - amt);
				amt = 0;
				break;
			}

			if (items[i].getAmount() == amt) {
				items[i] = null;
				amt = 0;
				break;
			}

			amt -= items[i].getAmount();
			items[i] = null;
		}

		if (amt == 0) {
			ItemStack[] updatedArmorContents = new ItemStack[4];
			System.arraycopy(items, 0, updatedArmorContents, 0, 4);
			entityEquipment.setArmorContents(updatedArmorContents);
			entityEquipment.setItemInMainHand(items[4]);
			entityEquipment.setItemInOffHand(items[5]);
			return true;
		}
		return false;
	}

	public static boolean addToInventory(Inventory inventory, ItemStack item, boolean stackExisting, boolean ignoreMaxStack) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getStorageContents();
		items = Arrays.copyOf(items, items.length);

		if (stackExisting) {
			for (ItemStack itemStack : items) {
				if (itemStack == null || !itemStack.isSimilar(item)) continue;

				if (itemStack.getAmount() + amt <= itemStack.getMaxStackSize()) {
					itemStack.setAmount(itemStack.getAmount() + amt);
					amt = 0;
					break;
				}

				int diff = itemStack.getMaxStackSize() - itemStack.getAmount();
				itemStack.setAmount(itemStack.getMaxStackSize());
				amt -= diff;
			}
		}

		if (amt > 0) {
			for (int i = 0; i < items.length; i++) {
				if (items[i] != null) continue;
				if (amt > item.getMaxStackSize() && !ignoreMaxStack) {
					items[i] = item.clone();
					items[i].setAmount(item.getMaxStackSize());
					amt -= item.getMaxStackSize();
					continue;
				}

				items[i] = item.clone();
				items[i].setAmount(amt);
				amt = 0;
				break;
			}
		}

		if (amt == 0) {
			inventory.setStorageContents(items);
			return true;
		}

		return false;
	}

	public static void rotateVector(Vector v, double degrees) {
		double rad = AccurateMath.toRadians(degrees);
		double sin = AccurateMath.sin(rad);
		double cos = AccurateMath.cos(rad);

		double x = (v.getX() * cos) - (v.getZ() * sin);
		double z = (v.getX() * sin) + (v.getZ() * cos);

		v.setX(x);
		v.setZ(z);
	}

	public static Location applyRelativeOffset(Location loc, Vector relativeOffset) {
		return loc.add(rotateVector(relativeOffset, loc));
	}

	public static Vector rotateVector(Vector v, Location location) {
		return rotateVector(v, location.getYaw(), location.getPitch());
	}

	public static Vector rotateVector(Vector v, float yawDegrees, float pitchDegrees) {
		double yaw = AccurateMath.toRadians(-1.0F * (yawDegrees + 90.0F));
		double pitch = AccurateMath.toRadians(-pitchDegrees);
		double cosYaw = AccurateMath.cos(yaw);
		double cosPitch = AccurateMath.cos(pitch);
		double sinYaw = AccurateMath.sin(yaw);
		double sinPitch = AccurateMath.sin(pitch);
		double initialX = v.getX();
		double initialY = v.getY();
		double x = initialX * cosPitch - initialY * sinPitch;
		double y = initialX * sinPitch + initialY * cosPitch;
		double initialZ = v.getZ();
		double z = initialZ * cosYaw - x * sinYaw;
		x = initialZ * sinYaw + x * cosYaw;
		return new Vector(x, y, z);
	}

	public static Vector getDirection(double yaw, double pitch) {
		return getDirection(new Vector(), yaw, pitch);
	}

	public static Vector getDirection(Vector vector, double yaw, double pitch) {
		yaw = AccurateMath.toRadians(yaw);
		pitch = AccurateMath.toRadians(pitch);

		vector.setY(-Math.sin(pitch));

		double xz = Math.cos(pitch);
		vector.setX(-xz * Math.sin(yaw));
		vector.setZ(xz * Math.cos(yaw));

		return vector;
	}

	public static Location applyAbsoluteOffset(Location loc, Vector offset) {
		return loc.add(offset);
	}

	public static Location applyOffsets(Location loc, Vector relativeOffset, Vector absoluteOffset) {
		return applyAbsoluteOffset(applyRelativeOffset(loc, relativeOffset), absoluteOffset);
	}

	public static Location faceTarget(Location origin, Location target) {
		return origin.setDirection(getVectorToTarget(origin, target));
	}

	public static Vector getVectorToTarget(Location origin, Location target) {
		return target.toVector().subtract(origin.toVector());
	}

	public static boolean downloadFile(String url, File file) {
		try {
			URL website = URI.create(url).toURL();
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(file);

			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
			rbc.close();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static final Map<String, String> uniqueIds = new HashMap<>();

	public static String getUniqueId(Player player) {
		String uid = player.getUniqueId().toString().replace("-", "");
		uniqueIds.put(player.getName(), uid);
		return uid;
	}

	public static String getUniqueId(String playerName) {
		if (uniqueIds.containsKey(playerName)) return uniqueIds.get(playerName);
		Player player = Bukkit.getPlayerExact(playerName);
		if (player != null) return getUniqueId(player);
		return null;
	}

	public static String flattenLineBreaks(String raw) {
		return raw.replaceAll("\n", "\\\\n");
	}

	public static <T> boolean containsParallel(Collection<T> elements, Predicate<? super T> predicate) {
		return elements.parallelStream().anyMatch(predicate);
	}

	public static <T> boolean containsValueParallel(Map<?, T> map, Predicate<? super T> predicate) {
		return containsParallel(map.values(), predicate);
	}

	public static void forEachPlayerOnline(Consumer<? super Player> consumer) {
		Bukkit.getOnlinePlayers().forEach(consumer);
	}

	public static void forEachPassenger(Entity entity, Consumer<Entity> consumer) {
		for (Entity passenger : entity.getPassengers()) {
			forEachPassenger(passenger, consumer);
			consumer.accept(passenger);
		}
	}

	public static <C extends Collection<Material>> C getMaterialList(List<String> strings, Supplier<C> supplier) {
		C ret = supplier.get();
		strings.forEach(string -> ret.add(getMaterial(string)));
		return ret;
	}

	public static <E extends Enum<E>> E enumValueSafe(Class<E> clazz, String name) {
		try {
			return Enum.valueOf(clazz, name);
		} catch (Exception e) {
			return null;
		}
	}

	public static void addExperience(Player player, int amount) {
		player.setExperienceLevelAndProgress(Math.max(player.calculateTotalExperiencePoints() + amount, 0));
	}

	public static double getMaxHealth(LivingEntity entity) {
		return entity.getAttribute(Attribute.MAX_HEALTH).getValue();
	}

	public static void setMaxHealth(LivingEntity entity, double maxHealth) {
		entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
	}

	public static Vector makeFinite(Vector vector) {
		double x = vector.getX();
		double y = vector.getY();
		double z = vector.getZ();

		if (Double.isNaN(x)) x = 0.0D;
		if (Double.isNaN(y)) y = 0.0D;
		if (Double.isNaN(z)) z = 0.0D;

		if (Double.isInfinite(x)) {
			boolean negative = (x < 0.0D);
			x = negative ? -1 : 1;
		}

		if (Double.isInfinite(y)) {
			boolean negative = (y < 0.0D);
			y = negative ? -1 : 1;
		}

		if (Double.isInfinite(z)) {
			boolean negative = (z < 0.0D);
			z = negative ? -1 : 1;
		}

		return new Vector(x, y, z);
	}

	public static Location makeFinite(Location location) {
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();

		float yaw = location.getYaw();
		float pitch = location.getPitch();

		if (Float.isNaN(yaw)) yaw = 0.0F;
		if (Float.isNaN(pitch)) pitch = 0.0F;

		if (Float.isInfinite(yaw)) {
			boolean negative = (yaw < 0.0F);
			yaw = negative ? -1F : 1F;
		}

		if (Float.isInfinite(pitch)) {
			boolean negative = (pitch < 0.0F);
			pitch = negative ? -1F : 1F;
		}

		if (Double.isNaN(x)) x = 0.0D;
		if (Double.isNaN(y)) y = 0.0D;
		if (Double.isNaN(z)) z = 0.0D;

		if (Double.isInfinite(x)) {
			boolean negative = (x < 0.0D);
			x = negative ? -1 : 1;
		}

		if (Double.isInfinite(y)) {
			boolean negative = (y < 0.0D);
			y = negative ? -1 : 1;
		}

		if (Double.isInfinite(z)) {
			boolean negative = (z < 0.0D);
			z = negative ? -1 : 1;
		}

		return new Location(location.getWorld(), x, y, z, yaw, pitch);
	}

	public static boolean checkPluginsEnabled(String[] plugins) {
		boolean all = true;
		for (String plugin : plugins) {
			if (Bukkit.getPluginManager().isPluginEnabled(plugin)) continue;
			MagicSpells.error("Plugin '" + plugin + "' is not enabled.");
			all = false;
		}
		return all;
	}

	public static Component getLegacyFromString(String input) {
		if (input.isEmpty()) return Component.text("");
		Component component = LegacyComponentSerializer.builder()
				.hexColors()
				.useUnusualXRepeatedCharacterHexFormat()
				.character(ChatColor.COLOR_CHAR)
				.build()
				.deserialize(colorize(input));
		return component.decoration(TextDecoration.ITALIC, component.hasDecoration(TextDecoration.ITALIC));
	}

	public static String getMiniMessageFromLegacy(String input) {
		StringBuilder builder = new StringBuilder();

		Matcher matcher = WEIRD_HEX_PATTERN.matcher(input);
		while (matcher.find()) {
			matcher.appendReplacement(builder, "<#" + matcher.group(1).replaceAll("[§&]", "") + ">");
		}
		matcher.appendTail(builder);

		matcher = HEX_PATTERN.matcher(builder.toString());
		builder.setLength(0);
		while (matcher.find()) {
			matcher.appendReplacement(builder, "<" + matcher.group(1) + ">");
		}
		matcher.appendTail(builder);

		matcher = COLOR_PATTERN.matcher(builder.toString());
		builder.setLength(0);
		while (matcher.find()) {
			ChatColor color = ChatColor.getByChar(matcher.group(1).toLowerCase());
			if (color == null) continue;

			matcher.appendReplacement(builder, color == ChatColor.UNDERLINE ? "<underlined>" : "<" + color.asBungee().getName() + ">");
		}
		matcher.appendTail(builder);

		return builder.toString();
	}

	public static String getLegacyFromComponent(Component component) {
		if (component == null) return "";
		return LegacyComponentSerializer.legacySection().serialize(component);
	}

	public static String getLegacyFromMiniMessage(String input) {
		if (input.isEmpty()) return "";
		return getLegacyFromComponent(getMiniMessage(input));
	}

	public static String getPlainString(Component component) {
		if (component == null) return "";

		// Return plain text component
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	public static Component getPlainComponent(Component input) {
		if (input == null) return Component.empty();

		// Serialize a plain text component and create a new component of the string.
		return Component.text(PlainTextComponentSerializer.plainText().serialize(input));
	}

	public static Component getPlainComponentFromString(String input) {
		if (input.isEmpty()) return Component.empty();

		// Convert legacy color patterns to MiniMessage format, then deserialize it.
		Component component = MiniMessage.miniMessage().deserialize(getMiniMessageFromLegacy(input));

		// Convert mini message component to a plain string and deserialize string to plain component.
		return PlainTextComponentSerializer.plainText().deserialize(PlainTextComponentSerializer.plainText().serialize(component));
	}

	public static Component getSmoothComponent(Component component) {
		return Component.empty().style(Style.style()
				.decoration(TextDecoration.BOLD, false)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.STRIKETHROUGH, false)
				.decoration(TextDecoration.OBFUSCATED, false)
				.decoration(TextDecoration.UNDERLINED, false)
				.build()).append(component).compact();
	}

	public static Component getMiniMessage(String input) {
		if (input == null) return null;
		if (input.isEmpty()) return Component.empty();

		// Convert legacy color patterns to MiniMessage format, then deserialize it.
		Component component = MiniMessage.miniMessage().deserialize(getMiniMessageFromLegacy(input));

		// Remove italics if they aren't present. Otherwise, item name and lore will render italic text.
		return component.decoration(TextDecoration.ITALIC, component.hasDecoration(TextDecoration.ITALIC));
	}

	public static Component getMiniMessage(String input, SpellData data) {
		return getMiniMessage(MagicSpells.doReplacements(input, data.caster(), data));

	}

	public static Component getMiniMessage(String input, SpellData data, String... replacements) {
		return getMiniMessage(MagicSpells.doReplacements(input, data.caster(), data, replacements));

	}

	public static Component getMiniMessage(String input, LivingEntity recipient, SpellData data) {
		return getMiniMessage(MagicSpells.doReplacements(input, recipient, data));
	}

	public static Component getMiniMessageWithVars(Player player, String input) {
		if (input.isEmpty()) return Component.text("");
		return getMiniMessage(MagicSpells.doReplacements(input, player, SpellData.NULL));
	}

	public static Component getMiniMessageWithArgsAndVars(Player player, String input, String[] args) {
		if (input.isEmpty()) return Component.text("");
		return getMiniMessage(MagicSpells.doReplacements(input, player, SpellData.NULL.args(args)));
	}

	public static String getStringFromComponent(Component component) {
		return component == null ? "" : MiniMessage.miniMessage().serialize(component);
	}

	public static String getStrictStringFromComponent(Component component) {
		return component == null ? "" : STRICT_SERIALIZER.serialize(component);
	}

	public static String colorize(String string) {
		if (string.isEmpty()) return "";

		Matcher matcher = ColorUtil.HEX_PATTERN.matcher(ChatColor.translateAlternateColorCodes('&', string));
		StringBuilder builder = new StringBuilder();
		while (matcher.find()) {
			String code = matcher.group(1);

			StringBuilder magic = new StringBuilder();
			magic.append(ChatColor.COLOR_CHAR).append('x');
			for (int i = 1; i < code.length(); i++) magic.append(ChatColor.COLOR_CHAR).append(code.charAt(i));

			matcher.appendReplacement(builder, magic.toString());
		}

		return matcher.appendTail(builder).toString();
	}

	public static void setSkin(Player player, String skin, String signature) {
		PlayerProfile profile = player.getPlayerProfile();
		profile.setProperty(new ProfileProperty("textures", skin, signature));
		player.setPlayerProfile(profile);
	}

	@Nullable
	public static Entity getNearestEntity(Entity entity, double range, @Nullable Predicate<Entity> predicate) {
		Entity nearestEntity = null;
		double nearestDistance = range * range;
		double distance;

		for (Entity nextEntity : entity.getNearbyEntities(range, range, range)) {
			if (predicate != null && !predicate.test(nextEntity)) {
				continue;
			}

			distance = entity.getLocation().distanceSquared(nextEntity.getLocation());

			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestEntity = nextEntity;
			}
		}
		return nearestEntity;
	}

	public static boolean hasCollisionsIn(@NotNull World world, @NotNull BoundingBox box, boolean ignorePassableBlocks, @NotNull FluidCollisionMode fluidCollisionMode, @NotNull Predicate<Block> predicate) {
		int minX = (int) Math.floor(box.getMinX() - 1.0E-7D) - 1;
		int minY = (int) Math.floor(box.getMinY() - 1.0E-7D) - 1;
		int minZ = (int) Math.floor(box.getMinZ() - 1.0E-7D) - 1;
		int maxX = (int) Math.floor(box.getMaxX() + 1.0E-7D) + 1;
		int maxY = (int) Math.floor(box.getMaxY() + 1.0E-7D) + 1;
		int maxZ = (int) Math.floor(box.getMaxZ() + 1.0E-7D) + 1;

		BoundingBox fluidShape = null;
		Location location = null;

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					if ((x == minX || x == maxX) && (y == minY || y == maxY) && (z == minZ || z == maxZ))
						continue;

					Block block = world.getBlockAt(x, y, z);

					Material type = block.getType();
					if (type.isAir() || !predicate.test(block)) continue;

					VoxelShape shape = block.getCollisionShape();

					Collection<BoundingBox> boxes = shape.getBoundingBoxes();
					if (!boxes.isEmpty()) {
						for (BoundingBox boundingBox : boxes) {
							boundingBox.shift(x, y, z);
							if (boundingBox.overlaps(box)) return true;
						}
					} else if (!ignorePassableBlocks) {
						BoundingBox boundingBox = block.getBoundingBox();
						if (boundingBox.overlaps(box)) return true;
					}

					if (fluidCollisionMode == FluidCollisionMode.NEVER) continue;

					FluidData data = world.getFluidData(x, y, z);
					if (data.getFluidType() == Fluid.EMPTY || !data.isSource() && fluidCollisionMode == FluidCollisionMode.SOURCE_ONLY)
						continue;

					if (location != null) location.set(x, y, z);
					else {
						location = new Location(world, x, y, z);
						fluidShape = new BoundingBox();
					}

					float height = data.computeHeight(location);
					fluidShape.resize(x, y, z, x + 1, y + height, z + 1);

					if (fluidShape.overlaps(box)) return true;
				}
			}
		}

		return false;
	}

	public static List<BoundingBox> getCollidingBoxes(@NotNull World world, @NotNull BoundingBox box, boolean ignorePassableBlocks, @NotNull FluidCollisionMode fluidCollisionMode, @NotNull Predicate<Block> predicate) {
		List<BoundingBox> collidingBoxes = new ArrayList<>();

		int minX = (int) Math.floor(box.getMinX() - 1.0E-7D) - 1;
		int minY = (int) Math.floor(box.getMinY() - 1.0E-7D) - 1;
		int minZ = (int) Math.floor(box.getMinZ() - 1.0E-7D) - 1;
		int maxX = (int) Math.floor(box.getMaxX() + 1.0E-7D) + 1;
		int maxY = (int) Math.floor(box.getMaxY() + 1.0E-7D) + 1;
		int maxZ = (int) Math.floor(box.getMaxZ() + 1.0E-7D) + 1;

		BoundingBox fluidShape = null;
		Location location = null;

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					if ((x == minX || x == maxX) && (y == minY || y == maxY) && (z == minZ || z == maxZ))
						continue;

					Block block = world.getBlockAt(x, y, z);

					Material type = block.getType();
					if (type.isAir() || !predicate.test(block)) continue;

					VoxelShape shape = block.getCollisionShape();

					Collection<BoundingBox> boxes = shape.getBoundingBoxes();
					if (!boxes.isEmpty()) {
						for (BoundingBox boundingBox : boxes) {
							boundingBox.shift(x, y, z);
							if (boundingBox.overlaps(box)) collidingBoxes.add(boundingBox);
						}
					} else if (!ignorePassableBlocks) {
						BoundingBox boundingBox = block.getBoundingBox();
						if (boundingBox.overlaps(box)) collidingBoxes.add(boundingBox);
					}

					if (fluidCollisionMode == FluidCollisionMode.NEVER) continue;

					FluidData data = world.getFluidData(x, y, z);
					if (data.getFluidType() == Fluid.EMPTY || !data.isSource() && fluidCollisionMode == FluidCollisionMode.SOURCE_ONLY)
						continue;

					if (location != null) location.set(x, y, z);
					else {
						location = new Location(world, x, y, z);
						fluidShape = new BoundingBox();
					}

					float height = data.computeHeight(location);
					fluidShape.resize(x, y, z, x + 1, y + height, z + 1);

					if (fluidShape.overlaps(box)) collidingBoxes.add(fluidShape);
				}
			}
		}

		return collidingBoxes;
	}

}
