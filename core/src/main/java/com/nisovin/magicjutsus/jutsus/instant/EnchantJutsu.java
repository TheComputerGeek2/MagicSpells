package com.nisovin.magicjutsus.jutsus.instant;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.handlers.EnchantmentHandler;

public class EnchantJutsu extends InstantJutsu {
	
	private Map<Enchantment, Integer> enchantments;

	private boolean safeEnchants;
	
	public EnchantJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		enchantments = new HashMap<>();

		List<String> enchantmentList = getConfigStringList("enchantments", null);

		safeEnchants = getConfigBoolean("safe-enchants", true);

		if (enchantmentList != null && !enchantmentList.isEmpty()) {
			for (String string : enchantmentList) {
				Enchantment enchant = null;
				int level = 1;
				String[] str = string.split(" ");
				if (str[0] != null) enchant = EnchantmentHandler.getEnchantment(str[0]);
				if (str.length > 1 && str[1] != null) level = Integer.parseInt(str[1]);
				if (enchant != null) enchantments.put(enchant, level);
			}
		} else MagicJutsus.error("EnchantJutsu '" + internalName + "' has invalid enchantments defined!");
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		ItemStack targetItem = livingEntity.getEquipment().getItemInMainHand();
		if (targetItem == null) return PostCastAction.ALREADY_HANDLED;
		enchant(targetItem);
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void enchant(ItemStack item) {
		for (Enchantment e : enchantments.keySet()) {
			enchant(item, e, enchantments.get(e));
		}
	}
	
	private void enchant(ItemStack item, Enchantment enchant, int level) {
		if (!enchant.canEnchantItem(item)) return;
		if (safeEnchants && level > enchant.getMaxLevel()) level = enchant.getMaxLevel();
		if (level <= 0) item.removeEnchantment(enchant);
		else {
			if (safeEnchants) item.addEnchantment(enchant, level);
			else item.addUnsafeEnchantment(enchant, level);
		}
	}

}
