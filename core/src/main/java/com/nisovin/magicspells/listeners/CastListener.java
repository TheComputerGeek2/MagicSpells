package com.nisovin.magicspells.listeners;

import java.util.Map;
import java.util.Set;
import java.util.EnumSet;
import java.util.HashMap;

import org.bukkit.Tag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BowSpell;

public class CastListener implements Listener {

	private final Map<String, Long> noCastUntil;

	private final Set<Material> interactable;

	public CastListener() {
		noCastUntil = new HashMap<>();

		interactable = EnumSet.of(Material.BARREL, Material.BEACON, Material.BLAST_FURNACE, Material.BREWING_STAND,
			Material.CARTOGRAPHY_TABLE, Material.CHEST, Material.COMPARATOR, Material.CRAFTING_TABLE,
			Material.DAYLIGHT_DETECTOR, Material.DISPENSER, Material.DROPPER, Material.ENCHANTING_TABLE,
			Material.ENDER_CHEST, Material.FURNACE, Material.GRINDSTONE, Material.HOPPER, Material.LEVER, Material.LOOM,
			Material.NOTE_BLOCK, Material.POLISHED_BLACKSTONE_BUTTON, Material.REPEATER, Material.SMITHING_TABLE,
			Material.SMOKER, Material.STONECUTTER, Material.TRAPPED_CHEST
		);

		interactable.addAll(Tag.ANVIL.getValues());
		interactable.addAll(Tag.BEDS.getValues());
		interactable.addAll(Tag.BUTTONS.getValues());
		interactable.addAll(Tag.DOORS.getValues());
		interactable.addAll(Tag.TRAPDOORS.getValues());
		interactable.addAll(Tag.FENCE_GATES.getValues());
		interactable.addAll(Tag.SHULKER_BOXES.getValues());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action == Action.PHYSICAL) return;

		Player player = event.getPlayer();

		if (action == Action.RIGHT_CLICK_BLOCK) {
			Material type = event.getClickedBlock().getType();

			// Force exp bar back to show exp when trying to enchant
			if (type == Material.ENCHANTING_TABLE)
				MagicSpells.getExpBarManager().update(player, player.getLevel(), player.getExp());

			if (event.isBlockInHand() || interactable.contains(type)) {
				// Special block -- don't do normal interactions
				noCastUntil.put(event.getPlayer().getName(), System.currentTimeMillis() + 150);
				return;
			}
		}

		if (MagicSpells.isCastingOnAnimate() && action.isLeftClick()) return;

		ItemStack item = player.getInventory().getItemInMainHand();
		if (MagicSpells.isRespectingItemCooldowns() && player.hasCooldown(item)) return;

		Material type = item.getType();
		if (!MagicSpells.canCastWithFist() && type.isAir()) return;

