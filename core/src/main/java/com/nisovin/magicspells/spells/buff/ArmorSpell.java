package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.event.inventory.InventoryType.SlotType;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;

public class ArmorSpell extends BuffSpell {

	private static final NamespacedKey MARKER = new NamespacedKey(MagicSpells.getInstance(), "armor_spell_item");

	private final Set<UUID> entities;

	private final boolean permanent;
	private final ConfigData<Boolean> replace;

	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private String strHasArmor;

	public ArmorSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		permanent = getConfigBoolean("permanent", false);
		replace = getConfigDataBoolean("replace", false);

		helmet = getItem(getConfigString("helmet", ""));
		chestplate = getItem(getConfigString("chestplate", ""));
		leggings = getItem(getConfigString("leggings", ""));
		boots = getItem(getConfigString("boots", ""));

		strHasArmor = getConfigString("str-has-armor", "You cannot cast this spell if you are wearing armor.");

		entities = new HashSet<>();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!permanent) registerEvents(new ArmorListener());
	}

	private ItemStack getItem(String s) {
		if (s.isEmpty()) return null;

		MagicItem magicItem = MagicItems.getMagicItemFromString(s);
		if (magicItem == null) return null;

		ItemStack item = magicItem.getItemStack();
		if (item == null) {
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("ItemStack is null");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return null;
		}

		item.setAmount(1);
		if (!permanent)
			item.editMeta(meta -> meta.getPersistentDataContainer().set(MARKER, PersistentDataType.STRING, internalName));

		return item;
	}

	@Override
	public boolean castBuff(SpellData data) {
		EntityEquipment eq = data.target().getEquipment();
		if (eq == null) return false;

		if (!replace.get(data) &&
			((helmet != null && eq.getHelmet() != null) ||
				(chestplate != null && eq.getChestplate() != null) ||
				(leggings != null && eq.getLeggings() != null) ||
				(boots != null && eq.getBoots() != null))
		) {
			sendMessage(strHasArmor, data.caster(), data);
			return false;
		}

		setArmor(eq);
		if (!permanent) entities.add(data.target().getUniqueId());

		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		if (!entities.remove(entity.getUniqueId()) || !entity.isValid()) return;

		EntityEquipment eq = entity.getEquipment();
		if (eq != null) removeArmor(eq);
	}

	@Override
	protected void turnOff() {
		for (UUID id : entities) {
			Entity entity = Bukkit.getEntity(id);
			if (entity == null || !entity.isValid()) continue;

			EntityEquipment eq = ((LivingEntity) entity).getEquipment();
			if (eq != null) removeArmor(eq);
		}

		entities.clear();
	}

	private void setArmor(EntityEquipment eq) {
		if (helmet != null) eq.setHelmet(helmet);
		if (chestplate != null) eq.setChestplate(chestplate);
		if (leggings != null) eq.setLeggings(leggings);
		if (boots != null) eq.setBoots(boots);
	}

	private void removeArmor(EntityEquipment eq) {
		ItemStack[] armor = eq.getArmorContents();
		boolean modified = false;

		for (int i = 0; i < armor.length; i++) {
			if (!hasMarker(armor[i])) continue;

			armor[i] = null;
			modified = true;
		}

		if (modified) eq.setArmorContents(armor);
	}

	private boolean hasMarker(ItemStack item) {
		if (item == null) return false;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return false;

		PersistentDataContainer container = meta.getPersistentDataContainer();
		if (!container.has(MARKER)) return false;

		return internalName.equals(container.get(MARKER, PersistentDataType.STRING));
	}

	private class ArmorListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onEntityDamage(EntityDamageEvent event) {
			Entity entity = event.getEntity();
			if (!(entity instanceof LivingEntity le) || !isActive(le) || le.getNoDamageTicks() >= 10) return;

			addUseAndChargeCost(le);
		}

		@EventHandler(ignoreCancelled = true)
		public void onInventoryClick(InventoryClickEvent event) {
			if (event.getSlotType() != SlotType.ARMOR) return;

			HumanEntity entity = event.getWhoClicked();
			if (isActive(entity) && hasMarker(event.getCurrentItem())) event.setCancelled(true);
		}

		@EventHandler(ignoreCancelled = true)
		public void onInventoryDrag(InventoryDragEvent event) {
			HumanEntity entity = event.getWhoClicked();
			if (!isActive(entity)) return;

			InventoryView view = event.getView();

			Set<Integer> slots = event.getRawSlots();
			for (int slot : slots) {
				if (view.getSlotType(slot) != SlotType.ARMOR || !hasMarker(view.getItem(slot))) continue;

				event.setCancelled(true);
				return;
			}
		}

		@EventHandler
		public void onInteract(PlayerInteractEvent event) {
			if (!event.getAction().isRightClick() || event.useItemInHand() == Event.Result.DENY) return;

			Player player = event.getPlayer();
			if (!isActive(player)) return;

			EquipmentSlot slot = event.getMaterial().getEquipmentSlot();
			if (!slot.isArmor()) return;

			ItemStack item = player.getEquipment().getItem(slot);
			if (hasMarker(item)) event.setUseItemInHand(Event.Result.DENY);
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		public void onEntityDeath(EntityDeathEvent event) {
			List<ItemStack> drops = event.getDrops();
			drops.removeIf(ArmorSpell.this::hasMarker);
		}

		@EventHandler
		public void onPlayerRespawn(PlayerRespawnEvent event) {
			Player player = event.getPlayer();
			if (!isActive(player) || isExpired(player)) return;

			EntityEquipment eq = player.getEquipment();
			player.getScheduler().run(MagicSpells.plugin, t -> setArmor(eq), null);
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

	public Set<UUID> getEntities() {
		return entities;
	}

	public ItemStack getHelmet() {
		return helmet;
	}

	public void setHelmet(ItemStack helmet) {
		this.helmet = helmet;
	}

	public ItemStack getChestplate() {
		return chestplate;
	}

	public void setChestplate(ItemStack chestplate) {
		this.chestplate = chestplate;
	}

	public ItemStack getLeggings() {
		return leggings;
	}

	public void setLeggings(ItemStack leggings) {
		this.leggings = leggings;
	}

	public ItemStack getBoots() {
		return boots;
	}

	public void setBoots(ItemStack boots) {
		this.boots = boots;
	}

	public String getHasArmorMessage() {
		return strHasArmor;
	}

	public void setHasArmorMessage(String strHasArmor) {
		this.strHasArmor = strHasArmor;
	}

}

