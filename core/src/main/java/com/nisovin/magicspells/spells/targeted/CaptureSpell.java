package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ValidTargetChecker;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class CaptureSpell extends TargetedSpell implements TargetedEntitySpell {

	private String itemName;
	private List<String> itemLore;

	private boolean gravity;
	private boolean addToInventory;
	private boolean powerAffectsQuantity;
	
	public CaptureSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		itemName = getConfigString("item-name", null);
		itemLore = getConfigStringList("item-lore", null);

		gravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", false);
		powerAffectsQuantity = getConfigBoolean("power-affects-quantity", true);

		if (itemName != null) itemName = Util.colorize(itemName);
		if (itemLore != null) {
			for (int i = 0; i < itemLore.size(); i++) {
				itemLore.set(i, Util.colorize(itemLore.get(i)));
			}
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, getValidTargetChecker(), args);
			if (target == null) return noTarget(caster);
			boolean ok = capture(caster, target.getTarget(), target.getPower());
			if (!ok) return noTarget(caster);

			sendMessages(caster, target.getTarget(), args);
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
		ItemStack item = MobUtil.getEggItemForEntityType(target.getType());
		if (item == null) return false;

		if (powerAffectsQuantity) {
			int q = Math.round(power);
			if (q > 1) item.setAmount(q);
		}

		String entityName = MagicSpells.getEntityNames().get(target.getType());
		if (itemName != null || itemLore != null) {
			if (entityName == null) entityName = "unknown";
			ItemMeta meta = item.getItemMeta();
			if (itemName != null) meta.displayName(Util.getMiniMessage(itemName.replace("%name%", entityName)));
			if (itemLore != null) {
				List<Component> lore = new ArrayList<>();
				for (String l : itemLore) lore.add(Util.getMiniMessage(l.replace("%name%", entityName)));
				meta.lore(lore);
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

		if (caster != null) playSpellEffects(caster, target.getLocation());
		else playSpellEffects(EffectPosition.TARGET, target.getLocation());

		return true;
	}

}
