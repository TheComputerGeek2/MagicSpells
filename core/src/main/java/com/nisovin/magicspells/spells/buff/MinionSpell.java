package com.nisovin.magicspells.spells.buff;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Multimap;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.event.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import io.papermc.paper.entity.TeleportFlag;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.handlers.PotionEffectHandler;
import com.nisovin.magicspells.util.itemreader.AttributeHandler;

public class MinionSpell extends BuffSpell {

	private final Map<UUID, Mob> minions = new HashMap<>();
	private final Map<LivingEntity, UUID> players = new HashMap<>();
	private final Map<EntityType, Integer> mobChances = new HashMap<>();
	private final Map<UUID, LivingEntity> targets = new ConcurrentHashMap<>();

	private ValidTargetList minionTargetList;

	private boolean preventCombust;
	private final ConfigData<Boolean> baby;
	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> powerAffectsHealth;

	private double followRange;
	private double followSpeed;
	private double maxDistance;
	private final ConfigData<Double> health;
	private final ConfigData<Double> maxHealth;
	private final ConfigData<Double> powerHealthFactor;

	private final ConfigData<Vector> spawnOffset;

	private String minionName;

	private final String spawnSpellName;
	private final String deathSpellName;
	private final String attackSpellName;

	private Subspell spawnSpell;
	private Subspell deathSpell;
	private Subspell attackSpell;

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

	private List<PotionEffect> potionEffects;

	private Multimap<Attribute, AttributeModifier> attributes;

	public MinionSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		// Formatted as <entity type> <chance>
		List<String> mobChanceList = getConfigStringList("mob-chances", new ArrayList<>());
		if (mobChanceList.isEmpty()) mobChanceList.add("Zombie 100");

		for (int i = 0; i < mobChanceList.size(); i++) {
			String[] splits = mobChanceList.get(i).split(" ");

			EntityType type = MobUtil.getEntityType(splits[0]);
			if (type == null) {
				MagicSpells.error("MinionSpell '" + internalName + "' has an invalid Entity Type specified in 'mob-chances' at index " + i + ": " + splits[0]);
				continue;
			}

			try {
				mobChances.put(type, Integer.parseInt(splits[1]));
			} catch (NumberFormatException ignored) {
				MagicSpells.error("MinionSpell '" + internalName + "' has an invalid chance value specified in 'mob-chances' at index " + i + ": " + splits[1]);
			}
		}

		// Potion effects
		List<String> potionEffectList = getConfigStringList("potion-effects", null);
		if (potionEffectList != null && !potionEffectList.isEmpty()) {
			potionEffects = new ArrayList<>();
			for (String potion : potionEffectList) {
				String[] split = potion.split(" ");
				try {
					PotionEffectType type = PotionEffectHandler.getPotionEffectType(split[0]);
					if (type == null) throw new Exception("");
					int duration = 600;
					if (split.length > 1) duration = Integer.parseInt(split[1]);
					int strength = 0;
					if (split.length > 2) strength = Integer.parseInt(split[2]);
					boolean ambient = split.length > 3 && split[3].equalsIgnoreCase("ambient");
					potionEffects.add(new PotionEffect(type, duration, strength, ambient));
				} catch (Exception e) {
					MagicSpells.error("MinionSpell '" + internalName + "' has an invalid potion effect string " + potion);
				}
			}
		}

		List<?> attributeList = getConfigList("attributes", null);
		if (attributeList != null && !attributeList.isEmpty())
			attributes = AttributeHandler.getAttributeModifiers(attributeList, internalName);

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

		// Minion target list
		minionTargetList = new ValidTargetList(this, getConfigStringList("minion-targets", null));

		mainHandItemDropChance = getConfigFloat("main-hand-drop-chance", 0) / 100F;
		offHandItemDropChance = getConfigFloat("off-hand-drop-chance", 0) / 100F;
		helmetDropChance = getConfigFloat("helmet-drop-chance", 0) / 100F;
		chestplateDropChance = getConfigFloat("chestplate-drop-chance", 0) / 100F;
		leggingsDropChance = getConfigFloat("leggings-drop-chance", 0) / 100F;
		bootsDropChance = getConfigFloat("boots-drop-chance", 0) / 100F;

