package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

// TODO: REMOVE LATER - REPLACED BY CONJURESPELL
public class ConjureFireworkSpell extends InstantSpell implements TargetedLocationSpell {

	private static final Pattern COLORS_PATTERN = Pattern.compile("^[A-Fa-f0-9]{6}(,[A-Fa-f0-9]{6})*$");

	private final ConfigData<Integer> count;
	private final ConfigData<Integer> flight;
	private final ConfigData<Integer> pickupDelay;

	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> addToInventory;

	private final ItemStack firework;

	public ConjureFireworkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		count = getConfigDataInt("count", 1);
		flight = getConfigDataInt("flight", 2);
		pickupDelay = getConfigDataInt("pickup-delay", 0);

		gravity = getConfigDataBoolean("gravity", true);
		addToInventory = getConfigDataBoolean("add-to-inventory", true);

		String fireworkName = getConfigString("firework-name", "");

		firework = new ItemStack(Material.FIREWORK_ROCKET);
		FireworkMeta meta = (FireworkMeta) firework.getItemMeta();

		if (!fireworkName.isEmpty()) meta.displayName(Util.getItemMiniMessage(fireworkName));

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
					} else if (COLORS_PATTERN.asMatchPredicate().test(s)) {
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
	public CastResult cast(SpellData data) {
		ItemStack firework = this.firework.clone();
		firework.setAmount(count.get(data));
		firework.editMeta(FireworkMeta.class, meta -> meta.setPower(flight.get(data)));

		boolean added = false;
		if (addToInventory.get(data) && data.caster() instanceof Player caster)
			added = Util.addToInventory(caster.getInventory(), firework, true, false);

		if (!added) {
			Item dropped = data.caster().getWorld().dropItem(data.caster().getLocation(), firework, item -> {
				item.setPickupDelay(Math.max(pickupDelay.get(data), 0));
				item.setGravity(gravity.get(data));
			});

			playSpellEffects(EffectPosition.SPECIAL, dropped, data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		ItemStack firework = this.firework.clone();
		firework.setAmount(count.get(data));
		firework.editMeta(FireworkMeta.class, meta -> meta.setPower(flight.get(data)));

		Location location = data.location();

		Item dropped = location.getWorld().dropItem(location, firework);
		dropped.setPickupDelay(Math.max(pickupDelay.get(data), 0));
		dropped.setGravity(gravity.get(data));

		playSpellEffects(data);
		playSpellEffects(EffectPosition.SPECIAL, dropped, data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
