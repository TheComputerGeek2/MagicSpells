package com.nisovin.magicjutsus;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerAnimationEvent;

import com.nisovin.magicjutsus.util.BlockUtils;

public class CastListener implements Listener {

	private MagicJutsus plugin;

	private Map<String, Long> noCastUntil;

	CastListener(MagicJutsus plugin) {
		this.plugin = plugin;
		noCastUntil = new HashMap<>();
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();

		// First check if player is interacting with a special block
		boolean noInteract = false;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Material m = event.getClickedBlock().getType();
			if (BlockUtils.isWoodDoor(m) ||
					BlockUtils.isWoodTrapdoor(m) ||
					BlockUtils.isWoodButton(m) ||
					BlockUtils.isBed(m) ||
					m == Material.CRAFTING_TABLE ||
					m == Material.CHEST ||
					m == Material.TRAPPED_CHEST ||
					m == Material.ENDER_CHEST ||
					m == Material.FURNACE ||
					m == Material.HOPPER ||
					m == Material.LEVER ||
					m == Material.STONE_BUTTON ||
					m == Material.ENCHANTING_TABLE) {
				noInteract = true;
			} else if (event.hasItem() && event.getItem().getType().isBlock()) {
				noInteract = true;
			}

			// Force exp bar back to show exp when trying to enchant
			if (m == Material.ENCHANTING_TABLE) MagicJutsus.getExpBarManager().update(player, player.getLevel(), player.getExp());
		}

		if (noInteract) {
			// Special block -- don't do normal interactions
			noCastUntil.put(event.getPlayer().getName(), System.currentTimeMillis() + 150);
			return;
		}

		if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// Left click - cast
			if (!plugin.castOnAnimate) castJutsu(event.getPlayer());
			return;
		}

		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && (plugin.cycleJutsusOnOffhandAction || event.getHand() == EquipmentSlot.HAND)) {
			// Right click -- cycle jutsu
			ItemStack inHand = player.getEquipment().getItemInMainHand();
			if ((inHand != null && !BlockUtils.isAir(inHand.getType())) || plugin.allowCastWithFist) {

				// Cycle jutsu
				Jutsu jutsu;
				if (!player.isSneaking()) jutsu = MagicJutsus.getJutsubook(player).nextJutsu(inHand);
				else jutsu = MagicJutsus.getJutsubook(player).prevJutsu(inHand);

				if (jutsu == null) return;
				// Send message
				MagicJutsus.sendMessageAndFormat(player, plugin.strJutsuChange, "%s", jutsu.getName());
				// Show jutsu icon
				if (plugin.jutsuIconSlot >= 0) showIcon(player, plugin.jutsuIconSlot, jutsu.getJutsuIcon());
				// Use cool new text thingy
				boolean yay = false;
				if (yay) {
					final ItemStack fake = inHand.clone();
					if (fake == null) return;
					ItemMeta meta = fake.getItemMeta();
					meta.setDisplayName("Jutsu: " + jutsu.getName());
					fake.setItemMeta(meta);
					MagicJutsus.scheduleDelayedTask(() -> MagicJutsus.getVolatileCodeHandler().sendFakeSlotUpdate(player, player.getInventory().getHeldItemSlot(), fake), 0);
				}
			}
		}
	}

	@EventHandler
	public void onItemHeldChange(final PlayerItemHeldEvent event) {
		if (plugin.jutsuIconSlot >= 0 && plugin.jutsuIconSlot <= 8) {
			Player player = event.getPlayer();
			if (event.getNewSlot() == plugin.jutsuIconSlot) {
				showIcon(player, plugin.jutsuIconSlot, null);
				return;
			}
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			Jutsu jutsu = jutsubook.getActiveJutsu(player.getInventory().getItem(event.getNewSlot()));
			if (jutsu != null) showIcon(player, plugin.jutsuIconSlot, jutsu.getJutsuIcon());
			else showIcon(player, plugin.jutsuIconSlot, null);
		}
	}

	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerAnimation(PlayerAnimationEvent event) {
		if (plugin.castOnAnimate) castJutsu(event.getPlayer());
	}

	private void castJutsu(Player player) {
		ItemStack inHand = player.getEquipment().getItemInMainHand();
		if (!plugin.allowCastWithFist && (inHand == null || BlockUtils.isAir(inHand.getType()))) return;

		Jutsu jutsu = MagicJutsus.getJutsubook(player).getActiveJutsu(inHand);
		if (jutsu == null || !jutsu.canCastWithItem()) return;
		// First check global cooldown
		if (plugin.globalCooldown > 0 && !jutsu.ignoreGlobalCooldown) {
			if (noCastUntil.containsKey(player.getName()) && noCastUntil.get(player.getName()) > System.currentTimeMillis()) return;
			noCastUntil.put(player.getName(), System.currentTimeMillis() + plugin.globalCooldown);
		}
		// Cast jutsu
		jutsu.cast(player);
	}

	private void showIcon(Player player, int slot, ItemStack icon) {
		if (icon == null) icon = player.getInventory().getItem(plugin.jutsuIconSlot);
		MagicJutsus.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, icon);
	}

}
