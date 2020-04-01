package com.nisovin.magicjutsus.util;

import java.util.Map;
import java.util.Arrays;
import java.util.Comparator;

import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;

import com.nisovin.magicjutsus.MagicJutsus;

public class CastItem {

	private static final Material MATERIAL_CAST_ITEM_NOTHING = null;

	private Material material = Material.AIR;
	private int durability = 0;
	private String name = "";
	private int[][] enchants = null;

	public CastItem() {

	}

	public CastItem(Material material) {
		this.material = material;
	}

	public CastItem(Material material, int durability) {
		this.material = material;
		if (MagicJutsus.ignoreCastItemDurability(material)) this.durability = 0;
		else this.durability = durability;
	}

	public CastItem(ItemStack item) {
		if (item == null) {
			material = Material.AIR;
			durability = 0;
		} else {
			material = item.getType();
			if (isCastItemMaterialAirOrNothing(material) || MagicJutsus.ignoreCastItemDurability(material)) durability = 0;
			else durability = Util.getItemDurability(item);

			if (!isCastItemMaterialAirOrNothing(material) && !MagicJutsus.ignoreCastItemNames() && item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();
				if (meta != null && meta.hasDisplayName()) {
					if (MagicJutsus.ignoreCastItemNameColors()) name = ChatColor.stripColor(meta.getDisplayName());
					else name = meta.getDisplayName();
				}
			}

			if (!isCastItemMaterialAirOrNothing(material) && !MagicJutsus.ignoreCastItemEnchants()) enchants = getEnchants(item);
		}
	}

	public CastItem(String string) {
		String s = string;
		if (s.contains("|")) {
			String[] temp = s.split("\\|");
			s = temp[0];
			if (!MagicJutsus.ignoreCastItemNames() && temp.length > 1) {
				if (MagicJutsus.ignoreCastItemNameColors()) name = ChatColor.stripColor(temp[1]);
				else name = temp[1];
			}
		}
		if (s.contains(";")) {
			String[] temp = s.split(";");
			s = temp[0];
			if (!MagicJutsus.ignoreCastItemEnchants()) {
				String[] split = temp[1].split("\\+");
				enchants = new int[split.length][];
				for (int i = 0; i < enchants.length; i++) {
					String[] enchantData = split[i].split("-");
					enchants[i] = new int[] { Integer.parseInt(enchantData[0]), Integer.parseInt(enchantData[1]) };
				}
				sortEnchants(enchants);
			}
		}
		if (s.contains(":")) {
			String[] split = s.split(":");
			material = Material.getMaterial(split[0].toUpperCase());
			if (MagicJutsus.ignoreCastItemDurability(material)) durability = 0;
			else durability = Integer.parseInt(split[1]);
		} else {
			material = Material.getMaterial(s.toUpperCase());
			durability = 0;
		}
	}

	public Material getItemType() {
		return material;
	}

	public boolean equals(CastItem i) {
		if (i == null) return false;
		if (i.material != material) return false;
		if (i.durability != durability) return false;
		if (!(MagicJutsus.ignoreCastItemNames() || i.name.equals(name))) return false;
		return MagicJutsus.ignoreCastItemEnchants() || compareEnchants(enchants, i.enchants);
	}

	public boolean equals(ItemStack i) {
		if (i.getType() != material) return false;
		if (Util.getItemDurability(i) != durability) return false;
		if (!(MagicJutsus.ignoreCastItemNames() || namesEqual(i))) return false;
		return MagicJutsus.ignoreCastItemEnchants() || compareEnchants(enchants, getEnchants(i));
	}

	private boolean namesEqual(ItemStack i) {
		String n = null;
		if (i.hasItemMeta()) {
			ItemMeta meta = i.getItemMeta();
			if (meta.hasDisplayName()) {
				if (MagicJutsus.ignoreCastItemNameColors()) n = ChatColor.stripColor(meta.getDisplayName());
				else n = meta.getDisplayName();
			}
		}
		if (n == null && (name == null || name.isEmpty())) return true;
		if (n == null || name == null) return false;
		return n.equals(name);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CastItem) return equals((CastItem) o);
		if (o instanceof ItemStack) return equals((ItemStack) o);
		return false;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (durability == 0) builder.append(material);
		else {
			builder.append(material);
			builder.append(':');
			builder.append(durability);
		}
		if (enchants != null) {
			builder.append(';');
			for (int i = 0; i < enchants.length; i++) {
				builder.append(enchants[i][0]);
				builder.append('-');
				builder.append(enchants[i][1]);
				if (i < enchants.length - 1) builder.append('+');
			}
		}
		String s = builder.toString();
		if (name != null && !name.isEmpty()) s += '|' + name;
		return s;
	}

	private int[][] getEnchants(ItemStack item) {
		if (item == null) return null;
		Map<Enchantment, Integer> enchantments = item.getEnchantments();
		if (enchantments == null) return null;
		if (enchantments.isEmpty()) return null;
		int[][] enchants = new int[enchantments.size()][];
		int i = 0;
		for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
			enchants[i] = new int[] { MagicValues.Enchantments.getId(entry.getKey()), entry.getValue() };
			i++;
		}
		sortEnchants(enchants);
		return enchants;
	}

	private static void sortEnchants(int[][] enchants) {
		Arrays.sort(enchants, enchantComparator);
	}

	private static final Comparator<int[]> enchantComparator = (int[] o1, int[] o2) -> o1[0] - o2[0];

	private boolean compareEnchants(int[][] o1, int[][] o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null || o2 == null) return false;
		if (o1.length != o2.length) return false;
		for (int i = 0; i < o1.length; i++) {
			if (o1[i][0] != o2[i][0]) return false;
			if (o1[i][1] != o2[i][1]) return false;
		}
		return true;
	}

	private static boolean isCastItemMaterialAirOrNothing(Material material) {
		return BlockUtils.isAir(material) || material == MATERIAL_CAST_ITEM_NOTHING;
	}

}
