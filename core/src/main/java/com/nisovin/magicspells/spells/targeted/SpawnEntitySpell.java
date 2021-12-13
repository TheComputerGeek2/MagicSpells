package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import com.destroystokyo.paper.entity.ai.MobGoals;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.block.Block;
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

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.ai.LookAtEntityGoal;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.managers.AttributeManager;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class SpawnEntitySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private List<LivingEntity> entities;

	private EntityData entityData;

	private ItemStack mainHandItem;
	private ItemStack offHandItem;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private float mainHandItemDropChance;
	private float offHandItemDropChance;
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

	private Subspell attackSpell;
	private String attackSpellName;

	private List<PotionEffect> potionEffects;
	private Set<AttributeManager.AttributeInfo> attributes;

	private Random random = ThreadLocalRandom.current();
	
	// DEBUG INFO: level 2, invalid potion effect on internalname spell data
	public SpawnEntitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new ArrayList<>();

		ConfigurationSection entitySection = getConfigSection("entity");
		if (entitySection != null) entityData = new EntityData(entitySection);

		// Equipment
		MagicItem magicMainHandItem = MagicItems.getMagicItemFromString(getConfigString("main-hand", ""));
		if (magicMainHandItem != null) {
			mainHandItem = magicMainHandItem.getItemStack();
			if (mainHandItem != null && BlockUtils.isAir(mainHandItem.getType())) mainHandItem = null;
		}

		MagicItem magicOffHandItem = MagicItems.getMagicItemFromString(getConfigString("off-hand", ""));
		if (magicOffHandItem != null) {
			offHandItem = magicOffHandItem.getItemStack();
			if (offHandItem != null && BlockUtils.isAir(offHandItem.getType())) offHandItem = null;
		}

		MagicItem magicHelmetItem = MagicItems.getMagicItemFromString(getConfigString("helmet", ""));
		if (magicHelmetItem != null) {
			helmet = magicHelmetItem.getItemStack();
			if (helmet != null && BlockUtils.isAir(helmet.getType())) helmet = null;
		}

		MagicItem magicChestplateItem = MagicItems.getMagicItemFromString(getConfigString("chestplate", ""));
		if (magicChestplateItem != null) {
			chestplate = magicChestplateItem.getItemStack();
			if (chestplate != null && BlockUtils.isAir(chestplate.getType())) chestplate = null;
		}

		MagicItem magicLeggingsItem = MagicItems.getMagicItemFromString(getConfigString("leggings", ""));
		if (magicLeggingsItem != null) {
			leggings = magicLeggingsItem.getItemStack();
			if (leggings != null && BlockUtils.isAir(leggings.getType())) leggings = null;
		}

		MagicItem magicBootsItem = MagicItems.getMagicItemFromString(getConfigString("boots", ""));
		if (magicBootsItem != null) {
			boots = magicBootsItem.getItemStack();
			if (boots != null && BlockUtils.isAir(boots.getType())) boots = null;
		}

		if (mainHandItem != null) mainHandItem.setAmount(1);
		if (offHandItem != null) offHandItem.setAmount(1);
		if (helmet != null) helmet.setAmount(1);
		if (chestplate != null) chestplate.setAmount(1);
		if (leggings != null) leggings.setAmount(1);
		if (boots != null) boots.setAmount(1);

		mainHandItemDropChance = getConfigFloat("main-hand-drop-chance", 0) / 100F;
		offHandItemDropChance = getConfigFloat("off-hand-drop-chance", 0) / 100F;
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
		if (nameplateFormatting) nameplateText = Util.colorize(nameplateText);

		attackSpellName = getConfigString("attack-spell", "");

		// Attributes
		// - [AttributeName] [Number] [Operation]
		List<String> attributeList = getConfigStringList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty()) attributes = MagicSpells.getAttributeManager().getAttributes(attributeList);

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
					boolean ambient = split.length > 3 && split[3].equalsIgnoreCase("ambient");
					potionEffects.add(new PotionEffect(type, duration, strength, ambient));
				} catch (Exception e) {
					MagicSpells.error("SpawnMonsterSpell '" + spellName + "' has an invalid potion effect defined: " + data);
				}
			}
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (entityData == null || entityData.getEntityType() == null) {
			MagicSpells.error("SpawnEntitySpell '" + internalName + "' has an invalid entity defined!");
			entityData = null;
		}

		attackSpell = new Subspell(attackSpellName);
		if (!attackSpellName.isEmpty() && !attackSpell.process()) {
			MagicSpells.error("SpawnEntitySpell '" + internalName + "' has an invalid attack-spell defined!");
			attackSpell = null;
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
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			LivingEntity target = null;

			switch (location.toLowerCase()) {
				case "focus" -> {
					loc = getRandomLocationFrom(caster.getLocation(), 3);
					TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
					if (targetInfo == null) return noTarget(caster);
					target = targetInfo.getTarget();
					power = targetInfo.getPower();
				}
				case "target" -> {
					Block block = getTargetedBlock(caster, power);
					if (block != null && block.getType() != Material.AIR) {
						if (BlockUtils.isPathable(block)) loc = block.getLocation();
						else if (BlockUtils.isPathable(block.getRelative(BlockFace.UP))) loc = block.getLocation().add(0, 1, 0);
					}
				}
				case "caster" -> loc = caster.getLocation();
				case "random" -> loc = getRandomLocationFrom(caster.getLocation(), getRange(power));
				case "casteroffset" -> {
					String[] split = location.split(":");
					float y = Float.parseFloat(split[1]);
					loc = caster.getLocation().add(0, y, 0);
					loc.setPitch(0);
				}
			}
			
			if (loc == null) return noTarget(caster);
			spawnMob(caster, caster.getLocation(), loc, target, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		switch (location.toLowerCase()) {
			case "target" -> spawnMob(caster, caster.getLocation(), target, null, power, args);
			case "caster" -> spawnMob(caster, caster.getLocation(), caster.getLocation(), null, power, args);
			case "random" -> {
				Location loc = getRandomLocationFrom(target, getRange(power));
				if (loc != null) spawnMob(caster, caster.getLocation(), loc, null, power, args);
			}
			case "offset" -> {
				String[] split = location.split(":");
				float y = Float.parseFloat(split[1]);
				Location loc = target.clone().add(0, y, 0);
				loc.setPitch(0);
				spawnMob(caster, caster.getLocation(), loc, null, power, args);
			}
		}
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		switch (location.toLowerCase()) {
			case "target", "caster" -> spawnMob(null, target, target, null, power, args);
			case "random" -> {
				Location loc = getRandomLocationFrom(target, getRange(power));
				if (loc != null) spawnMob(null, target, loc, null, power, args);
			}
			case "offset" -> {
				String[] split = location.split(":");
				float y = Float.parseFloat(split[1]);
				Location loc = target.clone().add(0, y, 0);
				loc.setPitch(0);
				spawnMob(null, target, loc, null, power, args);
			}
		}
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power, String[] args) {
		if (location.equals("focus")) spawnMob(caster, from, from, target, power, args);
		else castAtLocation(caster, from, power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(caster, from, target, power, null);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power, String[] args) {
		if (location.equals("focus")) spawnMob(null, from, from, target, power, args);
		else castAtLocation(from, power, args);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(from, target, power, null);
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

	private void spawnMob(LivingEntity caster, Location source, Location loc, LivingEntity target, float power, String[] args) {
		if (entityData == null || entityData.getEntityType() == null) return;
		if (entityData.isPlayer()) return;

		loc.setYaw((float) (Math.random() * 360));
		LivingEntity entity = (LivingEntity) entityData.spawn(loc.add(0.5, yOffset, 0.5));

		prepMob(caster, entity);

		if (fireTicks > 0) entity.setFireTicks(fireTicks);
		if (potionEffects != null) entity.addPotionEffects(potionEffects);

		// Apply attributes
		if (attributes != null) MagicSpells.getAttributeManager().addEntityAttributes(entity, attributes);

		if (removeAI) {
			if (addLookAtPlayerAI) {
				if (entity instanceof Mob mob) {
					MobGoals mobGoals = Bukkit.getMobGoals();
					mobGoals.removeAllGoals(mob);
					mobGoals.addGoal(mob, 1, new LookAtEntityGoal(mob, HumanEntity.class, 10.0F, 1.0F));
				}
			} else {
				entity.setAI(false);
			}
		}
		if (noAI) entity.setAI(false);

		if (target != null) MobUtil.setTarget(entity, target);
		if (targetInterval > 0) new Targeter(caster, entity);

		if (attackSpell != null) {
			AttackMonitor monitor = new AttackMonitor(caster, entity, target, power);
			MagicSpells.registerEvents(monitor);
			MagicSpells.scheduleDelayedTask(() -> HandlerList.unregisterAll(monitor), duration > 0 ? duration : 12000);
		}

		if (caster != null) playSpellEffects(caster, entity);
		else playSpellEffects(source, entity);

		entities.add(entity);
		if (duration > 0) {
			MagicSpells.scheduleDelayedTask(() -> {
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

		if (entity instanceof Enderman) {
			if (mainHandItem != null && !BlockUtils.isAir(mainHandItem.getType())) {
				((Enderman) entity).setCarriedMaterial(mainHandItem.getData());
			}
		} else if (entity instanceof LivingEntity) {
			EntityEquipment entityEquipment = ((LivingEntity) entity).getEquipment();
			if (mainHandItem != null && !BlockUtils.isAir(mainHandItem.getType())) {
				entityEquipment.setItemInMainHand(mainHandItem);
				entityEquipment.setItemInMainHandDropChance(mainHandItemDropChance);
			}
			if (offHandItem != null && !BlockUtils.isAir(offHandItem.getType())) {
				entityEquipment.setItemInOffHand(offHandItem);
				entityEquipment.setItemInOffHandDropChance(offHandItemDropChance);
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
			if (caster instanceof Player player) entity.setCustomName(Util.getStringFromComponent(player.displayName()));
			else entity.setCustomName(caster.getName());
			entity.setCustomNameVisible(true);
		} else if (nameplateText != null && !nameplateText.isEmpty()) {
			entity.setCustomName(nameplateText);
			entity.setCustomNameVisible(true);
		}
	}

	@EventHandler
	private void onEntityDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
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
			if (attackSpell == null || attackSpell.getSpell() == null || attackSpell.getSpell().onCooldown(caster)) return;

			Entity damager = event.getDamager();
			if (damager instanceof Projectile) {
				if (((Projectile) damager).getShooter() != null && ((Projectile) damager).getShooter() instanceof Entity) {
					damager = (Entity) ((Projectile) damager).getShooter();
				}
			}
			if (event.getEntity() instanceof LivingEntity && damager == monster) {
				if (attackSpell.isTargetedEntityFromLocationSpell()) {
					attackSpell.castAtEntityFromLocation(caster, monster.getLocation(), (LivingEntity) event.getEntity(), power);
				} else if (attackSpell.isTargetedEntitySpell()) {
					attackSpell.castAtEntity(caster, (LivingEntity) event.getEntity(), power);
				} else if (attackSpell.isTargetedLocationSpell()) {
					attackSpell.castAtLocation(caster, event.getEntity().getLocation(), power);
				} else {
					attackSpell.cast(caster, power);
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
				
				if (e instanceof Player p) {
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
			MobUtil.setTarget(monster, t);
		}
		
	}
	
	private class Targeter implements Runnable {

		private LivingEntity caster;
		private LivingEntity entity;
		private int taskId;
		
		private Targeter(LivingEntity caster, LivingEntity entity) {
			this.caster = caster;
			this.entity = entity;
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 1, targetInterval);
		}
		
		@Override
		public void run() {
			if (entity.isDead() || !entity.isValid()) {
				MagicSpells.cancelTask(taskId);
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
			MobUtil.setTarget(entity, target);
		}
		
	}
	
}
