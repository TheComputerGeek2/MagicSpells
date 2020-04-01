package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.ValidTargetChecker;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class CaptureJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private String itemName;
	private List<String> itemLore;

	private boolean gravity;
	private boolean addToInventory;
	private boolean powerAffectsQuantity;
	
	public CaptureJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		itemName = getConfigString("item-name", null);
		itemLore = getConfigStringList("item-lore", null);

		gravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", false);
		powerAffectsQuantity = getConfigBoolean("power-affects-quantity", true);

		if (itemName != null) itemName = ChatColor.translateAlternateColorCodes('&', itemName);
		if (itemLore != null) {
			for (int i = 0; i < itemLore.size(); i++) {
				itemLore.set(i, ChatColor.translateAlternateColorCodes('&', itemLore.get(i)));
			}
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power, getValidTargetChecker());
			if (target == null) return noTarget(livingEntity);
			boolean ok = capture(livingEntity, target.getTarget(), target.getPower());
			if (!ok) return noTarget(livingEntity);

			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!target.getType().isSpawnable()) return false;
		if (!validTargetList.canTarget(caster, target)) return false;
		return capture(caster, target, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!target.getType().isSpawnable()) return false;
		if (!validTargetList.canTarget(target)) return false;
		return capture(null, target, power);
	}

	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return (LivingEntity entity) -> !(entity instanceof Player) && entity.getType().isSpawnable();
	}
	
	private boolean capture(LivingEntity caster, LivingEntity target, float power) {
		ItemStack item = Util.getEggItemForEntityType(target.getType());
		if (item == null) return false;

		if (powerAffectsQuantity) {
			int q = Math.round(power);
			if (q > 1) item.setAmount(q);
		}

		String entityName = MagicJutsus.getEntityNames().get(target.getType());
		if (itemName != null || itemLore != null) {
			if (entityName == null) entityName = "unknown";
			ItemMeta meta = item.getItemMeta();
			if (itemName != null) meta.setDisplayName(itemName.replace("%name%", entityName));
			if (itemLore != null) {
				List<String> lore = new ArrayList<>();
				for (String l : itemLore) lore.add(l.replace("%name%", entityName));
				meta.setLore(lore);
			}

			item.setItemMeta(meta);
		}

		target.remove();
		boolean added = false;

		if (addToInventory && caster != null && caster instanceof Player) added = Util.addToInventory(((Player) caster).getInventory(), item, true, false);
		if (!added) {
			Item dropped = target.getWorld().dropItem(target.getLocation().add(0, 1, 0), item);
			dropped.setItemStack(item);
			dropped.setGravity(gravity);
		}

		if (caster != null) playJutsuEffects(caster, target.getLocation());
		else playJutsuEffects(EffectPosition.TARGET, target.getLocation());

		return true;
	}

}
