package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.ai.LookAtEntityGoal;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.managers.AttributeManager;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class SpawnEntitySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private List<LivingEntity> entities;
	private final Map<LivingEntity, EntityPulser> pulsers;

	private EntityData entityData;

	private ItemStack mainHandItem;
	private ItemStack offHandItem;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private ConfigData<Float> mainHandItemDropChance;
	private ConfigData<Float> offHandItemDropChance;
	private ConfigData<Float> helmetDropChance;
	private ConfigData<Float> chestplateDropChance;
	private ConfigData<Float> leggingsDropChance;
	private ConfigData<Float> bootsDropChance;
	private ConfigData<Float> yOffset;

	private ConfigData<Integer> duration;
	private ConfigData<Integer> fireTicks;
	private ConfigData<Integer> targetInterval;
	private final int spellInterval;

	private ConfigData<Double> targetRange;
	private ConfigData<Double> retargetRange;

	private String location;

	private Component nameplateText;

	private boolean noAI;
	private boolean gravity;
	private boolean removeAI;
	private boolean removeMob;
	private boolean useCasterName;
	private boolean addLookAtPlayerAI;
	private boolean allowSpawnInMidair;
	private boolean nameplateFormatting;
	private boolean cancelAttack;


	private Subspell attackSpell;
	private String attackSpellName;

	private Subspell intervalSpell;
	private String intervalSpellName;

	private List<PotionEffect> potionEffects;
	private Set<AttributeManager.AttributeInfo> attributes;

	private Random random = ThreadLocalRandom.current();

	private final EntityPulserTicker ticker;

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

		mainHandItemDropChance = getConfigDataFloat("main-hand-drop-chance", 0);
		offHandItemDropChance = getConfigDataFloat("off-hand-drop-chance", 0);
		helmetDropChance = getConfigDataFloat("helmet-drop-chance", 0);
		chestplateDropChance = getConfigDataFloat("chestplate-drop-chance", 0);
		leggingsDropChance = getConfigDataFloat("leggings-drop-chance", 0);
		bootsDropChance = getConfigDataFloat("boots-drop-chance", 0);
		yOffset = getConfigDataFloat("y-offset", 0.1F);

		duration = getConfigDataInt("duration", 0);
		fireTicks = getConfigDataInt("fire-ticks", 0);
		targetInterval = getConfigDataInt("target-interval", -1);
		spellInterval = getConfigInt("spell-interval", 20);

		targetRange = getConfigDataDouble("target-range", 20);
		retargetRange = getConfigDataDouble("retarget-range", 50);

		location = getConfigString("location", "target");
		nameplateText = Util.getMiniMessage(getConfigString("nameplate-text", null));

		noAI = getConfigBoolean("no-ai", false);
		gravity = getConfigBoolean("gravity", true);
		removeAI = getConfigBoolean("remove-ai", false);
		removeMob = getConfigBoolean("remove-mob", true);
		useCasterName = getConfigBoolean("use-caster-name", false);
		addLookAtPlayerAI = getConfigBoolean("add-look-at-player-ai", false);
		allowSpawnInMidair = getConfigBoolean("allow-spawn-in-midair", false);
		cancelAttack = getConfigBoolean("cancel-attack", true);

		attackSpellName = getConfigString("attack-spell", "");
		intervalSpellName = getConfigString("interval-spell", "");

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
					boolean ambient = split.length > 3 && (split[3].equalsIgnoreCase("ambient") || split[3].equalsIgnoreCase("true"));
					boolean hidden = split.length > 4 && (split[4].equalsIgnoreCase("hidden") || split[4].equalsIgnoreCase("true"));
					potionEffects.add(new PotionEffect(type, duration, strength, ambient, !hidden));
				} catch (Exception e) {
					MagicSpells.error("SpawnMonsterSpell '" + spellName + "' has an invalid potion effect defined: " + data);
				}
			}
		}

		pulsers = new HashMap<>();
		ticker = new EntityPulserTicker();
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
		intervalSpell = new Subspell(intervalSpellName);
		if (!intervalSpellName.isEmpty() && !intervalSpell.process()) {
			MagicSpells.error("SpawnEntitySpell '" + internalName + "' has an invalid interval-spell defined!");
			intervalSpell = null;
		}
	}

	@Override
	public void turnOff() {
		for (LivingEntity entity : entities) {
			entity.remove();
		}
		ticker.stop();
		entities.clear();
		pulsers.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			LivingEntity target = null;

			switch (location.toLowerCase()) {
				case "focus" -> {
					loc = getRandomLocationFrom(caster.getLocation(), 3);
					TargetInfo<LivingEntity> info = getTargetedEntity(caster, power, args);
					if (info.noTarget()) return noTarget(caster, args, info);
					target = info.target();
					power = info.power();
				}
				case "target" -> {
					Block block = getTargetedBlock(caster, power, args);
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

			if (loc == null) return noTarget(caster, args);
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
		if (!validTargetList.canTarget(caster, target)) return false;
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
		if (!validTargetList.canTarget(target)) return false;
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
		LivingEntity entity = (LivingEntity) entityData.spawn(
			loc.add(0.5, yOffset.get(caster, target, power, args), 0.5),
			e -> {
				LivingEntity preSpawned = (LivingEntity) e;
				prepMob(caster, target, preSpawned, power, args);

				int fireTicks = this.fireTicks.get(caster, target, power, args);
				if (fireTicks > 0) preSpawned.setFireTicks(fireTicks);
				if (potionEffects != null) preSpawned.addPotionEffects(potionEffects);

				// Apply attributes
				if (attributes != null) MagicSpells.getAttributeManager().addEntityAttributes(preSpawned, attributes);

				if (removeAI) {
					if (addLookAtPlayerAI) {
						if (preSpawned instanceof Mob mob) {
							MobGoals mobGoals = Bukkit.getMobGoals();
							mobGoals.removeAllGoals(mob);
							mobGoals.addGoal(mob, 1, new LookAtEntityGoal(mob, HumanEntity.class, 10.0F, 1.0F));
						}
					} else {
						preSpawned.setAI(false);
					}
				}
				if (noAI) preSpawned.setAI(false);

				if (target != null) MobUtil.setTarget(preSpawned, target);
			}
		);

		int targetInterval = this.targetInterval.get(caster, null, power, args);
		if (targetInterval > 0) new Targeter(caster, entity, power, args);

		int duration = this.duration.get(caster, target, power, args);
		if (attackSpell != null) {
			AttackMonitor monitor = new AttackMonitor(caster, entity, target, power, args);
			MagicSpells.registerEvents(monitor);

			MagicSpells.scheduleDelayedTask(() -> HandlerList.unregisterAll(monitor), duration > 0 ? duration : 12000);
		}

		if (caster != null) playSpellEffects(caster, source, entity, power, args);
		else playSpellEffects(source, entity, power, args);

		entities.add(entity);
		if (duration > 0) {
			MagicSpells.scheduleDelayedTask(() -> {
				entity.remove();
				entities.remove(entity);
			}, duration);
		}
		if (intervalSpell != null && spellInterval > 0) {
			ticker.start();
			pulsers.put(entity, new EntityPulser(caster, entity, power, args));
			if (duration > 0) {
				MagicSpells.scheduleDelayedTask(() -> {
					pulsers.remove(entity);
				}, duration);
			}
		}
	}

	private void prepMob(LivingEntity caster, LivingEntity target, Entity entity, float power, String[] args) {
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
				entityEquipment.setItemInMainHandDropChance(mainHandItemDropChance.get(caster, target, power, args) / 100f);
			}
			if (offHandItem != null && !BlockUtils.isAir(offHandItem.getType())) {
				entityEquipment.setItemInOffHand(offHandItem);
				entityEquipment.setItemInOffHandDropChance(offHandItemDropChance.get(caster, target, power, args) / 100f);
			}
		}

		final EntityEquipment equip = ((LivingEntity) entity).getEquipment();
		equip.setHelmet(helmet);
		equip.setChestplate(chestplate);
		equip.setLeggings(leggings);
		equip.setBoots(boots);
		if (!(entity instanceof ArmorStand)) {
			equip.setHelmetDropChance(helmetDropChance.get(caster, target, power, args) / 100f);
			equip.setChestplateDropChance(chestplateDropChance.get(caster, target, power, args) / 100f);
			equip.setLeggingsDropChance(leggingsDropChance.get(caster, target, power, args) / 100f);
			equip.setBootsDropChance(bootsDropChance.get(caster, target, power, args) / 100f);
		}

		if (useCasterName && caster != null) {
			if (caster instanceof Player player) entity.customName(player.displayName());
			else entity.customName(caster.name());
			entity.setCustomNameVisible(true);
		} else if (nameplateText != null) {
			entity.customName(nameplateText);
			entity.setCustomNameVisible(true);
		}
	}

	@EventHandler
	private void onEntityDeath(EntityDeathEvent event) {
		entityDeath(event.getEntity());
	}

	@EventHandler
	private void onExplode(EntityExplodeEvent event) {
		if (event.getEntity() instanceof LivingEntity) entityDeath((LivingEntity) event.getEntity());
	}

	private void entityDeath(LivingEntity entity) {
		if (!entities.contains(entity)) return;
		if (removeMob) {
			entities.remove(entity);
			if (pulsers.containsKey(entity)) pulsers.remove(entity);
		}
	}

	private class AttackMonitor implements Listener {

		private final LivingEntity caster;
		private final LivingEntity monster;
		private final String[] args;
		private final float power;
		private final boolean cancelAttack;

		private LivingEntity target;

		private AttackMonitor(LivingEntity caster, LivingEntity monster, LivingEntity target, float power, String[] args) {
			this.caster = caster;
			this.monster = monster;
			this.target = target;
			this.power = power;
			this.args = args;
			this.cancelAttack = SpawnEntitySpell.this.cancelAttack;
		}

		@EventHandler(ignoreCancelled = true)
		private void onDamage(EntityDamageByEntityEvent event) {
			if (attackSpell == null || attackSpell.getSpell() == null || attackSpell.getSpell().onCooldown(caster))
				return;

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
				event.setCancelled(this.cancelAttack);
			}
		}

		@EventHandler
		private void onTarget(EntityTargetEvent event) {
			if (event.getEntity() == monster) {
				if (!validTargetList.canTarget(caster, event.getTarget())) event.setCancelled(true);
				else if (event.getTarget() == null) retarget(null);
				else if (target != null && event.getTarget() != target) event.setTarget(target);
			}
		}

		@EventHandler
		private void onDeath(EntityDeathEvent event) {
			if (event.getEntity() != target) return;
			target = null;
			retarget(event.getEntity());
		}

		private void retarget(LivingEntity ignore) {
			LivingEntity t = null;

			double retargetRange = SpawnEntitySpell.this.retargetRange.get(caster, null, power, args);
			double r = retargetRange * retargetRange;

			for (Entity e : monster.getNearbyEntities(retargetRange, retargetRange, retargetRange)) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;
				if (e == ignore) continue;

				if (e instanceof Player p) {
					GameMode gamemode = p.getGameMode();
					if (gamemode == GameMode.CREATIVE || gamemode == GameMode.SPECTATOR) continue;
				}
				int distanceSquared = (int) monster.getLocation().distanceSquared(e.getLocation());
				if (distanceSquared < r) {
					r = distanceSquared;
					t = (LivingEntity) e;
					if (r < 25) break;
				}
			}
			target = t;
			if (t == null) return;
			MobUtil.setTarget(monster, t);
		}

	}

	private class Targeter implements Runnable {

		private final LivingEntity caster;
		private final LivingEntity entity;
		private final String[] args;
		private final float power;
		private final int taskId;

		private Targeter(LivingEntity caster, LivingEntity entity, float power, String[] args) {
			this.caster = caster;
			this.entity = entity;
			this.power = power;
			this.args = args;

			this.taskId = MagicSpells.scheduleRepeatingTask(this, 1, targetInterval.get(caster, null, power, args));
		}

		@Override
		public void run() {
			if (entity.isDead() || !entity.isValid()) {
				MagicSpells.cancelTask(taskId);
				return;
			}

			double targetRange = SpawnEntitySpell.this.targetRange.get(caster, null, power, args);
			List<Entity> list = entity.getNearbyEntities(targetRange, targetRange, targetRange);
			List<LivingEntity> targetable = new ArrayList<>();
			for (Entity e : list) {
				if (!(e instanceof LivingEntity)) continue;
				if (!validTargetList.canTarget(caster, e)) continue;
				targetable.add((LivingEntity) e);
			}

			if (targetable.isEmpty()) return;
			LivingEntity target = targetable.get(random.nextInt(targetable.size()));
			MobUtil.setTarget(entity, target);
		}

	}

	private class EntityPulser {

		private final LivingEntity caster;
		private final LivingEntity entity;
		private final float power;

		private EntityPulser(LivingEntity caster, LivingEntity entity, float power, String[] args) {
			this.caster = caster;
			this.entity = entity;
			this.power = power;
		}

		private boolean pulse() {
			if (entity != null) {
				if (entity.getWorld().isChunkLoaded(entity.getLocation().getBlockX() >> 4, entity.getLocation().getBlockZ() >> 4)) {
					activate();
				}
				return false;
			}
			return true;
		}

		private void activate() {
			LivingEntity target = null;
			if (entity instanceof Mob) target = ((Mob) entity).getTarget();

			if (intervalSpell.isTargetedEntityFromLocationSpell()) {
				if (target != null) intervalSpell.castAtEntityFromLocation(caster, entity.getLocation(), target, power);
			} else if (intervalSpell.isTargetedEntitySpell()) {
				if (target != null) intervalSpell.castAtEntity(caster, (LivingEntity) target, power);
			} else if (intervalSpell.isTargetedLocationSpell()) {
				if (target != null) intervalSpell.castAtLocation(caster, target.getLocation(), power);
			} else {
				intervalSpell.cast(entity, power);
			}
		}
	}


	private class EntityPulserTicker implements Runnable {

		private int taskId = -1;

		private void start() {
			if (taskId < 0) taskId = MagicSpells.scheduleRepeatingTask(this, 0, spellInterval);
		}

		private void stop() {
			if (taskId > 0) {
				MagicSpells.cancelTask(taskId);
				taskId = -1;
			}
		}

		@Override
		public void run() {
			for (Map.Entry<LivingEntity, EntityPulser> entry : new HashMap<>(pulsers).entrySet()) {
				boolean remove = entry.getValue().pulse();
				if (remove) pulsers.remove(entry.getKey());
			}
			if (pulsers.isEmpty()) stop();
		}

	}
}
