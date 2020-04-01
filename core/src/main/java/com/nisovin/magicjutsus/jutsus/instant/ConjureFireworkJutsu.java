package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class ConjureFireworkJutsu extends InstantJutsu implements TargetedLocationJutsu {

	private static final Pattern COLORS_PATTERN = Pattern.compile("^[A-Fa-f0-9]{6}(,[A-Fa-f0-9]{6})*$");

	private int count;
	private int flight;
	private int pickupDelay;

	private boolean gravity;
	private boolean addToInventory;

	private String fireworkName;

	private ItemStack firework;

	public ConjureFireworkJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		count = getConfigInt("count", 1);
		flight = getConfigInt("flight", 2);
		pickupDelay = getConfigInt("pickup-delay", 0);

		gravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", true);

		fireworkName = getConfigString("firework-name", "");

		firework = new ItemStack(Material.FIREWORK_ROCKET, count);
		FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
		
		meta.setPower(flight);
		if (!fireworkName.isEmpty()) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', fireworkName));
		
		List<String> fireworkEffects = getConfigStringList("firework-effects", null);
		if (fireworkEffects != null && !fireworkEffects.isEmpty()) {
			for (String e : fireworkEffects) {
				FireworkEffect.Type type = Type.BALL;
				boolean trail = false;
				boolean twinkle = false;
				int[] colors = null;
				int[] fadeColors = null;
				
				String[] data = e.split(" ");
				for (String s : data) {
					if (s.equalsIgnoreCase("ball") || s.equalsIgnoreCase("smallball")) {
						type = Type.BALL;
					} else if (s.equalsIgnoreCase("largeball")) {
						type = Type.BALL_LARGE;
					} else if (s.equalsIgnoreCase("star")) {
						type = Type.STAR;
					} else if (s.equalsIgnoreCase("burst")) {
						type = Type.BURST;
					} else if (s.equalsIgnoreCase("creeper")) {
						type = Type.CREEPER;
					} else if (s.equalsIgnoreCase("trail")) {
						trail = true;
					} else if (s.equalsIgnoreCase("twinkle") || s.equalsIgnoreCase("flicker")) {
						twinkle = true;
					} else if (RegexUtil.matches(COLORS_PATTERN, s)) {
						String[] scolors = s.split(",");
						int[] icolors = new int[scolors.length];
						for (int i = 0; i < scolors.length; i++) {
							icolors[i] = Integer.parseInt(scolors[i], 16);
						}

						if (colors == null) colors = icolors;
						else if (fadeColors == null) fadeColors = icolors;
					}
				}

				FireworkEffect.Builder builder = FireworkEffect.builder();
				builder.with(type);
				builder.trail(trail);
				builder.flicker(twinkle);
				if (colors != null) {
					for (int color : colors) {
						builder.withColor(Color.fromRGB(color));
					}
				}
				if (fadeColors != null) {
					for (int fadeColor : fadeColors) {
						builder.withColor(Color.fromRGB(fadeColor));
					}
				}
				meta.addEffect(builder.build());
			}
		}
		
		firework.setItemMeta(meta);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			boolean added = false;
			ItemStack item = firework.clone();
			if (addToInventory) added = Util.addToInventory(player.getInventory(), item, true, false);
			if (!added) {
				Item dropped = player.getWorld().dropItem(player.getLocation(), item);
				dropped.setItemStack(item);
				dropped.setPickupDelay(pickupDelay);
				dropped.setGravity(gravity);
				playJutsuEffects(EffectPosition.SPECIAL, dropped);
			}
			playJutsuEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		playJutsuEffects(EffectPosition.CASTER, caster);
		return castAtLocation(target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		ItemStack item = firework.clone();
		Item dropped = target.getWorld().dropItem(target, item);
		dropped.setItemStack(item);
		dropped.setPickupDelay(pickupDelay);
		dropped.setGravity(gravity);
		playJutsuEffects(EffectPosition.SPECIAL, dropped);
		return true;
	}

}