		minionName = getConfigString("minion-name", "");
		spawnSpellName = getConfigString("spell-on-spawn", "");
		deathSpellName = getConfigString("spell-on-death", "");
		attackSpellName = getConfigString("spell-on-attack", "");

		spawnOffset = getConfigDataVector("spawn-offset", new Vector(1, 0, 0));

		health = getConfigDataDouble("health", 20);
		followSpeed = getConfigDouble("follow-speed", 1);
		maxDistance = getConfigDouble("max-distance", 30);
		maxHealth = getConfigDataDouble("max-health", 20);
		followRange = getConfigDouble("follow-range", 1.5) * -1;
		powerHealthFactor = getConfigDataDouble("power-health-factor", 1);

		baby = getConfigDataBoolean("baby", false);
		gravity = getConfigDataBoolean("gravity", true);
		preventCombust = getConfigBoolean("prevent-sun-burn", true);
		powerAffectsHealth = getConfigDataBoolean("power-affects-health", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "MinionSpell '" + internalName + "' has an invalid '%s' defined!";
		spawnSpell = initSubspell(spawnSpellName,
				error.formatted("spell-on-spawn"),
				true);
		attackSpell = initSubspell(attackSpellName,
				error.formatted("spell-on-attack"),
				true);
		deathSpell = initSubspell(deathSpellName,
				error.formatted("spell-on-death"),
				true);
	}

	@Override
	public boolean castBuff(SpellData data) {
		if (!(data.target() instanceof Player target)) return false;
		// Selecting the mob
		EntityType entityType = null;
		int total = mobChances.values().stream().mapToInt(Integer::intValue).sum();
		int selected = random.nextInt(total);

		int current = 0;
		for (Map.Entry<EntityType, Integer> entry : mobChances.entrySet()) {
			current += entry.getValue();
			if (selected >= current) continue;

			entityType = entry.getKey();
			break;
		}

		if (entityType == null) {
			MagicSpells.error("MinionSpell '" + internalName + "' is missing entity type!");
			return false;
		}
		Class<? extends Entity> entityClass = entityType.getEntityClass();
		if (entityClass == null || !Mob.class.isAssignableFrom(entityClass)) {
			MagicSpells.error("MinionSpell '" + internalName + "' can only summon mobs!");
			return false;
		}
		//noinspection unchecked
		Class<? extends Mob> mobClass = (Class<? extends Mob>) entityClass;

		// Spawn location
		Location loc = target.getLocation().clone();
		loc.setPitch(0);
		Vector spawnOffset = this.spawnOffset.get(data);
		loc.add(0, spawnOffset.getY(), 0);
		Util.applyRelativeOffset(loc, spawnOffset.setY(0));

		// Spawn creature
		Mob minion = target.getWorld().spawn(loc, mobClass, mob -> {
			prepareMob(mob, target, data);

			if (!(mob instanceof Ageable ageable)) return;
			if (baby.get(data)) ageable.setBaby();
			else ageable.setAdult();
		});

		if (spawnSpell != null) {
			SpellData castData = data.builder().caster(target).target(minion).location(minion.getLocation()).recipient(null).build();
			spawnSpell.subcast(castData);
		}

		minions.put(target.getUniqueId(), minion);
		players.put(minion, target.getUniqueId());
		return true;
	}

	private void prepareMob(Mob minion, Player target, SpellData data) {
		minion.setGravity(gravity.get(data));

		String customName = MagicSpells.doReplacements(minionName, target, data, "%c", target.getName());
		if (!customName.isEmpty()) {
			minion.customName(Util.getMiniMessage(customName));
			minion.setCustomNameVisible(true);
		}

		double powerHealthFactor = this.powerHealthFactor.get(data);
		double maxHealth = this.maxHealth.get(data);
		double health = this.health.get(data);
		if (powerAffectsHealth.get(data)) {
			Util.setMaxHealth(minion, maxHealth * data.power() * powerHealthFactor);
			minion.setHealth(health * data.power() * powerHealthFactor);
		} else {
			Util.setMaxHealth(minion, maxHealth);
			minion.setHealth(health);
		}

		if (potionEffects != null) minion.addPotionEffects(potionEffects);

		if (attributes != null) {
			attributes.asMap().forEach((attribute, modifiers) -> {
				AttributeInstance attributeInstance = minion.getAttribute(attribute);
				if (attributeInstance == null) return;

				modifiers.forEach(attributeInstance::addModifier);
			});
		}

		EntityEquipment eq = minion.getEquipment();
		if (mainHandItem != null) eq.setItemInMainHand(mainHandItem.clone());
		if (offHandItem != null) eq.setItemInOffHand(offHandItem.clone());
		if (helmet != null) eq.setHelmet(helmet.clone());
		if (chestplate != null) eq.setChestplate(chestplate.clone());
		if (leggings != null) eq.setLeggings(leggings.clone());
		if (boots != null) eq.setBoots(boots.clone());

		eq.setItemInMainHandDropChance(mainHandItemDropChance);
		eq.setItemInOffHandDropChance(offHandItemDropChance);
		eq.setHelmetDropChance(helmetDropChance);
		eq.setChestplateDropChance(chestplateDropChance);
		eq.setLeggingsDropChance(leggingsDropChance);
		eq.setBootsDropChance(bootsDropChance);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return minions.containsKey(entity.getUniqueId());
	}

