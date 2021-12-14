package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

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

import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class SteedSpell extends InstantSpell {

	private final Map<UUID, Integer> mounted;

	private final Random random;

	private boolean gravity;
	private boolean hasChest;

	private ConfigData<Double> jumpStrength;

	private String strAlreadyMounted;

	private EntityType type;

	private Horse.Color color;
	private Horse.Style style;

	private ItemStack armor;

	public SteedSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		mounted = new HashMap<>();

		random = ThreadLocalRandom.current();

		gravity = getConfigBoolean("gravity", true);
		hasChest = getConfigBoolean("has-chest", false);

		jumpStrength = getConfigDataDouble("jump-strength", 1);

		strAlreadyMounted = getConfigString("str-already-mounted", "You are already mounted!");

		type = MobUtil.getEntityType(getConfigString("type", "horse"));

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
			if (!a.isEmpty()) {
				MagicItem magicItem = MagicItems.getMagicItemFromString(a);
				if (magicItem != null) armor = magicItem.getItemStack();
			}
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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (caster.getVehicle() != null) {
				sendMessage(strAlreadyMounted, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Entity entity = caster.getWorld().spawnEntity(caster.getLocation(), type);
			entity.setGravity(gravity);

			if (entity instanceof AbstractHorse abstractHorse) {
				abstractHorse.setAdult();
				abstractHorse.setTamed(true);
				if (caster instanceof AnimalTamer tamer) abstractHorse.setOwner(tamer);
				abstractHorse.setJumpStrength(jumpStrength.get(caster, null, power, args));
				abstractHorse.getInventory().setSaddle(new ItemStack(Material.SADDLE));

				if (entity instanceof Horse horse) {
					if (color != null) horse.setColor(color);
					else horse.setColor(Horse.Color.values()[random.nextInt(Horse.Color.values().length)]);
					if (style != null) horse.setStyle(style);
					else horse.setStyle(Horse.Style.values()[random.nextInt(Horse.Style.values().length)]);
					if (armor != null) horse.getInventory().setArmor(armor);
				} else if (entity instanceof ChestedHorse chestedHorse) {
					chestedHorse.setCarryingChest(hasChest);
				}
			}

			entity.addPassenger(caster);
			playSpellEffects(EffectPosition.CASTER, caster);
			mounted.put(caster.getUniqueId(), entity.getEntityId());
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
		if (!(event.getEntity() instanceof Player player)) return;
		if (!mounted.containsKey(player.getUniqueId())) return;
		mounted.remove(player.getUniqueId());
		event.getDismounted().remove();
		playSpellEffects(EffectPosition.DISABLED, player);
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

	public boolean hasGravity() {
		return gravity;
	}

	public void setGravity(boolean gravity) {
		this.gravity = gravity;
	}

	public boolean hasChest() {
		return hasChest;
	}

	public void setHasChest(boolean hasChest) {
		this.hasChest = hasChest;
	}

	public String getStrAlreadyMounted() {
		return strAlreadyMounted;
	}

	public void setStrAlreadyMounted(String strAlreadyMounted) {
		this.strAlreadyMounted = strAlreadyMounted;
	}

	public EntityType getType() {
		return type;
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public Horse.Color getHorseColor() {
		return color;
	}

	public void setHorseColor(Horse.Color color) {
		this.color = color;
	}

	public Horse.Style getHorseStyle() {
		return style;
	}

	public void setHorseStyle(Horse.Style style) {
		this.style = style;
	}

	public ItemStack getArmor() {
		return armor;
	}

	public void setArmor(ItemStack armor) {
		this.armor = armor;
	}

}
