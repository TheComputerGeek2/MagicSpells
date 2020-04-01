package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.Map;
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

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class DisarmJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private Map<Item, UUID> disarmedItems;
	private Set<Material> disarmable;

	private boolean dontDrop;
	private boolean preventTheft;

	private int disarmDuration;

	private String strInvalidItem;
	
	public DisarmJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		List<String> disarmableMaterials = getConfigStringList("disarmable-items", null);
		if (disarmableMaterials != null && !disarmableMaterials.isEmpty()) {
			disarmable = new HashSet<>();
			for (String itemName : disarmableMaterials) {
				ItemStack item = Util.getItemStackFromString(itemName);
				if (item != null) disarmable.add(item.getType());
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
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			
			LivingEntity realTarget = target.getTarget();
			
			boolean disarmed = disarm(realTarget);
			if (!disarmed) return noTarget(livingEntity, strInvalidItem);

			playJutsuEffects(livingEntity, realTarget);
			sendMessages(livingEntity, realTarget);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		boolean disarmed =  disarm(target);
		if (disarmed) playJutsuEffects(caster, target);
		return disarmed;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		boolean disarmed = disarm(target);
		if (disarmed) playJutsuEffects(EffectPosition.TARGET, target);
		return disarmed;
	}
	
	private boolean disarm(LivingEntity target) {
		final ItemStack inHand = getItemInHand(target);
		if (disarmable != null && !disarmable.contains(inHand.getType())) return false;
		if (!dontDrop) {
			setItemInHand(target, null);
			Item item = target.getWorld().dropItemNaturally(target.getLocation(), inHand.clone());
			item.setPickupDelay(disarmDuration);
			if (preventTheft && target instanceof Player) disarmedItems.put(item, target.getUniqueId());
			return true;
		}

		setItemInHand(target, null);
		MagicJutsus.scheduleDelayedTask(() -> {
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
