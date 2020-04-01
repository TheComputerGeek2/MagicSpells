package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.EntityData;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.util.managers.AttributeManager;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class SpawnEntityJutsu extends TargetedJutsu implements TargetedLocationJutsu, TargetedEntityFromLocationJutsu {

	private List<LivingEntity> entities;

	private EntityData entityData;

	private ItemStack holding;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private float holdingDropChance;
	private float helmetDropChance;
	private float chestplateDropChance;
	private float leggingsDropChance;
	private float bootsDropChance;
	private float yOffset;

	private int duration;
	private int fireTicks;
	private int targetInterval;

	private double targetRange;
	private double retargetRange;

	private String location;
	private String nameplateText;

	private boolean noAI;
	private boolean gravity;
	private boolean removeAI;
	private boolean removeMob;
	private boolean useCasterName;
	private boolean addLookAtPlayerAI;
	private boolean allowSpawnInMidair;
	private boolean nameplateFormatting;

	private Ninjutsu attackJutsu;
	private String attackJutsuName;

	private List<PotionEffect> potionEffects;
	private Set<AttributeManager.AttributeInfo> attributes;

	private Random random = new Random();
	
	// DEBUG INFO: level 2, invalid potion effect on internalname jutsu data
	public SpawnEntityJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		entities = new ArrayList<>();

		ConfigurationSection entitySection = getConfigSection("entity");
		if (entitySection != null) entityData = new EntityData(entitySection);

		holding = Util.getItemStackFromString(getConfigString("holding", "AIR"));
		helmet = Util.getItemStackFromString(getConfigString("helmet", "AIR"));
		chestplate = Util.getItemStackFromString(getConfigString("chestplate", "AIR"));
		leggings = Util.getItemStackFromString(getConfigString("leggings", "AIR"));
		boots = Util.getItemStackFromString(getConfigString("boots", "AIR"));

		if (holding != null && !BlockUtils.isAir(holding.getType())) holding.setAmount(1);
		if (helmet != null && !BlockUtils.isAir(helmet.getType())) helmet.setAmount(1);
		if (chestplate != null && !BlockUtils.isAir(chestplate.getType())) chestplate.setAmount(1);
		if (leggings != null && !BlockUtils.isAir(leggings.getType())) leggings.setAmount(1);
		if (boots != null && !BlockUtils.isAir(boots.getType())) boots.setAmount(1);

		holdingDropChance = getConfigFloat("holding-drop-chance", 0) / 100F;
		helmetDropChance = getConfigFloat("helmet-drop-chance", 0) / 100F;
		chestplateDropChance = getConfigFloat("chestplate-drop-chance", 0) / 100F;
		leggingsDropChance = getConfigFloat("leggings-drop-chance", 0) / 100F;
		bootsDropChance = getConfigFloat("boots-drop-chance", 0) / 100F;
		yOffset = getConfigFloat("y-offset", 0.1F);

		duration = getConfigInt("duration", 0);
		fireTicks = getConfigInt("fire-ticks", 0);
		targetInterval = getConfigInt("target-interval", -1);

		targetRange = getConfigDouble("target-range", 20);
		retargetRange = getConfigDouble("retarget-range", 50);

		location = getConfigString("location", "target");
		nameplateText = getConfigString("nameplate-text", "");

		noAI = getConfigBoolean("no-ai", false);
		gravity = getConfigBoolean("gravity", true);
		removeAI = getConfigBoolean("remove-ai", false);
		removeMob = getConfigBoolean("remove-mob", true);
		useCasterName = getConfigBoolean("use-caster-name", false);
		addLookAtPlayerAI = getConfigBoolean("add-look-at-player-ai", false);
		allowSpawnInMidair = getConfigBoolean("allow-spawn-in-midair", false);
		nameplateFormatting = getConfigBoolean("nameplate-formatting", false);
		if (nameplateFormatting) nameplateText = ChatColor.translateAlternateColorCodes('&', nameplateText);

		attackJutsuName = getConfigString("attack-jutsu", "");

		// Attributes
		// - [AttributeName] [Number] [Operation]
		List<String> attributeList = getConfigStringList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty()) attributes = MagicJutsus.getAttributeManager().getAttributes(attributeList);

		List<String> list = getConfigStringList("potion-effects", null);
		if (list != null && !list.isEmpty()) {
			potionEffects = new ArrayList<>();
			for (String data : list) {
				String[] split = data.split(" ");
				try {
					PotionEffectType type = Util.getPotionEffectType(split[0]);
					if (type == null) throw new Exception("");
					int duration = 600;
					if (split.length > 1) duration = Integer.parseInt(split[1]);
					int strength = 0;
					if (split.length > 2) strength = Integer.parseInt(split[2]);
					boolean ambient = false;
					if (split.length > 3 && split[3].equalsIgnoreCase("ambient")) ambient = true;
					potionEffects.add(new PotionEffect(type, duration, strength, ambient));
				} catch (Exception e) {
					MagicJutsus.error("SpawnMonsterJutsu '" + jutsuName + "' has an invalid potion effect defined: " + data);
				}
			}
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (entityData == null || entityData.getEntityType() == null) {
			MagicJutsus.error("SpawnEntityJutsu '" + internalName + "' has an invalid entity defined!");
			entityData = null;
		}

		attackJutsu = new Ninjutsu(attackJutsuName);
		if (!attackJutsuName.isEmpty() && !attackJutsu.process()) {
			MagicJutsus.error("SpawnEntityJutsu '" + internalName + "' has an invalid attack-jutsu defined!");
			attackJutsu = null;
		}
	}

	@Override
	public void turnOff() {
		for (LivingEntity entity : entities) {
			entity.remove();
		}

		entities.clear();
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location loc = null;
			LivingEntity target = null;
			
			if (location.equalsIgnoreCase("focus")) {
				loc = getRandomLocationFrom(livingEntity.getLocation(), 3);
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
				if (targetInfo == null) return noTarget(livingEntity);
				target = targetInfo.getTarget();
				power = targetInfo.getPower();
			} else if (location.equalsIgnoreCase("target")) {
				Block block = getTargetedBlock(livingEntity, power);
				if (block != null && block.getType() != Material.AIR) {
					if (BlockUtils.isPathable(block)) loc = block.getLocation();
					else if (BlockUtils.isPathable(block.getRelative(BlockFace.UP))) loc = block.getLocation().add(0, 1, 0);
				}
			} else if (location.equalsIgnoreCase("caster")) loc = livingEntity.getLocation();
			else if (location.equalsIgnoreCase("random")) loc = getRandomLocationFrom(livingEntity.getLocation(), getRange(power));
			else if (location.startsWith("casteroffset:")) {
				String[] split = location.split(":");
				float y = Float.parseFloat(split[1]);
				loc = livingEntity.getLocation().add(0, y, 0);
				loc.setPitch(0);
			}
			
			if (loc == null) return noTarget(livingEntity);
			spawnMob(livingEntity, livingEntity.getLocation(), loc, target, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (location.equalsIgnoreCase("target")) spawnMob(caster, caster.getLocation(), target, null, power);
		else if (location.equalsIgnoreCase("caster")) spawnMob(caster, caster.getLocation(), caster.getLocation(), null, power);
		else if (location.equalsIgnoreCase("random")) {
			Location loc = getRandomLocationFrom(target, getRange(power));
			if (loc != null) spawnMob(caster, caster.getLocation(), loc, null, power);
		} else if (location.startsWith("offset:")) {
			String[] split = location.split(":");
			float y = Float.parseFloat(split[1]);
			Location loc = target.clone().add(0, y, 0);
			loc.setPitch(0);
			spawnMob(caster, caster.getLocation(), loc, null, power);
		}
		return true;
	}
	
	@Override
	public boolean castAtLocation(Location target, float power) {
		if (location.equalsIgnoreCase("target")) spawnMob(null, target, target, null, power);
		else if (location.equalsIgnoreCase("caster")) spawnMob(null, target, target, null, power);
		else if (location.equalsIgnoreCase("random")) {
			Location loc = getRandomLocationFrom(target, getRange(power));
			if (loc != null) spawnMob(null, target, loc, null, power);
		} else if (location.startsWith("offset:")) {
			String[] split = location.split(":");
			float y = Float.parseFloat(split[1]);
			Location loc = target.clone().add(0, y, 0);
			loc.setPitch(0);
			spawnMob(null, target, loc, null, power);
		}
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (location.equals("focus")) spawnMob(caster, from, from, target, power);
		else castAtLocation(caster, from, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (location.equals("focus")) spawnMob(null, from, from, target, power);
		else castAtLocation(from, power);
		return true;
	}

	private Location getRandomLocationFrom(Location location, int range) {
		World world = location.getWorld();
		int x;
		int y;
		int z;
		int attempts = 0;
		Block block;
		Block block2;

		while (attempts < 10) {
			x = location.getBlockX() + random.nextInt(range << 1) - range;
			y = location.getBlockY() + 2;
			z = location.getBlockZ() + random.nextInt(range << 1) - range;

			block = world.getBlockAt(x, y, z);
			if (block.getType() == Material.WATER) return block.getLocation();
			if (BlockUtils.isPathable(block)) {
				if (allowSpawnInMidair) return block.getLocation();
				int c = 0;
				while (c < 5) {
					block2 = block.getRelative(BlockFace.DOWN);
					if (BlockUtils.isPathable(block2)) block = block2;
					else return block.getLocation();
					c++;
				}
			}

			attempts++;
		}
		return null;
	}

	private void spawnMob(LivingEntity caster, Location source, Location loc, LivingEntity target, float power) {
		if (entityData == null || entityData.getEntityType() == null) return;
		if (entityData.isPlayer()) return;

		loc.setYaw((float) (Math.random() * 360));
		LivingEntity entity = (LivingEntity) entityData.spawn(loc.add(0.5, yOffset, 0.5));

		prepMob(caster, entity);

		if (fireTicks > 0) entity.setFireTicks(fireTicks);
		if (potionEffects != null) entity.addPotionEffects(potionEffects);

		// Apply attributes
		if (attributes != null) MagicJutsus.getAttributeManager().addEntityAttributes(entity, attributes);

		if (removeAI) {
			entity.setAI(false);
			if (addLookAtPlayerAI) MagicJutsus.getVolatileCodeHandler().addAILookAtPlayer(entity, 10);
		}
		if (noAI) entity.setAI(false);

		if (target != null) MagicJutsus.getVolatileCodeHandler().setTarget(entity, target);
		if (targetInterval > 0) new Targeter(caster, entity);

		if (attackJutsu != null) {
			AttackMonitor monitor = new AttackMonitor(caster, entity, target, power);
			MagicJutsus.registerEvents(monitor);
			MagicJutsus.scheduleDelayedTask(() -> HandlerList.unregisterAll(monitor), duration > 0 ? duration : 12000);
		}

		if (caster != null) playJutsuEffects(caster, entity);
		else playJutsuEffects(source, entity);

		entities.add(entity);
		if (duration > 0) {
			MagicJutsus.scheduleDelayedTask(() -> {
				entity.remove();
				entities.remove(entity);
			}, duration);
		}
	}

	private void prepMob(LivingEntity caster, Entity entity) {
		entity.setGravity(gravity);

		if (entityData.isTamed() && entity instanceof Tameable && caster != null && caster instanceof Player) {
			((Tameable) entity).setOwner((Player) caster);
		}

		if (holding != null && holding.getType() != Material.AIR) {
			if (entity instanceof Enderman) ((Enderman) entity).setCarriedMaterial(holding.getData());
			else if (entity instanceof Skeleton || entity instanceof Zombie) {
				final EntityEquipment equip = ((LivingEntity)entity).getEquipment();
				equip.setItemInMainHand(holding.clone());
				equip.setItemInMainHandDropChance(holdingDropChance);
			}
		}

		final EntityEquipment equip = ((LivingEntity) entity).getEquipment();
		equip.setHelmet(helmet);
		equip.setChestplate(chestplate);
		equip.setLeggings(leggings);
		equip.setBoots(boots);
		if (!(entity instanceof ArmorStand)) {
			equip.setHelmetDropChance(helmetDropChance);
			equip.setChestplateDropChance(chestplateDropChance);
			equip.setLeggingsDropChance(leggingsDropChance);
			equip.setBootsDropChance(bootsDropChance);
		}

		if (useCasterName && caster != null) {
			if (caster instanceof Player) entity.setCustomName(((Player) caster).getDisplayName());
			else entity.setCustomName(caster.getName());
			entity.setCustomNameVisible(true);
		} else if (nameplateText != null && !nameplateText.isEmpty()) {
			entity.setCustomName(nameplateText);
			entity.setCustomNameVisible(true);
		}
	}

	@EventHandler
	private void onEntityDeath(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		if (!entities.contains(entity)) return;
		if (removeMob) entities.remove(entity);
	}

	private class AttackMonitor implements Listener {
		
		private LivingEntity caster;
		private LivingEntity monster;
		private LivingEntity target;
		private float power;

		private AttackMonitor(LivingEntity caster, LivingEntity monster, LivingEntity target, float power) {
			this.caster = caster;
			this.monster = monster;
			this.target = target;
			this.power = power;
		}
		
		@EventHandler(ignoreCancelled = true)
		private void onDamage(EntityDamageByEntityEvent event) {
			if (attackJutsu == null || attackJutsu.getJutsu() == null || attackJutsu.getJutsu().onCooldown(caster)) return;

			Entity damager = event.getDamager();
			if (damager instanceof Projectile) {
				if (((Projectile) damager).getShooter() != null && ((Projectile) damager).getShooter() instanceof Entity) {
					damager = (Entity) ((Projectile) damager).getShooter();
				}
			}
			if (event.getEntity() instanceof LivingEntity && damager == monster) {
				if (attackJutsu.isTargetedEntityFromLocationJutsu()) {
					attackJutsu.castAtEntityFromLocation(caster, monster.getLocation(), (LivingEntity) event.getEntity(), power);
				} else if (attackJutsu.isTargetedEntityJutsu()) {
					attackJutsu.castAtEntity(caster, (LivingEntity) event.getEntity(), power);
				} else if (attackJutsu.isTargetedLocationJutsu()) {
					attackJutsu.castAtLocation(caster, event.getEntity().getLocation(), power);
				} else {
					attackJutsu.cast(caster, power);
				}
				event.setCancelled(true);
			}
		}
		
		@EventHandler
		private void onTarget(EntityTargetEvent event) {
			if (event.getEntity() == monster && event.getTarget() == caster) event.setCancelled(true);
			else if (event.getTarget() == null) retarget(null);
			else if (target != null && event.getTarget() != target) event.setTarget(target);
		}
		
		@EventHandler
		private void onDeath(EntityDeathEvent event) {
			if (event.getEntity() != target) return;
			target = null;
			retarget(event.getEntity());
		}
		
		private void retarget(LivingEntity ignore) {
			LivingEntity t = null;
			double r = retargetRange * retargetRange;
			for (Entity e : monster.getNearbyEntities(retargetRange, retargetRange, retargetRange)) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;
				if (e == caster) continue;
				if (e == ignore) continue;
				
				if (e instanceof Player) {
					Player p = (Player)e;
					GameMode gamemode = p.getGameMode();
					if (gamemode == GameMode.CREATIVE || gamemode == GameMode.SPECTATOR) continue;
				}
				int distanceSquared = (int) monster.getLocation().distanceSquared(e.getLocation());
				if (distanceSquared < r) {
					r = distanceSquared;
					t = (LivingEntity)e;
					if (r < 25) break;
				}
			}
			target = t;
			if (t == null) return;
			MagicJutsus.getVolatileCodeHandler().setTarget(monster, t);
		}
		
	}
	
	private class Targeter implements Runnable {

		private LivingEntity caster;
		private LivingEntity entity;
		private int taskId;
		
		private Targeter(LivingEntity caster, LivingEntity entity) {
			this.caster = caster;
			this.entity = entity;
			this.taskId = MagicJutsus.scheduleRepeatingTask(this, 1, targetInterval);
		}
		
		@Override
		public void run() {
			if (entity.isDead() || !entity.isValid()) {
				MagicJutsus.cancelTask(taskId);
				return;
			}
			
			List<Entity> list = entity.getNearbyEntities(targetRange, targetRange, targetRange);
			List<LivingEntity> targetable = new ArrayList<>();
			for (Entity e : list) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;
				targetable.add((LivingEntity)e);
			}

			if (targetable.isEmpty()) return;
			LivingEntity target = targetable.get(random.nextInt(targetable.size()));
			MagicJutsus.getVolatileCodeHandler().setTarget(entity, target);
		}
		
	}
	
}
