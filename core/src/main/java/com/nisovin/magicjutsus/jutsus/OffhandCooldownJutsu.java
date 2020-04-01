package com.nisovin.magicjutsus.jutsus;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.MagicConfig;

public class OffhandCooldownJutsu extends InstantJutsu {

	private List<Player> players = new ArrayList<>();

	private ItemStack item;

	private Jutsu jutsuToCheck;
	private String jutsuToCheckName;

	public OffhandCooldownJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		if (isConfigString("item")) item = Util.getItemStackFromString(getConfigString("item", "stone"));
		else if (isConfigSection("item")) item = Util.getItemStackFromConfig(getConfigSection("item"));

		jutsuToCheckName = getConfigString("jutsu", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		jutsuToCheck = MagicJutsus.getJutsuByInternalName(jutsuToCheckName);

		if (jutsuToCheck == null || item == null) return;
		
		MagicJutsus.scheduleRepeatingTask(() -> {
			Iterator<Player> iter = players.iterator();
			while (iter.hasNext()) {
				Player pl = iter.next();
				if (!pl.isValid()) {
					iter.remove();
					continue;
				}
				float cd = jutsuToCheck.getCooldown(pl);
				int amt = 1;
				if (cd > 0) amt = (int) Math.ceil(cd);

				PlayerInventory inventory = pl.getInventory();
				ItemStack off = inventory.getItemInOffHand();
				off.setAmount(amt);
				if (off == null || !off.isSimilar(item)) inventory.setItemInOffHand(item.clone());
			}
		}, TimeUtil.TICKS_PER_SECOND, TimeUtil.TICKS_PER_SECOND);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			players.add((Player) livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
