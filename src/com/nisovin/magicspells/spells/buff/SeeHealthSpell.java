package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.TargetInfo;

public class SeeHealthSpell extends BuffSpell {

	private String mode;
	int interval;
	
	private String symbol = "=";
	private int barSize = 20;
	private boolean colorBlind = false;
	
	HashMap<String, Integer> bars;
	private Updater updater;
	
	public SeeHealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.mode = getConfigString("mode", "always");
		this.interval = getConfigInt("update-interval", 5);
		
		if (!this.mode.equals("attack") && !this.mode.equals("always")) this.mode = "attack";
		
		this.bars = new HashMap<>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (this.mode.equals("attack")) {
			registerEvents(new AttackListener());
		}
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		bars.put(player.getName(), player.getInventory().getHeldItemSlot());
		if (this.updater == null && this.mode.equals("always")) this.updater = new Updater();
		return true;
	}

	@Override
	public boolean isActive(Player player) {
		return this.bars.containsKey(player.getName());
	}
	
	void showHealthBar(Player player, LivingEntity entity) {
		int slot = player.getInventory().getHeldItemSlot();
		// Get item
		ItemStack item = HandHandler.getItemInMainHand(player);
		if (item == null || item.getType() == Material.AIR) {
			item = new ItemStack(Material.MOVING_PISTON, 0);
		} else {
			item = item.clone();
		}
		// Get pct health
		double pct = (double)entity.getHealth() / (double)entity.getMaxHealth();
		// Get bar color
		ChatColor color = ChatColor.WHITE;
		if (pct <= .2) {
			color = ChatColor.DARK_RED;
		} else if (pct <= .4) {
			color = ChatColor.RED;
		} else if (pct <= .6) {
			color = ChatColor.GOLD;
		} else if (pct <= .8) {
			color = ChatColor.YELLOW;
		} else if (!this.colorBlind) {
			color = ChatColor.GREEN;
		}
		// Get health bar string
		StringBuilder sb = new StringBuilder(this.barSize + 9);
		sb.append(getRandomColor().toString());
		int remain = (int)Math.round(this.barSize * pct);
		sb.append(color.toString());
		for (int i = 0; i < remain; i++) {
			sb.append(this.symbol);
		}
		if (remain < this.barSize) {
			sb.append(ChatColor.DARK_GRAY.toString());
			for (int i = 0; i < this.barSize - remain; i++) {
				sb.append(this.symbol);
			}
		}
		// Set health bar string
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(sb.toString());
		item.setItemMeta(meta);
		// Send update
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, item);
	}
	
	//private void resetHealthBar(Player player) {
	//	resetHealthBar(player, player.getInventory().getHeldItemSlot());
	//}
	
	private void resetHealthBar(Player player, int slot) {
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, HandHandler.getItemInMainHand(player));
	}
	
	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		if (isActive(event.getPlayer())) {
			resetHealthBar(event.getPlayer(), event.getPreviousSlot());
		}
	}
	
	@Override
	public void turnOffBuff(Player player) {
		Integer i = this.bars.remove(player.getName());
		if (i != null) {
			player.updateInventory();
			if (this.updater != null && this.bars.isEmpty()) {
				this.updater.stop();
				this.updater = null;
			}
		}
	}

	@Override
	protected void turnOff() {
		for (String playerName : this.bars.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player == null) continue;
			if (!player.isValid()) continue;
			player.updateInventory();
		}
		this.bars.clear();
		if (this.updater != null) {
			this.updater.stop();
			this.updater = null;
		}
	}
	
	private final String colors = "01234567890abcdef";
	private final Random random = new Random();
	
	private ChatColor getRandomColor() {
		return ChatColor.getByChar(this.colors.charAt(this.random.nextInt(this.colors.length())));
	}
	
	class AttackListener implements Listener {
		
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onAttack(EntityDamageByEntityEvent event) {
			if (event.getEntity() instanceof LivingEntity) {
				Entity damager = event.getDamager();
				if (damager instanceof Projectile && ((Projectile)damager).getShooter() != null) {
					damager = (LivingEntity)((Projectile)damager).getShooter();
				}
				// Update bar?
			}
		}
		
	}
	
	class Updater implements Runnable {
		
		private int taskId;
		
		public Updater() {
			this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 0, interval);
		}
		
		@Override
		public void run() {
			for (String playerName : bars.keySet()) {
				Player player = PlayerNameUtils.getPlayerExact(playerName);
				if (player == null) continue;
				if (!player.isValid()) continue;
				TargetInfo<LivingEntity> target = getTargetedEntity(player, 1F);
				if (target != null) {
					showHealthBar(player, target.getTarget());
				} else {
					//resetHealthBar(player);
				}
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(this.taskId);
		}
		
	}

}
