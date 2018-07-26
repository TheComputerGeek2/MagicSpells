package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.Map;

import com.nisovin.magicspells.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.util.HandHandler;

public class CastListener implements Listener {

	MagicSpells plugin;
	
	private HashMap<String, Long> noCastUntil = new HashMap<>();
	//private HashMap<String,Long> lastCast = new HashMap<String, Long>();

	public CastListener(MagicSpells plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		
		// First check if player is interacting with a special block
		boolean noInteract = false;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Material m = event.getClickedBlock().getType();
			if (m.name().endsWith("DOOR") ||
					m.name().endsWith("BED") ||
					m == Material.CRAFTING_TABLE ||
					m == Material.CHEST || 
					m == Material.TRAPPED_CHEST ||
					m == Material.ENDER_CHEST ||
					m == Material.FURNACE || 
					m == Material.HOPPER ||
					m == Material.LEVER ||
					m.name().endsWith("BUTTON") ||
					m == Material.ENCHANTING_TABLE) {
				noInteract = true;
			} else if (event.hasItem() && event.getItem().getType().isBlock()) {
				noInteract = true;
			}
			if (m == Material.ENCHANTING_TABLE) {
				// Force exp bar back to show exp when trying to enchant
				MagicSpells.getExpBarManager().update(player, player.getLevel(), player.getExp());
			}
		}
		if (noInteract) {
			// Special block -- don't do normal interactions
			noCastUntil.put(event.getPlayer().getName(), System.currentTimeMillis() + 150);
		} else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// Left click - cast
			if (!plugin.castOnAnimate) {
				castSpell(event.getPlayer());
			}
		} else if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && (plugin.cycleSpellsOnOffhandAction || HandHandler.isMainHand(event))) {
			// Right click -- cycle spell and/or process mana pots
			ItemStack inHand = HandHandler.getItemInMainHand(player);
			
			if ((inHand != null && inHand.getType() != Material.AIR) || plugin.allowCastWithFist) {
			
				// Cycle spell
				Spell spell = null;
				if (!player.isSneaking()) {
					spell = MagicSpells.getSpellbook(player).nextSpell(inHand);
				} else {
					spell = MagicSpells.getSpellbook(player).prevSpell(inHand);
				}
				if (spell != null) {
					// Send message
					MagicSpells.sendMessageAndFormat(player, plugin.strSpellChange, "%s", spell.getName());
					// Show spell icon
					if (plugin.spellIconSlot >= 0) {
						showIcon(player, plugin.spellIconSlot, spell.getSpellIcon());
					}
					// Use cool new text thingy
					boolean yay = false;
					if (yay) {
						final ItemStack fake = inHand.clone(); //TODO ensure this is not null
						ItemMeta meta = fake.getItemMeta();
						meta.setDisplayName("Spell: " + spell.getName());
						fake.setItemMeta(meta);
						MagicSpells.scheduleDelayedTask(() -> MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, player.getInventory().getHeldItemSlot(), fake), 0);
					}
				}
				
				// Check for mana pots
				if (plugin.enableManaBars && plugin.manaPotions != null) {
					// Find mana potion TODO: fix this, it's not good
					int restoreAmt = 0;
					for (Map.Entry<ItemStack, Integer> entry : plugin.manaPotions.entrySet()) {
						if (inHand.isSimilar(entry.getKey())) { //TODO make sure this is not null
							restoreAmt = entry.getValue();
							break;
						}
					}
					if (restoreAmt > 0) {
						// Check cooldown
						if (plugin.manaPotionCooldown > 0) {
							Long c = plugin.manaPotionCooldowns.get(player);
							if (c != null && c > System.currentTimeMillis()) {
								MagicSpells.sendMessage(plugin.strManaPotionOnCooldown.replace("%c", "" + (int)((c - System.currentTimeMillis())/TimeUtil.MILLISECONDS_PER_SECOND)), player, MagicSpells.NULL_ARGS);
								return;
							}
						}
						// Add mana
						boolean added = plugin.mana.addMana(player, restoreAmt, ManaChangeReason.POTION);
						if (added) {
							// Set cooldown
							if (plugin.manaPotionCooldown > 0) {
								plugin.manaPotionCooldowns.put(player, System.currentTimeMillis() + plugin.manaPotionCooldown * TimeUtil.MILLISECONDS_PER_SECOND);
							}
							// Remove item
							if (inHand.getAmount() == 1) { //TODO make sure this is not null
								inHand = null;
							} else {
								inHand.setAmount(inHand.getAmount() - 1);
							}
							HandHandler.setItemInMainHand(player, inHand);
							player.updateInventory();
						}
					}
				}
				
			}
		}
	}
	
	@EventHandler
	public void onItemHeldChange(final PlayerItemHeldEvent event) {
		if (plugin.spellIconSlot >= 0 && plugin.spellIconSlot <= 8) {
			Player player = event.getPlayer();
			if (event.getNewSlot() == plugin.spellIconSlot) {
				showIcon(player, plugin.spellIconSlot, null);
			} else {
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				Spell spell = spellbook.getActiveSpell(player.getInventory().getItem(event.getNewSlot()));
				if (spell != null) {
					showIcon(player, plugin.spellIconSlot, spell.getSpellIcon());
				} else {
					showIcon(player, plugin.spellIconSlot, null);
				}
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerAnimation(PlayerAnimationEvent event) {		
		if (plugin.castOnAnimate) {
			castSpell(event.getPlayer());
		}
	}
	
	private void castSpell(Player player) {		
		ItemStack inHand = HandHandler.getItemInMainHand(player);
		if (!plugin.allowCastWithFist && (inHand == null || inHand.getType() == Material.AIR)) return;
		
		Spell spell = MagicSpells.getSpellbook(player).getActiveSpell(inHand);
		if (spell != null && spell.canCastWithItem()) {			
			// First check global cooldown
			if (plugin.globalCooldown > 0 && !spell.ignoreGlobalCooldown) {
				if (noCastUntil.containsKey(player.getName()) && noCastUntil.get(player.getName()) > System.currentTimeMillis()) return;
				noCastUntil.put(player.getName(), System.currentTimeMillis() + plugin.globalCooldown);
			}
			// Cast spell
			spell.cast(player);
		}		
	}
	
	private void showIcon(Player player, int slot, ItemStack icon) {
		if (icon == null) icon = player.getInventory().getItem(plugin.spellIconSlot);
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, icon);
	}

}
