package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.Set;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.EntityEquipment;

import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.Condition;

public class DurabilityCondition extends Condition {

	private Set<DurabilityChecker> durabilitySet;

	@Override
	public boolean initialize(String var) {
		durabilitySet = new HashSet<>();

		String[] args = var.split(",");
		for (int i = 0; i < args.length; i++) {
			DurabilityChecker durability = new DurabilityChecker();
			args[i] = args[i].toLowerCase();

			if (args[i].startsWith("helmet")) {
				durability.slot = 0;
				if (!durability.determineOperatorAndValue(args[i].substring(6))) continue;
			} else if (args[i].startsWith("helm")) {
				durability.slot = 0;
				if (!durability.determineOperatorAndValue(args[i].substring(5))) continue;
			} else if (args[i].startsWith("chestplate")) {
				durability.slot = 1;
				if (!durability.determineOperatorAndValue(args[i].substring(10))) continue;
			} else if (args[i].startsWith("leggings")) {
				durability.slot = 2;
				if (!durability.determineOperatorAndValue(args[i].substring(8))) continue;
			} else if (args[i].startsWith("boots")) {
				durability.slot = 3;
				if (!durability.determineOperatorAndValue(args[i].substring(5))) continue;
			} else if (args[i].startsWith("offhand")) {
				durability.slot = 4;
				if (!durability.determineOperatorAndValue(args[i].substring(7))) continue;
			} else if (args[i].startsWith("hand")) {
				durability.slot = 5;
				if (!durability.determineOperatorAndValue(args[i].substring(4))) continue;
			} else if (args[i].startsWith("mainhand")) {
				durability.slot = 5;
				if (!durability.determineOperatorAndValue(args[i].substring(8))) continue;
			}

			durabilitySet.add(durability);
		}

		return true;
	}

	@Override
	public boolean check(LivingEntity livingEntity) {
		return checkDurability(livingEntity);
	}

	@Override
	public boolean check(LivingEntity livingEntity, LivingEntity target) {
		return checkDurability(target);
	}
	
	@Override
	public boolean check(LivingEntity livingEntity, Location location) {
		return false;
	}

	private boolean checkDurability(LivingEntity livingEntity) {
		EntityEquipment equipment = livingEntity.getEquipment();
		for (DurabilityChecker d : durabilitySet) {
			ItemStack item = null;
			switch (d.slot) {
				case 0:
					item = equipment.getHelmet();
					break;
				case 1:
					item = equipment.getChestplate();
					break;
				case 2:
					item = equipment.getLeggings();
					break;
				case 3:
					item = equipment.getBoots();
					break;
				case 4:
					item = equipment.getItemInOffHand();
					break;
				case 5:
					item = equipment.getItemInMainHand();
					break;
			}

			if (item == null) return false;
			ItemMeta meta = item.getItemMeta();
			if (meta == null) return false;
			if (!(meta instanceof Damageable)) return false;

			int max = item.getType().getMaxDurability();
			if (max > 0) {
				if (d.equals && max - ((Damageable) meta).getDamage() != d.durability) return false;
				else if (d.moreThan && max - ((Damageable) meta).getDamage() <= d.durability) return false;
				else if (d.lessThan && max - ((Damageable) meta).getDamage() >= d.durability) return false;
			}
		}

		return true;
	}

	private static class DurabilityChecker {

		private int slot;
		private int durability;

		private boolean equals;
		private boolean moreThan;
		private boolean lessThan;

		private DurabilityChecker() {

		}

		private boolean determineOperatorAndValue(String str) {
			switch (str.charAt(0)) {
				case '=':
				case ':':
					equals = true;
					break;
				case '<':
					lessThan = true;
					break;
				case '>':
					moreThan = true;
					break;
				default:
					return false;
			}

			try {
				durability = Integer.parseInt(str.substring(1));
				return true;
			} catch (NumberFormatException e) {
				DebugHandler.debugNumberFormat(e);
				return false;
			}
		}

	}

}
