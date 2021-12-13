package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public class DisarmSpell extends TargetedSpell implements TargetedEntitySpell {

	private Set<MagicItemData> disarmable;
	private Map<Item, UUID> disarmedItems;

	private boolean dontDrop;
	private boolean preventTheft;

	private int disarmDuration;

	private String strInvalidItem;
	
	public DisarmSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		List<String> disarmableItems = getConfigStringList("disarmable-items", null);
		if (disarmableItems != null && !disarmableItems.isEmpty()) {
			disarmable = new HashSet<>();

			for (String itemName : disarmableItems) {
				MagicItemData data = MagicItems.getMagicItemDataFromString(itemName);
				if (data != null) disarmable.add(data);
			}
		}

		dontDrop = getConfigBoolean("dont-drop", false);
		preventTheft = getConfigBoolean("prevent-theft", true);

		disarmDuration = getConfigInt("disarm-duration", 100);

		strInvalidItem = getConfigString("str-invalid-item", "Your target could not be disarmed.");
		
		if (dontDrop) preventTheft = false;
		if (preventTheft) disarmedItems = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			
			LivingEntity realTarget = target.getTarget();
			
			boolean disarmed = disarm(caster, realTarget, power, args);
			if (!disarmed) return noTarget(caster, strInvalidItem);

			playSpellEffects(caster, realTarget);
			sendMessages(caster, realTarget, args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		boolean disarmed =  disarm(caster, target, power, args);
		if (disarmed) playSpellEffects(caster, target);
		return disarmed;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		boolean disarmed = disarm(null, target, power, args);
		if (disarmed) playSpellEffects(EffectPosition.TARGET, target);
		return disarmed;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	private boolean disarm(LivingEntity caster, LivingEntity target, float power, String[] args) {
		final ItemStack inHand = getItemInHand(target);
		if (inHand == null) return false;

		if (disarmable != null) {
			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(inHand);
			if (itemData == null || !contains(itemData)) return false;
		}

		if (!dontDrop) {
			setItemInHand(target, null);
			Item item = target.getWorld().dropItemNaturally(target.getLocation(), inHand.clone());
			item.setPickupDelay(disarmDuration);
			if (preventTheft && target instanceof Player) disarmedItems.put(item, target.getUniqueId());
			return true;
		}

		setItemInHand(target, null);
		MagicSpells.scheduleDelayedTask(() -> {
			ItemStack inHand2 = getItemInHand(target);
			if (inHand2 == null || inHand2.getType() == Material.AIR) {
				setItemInHand(target, inHand);
			} else if (target instanceof Player) {
				int slot = ((Player) target).getInventory().firstEmpty();
				if (slot >= 0) ((Player) target).getInventory().setItem(slot, inHand);
				else {
					Item item = target.getWorld().dropItem(target.getLocation(), inHand);
					item.setPickupDelay(0);
				}
			}
		}, disarmDuration);

		return true;
	}

	private boolean contains(MagicItemData itemData) {
		for (MagicItemData data : disarmable) {
			if (data.matches(itemData)) return true;
		}
		return false;
	}
	
	private ItemStack getItemInHand(LivingEntity entity) {
		EntityEquipment equip = entity.getEquipment();
		if (equip == null) return null;
		return equip.getItemInMainHand();
	}
	
	private void setItemInHand(LivingEntity entity, ItemStack item) {
		EntityEquipment equip = entity.getEquipment();
		if (equip == null) return;
		equip.setItemInMainHand(item);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (!preventTheft) return;
		
		Item item = event.getItem();
		if (!disarmedItems.containsKey(item)) return;
		if (disarmedItems.get(item).equals(event.getEntity().getUniqueId())) disarmedItems.remove(item);
		else event.setCancelled(true);
	}

}
