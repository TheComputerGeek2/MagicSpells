package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;

/**
 * InvisibilitySpell<br>
 * <table border=1>
 *     <tr>
 *         <th>
 *             Config Field
 *         </th>
 *         <th>
 *             Data Type
 *         </th>
 *         <th>
 *             Description
 *         </th>
 *         <th>
 *             Default
 *         </th>
 *     </tr>
 *     <tr>
 *         <td>
 *             prevent-pickups
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             true
 *         </td>
 *     </tr>
 *     <tr>
 *         <td>
 *             cancel-on-spell-cast
 *         </td>
 *         <td>
 *             Boolean
 *         </td>
 *         <td>
 *             ???
 *         </td>
 *         <td>
 *             false
 *         </td>
 *     </tr>
 * </table>
 */
public class InvisibilitySpell extends BuffSpell {

	private boolean preventPickups;
	
	private boolean cancelOnSpellCast;
	
	private HashMap<String,CostCharger> invisibles = new HashMap<String, InvisibilitySpell.CostCharger>();
	
	public InvisibilitySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		preventPickups = getConfigBoolean("prevent-pickups", true);
		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (cancelOnSpellCast) {
			registerEvents(new SpellCastListener());
		}
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		makeInvisible(player);
		invisibles.put(player.getName(), new CostCharger(player));
		return true;
	}
	
	@Override
	public boolean recastBuff(Player player, float power, String[] args) {
		makeInvisible(player);
		if (invisibles.containsKey(player.getName())) {
			invisibles.put(player.getName(), new CostCharger(player));
		}
		return true;
	}
	
	private void makeInvisible(Player player) {
		// make player invisible
		for (Player p : Bukkit.getOnlinePlayers()) {
			p.hidePlayer(player);
		}
		// detarget monsters
		Creature creature;
		for (Entity e : player.getNearbyEntities(30, 30, 30)) {
			if (e instanceof Creature) {
				creature = (Creature)e;
				if (creature.getTarget() != null && creature.getTarget().equals(player)) {
					creature.setTarget(null);
				}
			}
		}
	}
	
	
	@EventHandler
	public void onPlayerItemPickup(PlayerPickupItemEvent event) {
		if (preventPickups && invisibles.containsKey(event.getPlayer().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if (!event.isCancelled() && event.getTarget() instanceof Player) {
			if (invisibles.containsKey(((Player)event.getTarget()).getName())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		for (String name : invisibles.keySet()) {
			Player p = PlayerNameUtils.getPlayerExact(name);
			if (p != null && !name.equals(player.getName())) {
				player.hidePlayer(p);
			}
		}
		if (invisibles.containsKey(player.getName())) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.hidePlayer(player);
			}
		}
	}
	
	@Override
	public void turnOffBuff(Player player) {
		// stop charge ticker
		CostCharger c = invisibles.remove(player.getName());
		if (c != null) {
			c.stop();
			// force visible
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.showPlayer(player);
			}
		}
	}

	@Override
	protected void turnOff() {
		for (CostCharger c : invisibles.values()) {
			c.stop();
		}
		invisibles.clear();
	}
	
	public class SpellCastListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (isActive(event.getCaster()) && !event.getSpell().getInternalName().equals(internalName)) {
				turnOff(event.getCaster());
			}
		}
	}
	
	private class CostCharger implements Runnable {
		int taskId = -1;
		Player player;
		
		public CostCharger(Player player) {
			this.player = player;
			if (useCostInterval > 0) {
				taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 20, 20);
			}
		}
		
		@Override
		public void run() {
			addUseAndChargeCost(player);
		}
		
		public void stop() {
			if (taskId != -1) {
				Bukkit.getScheduler().cancelTask(taskId);
			}
		}
	}

	@Override
	public boolean isActive(Player player) {
		return player != null && invisibles.containsKey(player.getName());
	}

}
