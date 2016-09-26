package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;
public class CaptureSpell extends TargetedSpell implements TargetedEntitySpell {

	boolean powerAffectsQuantity;
	boolean addToInventory;
	String itemName;
	List<String> itemLore;
	private boolean gravity;
	
	public CaptureSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		powerAffectsQuantity = getConfigBoolean("power-affects-quantity", true);
		addToInventory = getConfigBoolean("add-to-inventory", false);
		gravity = getConfigBoolean("gravity", true);
		itemName = getConfigString("item-name", null);
		itemLore = getConfigStringList("item-lore", null);
		
		if (itemName != null) {
			itemName = ChatColor.translateAlternateColorCodes('&', itemName);
		}
		if (itemLore != null) {
			for (int i = 0; i < itemLore.size(); i++) {
				itemLore.set(i, ChatColor.translateAlternateColorCodes('&', itemLore.get(i)));
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power, getValidTargetChecker());
			if (target == null) {
				return noTarget(player);
			}
			boolean ok = capture(player, target.getTarget(), target.getPower());
			if (!ok) {
				return noTarget(player);
			}
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public ValidTargetChecker getValidTargetChecker() {
		return new ValidTargetChecker() {
			@Override
			public boolean isValidTarget(LivingEntity entity) {
				return (!(entity instanceof Player) && entity.getType().isSpawnable());
			}
		};
	}
	
	boolean capture(Player caster, LivingEntity target, float power) {
		ItemStack item = Util.getEggItemForEntityType(target.getType());
		if (item != null) {
			if (powerAffectsQuantity) {
				int q = Math.round(power);
				if (q > 1) {
					item.setAmount(q);
				}
			}
			String entityName = MagicSpells.getEntityNames().get(target.getType());
			if (itemName != null || itemLore != null) {
				if (entityName == null) entityName = "unknown";
				ItemMeta meta = item.getItemMeta();
				if (itemName != null) {
					meta.setDisplayName(itemName.replace("%name%", entityName));
				}
				if (itemLore != null) {
					List<String> lore = new ArrayList<String>();
					for (String l : itemLore) {
						lore.add(l.replace("%name%", entityName));
					}
					meta.setLore(lore);
				}
				item.setItemMeta(meta);
			}
			target.remove();
			boolean added = false;
			if (addToInventory && caster != null) {
				added = Util.addToInventory(caster.getInventory(), item, true, false);
			}
			if (!added) {
				Item dropped = target.getWorld().dropItem(target.getLocation().add(0, 1, 0), item);
				dropped.setItemStack(item);
				dropped.setGravity(gravity);
				
			}
			if (caster != null) {
				playSpellEffects(caster, target.getLocation());
			} else {
				playSpellEffects(EffectPosition.TARGET, target.getLocation());
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
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

}
