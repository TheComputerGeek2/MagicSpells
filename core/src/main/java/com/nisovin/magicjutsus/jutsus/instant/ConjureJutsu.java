package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.InventoryUtil;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsus.command.TomeJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsus.command.ScrollJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class ConjureJutsu extends InstantJutsu implements TargetedEntityJutsu, TargetedLocationJutsu {

	private static ExpirationHandler expirationHandler = null;

	private Random rand = new Random();

	private int delay;
	private int pickupDelay;
	private int requiredSlot;
	private int preferredSlot;

	private double expiration;

	private float randomVelocity;

	private boolean offhand;
	private boolean autoEquip;
	private boolean stackExisting;
	private boolean itemHasGravity;
	private boolean addToInventory;
	private boolean addToEnderChest;
	private boolean ignoreMaxStackSize;
	private boolean powerAffectsChance;
	private boolean dropIfInventoryFull;
	private boolean powerAffectsQuantity;
	private boolean forceUpdateInventory;
	private boolean calculateDropsIndividually;

	private List<String> itemList;

	private ItemStack[] itemTypes;

	private double[] itemChances;

	private int[] itemMinQuantities;
	private int[] itemMaxQuantities;

	public ConjureJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		delay = getConfigInt("delay", -1);
		pickupDelay = getConfigInt("pickup-delay", 0);
		requiredSlot = getConfigInt("required-slot", -1);
		preferredSlot = getConfigInt("preferred-slot", -1);

		expiration = getConfigDouble("expiration", 0L);

		randomVelocity = getConfigFloat("random-velocity", 0F);

		offhand = getConfigBoolean("offhand", false);
		autoEquip = getConfigBoolean("auto-equip", false);
		stackExisting = getConfigBoolean("stack-existing", true);
		itemHasGravity = getConfigBoolean("gravity", true);
		addToInventory = getConfigBoolean("add-to-inventory", false);
		addToEnderChest = getConfigBoolean("add-to-ender-chest", false);
		ignoreMaxStackSize = getConfigBoolean("ignore-max-stack-size", false);
		powerAffectsChance = getConfigBoolean("power-affects-chance", true);
		dropIfInventoryFull = getConfigBoolean("drop-if-inventory-full", true);
		powerAffectsQuantity = getConfigBoolean("power-affects-quantity", false);
		forceUpdateInventory = getConfigBoolean("force-update-inventory", true);
		calculateDropsIndividually = getConfigBoolean("calculate-drops-individually", true);

		itemList = getConfigStringList("items", null);

		pickupDelay = Math.max(pickupDelay, 0);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (expiration > 0 && expirationHandler == null) expirationHandler = new ExpirationHandler();
		
		if (itemList != null && !itemList.isEmpty()) {
			itemTypes = new ItemStack[itemList.size()];
			itemMinQuantities = new int[itemList.size()];
			itemMaxQuantities = new int[itemList.size()];
			itemChances = new double[itemList.size()];
			
			for (int i = 0; i < itemList.size(); i++) {
				try {
					String[] data = Util.splitParams(itemList.get(i));
					String[] quantityData = data.length == 1 ? new String[]{ "1" } : data[1].split("-");
					
					if (data[0].startsWith("TOME:")) {
						String[] tomeData = data[0].split(":");
						TomeJutsu tomeJutsu = (TomeJutsu) MagicJutsus.getJutsuByInternalName(tomeData[1]);
						Jutsu jutsu = MagicJutsus.getJutsuByInternalName(tomeData[2]);
						int uses = tomeData.length > 3 ? Integer.parseInt(tomeData[3]) : -1;
						itemTypes[i] = tomeJutsu.createTome(jutsu, uses, null);
					} else if (data[0].startsWith("SCROLL:")) {
						String[] scrollData = data[0].split(":");
						ScrollJutsu scrollJutsu = (ScrollJutsu) MagicJutsus.getJutsuByInternalName(scrollData[1]);
						Jutsu jutsu = MagicJutsus.getJutsuByInternalName(scrollData[2]);
						int uses = scrollData.length > 3 ? Integer.parseInt(scrollData[3]) : -1;
						itemTypes[i] = scrollJutsu.createScroll(jutsu, uses, null);
					} else itemTypes[i] = Util.getItemStackFromString(data[0]);

					if (itemTypes[i] == null) {
						MagicJutsus.error("ConjureJutsu '" + internalName + "' has specified invalid item (e1): " + itemList.get(i));
						continue;
					}
					
					if (quantityData.length == 1) {
						itemMinQuantities[i] = Integer.parseInt(quantityData[0]);
						itemMaxQuantities[i] = itemMinQuantities[i];
					} else {
						itemMinQuantities[i] = Integer.parseInt(quantityData[0]);
						itemMaxQuantities[i] = Integer.parseInt(quantityData[1]);	
					}
					
					if (data.length > 2) {
						itemChances[i] = Double.parseDouble(data[2].replace("%", ""));
					} else {
						itemChances[i] = 100;
					}
				} catch (Exception e) {
					MagicJutsus.error("ConjureJutsu '" + internalName + "' has specified invalid item (e2): " + itemList.get(i));
					itemTypes[i] = null;
				}
			}
		}
		itemList = null;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (itemTypes == null) return PostCastAction.ALREADY_HANDLED;
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			if (delay >= 0) MagicJutsus.scheduleDelayedTask(() -> conjureItems((Player) livingEntity, power), delay);
			else conjureItems((Player) livingEntity, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
		
	}
	
	private void conjureItems(Player player, float power) {
		List<ItemStack> items = new ArrayList<>();
		if (calculateDropsIndividually) individual(items, power);
		else together(items, power);

		Location loc = player.getEyeLocation().add(player.getLocation().getDirection());
		boolean updateInv = false;
		for (ItemStack item : items) {
			boolean added = false;
			PlayerInventory inv = player.getInventory();
			if (autoEquip && item.getAmount() == 1) {
				if (item.getType().name().endsWith("HELMET") && InventoryUtil.isNothing(inv.getHelmet())) {
					inv.setHelmet(item);
					added = true;
				} else if (item.getType().name().endsWith("CHESTPLATE") && InventoryUtil.isNothing(inv.getChestplate())) {
					inv.setChestplate(item);
					added = true;
				} else if (item.getType().name().endsWith("LEGGINGS") && InventoryUtil.isNothing(inv.getLeggings())) {
					inv.setLeggings(item);
					added = true;
				} else if (item.getType().name().endsWith("BOOTS") && InventoryUtil.isNothing(inv.getBoots())) {
					inv.setBoots(item);
					added = true;
				}
			}

			if (!added) {
				if (addToEnderChest) added = Util.addToInventory(player.getEnderChest(), item, stackExisting, ignoreMaxStackSize);
				if (!added && addToInventory) {
					if (offhand) player.getEquipment().setItemInOffHand(item);
					else if (requiredSlot >= 0) {
						ItemStack old = inv.getItem(requiredSlot);
						if (old != null && old.isSimilar(item)) item.setAmount(item.getAmount() + old.getAmount());
						inv.setItem(requiredSlot, item);
						added = true;
						updateInv = true;
					} else if (preferredSlot >= 0 && InventoryUtil.isNothing(inv.getItem(preferredSlot))) {
						inv.setItem(preferredSlot, item);
						added = true;
						updateInv = true;
					} else if (preferredSlot >= 0 && inv.getItem(preferredSlot).isSimilar(item) && inv.getItem(preferredSlot).getAmount() + item.getAmount() < item.getType().getMaxStackSize()) {
						item.setAmount(item.getAmount() + inv.getItem(preferredSlot).getAmount());
						inv.setItem(preferredSlot, item);
						added = true;
						updateInv = true;
					} else if (!added) {
						added = Util.addToInventory(inv, item, stackExisting, ignoreMaxStackSize);
						if (added) updateInv = true;
					}
				}
				if (!added && (dropIfInventoryFull || !addToInventory)) {
					Item i = player.getWorld().dropItem(loc, item);
					i.setItemStack(item);
					i.setPickupDelay(pickupDelay);
					i.setGravity(itemHasGravity);
					playJutsuEffects(EffectPosition.SPECIAL, i);
				}
			} else updateInv = true;
		}

		if (updateInv && forceUpdateInventory) player.updateInventory();
		playJutsuEffects(EffectPosition.CASTER, player);
	}
	
	private void individual(List<ItemStack> items, float power) {
		for (int i = 0; i < itemTypes.length; i++) {
			double r = rand.nextDouble() * 100;
			if (powerAffectsChance) r = r / power;
			if (itemTypes[i] != null && r < itemChances[i]) addItem(i, items, power);
		}
	}
	
	private void together(List<ItemStack> items, float power) {
		double r = rand.nextDouble() * 100;
		double m = 0;
		for (int i = 0; i < itemTypes.length; i++) {
			if (itemTypes[i] != null && r < itemChances[i] + m) {
				addItem(i, items, power);
				return;
			} else m += itemChances[i];
		}
	}
	
	private void addItem(int i, List<ItemStack> items, float power) {
		int quant = itemMinQuantities[i];
		if (itemMaxQuantities[i] > itemMinQuantities[i]) quant = rand.nextInt(itemMaxQuantities[i] - itemMinQuantities[i]) + itemMinQuantities[i];
		if (powerAffectsQuantity) quant = Math.round(quant * power);
		if (quant > 0) {
			ItemStack item = itemTypes[i].clone();
			item.setAmount(quant);
			if (expiration > 0) expirationHandler.addExpiresLine(item, expiration);
			items.add(item);
		}
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(target, power);
	}
	
	@Override
	public boolean castAtLocation(Location target, float power) {
		List<ItemStack> items = new ArrayList<>();
		if (calculateDropsIndividually) individual(items, power);
		else together(items, power);

		Location loc = target.clone();
		if (!BlockUtils.isAir(loc.getBlock().getType())) loc.add(0, 1, 0);
		if (!BlockUtils.isAir(loc.getBlock().getType())) loc.add(0, 1, 0);
		for (ItemStack item : items) {
			Item dropped = loc.getWorld().dropItem(loc, item);
			dropped.setItemStack(item);
			dropped.setPickupDelay(pickupDelay);
			if (randomVelocity > 0) {
				Vector v = new Vector(rand.nextDouble() - 0.5, rand.nextDouble() / 2, rand.nextDouble() - 0.5);
				v.normalize().multiply(randomVelocity);
				dropped.setVelocity(v);
			}
			dropped.setGravity(itemHasGravity);
			playJutsuEffects(EffectPosition.SPECIAL, dropped);
		}
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(target, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Player)) {
			castAtLocation(target.getLocation(), power);
			return true;
		}
		conjureItems((Player) target, power);
		return true;
	}
	
	@Override
	public void turnOff() {
		expirationHandler = null;
	}
	
	private static class ExpirationHandler implements Listener {
		
		private final String expPrefix =  ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + "MSExp:";
		
		private ExpirationHandler() {
			MagicJutsus.registerEvents(this);
		}

		private void addExpiresLine(ItemStack item, double expireHours) {
			ItemMeta meta = item.getItemMeta();
			List<String> lore;
			if (meta.hasLore()) lore = new ArrayList<>(meta.getLore());
			else lore = new ArrayList<>();

			long expiresAt = System.currentTimeMillis() + (long) (expireHours * TimeUtil.MILLISECONDS_PER_HOUR);
			lore.add(getExpiresText(expiresAt));
			lore.add(expPrefix + expiresAt);
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		private void onJoin(PlayerJoinEvent event) {
			PlayerInventory inv = event.getPlayer().getInventory();
			processInventory(inv);
			ItemStack[] armor = inv.getArmorContents();
			processInventoryContents(armor);
			inv.setArmorContents(armor);
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		private void onInvOpen(InventoryOpenEvent event) {
			processInventory(event.getInventory());
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		private void onRightClick(PlayerInteractEvent event) {
			if (!event.hasItem()) return;
			ItemStack item = event.getPlayer().getEquipment().getItemInMainHand();
			ExpirationResult result = updateExpiresLineIfNeeded(item);
			if (result == ExpirationResult.EXPIRED) {
				event.getPlayer().getEquipment().setItemInMainHand(null);
				event.setCancelled(true);
			}
		}
		
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		private void onPickup(EntityPickupItemEvent event) {
			processItemDrop(event.getItem());
			if (event.getItem().isDead()) event.setCancelled(true);
		}
		
		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		private void onDrop(PlayerDropItemEvent event) {
			processItemDrop(event.getItemDrop());
		}
		
		@EventHandler(priority = EventPriority.LOWEST)
		private void onItemSpawn(ItemSpawnEvent event) {
			processItemDrop(event.getEntity());
		}
		
		private void processInventory(Inventory inv) {
			ItemStack[] contents = inv.getContents();
			processInventoryContents(contents);
			inv.setContents(contents);
		}
		
		private void processInventoryContents(ItemStack[] contents) {
			for (int i = 0; i < contents.length; i++) {
				ExpirationResult result = updateExpiresLineIfNeeded(contents[i]);
				if (result == ExpirationResult.EXPIRED) contents[i] = null;
			}
		}
		
		private boolean processItemDrop(Item drop) {
			ItemStack item = drop.getItemStack();
			ExpirationResult result = updateExpiresLineIfNeeded(item);
			if (result == ExpirationResult.UPDATE) drop.setItemStack(item);
			else if (result == ExpirationResult.EXPIRED) {
				drop.remove();
				return true;
			}
			return false;
		}
		
		private ExpirationResult updateExpiresLineIfNeeded(ItemStack item) {
			if (item == null) return ExpirationResult.NO_UPDATE;
			if (!item.hasItemMeta()) return ExpirationResult.NO_UPDATE;
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasLore()) return ExpirationResult.NO_UPDATE;
			ArrayList<String> lore = new ArrayList<>(meta.getLore());
			if (lore.size() < 2) return ExpirationResult.NO_UPDATE;
			String lastLine = lore.get(lore.size() - 1);
			if (!lastLine.startsWith(expPrefix)) return ExpirationResult.NO_UPDATE;
			long expiresAt = Long.parseLong(lastLine.replace(expPrefix, ""));
			if (expiresAt < System.currentTimeMillis()) return ExpirationResult.EXPIRED;
			lore.set(lore.size() - 2, getExpiresText(expiresAt));
			meta.setLore(lore);
			item.setItemMeta(meta);
			return ExpirationResult.UPDATE;
		}
	
		private String getExpiresText(long expiresAt) {
			if (expiresAt < System.currentTimeMillis()) return ChatColor.GRAY + "Expired";
			double hours = (expiresAt - System.currentTimeMillis()) / ((double) TimeUtil.MILLISECONDS_PER_HOUR);
			if (hours / 24 >= 15) return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + ((long) hours / TimeUtil.HOURS_PER_WEEK) + ChatColor.GRAY + " weeks";
			if (hours / 24 >= 3) return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + ((long) hours / TimeUtil.HOURS_PER_DAY) + ChatColor.GRAY + " days";
			if (hours >= 2) return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + (long) hours + ChatColor.GRAY + " hours";
			return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + '1' + ChatColor.GRAY + " hour";
		}		
		
	}
	
	private enum ExpirationResult {
		
		NO_UPDATE,
		UPDATE,
		EXPIRED
		
	}
	
}
