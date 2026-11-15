package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;

import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class SteedSpell extends InstantSpell {

	private final Map<UUID, Integer> mounted;

	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> hasChest;

	private final ConfigData<Double> jumpStrength;

	private final String strInvalidType;
	private String strAlreadyMounted;

	private final ConfigData<EntityType> type;

	private final ConfigData<Horse.Color> color;
	private final ConfigData<Horse.Style> style;

	private ItemStack armor;

	private Subspell spellOnSpawn;
	private final String spellOnSpawnName;

	public SteedSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		mounted = new HashMap<>();

		gravity = getConfigDataBoolean("gravity", true);
		hasChest = getConfigDataBoolean("has-chest", false);

		jumpStrength = getConfigDataDouble("jump-strength", 1);

		strInvalidType = getConfigString("str-invalid-type", "Invalid entity type.");
		spellOnSpawnName = getConfigString("spell-on-spawn", "");
		strAlreadyMounted = getConfigString("str-already-mounted", "You are already mounted!");

		type = getConfigDataEntityType("type", EntityType.HORSE);

		color = getConfigDataEnum("color", Horse.Color.class, null);
		style = getConfigDataEnum("style", Horse.Style.class, null);

		String armor = getConfigString("armor", null);
		if (armor != null) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(armor);
			if (magicItem != null) this.armor = magicItem.getItemStack();
			else MagicSpells.error("Invalid magic item '" + armor + "' in SteedSpell '" + internalName + "'.");
		}
	}

	@Override
	protected void initialize() {
		super.initialize();

		spellOnSpawn = initSubspell(spellOnSpawnName,
				"SteedSpell '" + internalName + "' has an invalid spell-on-spawn '" + spellOnSpawnName + "' defined!",
				true);
	}

	@Override
	public void turnOff() {
		for (UUID id : mounted.keySet()) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) continue;
			if (player.getVehicle() == null) continue;
			player.getVehicle().eject();
		}
		mounted.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (data.caster().getVehicle() != null) {
			sendMessage(strAlreadyMounted, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Class<? extends Entity> entityClass = type.get(data).getEntityClass();
		if (entityClass == null) {
			sendMessage(strInvalidType, data.caster(), data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Entity entity = data.caster().getWorld().spawn(data.caster().getLocation(), entityClass, e -> {
			e.setGravity(gravity.get(data));

			if (e instanceof AbstractHorse abstractHorse) {
				abstractHorse.setAdult();
				abstractHorse.setTamed(true);
				if (data.caster() instanceof AnimalTamer tamer) abstractHorse.setOwner(tamer);
				abstractHorse.setJumpStrength(jumpStrength.get(data));
				abstractHorse.getInventory().setSaddle(ItemStack.of(Material.SADDLE));

				if (abstractHorse instanceof Horse horse) {
					Horse.Color color = this.color.get(data);
					if (color != null) horse.setColor(color);
					else horse.setColor(Horse.Color.values()[random.nextInt(Horse.Color.values().length)]);

					Horse.Style style = this.style.get(data);
					if (style != null) horse.setStyle(style);
					else horse.setStyle(Horse.Style.values()[random.nextInt(Horse.Style.values().length)]);

					if (armor != null) horse.getInventory().setArmor(armor);
				} else if (abstractHorse instanceof ChestedHorse chestedHorse) {
					chestedHorse.setCarryingChest(hasChest.get(data));
				}
			}
		});
		entity.addPassenger(data.caster());

		if (spellOnSpawn != null) {
			if (entity instanceof LivingEntity le) spellOnSpawn.subcast(data.target(le));
			else spellOnSpawn.subcast(data.location(entity.getLocation()));
		}

		mounted.put(data.caster().getUniqueId(), entity.getEntityId());
		playSpellEffects(data.caster(), entity, entity instanceof LivingEntity le ? data.target(le) : data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler
	private void onDamage(EntityDamageEvent event) {
		if (mounted.containsValue(event.getEntity().getEntityId())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onInventoryInteract(InventoryClickEvent event) {
		Player pl = (Player) event.getWhoClicked();
		Inventory inv = event.getInventory();
		if (inv.getType() != InventoryType.CHEST) return;
		if (!mounted.containsKey(pl.getUniqueId())) return;
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onDismount(EntityDismountEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		if (!mounted.containsKey(player.getUniqueId())) return;
		mounted.remove(player.getUniqueId());
		event.getDismounted().remove();
		playSpellEffects(EffectPosition.DISABLED, player, new SpellData(player));
	}

	@EventHandler
	private void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (!mounted.containsKey(player.getUniqueId())) return;
		if (player.getVehicle() == null) return;
		mounted.remove(player.getUniqueId());
		Entity vehicle = player.getVehicle();
		vehicle.eject();
		vehicle.remove();
	}

	@EventHandler
	private void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (!mounted.containsKey(player.getUniqueId())) return;
		if (player.getVehicle() == null) return;
		mounted.remove(player.getUniqueId());
		Entity vehicle = player.getVehicle();
		vehicle.eject();
		vehicle.remove();
	}

	public Map<UUID, Integer> getMounted() {
		return mounted;
	}

	public String getStrAlreadyMounted() {
		return strAlreadyMounted;
	}

	public void setStrAlreadyMounted(String strAlreadyMounted) {
		this.strAlreadyMounted = strAlreadyMounted;
	}

	public ItemStack getArmor() {
		return armor;
	}

	public void setArmor(ItemStack armor) {
		this.armor = armor;
	}

}