	public boolean isMinion(LivingEntity entity) {
		return minions.values().stream().anyMatch(mob -> mob.getUniqueId().equals(entity.getUniqueId()));
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		LivingEntity minion = minions.remove(entity.getUniqueId());
		if (minion != null && !minion.isDead()) minion.remove();

		players.remove(minion);
		targets.remove(entity.getUniqueId());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return minions.keySet();
	}

	@EventHandler
	public void onEntityTarget(EntityTargetEvent e) {
		if (minions.isEmpty() || e.getTarget() == null) return;
		if (!(e.getEntity() instanceof LivingEntity minion)) return;
		if (!isMinion(minion)) return;

		Player pl = Bukkit.getPlayer(players.get(minion));
		if (pl == null) return;

		if (!targets.containsKey(pl.getUniqueId()) || !targets.get(pl.getUniqueId()).isValid()) {
			e.setCancelled(true);
			return;
		}

		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		LivingEntity target = targets.get(pl.getUniqueId());

		// Minion is targeting the right entity
		if (e.getTarget().equals(target)) return;

		// If its dead or owner/minion is the target, cancel and return
		if (target.isDead() || target.equals(pl) || target.equals(minion)) {
			e.setCancelled(true);
			return;
		}

		// Set the correct target
		e.setTarget(target);
		addUseAndChargeCost(pl);
		MobUtil.setTarget(minion, target);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		LivingEntity damager = null;
		// Check if the minion is a projectile, if so, get the shooter, otherwise get the living entity damager.
		if (e.getDamager() instanceof LivingEntity damagerEntity) damager = damagerEntity;
		else if (e.getDamager() instanceof Projectile projectile) {
			if (projectile.getShooter() instanceof LivingEntity shooter) damager = shooter;
		}
		// Check if the damager is alive.
		if (damager == null || !damager.isValid()) return;

		if (!(e.getEntity() instanceof LivingEntity entity) || !entity.isValid()) return;
		// Check if the damaged entity is a player
		if (entity instanceof Player pl && isActive(pl)) {
			// If a Minion tries to attack his owner, cancel the damage and stop the minion
			if (minions.get(pl.getUniqueId()).equals(damager)) {
				targets.remove(pl.getUniqueId());
				MobUtil.setTarget(minions.get(pl.getUniqueId()), null);
				e.setCancelled(true);
				return;
			}

			// If distance between previous target and the player is less than between the new target, the minion will keep focusing the previous target
			LivingEntity previousTarget = targets.get(pl.getUniqueId());
			if (previousTarget != null && previousTarget.getWorld().equals(pl.getWorld()) && pl.getLocation().distanceSquared(previousTarget.getLocation()) < pl.getLocation().distanceSquared(damager.getLocation())) return;

			targets.put(pl.getUniqueId(), damager);
			MobUtil.setTarget(minions.get(pl.getUniqueId()), damager);
			return;
		}

		// Check if the damaged entity is a minion
		if (isMinion(entity)) {
			Player owner = Bukkit.getPlayer(players.get(entity));
			if (owner == null || !owner.isOnline() || !owner.isValid()) return;
			// Owner cant damage his minion
			if (damager.equals(owner)) {
				e.setCancelled(true);
				return;
			}

			// If the minion is far away from the owner, forget about attacking
			if (owner.getWorld().equals(entity.getWorld()) && owner.getLocation().distanceSquared(entity.getLocation()) > maxDistance * maxDistance) return;

			// If the owner has no targets and someone will attack the minion, he will strike back
			if (targets.get(owner.getUniqueId()) == null || targets.get(owner.getUniqueId()).isDead() || !targets.get(owner.getUniqueId()).isValid()) {
				targets.put(owner.getUniqueId(), damager);
				MobUtil.setTarget(entity, damager);
			}
		}

		if (damager instanceof Player pl) {
			// Check if player's damaged target is his minion, if it's not, make him attack your target
			if (!isActive(pl)) return;
			for (BuffSpell buff : MagicSpells.getBuffManager().getActiveBuffs(pl)) {
				if (!(buff instanceof MinionSpell minionBuff)) continue;
				if (entity.equals(minionBuff.minions.get(pl.getUniqueId()))) {
					e.setCancelled(true);
					return;
				}
			}

			if (isMinion(entity) && minions.get(pl.getUniqueId()).equals(entity)) {
				e.setCancelled(true);
				return;
			}

			// Check if the entity can be targeted by the minion
			if (!minionTargetList.canTarget(pl, entity)) return;

			targets.put(pl.getUniqueId(), entity);
			MobUtil.setTarget(minions.get(pl.getUniqueId()), entity);
			addUseAndChargeCost(pl);

		}

		if (isMinion(damager)) {
			Player owner = Bukkit.getPlayer(players.get(damager));
			if (owner == null || !owner.isOnline() || !owner.isValid()) return;

			if (attackSpell != null) attackSpell.subcast(new SpellData(owner, entity, damager.getLocation(), 1f, null));
		}

		// The target died, the minion will follow his owner
		if (targets.containsValue(entity) && entity.getHealth() - e.getFinalDamage() <= 0) {
			for (UUID id : targets.keySet()) {
				if (!targets.get(id).equals(entity)) continue;
				Player pl = Bukkit.getPlayer(id);

				if (pl == null || !pl.isValid() || !pl.isOnline()) continue;

				targets.remove(id);
				MobUtil.setTarget(minions.get(id), null);

				Location loc = pl.getLocation().clone();
				loc.add(loc.getDirection().setY(0).normalize().multiply(followRange));
				minions.get(pl.getUniqueId()).getPathfinder().moveTo(loc, followSpeed);
			}
		}
	}