		boolean isBow = type == Material.BOW || type == Material.CROSSBOW;
		if (action.isLeftClick() == (MagicSpells.areBowCycleButtonsReversed() && isBow)) {
			if (!MagicSpells.isCyclingSpellsOnOffhandAction() && event.getHand() != EquipmentSlot.HAND) return;
			if (isBow && !MagicSpells.canBowCycleSpellsSneaking() && player.isSneaking()) return;

			cycleSpell(player, item);
		} else castSpell(player, item);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerArmSwing(PlayerArmSwingEvent event) {
		if (!MagicSpells.isCastingOnAnimate()) return;

		Player player = event.getPlayer();

		InventoryType inventoryType = player.getOpenInventory().getType();
		if (inventoryType != InventoryType.CRAFTING && inventoryType != InventoryType.CREATIVE) return;

		ItemStack item = player.getInventory().getItemInMainHand();
		if (MagicSpells.isRespectingItemCooldowns() && player.hasCooldown(item)) return;

		Material type = item.getType();
		if (!MagicSpells.canCastWithFist() && type.isAir()) return;

		if (MagicSpells.areBowCycleButtonsReversed() && (type == Material.BOW || type == Material.CROSSBOW)) {
			if (!MagicSpells.isCyclingSpellsOnOffhandAction() && event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
			if (!MagicSpells.canBowCycleSpellsSneaking() && player.isSneaking()) return;

			cycleSpell(player, item);
		} else castSpell(player, item);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPrePlayerAttack(PrePlayerAttackEntityEvent event) {
		if (MagicSpells.isCastingOnAnimate()) return;

		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();

		Material type = item.getType();
		if (!MagicSpells.canCastWithFist() && type.isAir()) return;

		if (MagicSpells.areBowCycleButtonsReversed() && (type == Material.BOW || type == Material.CROSSBOW)) {
			if (!MagicSpells.canBowCycleSpellsSneaking() && player.isSneaking()) return;

			cycleSpell(player, item);
		} else castSpell(player, item);
	}

	@EventHandler
	public void onPlayerShootBow(EntityShootBowEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		if (event.getHand() == EquipmentSlot.OFF_HAND && !MagicSpells.castBoundBowSpellsFromOffhand()) return;

		ItemStack bow = event.getBow();
		if (bow == null) return;

		Spellbook spellbook = MagicSpells.getSpellbook(player);
		Spell spell = spellbook.getActiveSpell(bow);

		if (spell instanceof BowSpell bowSpell && bowSpell.canCastWithItem() && bowSpell.isBindRequired() && checkGlobalCooldown(player, spell))
			bowSpell.handleBowCast(event);

		event.getProjectile().setMetadata("bow-draw-strength", new FixedMetadataValue(MagicSpells.plugin, event.getForce()));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		int slot = MagicSpells.getSpellIconSlot();
		if (slot < 0 || slot > 8) return;

		Player player = event.getPlayer();
		if (event.getNewSlot() == MagicSpells.getSpellIconSlot()) {
			showIcon(player, MagicSpells.getSpellIconSlot(), null);
			return;
		}

		Spellbook spellbook = MagicSpells.getSpellbook(player);
		Spell spell = spellbook.getActiveSpell(player.getInventory().getItem(event.getNewSlot()));
		if (spell != null) showIcon(player, MagicSpells.getSpellIconSlot(), spell.getSpellIcon());
		else showIcon(player, MagicSpells.getSpellIconSlot(), null);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDrop(PlayerDropItemEvent event) {
		noCastUntil.put(event.getPlayer().getName(), System.currentTimeMillis() + 150);
	}

	private void castSpell(Player player, ItemStack item) {
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		Spell spell = spellbook.getActiveSpell(item);

		castSpell(player, spell);
	}

	private void castSpell(Player player, Spell spell) {
		if (spell == null || !spell.canCastWithItem()) return;
		if (!checkGlobalCooldown(player, spell)) return;

		// Cast spell
		spell.hardCast(new SpellData(player));
	}

	private void cycleSpell(Player player, ItemStack item) {
		Spell spell;
		if (!player.isSneaking()) spell = MagicSpells.getSpellbook(player).nextSpell(item);
		else spell = MagicSpells.getSpellbook(player).prevSpell(item);
		if (spell == null) return;

		MagicSpells.sendMessageAndFormat(player, MagicSpells.getSpellChangeMessage(), "%s", spell.getName());
		if (MagicSpells.getSpellIconSlot() >= 0) showIcon(player, MagicSpells.getSpellIconSlot(), spell.getSpellIcon());
	}

	private boolean checkGlobalCooldown(Player player, Spell spell) {
		if (MagicSpells.getGlobalCooldown() > 0 && !spell.isIgnoringGlobalCooldown()) {
			if (noCastUntil.containsKey(player.getName()) && noCastUntil.get(player.getName()) > System.currentTimeMillis()) return false;
			noCastUntil.put(player.getName(), System.currentTimeMillis() + MagicSpells.getGlobalCooldown());
		}

		return true;
	}

	private void showIcon(Player player, int slot, ItemStack icon) {
		if (icon == null) icon = player.getInventory().getItem(MagicSpells.getSpellIconSlot());
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, icon);
	}

}
