package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.handlers.EnchantmentHandler;

public class ArmorJutsu extends BuffJutsu {

	private Set<UUID> armored;

	private boolean permanent;
	private boolean replace;

	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private String strHasArmor;
	private String strLoreText;

	public ArmorJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		permanent = getConfigBoolean("permanent", false);
		replace = getConfigBoolean("replace", false);

		helmet = getItem(getConfigString("helmet", ""));
		chestplate = getItem(getConfigString("chestplate", ""));
		leggings = getItem(getConfigString("leggings", ""));
		boots = getItem(getConfigString("boots", ""));

		strHasArmor = getConfigString("str-has-armor", "You cannot cast this jutsu if you are wearing armor.");
		strLoreText = ChatColor.translateAlternateColorCodes('&', getConfigString("str-lore-text", "Conjured"));

		armored = new HashSet<>();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!permanent) registerEvents(new ArmorListener());
	}

	private ItemStack getItem(String s) {
		if (s.isEmpty()) return null;
		String[] info = s.split(" ");
		try {

			// Get type and data
			ItemStack item = Util.getItemStackFromString(info[0]);
			if (item == null) {
				if (DebugHandler.isNullCheckEnabled()) {
					NullPointerException e = new NullPointerException("ItemStack is null");
					e.fillInStackTrace();
					DebugHandler.nullCheck(e);
				}
				return null;
			}

			item.setAmount(1);

			if (!permanent) {
				ItemMeta meta = item.getItemMeta();

				List<String> lore;
				if (meta.hasLore()) lore = meta.getLore();
				else lore = new ArrayList<>();

				lore.add(strLoreText);
				meta.setLore(lore);
				item.setItemMeta(meta);
			}

			// Get enchantments (left for backwards compatibility)
			if (info.length > 1) {
				for (int i = 1; i < info.length; i++) {
					String[] enchinfo = info[i].split(":");
					Enchantment ench = EnchantmentHandler.getEnchantment(enchinfo[0]);
					int lvl = 1;
					if (enchinfo.length > 1) lvl = Integer.parseInt(enchinfo[1].toUpperCase().replace(" ", "_"));
					if (ench != null) item.addUnsafeEnchantment(ench, lvl);
				}
			}

			return item;

		} catch (NumberFormatException e) {
			DebugHandler.debugNumberFormat(e);
			return null;
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		EntityEquipment inv = entity.getEquipment();
		if (!replace && ((helmet != null && inv.getHelmet() != null) || (chestplate != null && inv.getChestplate() != null) || (leggings != null && inv.getLeggings() != null) || (boots != null && inv.getBoots() != null))) {
			// error
			if (entity instanceof Player) sendMessage(strHasArmor, entity, args);
			return false;
		}

		setArmor(inv);

		if (!permanent) armored.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		return castBuff(entity, power, args);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return armored.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (!armored.remove(entity.getUniqueId())) return;
		if (!entity.isValid()) return;
		EntityEquipment inv = entity.getEquipment();
		removeArmor(inv);
	}

	@Override
	protected void turnOff() {
		for (UUID id : armored) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null) continue;
			if (!entity.isValid()) continue;
			EntityEquipment inv = ((LivingEntity) entity).getEquipment();
			removeArmor(inv);
		}
		armored.clear();
	}

	private void setArmor(EntityEquipment inv) {
		if (helmet != null) {
			if (replace) inv.setHelmet(null);
			inv.setHelmet(helmet.clone());
		}

		if (chestplate != null) {
			if (replace) inv.setChestplate(null);
			inv.setChestplate(chestplate.clone());
		}

		if (leggings != null) {
			if (replace) inv.setLeggings(null);
			inv.setLeggings(leggings.clone());
		}

		if (boots != null) {
			if (replace) inv.setBoots(null);
			inv.setBoots(boots.clone());
		}
	}

	private void removeArmor(EntityEquipment inv) {
		ItemStack invHelmet = inv.getHelmet();
		if (helmet != null && invHelmet != null && invHelmet.getType() == helmet.getType()) {
			inv.setHelmet(null);
		}

		ItemStack invChestplate = inv.getChestplate();
		if (chestplate != null && invChestplate != null && invChestplate.getType() == chestplate.getType()) {
			inv.setChestplate(null);
		}

		ItemStack invLeggings = inv.getLeggings();
		if (leggings != null && invLeggings != null && invLeggings.getType() == leggings.getType()) {
			inv.setLeggings(null);
		}

		ItemStack invBoots = inv.getBoots();
		if (boots != null && invBoots != null && invBoots.getType() == boots.getType()) {
			inv.setBoots(null);
		}
	}

	private class ArmorListener implements Listener {

		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onEntityDamage(EntityDamageEvent event) {
			Entity entity = event.getEntity();
			if (!(entity instanceof LivingEntity)) return;

			LivingEntity livingEntity = (LivingEntity) entity;
			if (!isActive(livingEntity)) return;

			if (livingEntity.getNoDamageTicks() >= 10) return;
			addUseAndChargeCost(livingEntity);
		}

		@EventHandler(ignoreCancelled=true)
		public void onInventoryClick(InventoryClickEvent event) {
			if (event.getSlotType() != SlotType.ARMOR) return;

			HumanEntity entity = event.getWhoClicked();

			if (!(entity instanceof Player)) return;

			Player p = (Player)entity;
			if (!isActive(p)) return;

			event.setCancelled(true);
		}

		@EventHandler
		public void onEntityDeath(EntityDeathEvent event) {
			Iterator<ItemStack> drops = event.getDrops().iterator();
			while (drops.hasNext()) {
				ItemStack drop = drops.next();
				if (!drop.hasItemMeta()) continue;

				ItemMeta dropMeta = drop.getItemMeta();
				if (dropMeta == null) continue;

				List<String> lore = dropMeta.getLore();
				if (lore == null) continue;
				if (lore.isEmpty()) continue;

				if (!lore.get(lore.size() - 1).equals(strLoreText)) continue;

				drops.remove();
			}
		}

		@EventHandler
		public void onPlayerRespawn(PlayerRespawnEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;
			if (isExpired(player)) return;

			final EntityEquipment inv = player.getEquipment();
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicJutsus.plugin, () -> setArmor(inv));
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;

			if (cancelOnLogout) turnOff(player);
			else removeArmor(player.getEquipment());

		}

		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player)) return;

			if (!isExpired(player)) setArmor(player.getEquipment());
			else turnOff(player);

		}

	}

}
