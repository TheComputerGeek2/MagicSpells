package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;

import net.kyori.adventure.text.Component;

import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

import io.papermc.paper.datacomponent.item.Equippable;
import io.papermc.paper.datacomponent.DataComponentTypes;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.command.TomeSpell;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.command.ScrollSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ConjureSpell extends InstantSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static ExpirationHandler expirationHandler = null;

	private final ConfigData<Integer> delay;
	private final ConfigData<Integer> pickupDelay;
	private final ConfigData<Integer> requiredSlot;
	private final ConfigData<Integer> preferredSlot;

	private final ConfigData<Double> expiration;
	private final ConfigData<Double> randomVelocity;

	private final ConfigData<Boolean> offhand;
	private final ConfigData<Boolean> autoEquip;
	private final ConfigData<Boolean> stackExisting;
	private final ConfigData<Boolean> itemHasGravity;
	private final ConfigData<Boolean> addToInventory;
	private final ConfigData<Boolean> addToEnderChest;
	private final ConfigData<Boolean> ignoreMaxStackSize;
	private final ConfigData<Boolean> powerAffectsChance;
	private final ConfigData<Boolean> dropIfInventoryFull;
	private final ConfigData<Boolean> powerAffectsQuantity;
	private final ConfigData<Boolean> forceUpdateInventory;
	private final ConfigData<Boolean> calculateDropsIndividually;

	private List<String> itemList;

	private ItemStack[] itemTypes;

	private double[] itemChances;

	private int[] itemMinQuantities;
	private int[] itemMaxQuantities;

	public ConjureSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		delay = getConfigDataInt("delay", -1);
		pickupDelay = getConfigDataInt("pickup-delay", 0);
		requiredSlot = getConfigDataInt("required-slot", -1);
		preferredSlot = getConfigDataInt("preferred-slot", -1);

		expiration = getConfigDataDouble("expiration", 0);
		randomVelocity = getConfigDataDouble("random-velocity", 0);

		offhand = getConfigDataBoolean("offhand", false);
		autoEquip = getConfigDataBoolean("auto-equip", false);
		stackExisting = getConfigDataBoolean("stack-existing", true);
		itemHasGravity = getConfigDataBoolean("gravity", true);
		addToInventory = getConfigDataBoolean("add-to-inventory", false);
		addToEnderChest = getConfigDataBoolean("add-to-ender-chest", false);
		ignoreMaxStackSize = getConfigDataBoolean("ignore-max-stack-size", false);
		powerAffectsChance = getConfigDataBoolean("power-affects-chance", true);
		dropIfInventoryFull = getConfigDataBoolean("drop-if-inventory-full", true);
		powerAffectsQuantity = getConfigDataBoolean("power-affects-quantity", false);
		forceUpdateInventory = getConfigDataBoolean("force-update-inventory", true);
		calculateDropsIndividually = getConfigDataBoolean("calculate-drops-individually", true);

		itemList = getConfigStringList("items", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (expirationHandler == null) expirationHandler = new ExpirationHandler();

		if (itemList != null && !itemList.isEmpty()) {
			itemTypes = new ItemStack[itemList.size()];
			itemMinQuantities = new int[itemList.size()];
			itemMaxQuantities = new int[itemList.size()];
			itemChances = new double[itemList.size()];

			for (int i = 0; i < itemList.size(); i++) {
				try {
					String str = itemList.get(i);

					int brackets = 0;
					int closedBrackets = 0;
					for (int j = 0; j < str.length(); j++) {
						char ch = str.charAt(j);
						if (ch == '{') brackets++;
						if (ch == '}') closedBrackets++;
					}

					// checks if all brackets are properly closed
					if (brackets != closedBrackets) {
						MagicSpells.error("ConjureSpell '" + internalName + "' has an invalid item defined (e1): " + str);
						continue;
					}

					brackets = 0;
					closedBrackets = 0;

					String[] data = str.split(" ");
					String[] conjureData = null;

					StringBuilder itemData = new StringBuilder();

					for (int j = 0; j < data.length; j++) {
						for (char ch : data[j].toCharArray()) {
							if (ch == '{') brackets++;
							if (ch == '}') closedBrackets++;
						}

						itemData.append(data[j]).append(" ");
						// magicItemData is ready, add the conjureData
						if (brackets == closedBrackets) {
							int dataLeft = data.length - j - 1;
							conjureData = new String[dataLeft];

							// fill the conjureData array with stuff like amount and chance
							for (int d = 0; d < dataLeft; d++) {
								conjureData[d] = data[j + d + 1];
							}
							break;
						}
					}

					String strItemData = itemData.toString().trim();

					if (strItemData.startsWith("TOME:")) {
						String[] tomeData = strItemData.split(":");
						TomeSpell tomeSpell = (TomeSpell) MagicSpells.getSpellByInternalName(tomeData[1]);
						Spell spell = MagicSpells.getSpellByInternalName(tomeData[2]);
						int uses = tomeData.length > 3 ? Integer.parseInt(tomeData[3].trim()) : -1;
						itemTypes[i] = tomeSpell.createTome(spell, uses, null, SpellData.NULL);
					} else if (strItemData.startsWith("SCROLL:")) {
						String[] scrollData = strItemData.split(":");
						ScrollSpell scrollSpell = (ScrollSpell) MagicSpells.getSpellByInternalName(scrollData[1]);
						Spell spell = MagicSpells.getSpellByInternalName(scrollData[2]);
						int uses = scrollData.length > 3 ? Integer.parseInt(scrollData[3].trim()) : -1;
						itemTypes[i] = scrollSpell.createScroll(spell, uses, null);
					} else {
						MagicItem magicItem = MagicItems.getMagicItemFromString(strItemData);
						if (magicItem == null) continue;
						itemTypes[i] = magicItem.getItemStack();
					}

					int minAmount = 1;
					int maxAmount = 1;

					double chance = 100;

					// add default values if there aren't any specified
					if (conjureData == null) {
						itemMinQuantities[i] = minAmount;
						itemMaxQuantities[i] = maxAmount;
						itemChances[i] = chance;
						continue;
					}

					// parse minAmount, maxAmount
					if (conjureData.length >= 1) {
						String[] amount = conjureData[0].split("-");
						if (amount.length == 1) {
							minAmount = Integer.parseInt(amount[0].trim());
							maxAmount = minAmount;
						} else if (amount.length >= 2) {
							minAmount = Integer.parseInt(amount[0].trim());
							maxAmount = Integer.parseInt(amount[1].trim()) + 1;
						}
					}

					// parse chance
					if (conjureData.length >= 2) {
						chance = Double.parseDouble(conjureData[1].replace("%", "").trim());
					}

					itemMinQuantities[i] = minAmount;
					itemMaxQuantities[i] = maxAmount;
					itemChances[i] = chance;

				} catch (Exception e) {
					MagicSpells.error("ConjureSpell '" + internalName + "' has specified invalid item (e2): " + itemList.get(i));
					itemTypes[i] = null;
				}
			}
		}
		itemList = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (data.caster() instanceof Player caster) {
			int delay = this.delay.get(data);
			if (delay < 0) conjureItems(caster, data);
			else MagicSpells.scheduleDelayedTask(() -> conjureItems(caster, data), delay);

			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		return castAtLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Location loc = data.location();
		if (!loc.getBlock().getType().isAir()) loc.add(0, 1, 0);
		if (!loc.getBlock().getType().isAir()) loc.add(0, 1, 0);

		data = data.location(loc);
		SpellData finalData = data;

		int delay = this.delay.get(data);
		if (delay < 0) conjureItems(loc, data);
		else MagicSpells.scheduleDelayedTask(() -> conjureItems(loc, finalData), delay);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (data.target() instanceof Player target) {
			int delay = this.delay.get(data);
			if (delay < 0) conjureItems(target, data);
			else MagicSpells.scheduleDelayedTask(() -> conjureItems(target, data), delay);

			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		return castAtLocation(data.location(data.target().getLocation()));
	}

	private void conjureItems(Location loc, SpellData data) {
		List<ItemStack> items = new ArrayList<>();
		if (calculateDropsIndividually.get(data)) individual(items, data);
		else together(items, data);

		boolean itemHasGravity = this.itemHasGravity.get(data);
		int pickupDelay = Math.max(this.pickupDelay.get(data), 0);
		double randomVelocity = this.randomVelocity.get(data);

		for (ItemStack item : items) {
			Item dropped = loc.getWorld().dropItem(loc, item, it -> {
				it.setPickupDelay(pickupDelay);
				it.setGravity(itemHasGravity);

				if (randomVelocity > 0) {
					Vector v = new Vector(random.nextDouble() - 0.5, random.nextDouble() / 2, random.nextDouble() - 0.5);
					v.normalize().multiply(randomVelocity);
					it.setVelocity(v);
				}
			});

			playSpellEffects(EffectPosition.SPECIAL, dropped, data);
		}

		playSpellEffects(data);
	}

	@SuppressWarnings("UnstableApiUsage")
	private void conjureItems(Player player, SpellData data) {
		List<ItemStack> items = new ArrayList<>();
		if (calculateDropsIndividually.get(data)) individual(items, data);
		else together(items, data);

		boolean offhand = this.offhand.get(data);
		boolean autoEquip = this.autoEquip.get(data);
		boolean stackExisting = this.stackExisting.get(data);
		boolean addToInventory = this.addToInventory.get(data);
		boolean itemHasGravity = this.itemHasGravity.get(data);
		boolean addToEnderChest = this.addToEnderChest.get(data);
		boolean ignoreMaxStackSize = this.ignoreMaxStackSize.get(data);
		boolean dropIfInventoryFull = this.dropIfInventoryFull.get(data);
		boolean forceUpdateInventory = this.forceUpdateInventory.get(data);

		int pickupDelay = Math.max(this.pickupDelay.get(data), 0);
		int requiredSlot = this.requiredSlot.get(data);
		int preferredSlot = this.preferredSlot.get(data);

		double randomVelocity = this.randomVelocity.get(data);

		Location loc = player.getEyeLocation().add(player.getLocation().getDirection());
		data = data.location(loc);

		boolean updateInv = false;
		for (ItemStack item : items) {
			if (item == null) continue;

			boolean added = false;
			PlayerInventory inv = player.getInventory();
			if (autoEquip && item.getAmount() == 1) {
				Equippable equippable =	item.getData(DataComponentTypes.EQUIPPABLE);
				if (equippable != null && player.canUseEquipmentSlot(equippable.slot()) && inv.getItem(equippable.slot()).isEmpty()) {
					inv.setItem(equippable.slot(), item);
					added = true;
				}
			}

			if (!added) {
				if (addToEnderChest)
					added = Util.addToInventory(player.getEnderChest(), item, stackExisting, ignoreMaxStackSize);
				if (!added && addToInventory) {

					ItemStack preferredItem = null;
					if (preferredSlot >= 0) {
						preferredItem = inv.getItem(preferredSlot);
					}

					if (offhand) {
						player.getEquipment().setItemInOffHand(item);
						added = true;
						updateInv = true;
					}
					else if (requiredSlot >= 0) {
						ItemStack old = inv.getItem(requiredSlot);
						if (old != null && item.isSimilar(old)) item.setAmount(item.getAmount() + old.getAmount());
						inv.setItem(requiredSlot, item);
						added = true;
						updateInv = true;
					} else if (preferredSlot >= 0 && (preferredItem == null || preferredItem.isEmpty())) {
						inv.setItem(preferredSlot, item);
						added = true;
						updateInv = true;
					} else if (preferredSlot >= 0 && item.isSimilar(preferredItem) && preferredItem.getAmount() + item.getAmount() < item.getType().getMaxStackSize()) {
						item.setAmount(item.getAmount() + preferredItem.getAmount());
						inv.setItem(preferredSlot, item);
						added = true;
						updateInv = true;
					} else {
						added = Util.addToInventory(inv, item, stackExisting, ignoreMaxStackSize);
						if (added) updateInv = true;
					}
				}

				if (!added && (dropIfInventoryFull || !addToInventory)) {
					Item i = player.getWorld().dropItem(loc, item, it -> {
						it.setPickupDelay(pickupDelay);
						it.setGravity(itemHasGravity);

						if (randomVelocity > 0) {
							Vector v = new Vector(random.nextDouble() - 0.5, random.nextDouble() / 2, random.nextDouble() - 0.5);
							v.normalize().multiply(randomVelocity);
							it.setVelocity(v);
						}
					});

					playSpellEffects(EffectPosition.SPECIAL, i, data);
				}
			} else updateInv = true;
		}

		if (updateInv && forceUpdateInventory) player.updateInventory();
		playSpellEffects(EffectPosition.CASTER, player, data);
	}

	private void individual(List<ItemStack> items, SpellData data) {
		double expiration = this.expiration.get(data);
		boolean powerAffectsChance = this.powerAffectsChance.get(data);
		boolean powerAffectsQuantity = this.powerAffectsQuantity.get(data);

		for (int i = 0; i < itemTypes.length; i++) {
			double r = random.nextDouble() * 100;
			if (powerAffectsChance) r = r / data.power();
			if (itemTypes[i] != null && r < itemChances[i])
				addItem(i, items, data.power(), powerAffectsQuantity, expiration);
		}
	}

	private void together(List<ItemStack> items, SpellData data) {
		double expiration = this.expiration.get(data);
		boolean powerAffectsQuantity = this.powerAffectsQuantity.get(data);

		double r = random.nextDouble() * 100;
		double m = 0;
		for (int i = 0; i < itemTypes.length; i++) {
			if (itemTypes[i] != null && r < itemChances[i] + m) {
				addItem(i, items, data.power(), powerAffectsQuantity, expiration);
				return;
			} else m += itemChances[i];
		}
	}

	private void addItem(int i, List<ItemStack> items, float power, boolean powerAffectsQuantity, double expiration) {
		int quant = itemMinQuantities[i];
		if (itemMaxQuantities[i] > itemMinQuantities[i])
			quant = random.nextInt(itemMaxQuantities[i] - itemMinQuantities[i]) + itemMinQuantities[i];
		if (powerAffectsQuantity) quant = Math.round(quant * power);
		if (quant > 0) {
			ItemStack item = itemTypes[i].clone();
			item.setAmount(quant);
			if (expiration > 0) expirationHandler.addExpiresLine(item, expiration);
			items.add(item);
		}
	}

	@Override
	public void turnOff() {
		expirationHandler = null;
	}

	public List<String> getItemList() {
		return itemList;
	}

	public ItemStack[] getItemTypes() {
		return itemTypes;
	}

	public double[] getItemChances() {
		return itemChances;
	}

	public int[] getItemMinQuantities() {
		return itemMinQuantities;
	}

	public int[] getItemMaxQuantities() {
		return itemMaxQuantities;
	}

	private static class ExpirationHandler implements Listener {

		private final String expPrefix = ChatColor.BLACK + ChatColor.MAGIC.toString() + "MSExp:";

		private ExpirationHandler() {
			MagicSpells.registerEvents(this);
		}

		private void addExpiresLine(ItemStack item, double expireHours) {
			ItemMeta meta = item.getItemMeta();
			List<Component> lore = null;
			if (meta.hasLore()) lore = meta.lore();
			if (lore == null) lore = new ArrayList<>();

			long expiresAt = System.currentTimeMillis() + (long) (expireHours * TimeUtil.MILLISECONDS_PER_HOUR);
			lore.add(Util.getMiniMessage(getExpiresText(expiresAt)));
			lore.add(Util.getMiniMessage(expPrefix + expiresAt));
			meta.lore(lore);
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
			ItemStack item = event.getItem();
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

			List<Component> lore = meta.lore();
			if (lore == null || lore.size() < 2) return ExpirationResult.NO_UPDATE;

			String lastLine = Util.getStringFromComponent(lore.get(lore.size() - 1));
			if (!lastLine.startsWith(expPrefix)) return ExpirationResult.NO_UPDATE;

			long expiresAt = Long.parseLong(lastLine.replace(expPrefix, ""));
			if (expiresAt < System.currentTimeMillis()) return ExpirationResult.EXPIRED;

			lore.set(lore.size() - 2, Util.getMiniMessage(getExpiresText(expiresAt)));
			meta.lore(lore);
			item.setItemMeta(meta);
			return ExpirationResult.UPDATE;
		}

		private String getExpiresText(long expiresAt) {
			if (expiresAt < System.currentTimeMillis()) return ChatColor.GRAY + "Expired";
			double hours = (expiresAt - System.currentTimeMillis()) / ((double) TimeUtil.MILLISECONDS_PER_HOUR);
			if (hours / 24 >= 15)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + ((long) hours / TimeUtil.HOURS_PER_WEEK) + ChatColor.GRAY + " weeks";
			if (hours / 24 >= 3)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + ((long) hours / TimeUtil.HOURS_PER_DAY) + ChatColor.GRAY + " days";
			if (hours >= 2)
				return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + (long) hours + ChatColor.GRAY + " hours";
			return ChatColor.GRAY + "Expires in " + ChatColor.WHITE + '1' + ChatColor.GRAY + " hour";
		}

	}

	private enum ExpirationResult {

		NO_UPDATE,
		UPDATE,
		EXPIRED

	}

}
