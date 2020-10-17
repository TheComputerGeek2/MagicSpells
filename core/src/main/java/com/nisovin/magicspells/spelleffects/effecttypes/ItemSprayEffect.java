package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.Arrays;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.SpellEffect;

public class ItemSprayEffect extends SpellEffect {

	private Material material;
	private String materialName;
	private ItemStack itemStack;

	private int amount;
	private int duration;
	private float force;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		materialName = config.getString("type", "");
		material = Util.getMaterial(materialName);

		amount = config.getInt("amount", 15);
		duration = config.getInt("duration", 10);
		force = (float) config.getDouble("force", 1.0F);

		if (material != null && material.isItem()) {
			itemStack = new ItemStack(material);
			itemStack.setAmount(amount);
		}

		if (material == null) {
			itemStack = null;
			MagicSpells.error("Wrong type defined! '" + materialName + "'");
		}
	}

	@Override
	public Runnable playEffectLocation(Location location) {
		if (itemStack == null) return null;

		Random rand = new Random();
		Location loc = location.clone().add(0, 1, 0);
		final Item[] items = new Item[amount];
		for (int i = 0; i < amount; i++) {
			items[i] = loc.getWorld().dropItem(loc, itemStack);
			items[i].setVelocity(new Vector((rand.nextDouble() - .5) * force, (rand.nextDouble() - .5) * force, (rand.nextDouble() - .5) * force));
			items[i].setPickupDelay(duration << 1);
		}

		MagicSpells.scheduleDelayedTask(() -> Arrays.stream(items).forEach(Item::remove), duration);
		return null;
	}

}
