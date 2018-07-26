package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.regex.Pattern;

import com.nisovin.magicspells.util.RegexUtil;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

// The special position plays on the dropped items
public class ConjureFireworkSpell extends InstantSpell implements TargetedLocationSpell {

	private static final Pattern COLORS_PATTERN = Pattern.compile("^[A-Fa-f0-9]{6}(,[A-Fa-f0-9]{6})*$");
	
	boolean addToInventory;
	ItemStack firework;
	private boolean itemHasGravity;
	private int pickupDelay;
	
	public ConjureFireworkSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		addToInventory = getConfigBoolean("add-to-inventory", true);
		pickupDelay = getConfigInt("pickup-delay", 0);
		pickupDelay = Math.max(pickupDelay, 0);
		itemHasGravity = getConfigBoolean("gravity", true);
		firework = new ItemStack(Material.FIREWORK_ROCKET, getConfigInt("count", 1));
		FireworkMeta meta = (FireworkMeta)firework.getItemMeta();
		
		meta.setPower(getConfigInt("flight", 2));
		String fireworkName = getConfigString("firework-name", "");
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
						if (colors == null) {
							colors = icolors;
						} else if (fadeColors == null) {
							fadeColors = icolors;
						}
					}
				}

				FireworkEffect.Builder builder = FireworkEffect.builder();
				builder.with(type);
				builder.trail(trail);
				builder.flicker(twinkle);
				if (colors != null) {
					for (int i = 0; i < colors.length; i++) {
						builder.withColor(Color.fromRGB(colors[i]));
					}
				}
				if (fadeColors != null) {
					for (int i = 0; i < fadeColors.length; i++) {
						builder.withColor(Color.fromRGB(fadeColors[i]));
					}
				}
				meta.addEffect(builder.build());
			}
		}
		
		firework.setItemMeta(meta);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			boolean added = false;
			ItemStack item = firework.clone();
			if (addToInventory) added = Util.addToInventory(player.getInventory(), item, true, false);
			if (!added) {
				Item dropped = player.getWorld().dropItem(player.getLocation(), item);
				dropped.setItemStack(item);
				dropped.setPickupDelay(pickupDelay);
				MagicSpells.getVolatileCodeHandler().setGravity(dropped, itemHasGravity);
				playSpellEffects(EffectPosition.SPECIAL, dropped);
				//player.getWorld().dropItem(player.getLocation(), item).setItemStack(item);
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		playSpellEffects(EffectPosition.CASTER, caster);
		return castAtLocation(target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		ItemStack item = firework.clone();
		Item dropped = target.getWorld().dropItem(target, item);
		dropped.setItemStack(item);
		dropped.setPickupDelay(pickupDelay);
		MagicSpells.getVolatileCodeHandler().setGravity(dropped, itemHasGravity);
		playSpellEffects(EffectPosition.SPECIAL, dropped);
		//target.getWorld().dropItem(target, item).setItemStack(item);
		return true;
	}

}
