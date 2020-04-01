package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;

public class UnconjureJutsu extends InstantJutsu {

	private List<String> itemNames;
	private List<ItemStack> items;
	
	public UnconjureJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		itemNames = getConfigStringList("items", null);
	}
	
	@Override
	public void initialize() {
		items = new ArrayList<>();

		if (itemNames == null) return;
		for (String s : itemNames) {
			ItemStack i = Util.getItemStackFromString(s);
			if (i == null) continue;
			items.add(i);
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			ItemStack[] inv = player.getInventory().getContents();
			if (check(inv)) player.getInventory().setContents(inv);
			ItemStack[] armor = player.getInventory().getArmorContents();
			if (check(armor)) player.getInventory().setArmorContents(armor);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean check(ItemStack[] inv) {
		boolean ch = false;
		for (int i = 0; i < inv.length; i++) {
			if (inv[i] == null) continue;
			for (ItemStack item : items) {
				if (!item.isSimilar(inv[i])) continue;
				inv[i] = null;
				ch = true;
				break;
			}
		}
		return ch;
	}

}
