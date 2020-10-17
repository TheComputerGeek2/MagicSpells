package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.advancement.Advancement;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;

public class AdvancementCondition extends Condition {

	private Set<Advancement> advancements;

	@Override
	public boolean initialize(String var) {
		if (var == null || var.isEmpty()) return false;

		advancements = new HashSet<>();

		// minecraft advancements must start with minecraft:
		String[] adv = var.split(",");
		for (String str : adv) {
			String[] keys = str.split(":");
			Advancement advancement;
			NamespacedKey key;
			if (keys.length >= 2) {
				if (!keys[0].equalsIgnoreCase("minecraft")) continue;
				key = NamespacedKey.minecraft(keys[1].toLowerCase());
				advancement = Bukkit.getAdvancement(key);
				if (advancement == null) continue;

				advancements.add(advancement);
				continue;
			}
			if (keys.length >= 1) {
				key = new NamespacedKey(MagicSpells.getInstance(), keys[0]);
				advancement = Bukkit.getAdvancement(key);
				if (advancement == null) continue;
				advancements.add(advancement);
			}
		}

		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return hasAdvancement(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return hasAdvancement(target);
	}

	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean hasAdvancement(LivingEntity livingEntity) {
		if (!(livingEntity instanceof Player)) return false;
		Player player = (Player) livingEntity;

		for (Advancement advancement : advancements) {
			if (!player.getAdvancementProgress(advancement).isDone()) return false;
		}

		return true;
	}

}
