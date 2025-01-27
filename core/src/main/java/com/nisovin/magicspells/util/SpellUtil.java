package com.nisovin.magicspells.util;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.function.Predicate;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.Damageable;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.mana.ManaSystem;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.handlers.MoneyHandler;
import com.nisovin.magicspells.util.managers.VariableManager;

import org.apache.commons.math4.core.jdkmath.AccurateMath;

public class SpellUtil {
	
	// Currently will only work with direct permission nodes, doesn't handle child nodes yet
	// NOTE: allSpells should be a thread safe collection for read access
	public static Collection<Spell> getSpellsByPermissionNames(final Collection<Spell> allSpells, final Set<String> names) {
		Predicate<Spell> predicate = spell -> names.contains(spell.getPermissionName());
		return getSpellsByX(allSpells, predicate);
	}
	
	// NOTE: allSpells should be a thread safe collection for read access
	// NOTE: streams do work for making the collection thread safe
	public static Collection<Spell> getSpellsByX(final Collection<Spell> allSpells, final Predicate<Spell> predicate) {
		return allSpells
			.parallelStream()
			.filter(predicate)
			.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Checks if a player has the reagents required to cast this spell
	 * @param livingEntity the living entity to check
	 * @param reagents the reagents to check for
	 * @return true if the player has the reagents, false otherwise
	 */
	public static boolean hasReagents(LivingEntity livingEntity, SpellReagents reagents) {
		if (reagents == null) return true;
		return hasReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	/**
	 * Checks if a player has the specified reagents, including health and mana
	 * @param livingEntity the living entity to check
	 * @param reagents the inventory item reagents to look for
	 * @param healthCost the health cost, in half-hearts
	 * @param manaCost the mana cost
	 * @return true if the player has all the reagents, false otherwise
	 */
	public static boolean hasReagents(LivingEntity livingEntity, SpellReagents.ReagentItem[] reagents, double healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		// Is the livingEntity exempt from reagent costs?
		if (Perm.NO_REAGENTS.has(livingEntity)) return true;

		// player reagents
		if (livingEntity instanceof Player player) {
			// Mana costs
			if (manaCost > 0 && (MagicSpells.getManaHandler() == null || !MagicSpells.getManaHandler().hasMana(player, manaCost))) return false;

			// Hunger costs
			if (hungerCost > 0 && player.getFoodLevel() < hungerCost) return false;

			// Experience costs
			if (experienceCost > 0 && experienceCost > player.calculateTotalExperiencePoints()) return false;

			// Level costs
			if (levelsCost > 0 && player.getLevel() < levelsCost) return false;

			// Money costs
			if (moneyCost > 0) {
				MoneyHandler handler = MagicSpells.getMoneyHandler();
				if (handler == null || !handler.hasMoney(player, moneyCost)) {
					return false;
				}
			}

			// Variable costs
			if (variables != null) {
				VariableManager manager = MagicSpells.getVariableManager();
				if (manager == null) return false;
				for (Map.Entry<String, Double> var : variables.entrySet()) {
					double val = var.getValue();
					if (val > 0 && manager.getValue(var.getKey(), player) < val) return false;
				}
			}
		}

		// Health costs
		if (healthCost > 0 && livingEntity.getHealth() <= healthCost) return false;

		// Durabilty costs
		if (durabilityCost > 0) {
			// Durability cost is charged from the main hand item
			EntityEquipment equipment = livingEntity.getEquipment();
			if (equipment == null) return false;
			ItemStack inHand = equipment.getItemInMainHand();
			if (!(inHand.getItemMeta() instanceof Damageable damageable)) return false;
			if (damageable.getDamage() >= inHand.getType().getMaxDurability()) return false;
		}

		// Item costs
		if (reagents != null) {
			if (livingEntity instanceof Player player) {
				Inventory inventory = player.getInventory();
				for (SpellReagents.ReagentItem item : reagents) {
					if (item == null) continue;
					if (InventoryUtil.inventoryContains(inventory, item)) continue;
					return false;
				}
			} else {
				EntityEquipment entityEquipment = livingEntity.getEquipment();
				for (SpellReagents.ReagentItem item : reagents) {
					if (item == null) continue;
					if (InventoryUtil.inventoryContains(entityEquipment, item)) continue;
					return false;
				}
			}
		}

		return true;
	}

	public static void removeReagents(LivingEntity livingEntity, SpellReagents reagents) {
		removeReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	/**
	 * Removes the specified reagents, including health and mana, from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 * @param livingEntity the living entity to remove the reagents from
	 * @param reagents the inventory item reagents to remove
	 * @param healthCost the health to remove
	 * @param manaCost the mana to remove
	 */
	public static void removeReagents(LivingEntity livingEntity, SpellReagents.ReagentItem[] reagents, double healthCost, int manaCost, int hungerCost, int experienceCost, int levelsCost, int durabilityCost, float moneyCost, Map<String, Double> variables) {
		if (Perm.NO_REAGENTS.has(livingEntity)) return;

		if (reagents != null) {
			for (SpellReagents.ReagentItem item : reagents) {
				if (item == null) continue;
				if (livingEntity instanceof Player player) Util.removeFromInventory(player.getInventory(), item);
				else if (livingEntity.getEquipment() != null) Util.removeFromInventory(livingEntity.getEquipment(), item);
			}
		}

		if (livingEntity instanceof Player player) {
			if (manaCost != 0) MagicSpells.getManaHandler().addMana(player, -manaCost, ManaChangeReason.SPELL_COST);

			if (hungerCost != 0) {
				int f = player.getFoodLevel() - hungerCost;
				if (f < 0) f = 0;
				if (f > 20) f = 20;
				player.setFoodLevel(f);
			}

			if (experienceCost != 0) Util.addExperience(player, -experienceCost);

			if (moneyCost != 0) {
				MoneyHandler handler = MagicSpells.getMoneyHandler();
				if (handler != null) {
					if (moneyCost > 0) handler.removeMoney(player, moneyCost);
					else handler.addMoney(player, -moneyCost);
				}
			}

			if (levelsCost != 0) {
				int lvl = player.getLevel() - levelsCost;
				if (lvl < 0) lvl = 0;
				player.setLevel(lvl);
			}

			if (variables != null) {
				VariableManager manager = MagicSpells.getVariableManager();
				if (manager != null) {
					for (Map.Entry<String, Double> var : variables.entrySet()) {
						manager.set(var.getKey(), player, manager.getValue(var.getKey(), player) - var.getValue());
					}
				}
			}
		}

		if (healthCost != 0) {
			double h = livingEntity.getHealth() - healthCost;
			if (h < 0) h = 0;
			if (h > Util.getMaxHealth(livingEntity)) h = Util.getMaxHealth(livingEntity);
			livingEntity.setHealth(h);
		}

		if (durabilityCost != 0) {
			EntityEquipment eq = livingEntity.getEquipment();

			if (eq != null) {
				ItemStack item = eq.getItemInMainHand();
				ItemMeta meta = item.getItemMeta();

				int maxDurability = item.getType().getMaxDurability();
				if (maxDurability > 0 && meta instanceof Damageable damageable) {
					int damage = damageable.getDamage() + durabilityCost;
					damage = AccurateMath.max(AccurateMath.min(damage, maxDurability), 0);

					damageable.setDamage(damage);
					item.setItemMeta(meta);
				}
			}
		}
	}

	public static void updateManaBar(Player player) {
		if (!(MagicSpells.getManaHandler() instanceof ManaSystem system)) return;
		if (!system.usingHungerBar()) return;
		MagicSpells.scheduleDelayedTask(() -> system.showMana(player), 1, player);
	}
	
}
