package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.FireworkMeta;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

// REMOVE LATER - REPLACED BY CONJURESPELL
public class ConjureFireworkSpell extends InstantSpell implements TargetedLocationSpell {

	private static final Pattern COLORS_PATTERN = Pattern.compile("^[A-Fa-f0-9]{6}(,[A-Fa-f0-9]{6})*$");

	private ConfigData<Integer> count;
	private ConfigData<Integer> flight;
	private ConfigData<Integer> pickupDelay;

	private boolean gravity;
	private boolean addToInventory;

	private String fireworkName;

	private ItemStack firework;

	public ConjureFireworkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		count = getConfigDataInt("count", 1);
		flight = getConfigDataInt("flight", 2);
		pickupDelay = getConfigDataInt("pickup-delay", 0);

		gravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", true);

		fireworkName = getConfigString("firework-name", "");

		firework = new ItemStack(Material.FIREWORK_ROCKET);
		FireworkMeta meta = (FireworkMeta) firework.getItemMeta();
		
		if (!fireworkName.isEmpty()) meta.displayName(Util.getMiniMessage(fireworkName));
		
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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			boolean added = false;
			ItemStack item = firework.clone();
			item.setAmount(count.get(caster, null, power, args));

			ItemMeta meta = item.getItemMeta();
			if (meta instanceof FireworkMeta fMeta) fMeta.setPower(flight.get(caster, null, power, args));

			if (addToInventory) added = Util.addToInventory(player.getInventory(), item, true, false);
			if (!added) {
				Item dropped = player.getWorld().dropItem(player.getLocation(), item);
				dropped.setItemStack(item);
				dropped.setGravity(gravity);

				int delay = Math.max(pickupDelay.get(caster, null, power, args), 0);
				dropped.setPickupDelay(delay);

				playSpellEffects(EffectPosition.SPECIAL, dropped);
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		playSpellEffects(EffectPosition.CASTER, caster);

		ItemStack item = firework.clone();
		item.setAmount(count.get(caster, null, power, args));

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof FireworkMeta fMeta) fMeta.setPower(flight.get(caster, null, power, args));

		Item dropped = target.getWorld().dropItem(target, item);
		dropped.setItemStack(item);
		dropped.setGravity(gravity);

		int delay = Math.max(pickupDelay.get(caster, null, power, args), 0);
		dropped.setPickupDelay(delay);

		playSpellEffects(EffectPosition.SPECIAL, dropped);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		playSpellEffects(EffectPosition.CASTER, caster);
		return castAtLocation(null, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(null, target, power, args);

	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power, null);
	}

}