	// Owner cant damage his minion with spells
	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent e) {
		if (!isActive(e.getCaster())) return;
		if (!e.getSpell().isBeneficial() && e.getTarget().equals(minions.get(e.getCaster().getUniqueId()))) e.setCancelled(true);
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		LivingEntity minion = event.getEntity();
		if (!isMinion(minion)) return;
		EntityEquipment eq = minion.getEquipment();
		List<ItemStack> newDrops = new ArrayList<>();
		if (eq != null) {
			for (ItemStack drop : event.getDrops()) {
				for (int i = 0; i < eq.getArmorContents().length; i++) {
					if (drop.equals(eq.getArmorContents()[i])) newDrops.add(drop);
				}
				if (drop.equals(mainHandItem) || drop.equals(offHandItem)) newDrops.add(drop);
			}
		}

		// Clear all the regular drops
		event.getDrops().clear();
		event.setDroppedExp(0);

		// Apply new drops
		for (ItemStack item : newDrops) {
			event.getDrops().add(item);
		}
		Player pl = Bukkit.getPlayer(players.get(event.getEntity()));
		if (pl == null || !pl.isValid() || !pl.isOnline()) return;

		if (deathSpell != null) deathSpell.subcast(new SpellData(pl, minion, minion.getLocation(), 1, null));

		turnOff(pl);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getFrom().getBlock().equals(e.getTo().getBlock())) return;
		Player pl = e.getPlayer();
		if (!isActive(pl)) return;
		Mob minion = minions.get(pl.getUniqueId());

		if ((pl.getWorld().equals(minion.getWorld()) && pl.getLocation().distanceSquared(minion.getLocation()) > maxDistance * maxDistance) || targets.get(pl.getUniqueId()) == null || !targets.containsKey(pl.getUniqueId())) {
			// The minion has a target, but he is far away from his owner, remove his current target
			if (targets.get(pl.getUniqueId()) != null) {
				targets.remove(pl.getUniqueId());
				MobUtil.setTarget(minion, null);
			}

			// The distance between minion and his owner is greater that the defined max distance or the minion has no targets, he will follow his owner
			Location loc = pl.getLocation().clone();
			loc.add(loc.getDirection().setY(0).normalize().multiply(followRange));
			minion.getPathfinder().moveTo(loc, followSpeed);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent e) {
		if (!preventCombust || !(e.getEntity() instanceof LivingEntity le) || !isMinion(le)) return;
		e.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityUnload(ChunkUnloadEvent event) {
		List<Entity> entities = Arrays.asList(event.getChunk().getEntities());
		Player owner;

		for (LivingEntity minion : minions.values()) {
			if (!entities.contains(minion)) continue;

			owner = Bukkit.getPlayer(players.get(minion));
			if (owner == null) continue;
			if (!owner.isOnline()) continue;
			if (owner.isDead()) continue;

			minion.teleport(owner.getLocation(), TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
		}
	}

	public Map<UUID, Mob> getMinions() {
		return minions;
	}

	public Map<LivingEntity, UUID> getPlayers() {
		return players;
	}

	public Map<UUID, LivingEntity> getTargets() {
		return targets;
	}

	public List<PotionEffect> getPotionEffects() {
		return potionEffects;
	}

	public Multimap<Attribute, AttributeModifier> getAttributes() {
		return attributes;
	}

	public ValidTargetList getMinionTargetList() {
		return minionTargetList;
	}

	public void setMinionTargetList(ValidTargetList minionTargetList) {
		this.minionTargetList = minionTargetList;
	}

	public boolean shouldPreventCombust() {
		return preventCombust;
	}

	public void setPreventCombust(boolean preventCombust) {
		this.preventCombust = preventCombust;
	}

	public String getMinionName() {
		return minionName;
	}

	public void setMinionName(String minionName) {
		this.minionName = minionName;
	}

	public double getFollowRange() {
		return followRange;
	}

	public void setFollowRange(double followRange) {
		this.followRange = followRange;
	}

	public double getFollowSpeed() {
		return followSpeed;
	}

	public void setFollowSpeed(float followSpeed) {
		this.followSpeed = followSpeed;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(float maxDistance) {
		this.maxDistance = maxDistance;
	}

	public Subspell getSpawnSpell() {
		return spawnSpell;
	}

	public void setSpawnSpell(Subspell spawnSpell) {
		this.spawnSpell = spawnSpell;
	}

	public Subspell getDeathSpell() {
		return deathSpell;
	}

	public void setDeathSpell(Subspell deathSpell) {
		this.deathSpell = deathSpell;
	}

	public Subspell getAttackSpell() {
		return attackSpell;
	}

	public void setAttackSpell(Subspell attackSpell) {
		this.attackSpell = attackSpell;
	}

	public ItemStack getMainHandItem() {
		return mainHandItem;
	}

	public void setMainHandItem(ItemStack mainHandItem) {
		this.mainHandItem = mainHandItem;
	}

	public ItemStack getOffHandItem() {
		return offHandItem;
	}

	public void setOffHandItem(ItemStack offHandItem) {
		this.offHandItem = offHandItem;
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

	public float getMainHandItemDropChance() {
		return mainHandItemDropChance;
	}

	public void setMainHandItemDropChance(float mainHandItemDropChance) {
		this.mainHandItemDropChance = mainHandItemDropChance;
	}

	public float getOffHandItemDropChance() {
		return offHandItemDropChance;
	}

	public void setOffHandItemDropChance(float offHandItemDropChance) {
		this.offHandItemDropChance = offHandItemDropChance;
	}

	public float getHelmetDropChance() {
		return helmetDropChance;
	}

	public void setHelmetDropChance(float helmetDropChance) {
		this.helmetDropChance = helmetDropChance;
	}

	public float getChestplateDropChance() {
		return chestplateDropChance;
	}

	public void setChestplateDropChance(float chestplateDropChance) {
		this.chestplateDropChance = chestplateDropChance;
	}

	public float getLeggingsDropChance() {
		return leggingsDropChance;
	}

	public void setLeggingsDropChance(float leggingsDropChance) {
		this.leggingsDropChance = leggingsDropChance;
	}

	public float getBootsDropChance() {
		return bootsDropChance;
	}

	public void setBootsDropChance(float bootsDropChance) {
		this.bootsDropChance = bootsDropChance;
	}

}
