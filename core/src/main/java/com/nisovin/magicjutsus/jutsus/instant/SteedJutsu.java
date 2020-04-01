package com.nisovin.magicjutsus.jutsus.instant;

import java.util.Map;
import java.util.UUID;
import java.util.Random;
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
import org.spigotmc.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class SteedJutsu extends InstantJutsu {

	private Map<UUID, Integer> mounted;
	private Random random;

	private boolean gravity;
	private boolean hasChest;

	private double jumpStrength;

	private String strAlreadyMounted;

	private EntityType type;

	private Horse.Color color;
	private Horse.Style style;

	private ItemStack armor;

	public SteedJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		mounted = new HashMap<>();
		random = new Random();

		gravity = getConfigBoolean("gravity", true);
		hasChest = getConfigBoolean("has-chest", false);

		jumpStrength = getConfigDouble("jump-strength", 1);

		strAlreadyMounted = getConfigString("str-already-mounted", "You are already mounted!");

		type = Util.getEntityType(getConfigString("type", "horse"));

		if (type == EntityType.HORSE) {
			String c = getConfigString("color", "");
			String s = getConfigString("style", "");
			String a = getConfigString("armor", "");
			if (!c.isEmpty()) {
				for (Horse.Color h : Horse.Color.values()) {
					if (!h.name().equalsIgnoreCase(c)) continue;
					color = h;
					break;
				}
				if (color == null) DebugHandler.debugBadEnumValue(Horse.Color.class, c);
			}
			if (!s.isEmpty()) {
				for (Horse.Style h : Horse.Style.values()) {
					if (!h.name().equalsIgnoreCase(s)) continue;
					style = h;
					break;
				}
				if (style == null) DebugHandler.debugBadEnumValue(Horse.Style.class, s);
			}
			if (!a.isEmpty()) armor = Util.getItemStackFromString(a);
		}
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
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			if (livingEntity.getVehicle() != null) {
				sendMessage(strAlreadyMounted, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Entity entity = livingEntity.getWorld().spawnEntity(livingEntity.getLocation(), type);
			entity.setGravity(gravity);

			if (entity instanceof AbstractHorse) {
				((AbstractHorse) entity).setAdult();
				((AbstractHorse) entity).setTamed(true);
				if (livingEntity instanceof AnimalTamer) ((AbstractHorse) entity).setOwner((AnimalTamer) livingEntity);
				((AbstractHorse) entity).setJumpStrength(jumpStrength);
				((AbstractHorse) entity).getInventory().setSaddle(new ItemStack(Material.SADDLE));

				if (entity instanceof Horse) {
					if (color != null) ((Horse) entity).setColor(color);
					else ((Horse) entity).setColor(Horse.Color.values()[random.nextInt(Horse.Color.values().length)]);
					if (style != null) ((Horse) entity).setStyle(style);
					else ((Horse) entity).setStyle(Horse.Style.values()[random.nextInt(Horse.Style.values().length)]);
					if (armor != null) ((Horse) entity).getInventory().setArmor(armor);
				} else if (entity instanceof ChestedHorse) {
					((ChestedHorse) entity).setCarryingChest(hasChest);
				}
			}

			entity.addPassenger(livingEntity);
			playJutsuEffects(EffectPosition.CASTER, livingEntity);
			mounted.put(livingEntity.getUniqueId(), entity.getEntityId());
		}
		return PostCastAction.HANDLE_NORMALLY;
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
		if (!(event.getEntity() instanceof Player)) return;
		Player player = (Player) event.getEntity();
		if (!mounted.containsKey(player.getUniqueId())) return;
		mounted.remove(player.getUniqueId());
		event.getDismounted().remove();
		playJutsuEffects(EffectPosition.DISABLED, player);
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

}
