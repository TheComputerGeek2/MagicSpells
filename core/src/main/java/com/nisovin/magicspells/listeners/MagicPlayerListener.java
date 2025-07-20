package com.nisovin.magicspells.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellUtil;
import com.nisovin.magicspells.util.recipes.CustomRecipes;
import com.nisovin.magicspells.util.recipes.wrapper.CustomRecipe;

public class MagicPlayerListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		MagicSpells.getSpellbooks().put(player.getName(), new Spellbook(player));

		if (MagicSpells.getManaHandler() != null) MagicSpells.getManaHandler().createManaBar(player);

		player.discoverRecipes(CustomRecipes.getRecipes()
			.stream()
			.map(CustomRecipe::getKey)
			.toList()
		);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		Spellbook spellbook = MagicSpells.getSpellbooks().remove(player.getName());
		if (spellbook != null) spellbook.destroy();

		player.undiscoverRecipes(CustomRecipes.getRecipes()
			.stream()
			.map(CustomRecipe::getKey)
			.toList()
		);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		if (!MagicSpells.arePlayerSpellsSeparatedPerWorld()) return;
		Player player = event.getPlayer();
		MagicSpells.debug("Player '" + player.getName() + "' changed from world '" + event.getFrom().getName() + "' to '" + player.getWorld().getName() + "', reloading spells");
		MagicSpells.getSpellbook(player).reload();
	}

	@EventHandler(ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		SpellUtil.updateManaBar(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onHealthRegain(EntityRegainHealthEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		SpellUtil.updateManaBar(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onFoodLevelChange(FoodLevelChangeEvent event) {
		SpellUtil.updateManaBar((Player) event.getEntity());
	}
	
}
