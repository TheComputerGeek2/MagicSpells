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
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityPickupItemEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public class DisarmSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final Map<Item, UUID> disarmedItems = new HashMap<>();
	private static PickupListener pickupListener;

	private Set<MagicItemData> disarmable;

	private final ConfigData<Boolean> dontDrop;
	private final ConfigData<Boolean> preventTheft;

	private final ConfigData<Integer> disarmDuration;

	private final String strInvalidItem;

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

		dontDrop = getConfigDataBoolean("dont-drop", false);
		preventTheft = getConfigDataBoolean("prevent-theft", true);

		disarmDuration = getConfigDataInt("disarm-duration", 100);

		strInvalidItem = getConfigString("str-invalid-item", "Your target could not be disarmed.");
	}

	@Override
	protected void initialize() {
		super.initialize();

		if (pickupListener == null) {
			pickupListener = new PickupListener();
			registerEvents(pickupListener);
		}
	}

	@Override
	protected void turnOff() {
		pickupListener = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		LivingEntity target = data.target();

		ItemStack inHand = getItemInHand(target);
		if (inHand == null) return noTarget(strInvalidItem, data);

		if (disarmable != null) {
			MagicItemData itemData = MagicItems.getMagicItemDataFromItemStack(inHand);
			if (itemData == null || !contains(itemData)) return noTarget(strInvalidItem, data);
		}

		int disarmDuration = this.disarmDuration.get(data);
		if (!dontDrop.get(data)) {
			setItemInHand(target, null);

			Item item = target.getWorld().dropItemNaturally(target.getLocation(), inHand.clone());
			item.setPickupDelay(disarmDuration);
			if (preventTheft.get(data)) disarmedItems.put(item, target.getUniqueId());

			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		setItemInHand(target, null);
		MagicSpells.scheduleDelayedTask(() -> {
			ItemStack inHand2 = getItemInHand(target);
			if (inHand2 == null || inHand2.getType() == Material.AIR) {
				setItemInHand(target, inHand);
			} else if (target instanceof Player player) {
				int slot = player.getInventory().firstEmpty();
				if (slot >= 0) player.getInventory().setItem(slot, inHand);
				else {
					Item item = target.getWorld().dropItem(target.getLocation(), inHand);
					item.setPickupDelay(0);

					if (preventTheft.get(data)) disarmedItems.put(item, target.getUniqueId());
				}
			}
		}, disarmDuration, target);

		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
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

	private static class PickupListener implements Listener {

		public PickupListener() {
			MagicSpells.registerEvents(this);
		}

		@EventHandler(ignoreCancelled = true)
		public void onItemPickup(EntityPickupItemEvent event) {
			Item item = event.getItem();

			UUID uuid = disarmedItems.get(item);
			if (uuid == null) return;

			if (uuid.equals(event.getEntity().getUniqueId())) disarmedItems.remove(item);
			else event.setCancelled(true);
		}

	}

}
