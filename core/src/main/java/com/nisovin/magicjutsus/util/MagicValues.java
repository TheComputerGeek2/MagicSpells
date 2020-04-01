package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

@Deprecated
public class MagicValues {

	public enum Enchantments {
		PROTECTION_ENVIRONMENTAL(0, "prot", "protection"),
		PROTECTION_FIRE(1, "fireprot" , "fireprotection"),
		PROTECTION_FALL(2, "featherfalling"),
		PROTECTION_EXPLOSIONS(3, "blastprotection", "blastprot"),
		PROTECTION_PROJECTILE(4, "projectileprotection", "projectileprot"),
		OXYGEN(5, "respiration"),
		WATER_WORKER(6, "aquaaffinity"),
		THORNS(7),
		DEPTH_STRIDER(8),
		FROST_WALKER(9),
		BINDING_CURSE(10, "binding"),
		DAMAGE_ALL(16, "sharp", "sharpness"),
		DAMAGE_UNDEAD(17, "smite"),
		DAMAGE_ARTHROPODS(18, "bane", "baneofarthropods", "ardmg"),
		KNOCKBACK(19),
		FIRE_ASPECT(20),
		LOOT_BONUS_MOBS(21, "looting"),
		SWEEPING_EDGE(22, "sweeping"),
		DIG_SPEED(32, "efficiency"),
		SILK_TOUCH(33, "silk"),
		DURABILITY(34, "unbreaking"),
		LOOT_BONUS_BLOCKS(35, "fortune"),
		ARROW_DAMAGE(48, "power"),
		ARROW_KNOCKBACK(49, "punch"),
		ARROW_FIRE(50, "flame"),
		ARROW_INFINITE(51, "infinity"),
		LUCK(61, "luckofthesea"),
		LURE(62),
		MENDING(70),
		VANISHING_CURSE(71, "vanishing");

		private String[] names;
		private int id;

		Enchantments(int id, String... labels) {
			this.id = id;
			this.names = labels;
		}
		private static Map<String, Enchantment> namesToType = null;
		private static Map<Enchantment, Integer> enchantToId = null;
		private static boolean initialized = false;

		private static void initialize() {
			if (initialized) return;

			namesToType = new HashMap<>();
			enchantToId = new HashMap<>();

			for (Enchantments pe: Enchantments.values()) {
				Enchantment type = Enchantment.getByName(pe.name());
				if (type == null) continue;

				// deal with the names
				namesToType.put(pe.name().toLowerCase().replace("_", ""), type);
				namesToType.put(pe.id + "", type);
				for (String s: pe.names) {
					namesToType.put(s.toLowerCase().replace("_", ""), type);
				}

				// deal with the enchant to id
				enchantToId.put(type, pe.id);
			}

			initialized = true;
		}

		public static Enchantment getEnchantmentType(String identification) {
			initialize();
			Enchantment enchant = namesToType.get(identification.toLowerCase().replace("_", ""));
			if (enchant != null) return enchant;

			// Also check normal enchant class by identifier so this class doesn't need to be updated every time a new enchant is added
			return Enchantment.getByKey(NamespacedKey.minecraft(identification.toLowerCase()));
		}

		public static int getId(Enchantment enchant) {
			initialize();
			return enchantToId.get(enchant);
		}
	}

}
