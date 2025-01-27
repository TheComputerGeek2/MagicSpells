package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import com.google.common.collect.Multimap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.block.BlockFace;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.RayTraceResult;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import net.kyori.adventure.text.Component;

import com.destroystokyo.paper.entity.ai.MobGoals;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.util.itemreader.AttributeHandler;
import com.nisovin.magicspells.util.ai.goals.LookAtEntityTypeGoal;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class SpawnEntitySpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private final Map<UUID, SpellData> entities;

	private EntityData entityData;

	private ItemStack mainHandItem;
	private ItemStack offHandItem;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;

	private final ConfigData<Float> mainHandItemDropChance;
	private final ConfigData<Float> offHandItemDropChance;
	private final ConfigData<Float> helmetDropChance;
	private final ConfigData<Float> chestplateDropChance;
	private final ConfigData<Float> leggingsDropChance;
	private final ConfigData<Float> bootsDropChance;
	private final ConfigData<Float> yOffset;

	private final ConfigData<Integer> duration;
	private final ConfigData<Integer> fireTicks;
	private final ConfigData<Integer> targetInterval;

	private final ConfigData<Double> targetRange;
	private final ConfigData<Double> retargetRange;

	private final ConfigData<String> location;
	private final ConfigData<Component> nameplateText;

	private final boolean removeMob;
	private final ConfigData<Boolean> noAI;
	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> removeAI;
	private final ConfigData<Boolean> setOwner;
	private final ConfigData<Boolean> cancelAttack;
	private final ConfigData<Boolean> invulnerable;
	private final ConfigData<Boolean> useCasterName;
	private final ConfigData<Boolean> centerLocation;
	private final ConfigData<Boolean> addLookAtPlayerAI;
	private final ConfigData<Boolean> allowSpawnInMidair;

	private Subspell attackSpell;
	private final String attackSpellName;

	private Subspell spellOnSpawn;
	private final String spellOnSpawnName;

	private Subspell spellOnDeath;
	private final String spellOnDeathName;

	private Subspell spellOnTarget;
	private final String spellOnTargetName;

	private List<PotionEffect> potionEffects;
	private Multimap<Attribute, AttributeModifier> attributes;

	// DEBUG INFO: level 2, invalid potion effect on internalName spell data
	public SpawnEntitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		entities = new HashMap<>();

		ConfigurationSection entitySection = getConfigSection("entity");
		if (entitySection != null) entityData = new EntityData(entitySection);

		// Equipment
		MagicItem magicMainHandItem = MagicItems.getMagicItemFromString(getConfigString("main-hand", ""));
		if (magicMainHandItem != null) {
			mainHandItem = magicMainHandItem.getItemStack();
			if (mainHandItem != null && mainHandItem.getType().isAir()) mainHandItem = null;
		}

		MagicItem magicOffHandItem = MagicItems.getMagicItemFromString(getConfigString("off-hand", ""));
		if (magicOffHandItem != null) {
			offHandItem = magicOffHandItem.getItemStack();
			if (offHandItem != null && offHandItem.getType().isAir()) offHandItem = null;
		}

		MagicItem magicHelmetItem = MagicItems.getMagicItemFromString(getConfigString("helmet", ""));
		if (magicHelmetItem != null) {
			helmet = magicHelmetItem.getItemStack();
			if (helmet != null && helmet.getType().isAir()) helmet = null;
		}

		MagicItem magicChestplateItem = MagicItems.getMagicItemFromString(getConfigString("chestplate", ""));
		if (magicChestplateItem != null) {
			chestplate = magicChestplateItem.getItemStack();
			if (chestplate != null && chestplate.getType().isAir()) chestplate = null;
		}

		MagicItem magicLeggingsItem = MagicItems.getMagicItemFromString(getConfigString("leggings", ""));
		if (magicLeggingsItem != null) {
			leggings = magicLeggingsItem.getItemStack();
			if (leggings != null && leggings.getType().isAir()) leggings = null;
		}

		MagicItem magicBootsItem = MagicItems.getMagicItemFromString(getConfigString("boots", ""));
		if (magicBootsItem != null) {
			boots = magicBootsItem.getItemStack();
			if (boots != null && boots.getType().isAir()) boots = null;
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

		targetRange = getConfigDataDouble("target-range", 20);
		retargetRange = getConfigDataDouble("retarget-range", 50);

		location = getConfigDataString("location", "target");
		nameplateText = getConfigDataComponent("nameplate-text", null);

		noAI = getConfigDataBoolean("no-ai", false);
		gravity = getConfigDataBoolean("gravity", true);
		removeMob = getConfigBoolean("remove-mob", true);
		setOwner = getConfigDataBoolean("set-owner", true);
		removeAI = getConfigDataBoolean("remove-ai", false);
		invulnerable = getConfigDataBoolean("invulnerable", false);
		useCasterName = getConfigDataBoolean("use-caster-name", false);
		centerLocation = getConfigDataBoolean("center-location", false);
		addLookAtPlayerAI = getConfigDataBoolean("add-look-at-player-ai", false);
		allowSpawnInMidair = getConfigDataBoolean("allow-spawn-in-midair", false);
		cancelAttack = getConfigDataBoolean("cancel-attack", true);

		attackSpellName = getConfigString("attack-spell", "");
		spellOnSpawnName = getConfigString("spell-on-spawn", "");
		spellOnDeathName = getConfigString("spell-on-death", "");
		spellOnTargetName = getConfigString("spell-on-target", "");

		List<?> attributeList = getConfigList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty())
			attributes = AttributeHandler.getAttributeModifiers(attributeList, internalName);

		List<String> list = getConfigStringList("potion-effects", null);
		if (list != null && !list.isEmpty()) {
			potionEffects = new ArrayList<>();

			String[] split;
			PotionEffectType type;
			int duration;
			int strength;
			boolean ambient;
			for (String data : list) {
				split = data.split(" ");
				try {
					type = PotionEffectHandler.getPotionEffectType(split[0]);
					if (type == null) throw new Exception("");

					duration = 600;
					if (split.length > 1) duration = Integer.parseInt(split[1]);

					strength = 0;
					if (split.length > 2) strength = Integer.parseInt(split[2]);

					ambient = split.length > 3 && split[3].equalsIgnoreCase("ambient");
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

		String prefix = "SpawnEntitySpell '" + internalName + "' has an invalid ";

		if (entityData == null || entityData.getEntityType() == null) {
			MagicSpells.error(prefix + "entity defined!");
			entityData = null;
		}

		spellOnSpawn = initSubspell(spellOnSpawnName,
				prefix + "spell-on-spawn: '" + spellOnSpawnName + "' defined!",
				true);
		spellOnDeath = initSubspell(spellOnDeathName,
				prefix + "spell-on-death: '" + spellOnDeathName + "' defined!",
				true);
		spellOnTarget = initSubspell(spellOnTargetName,
				prefix + "spell-on-target: '" + spellOnTargetName + "' defined!",
				true);
		attackSpell = initSubspell(attackSpellName,
				prefix + "attack-spell: '" + spellOnSpawnName + "' defined!",
				true);
	}

	@Override
	public void turnOff() {
		if (!removeMob) {
			entities.clear();
			return;
		}

		for (UUID uuid : new HashSet<>(entities.keySet())) {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity != null) entity.remove();
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		String spawnLocation = this.location.get(data).toLowerCase();

		if (spawnLocation.startsWith("casteroffset:")) {
			String[] split = spawnLocation.split(":", 2);

			float y;
			try {
				y = Float.parseFloat(split[1]);
			} catch (NumberFormatException e) {
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}

			Location location = data.caster().getLocation().add(0, y, 0);
			location.setPitch(0);

			SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, location);
			if (!targetEvent.callEvent()) return noTarget(targetEvent);
			data = targetEvent.getSpellData();
		} else {
			switch (spawnLocation) {
				case "caster" -> {
					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
					if (!targetEvent.callEvent()) return noTarget(targetEvent);
					data = targetEvent.getSpellData();
				}
				case "target" -> {
					RayTraceResult result = rayTraceBlocks(data);
					if (result == null) return noTarget(data);

					Block block = result.getHitBlock();
					if (!block.isPassable()) {
						Block upper = block.getRelative(BlockFace.UP);
						if (!upper.isPassable()) return noTarget(data);
						block = upper;
					}

					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, block.getLocation());
					if (!targetEvent.callEvent()) return noTarget(targetEvent);
					data = targetEvent.getSpellData();
				}
				case "focus" -> {
					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, getRandomLocationFrom(data.caster().getLocation(), data, 3));
					if (!targetEvent.callEvent()) return noTarget(targetEvent);
					data = targetEvent.getSpellData();

					TargetInfo<LivingEntity> info = getTargetedEntity(data);
					if (info.noTarget()) return noTarget(info);
					data = info.spellData();
				}
				case "random" -> {
					SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, getRandomLocationFrom(data.caster().getLocation(), data, getRange(data)));
					if (!targetEvent.callEvent()) return noTarget(targetEvent);
					data = targetEvent.getSpellData();
				}
				default -> {
					return new CastResult(PostCastAction.ALREADY_HANDLED, data);
				}
			}
		}

		if (!data.hasLocation()) return noTarget(data);

		return spawnMob(data.caster().getLocation(), data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		return castAtSpawnLocation(data, location.get(data).toLowerCase());
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		String loc = location.get(data).toLowerCase();
		return loc.equals("focus") ? spawnMob(data.location(), data) : castAtSpawnLocation(data, loc);
	}

	private CastResult castAtSpawnLocation(SpellData data, String spawnLocation) {
		if (spawnLocation.startsWith("offset:")) {
			String[] split = spawnLocation.split(":", 2);

			float y;
			try {
				y = Float.parseFloat(split[1]);
			} catch (NumberFormatException e) {
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}

			Location location = data.location().add(0, y, 0);
			location.setPitch(0);

			Location source = data.hasCaster() ? data.caster().getLocation() : data.location();
			data = data.location(location);

			return spawnMob(source, data);
		}

		return switch (spawnLocation) {
			case "caster" -> {
				if (!data.hasCaster()) yield new CastResult(PostCastAction.ALREADY_HANDLED, data);

				Location source = data.caster().getLocation();
				data = data.location(source);

				yield spawnMob(source, data);
			}
			case "target" -> {
				Location source = data.hasCaster() ? data.caster().getLocation() : data.location();
				yield spawnMob(source, data);
			}
			case "random" -> {
				Location source = data.location();

				Location location = getRandomLocationFrom(source, data, getRange(data));
				if (location == null) yield noTarget(data);
				data = data.location(location);

				yield spawnMob(source, data);
			}
			default -> new CastResult(PostCastAction.ALREADY_HANDLED, data);
		};
	}

	private Location getRandomLocationFrom(Location location, SpellData data, int range) {
		World world = location.getWorld();
		int x;
		int y;
		int z;
		int attempts = 0;
		Block block;
		Block block2;

		boolean allowSpawnInMidair = this.allowSpawnInMidair.get(data);
		while (attempts < 10) {
			x = location.getBlockX() + random.nextInt(range << 1) - range;
			y = location.getBlockY() + 2;
			z = location.getBlockZ() + random.nextInt(range << 1) - range;

			block = world.getBlockAt(x, y, z);
			if (block.getType() == Material.WATER) return block.getLocation();
			if (block.isPassable()) {
				if (allowSpawnInMidair) return block.getLocation();
				int c = 0;
				while (c < 5) {
					block2 = block.getRelative(BlockFace.DOWN);
					if (block2.isPassable()) block = block2;
					else return block.getLocation();
					c++;
				}
			}

			attempts++;
		}
		return null;
	}

	private CastResult spawnMob(Location source, SpellData data) {
		if (entityData == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Location loc = data.location();

		loc.add(0, yOffset.get(data), 0);
		data = data.location(loc);

		if (centerLocation.get(data)) {
			loc = loc.toBlockLocation().add(0.5, 0, 0.5);
			data = data.location(loc);
		}

		SpellData finalData = data;
		Entity entity = entityData.spawn(loc, data, mob -> prepMob(mob, finalData));
		if (entity == null) return noTarget(data);

		UUID uuid = entity.getUniqueId();
		entities.put(uuid, data);

		int duration = this.duration.get(data);

		if (duration > 0) {
			entity.getScheduler().runDelayed(
				MagicSpells.getInstance(),
				task -> {
					entity.remove();
					entities.remove(uuid);
				},
				() -> entities.remove(uuid),
				duration
			);
		}

		if (spellOnSpawn != null) {
			if (entity instanceof LivingEntity le) spellOnSpawn.subcast(data.retarget(le, null));
			else spellOnSpawn.subcast(data.retarget(null, entity.getLocation()));
		}

		if (entity instanceof Mob mob) {
			int targetInterval = this.targetInterval.get(data);
			if (targetInterval > 0) new Targeter(mob, data, targetInterval);

			if (attackSpell != null) {
				AttackMonitor monitor = new AttackMonitor(mob, data);
				MagicSpells.registerEvents(monitor);

				if (duration > 0) MagicSpells.scheduleDelayedTask(() -> HandlerList.unregisterAll(monitor), duration, source);
			}
		}

		if (data.hasCaster()) playSpellEffects(data.caster(), source, entity, data);
		else playSpellEffects(source, entity, data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void prepMob(Entity entity, SpellData data) {
		if (removeMob) entity.setPersistent(false);

		entity.setGravity(gravity.get(data));
		entity.setInvulnerable(invulnerable.get(data));

		int fireTicks = this.fireTicks.get(data);
		if (fireTicks > 0) entity.setFireTicks(fireTicks);

		if (useCasterName.get(data) && data.hasCaster()) {
			if (data.caster() instanceof Player player) entity.customName(player.displayName());
			else entity.customName(data.caster().name());
			entity.setCustomNameVisible(true);
		} else {
			Component nameplateText = this.nameplateText.get(data);
			if (nameplateText != null) {
				entity.customName(nameplateText);
				entity.setCustomNameVisible(true);
			}
		}

		if (setOwner.get(data) && entity instanceof Tameable tameable && tameable.isTamed() && data.caster() instanceof AnimalTamer tamer)
			tameable.setOwner(tamer);

		if (entity instanceof Enderman enderman && mainHandItem != null) {
			ItemMeta meta = mainHandItem.getItemMeta();

			if (meta instanceof BlockDataMeta blockMeta)
				enderman.setCarriedBlock(blockMeta.getBlockData(mainHandItem.getType()));
		}

		if (entity instanceof LivingEntity livingEntity) {
			EntityEquipment equipment = livingEntity.getEquipment();
			if (equipment != null) {
				equipment.setItemInMainHand(mainHandItem);
				equipment.setItemInOffHand(offHandItem);
				equipment.setHelmet(helmet);
				equipment.setChestplate(chestplate);
				equipment.setLeggings(leggings);
				equipment.setBoots(boots);

				if (livingEntity instanceof Mob) {
					equipment.setItemInMainHandDropChance(mainHandItemDropChance.get(data) / 100);
					equipment.setItemInOffHandDropChance(offHandItemDropChance.get(data) / 100);
					equipment.setHelmetDropChance(helmetDropChance.get(data) / 100);
					equipment.setChestplateDropChance(chestplateDropChance.get(data) / 100);
					equipment.setLeggingsDropChance(leggingsDropChance.get(data) / 100);
					equipment.setBootsDropChance(bootsDropChance.get(data) / 100);
				}
			}

			if (potionEffects != null) livingEntity.addPotionEffects(potionEffects);
			if (attributes != null) {
				attributes.asMap().forEach((attribute, modifiers) -> {
					AttributeInstance attributeInstance = livingEntity.getAttribute(attribute);
					if (attributeInstance == null) return;

					modifiers.forEach(attributeInstance::addModifier);
				});
			}

			if (removeAI.get(data)) {
				if (addLookAtPlayerAI.get(data) && livingEntity instanceof Mob mob) {
					MobGoals mobGoals = Bukkit.getMobGoals();

					mobGoals.removeAllGoals(mob);
					mobGoals.addGoal(mob, 1, new LookAtEntityTypeGoal(mob, data));
				} else livingEntity.setAI(false);
			}

			if (noAI.get(data)) livingEntity.setAI(false);
			if (livingEntity instanceof Mob mob && data.hasTarget()) mob.setTarget(data.target());
		}
	}

	@EventHandler
	private void onEntityDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();

		SpellData data = entities.remove(entity.getUniqueId());
		if (data == null) return;

		if (spellOnDeath != null) spellOnDeath.subcast(data.retarget(entity, null));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onEntityTarget(EntityTargetLivingEntityEvent event) {
		LivingEntity target = event.getTarget();
		if (target == null || !(event.getEntity() instanceof Mob mob)) return;

		SpellData data = entities.get(mob.getUniqueId());
		if (data == null) return;

		if (spellOnTarget != null && !target.equals(mob.getTarget()))
			spellOnTarget.subcast(data.retarget(target, mob.getLocation()));
	}

	private class AttackMonitor implements Listener {

		private final Mob mob;
		private final SpellData data;
		private final boolean cancelAttack;

		private AttackMonitor(Mob mob, SpellData data) {
			this.mob = mob;
			this.data = data.noTargeting();
			this.cancelAttack = SpawnEntitySpell.this.cancelAttack.get(data);
		}

		@EventHandler(ignoreCancelled = true)
		private void onDamage(EntityDamageByEntityEvent event) {
			if (!(event.getEntity() instanceof LivingEntity damaged)) return;

			DamageSource source = event.getDamageSource();
			if (!mob.equals(source.getCausingEntity()) && !mob.equals(source.getDirectEntity())) return;

			attackSpell.subcast(data.retarget(damaged, mob.getLocation()));
			event.setCancelled(cancelAttack);
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		private void onTarget(EntityTargetLivingEntityEvent event) {
			if (!mob.equals(event.getEntity())) return;

			LivingEntity target = event.getTarget();
			if (target == null || !validTargetList.canTarget(data.caster(), target)) {
				// No need to cast spell-on-target here as the SpawnEntitySpell#onEntityTarget takes care of it.
				target = findTarget(target);
				event.setTarget(target);
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		private void onDeath(EntityDeathEvent event) {
			LivingEntity entity = event.getEntity();
			if (mob.equals(entity)) {
				HandlerList.unregisterAll(this);
				return;
			}

			if (entity.equals(mob.getTarget())) {
				LivingEntity target = findTarget(entity);

				if (spellOnTarget != null && target != null)
					spellOnTarget.subcast(data.retarget(target, mob.getLocation()));

				mob.setTarget(target);
			}
		}

		@EventHandler
		public void onRemove(EntityRemoveFromWorldEvent event) {
			Entity entity = event.getEntity();
			if (entity.isValid()) return;

			if (entity.equals(mob.getTarget())) {
				LivingEntity target = findTarget(entity);

				if (spellOnTarget != null && target != null)
					spellOnTarget.subcast(data.retarget(target, mob.getLocation()));

				mob.setTarget(target);
			}
		}

		private LivingEntity findTarget(Entity ignore) {
			double range = retargetRange.get(data);
			double rangeSquared = range * range;

			for (LivingEntity target : mob.getLocation().getNearbyLivingEntities(range)) {
				if (!target.isValid()|| mob.equals(target) || target.equals(ignore) || !validTargetList.canTarget(data.caster(), target))
					continue;

				double distanceSquared = mob.getLocation().distanceSquared(target.getLocation());
				if (distanceSquared > rangeSquared) continue;

				return target;
			}

			return null;
		}

	}

	private class Targeter implements Runnable {

		private final Mob mob;

		private final SpellData data;
		private final ScheduledTask task;

		private Targeter(Mob mob, SpellData data, int interval) {
			this.data = data.noTargeting();
			this.mob = mob;

			this.task = MagicSpells.scheduleRepeatingTask(this, 1, interval, mob);
		}

		@Override
		public void run() {
			if (!mob.isValid()) {
				MagicSpells.cancelTask(task);
				return;
			}

			double range = targetRange.get(data);

			List<LivingEntity> targets = new ArrayList<>(mob.getLocation().getNearbyLivingEntities(range));
			targets.removeIf(target -> mob.equals(target) || !validTargetList.canTarget(data.caster(), target));
			if (targets.isEmpty()) return;

			LivingEntity target = targets.get(random.nextInt(targets.size()));
			if (!target.equals(mob.getTarget())) {
				if (spellOnTarget != null) spellOnTarget.subcast(data.retarget(target, mob.getLocation()));
				mob.setTarget(target);
			}
		}

	}

}
