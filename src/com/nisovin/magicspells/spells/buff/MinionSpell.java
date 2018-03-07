package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.SpellDamageSpell;

public class MinionSpell extends BuffSpell {

	private Map<UUID, LivingEntity> minions;
	private Map<LivingEntity, UUID> players;
	private Map<UUID, LivingEntity> targets;

	private Random random;
	private ValidTargetList minionTargetList;
	private EntityType[] creatureTypes;
	private int[] chances;
	private boolean powerAffectsHealth;
	private boolean preventCombust;
	private boolean gravity;
	private boolean baby;
	private double powerHealthFactor;
	private double maxHealth;
	private double health;
	private String minionName;
	private Vector spawnOffset;
	private double followRange;
	private float followSpeed;
	private float maxDistance;

	private String spawnSpellName;
	private String deathSpellName;
	private String attackSpellName;
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

	private String[] attributeTypes;
	private double[] attributeValues;
	private int[] attributeOperations;
	private List<PotionEffect> potionEffects;

	public MinionSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		random = new Random();
		minions = new HashMap<>();
		players = new HashMap<>();
		targets = new ConcurrentHashMap<>();

		// Formatted as <entity type> <chance>
		List<String> c = getConfigStringList("mob-chances", null);
		if (c == null) c = new ArrayList<>();
		if (c.isEmpty()) {
			c.add("Zombie 100");
		}
		creatureTypes = new EntityType[c.size()];
		chances = new int[c.size()];
		for (int i = 0; i < c.size(); i++) {
			String[] data = c.get(i).split(" ");
			EntityType creatureType = Util.getEntityType(data[0]);
			int chance = 0;
			if (creatureType != null) {
				try {
					chance = Integer.parseInt(data[1]);
				} catch (NumberFormatException e) {
					// No op
				}
			}
			creatureTypes[i] = creatureType;
			chances[i] = chance;
		}

		// Potion effects
		List<String> potionEffectList = getConfigStringList("potion-effects", null);
		if (potionEffectList != null && !potionEffectList.isEmpty()) {
			potionEffects = new ArrayList<>();
			for (String potion : potionEffectList) {
				String[] split = potion.split(" ");
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
					MagicSpells.error("MinionSpell '" + internalName + "' has an invalid potion effect string " + potion);
				}
			}
		}

		// Attributes
		List<String> attributes = getConfigStringList("attributes", null);
		if (attributes != null && !attributes.isEmpty()) {
			attributeTypes = new String[attributes.size()];
			attributeValues = new double[attributes.size()];
			attributeOperations = new int[attributes.size()];
			for (int i = 0; i < attributes.size(); i++) {
				String s = attributes.get(i);
				try {
					String[] data = s.split(" ");
					String type = data[0];
					double val = Double.parseDouble(data[1]);
					int op = 0;
					if (data.length > 2) {
						if (data[2].equalsIgnoreCase("mult")) {
							op = 1;
						} else if (data[2].toLowerCase().contains("add") && data[2].toLowerCase().contains("perc")) {
							op = 2;
						}
					}
					attributeTypes[i] = type;
					attributeValues[i] = val;
					attributeOperations[i] = op;
				} catch (Exception e) {
					MagicSpells.error("MinionSpell '" + internalName + "' has an invalid attribute " + s);
				}
			}
		}

		// Equipment
		mainHandItem = Util.getItemStackFromString(getConfigString("main-hand", ""));
		if (mainHandItem != null && mainHandItem.getType() != Material.AIR) mainHandItem.setAmount(1);
		offHandItem = Util.getItemStackFromString(getConfigString("off-hand", ""));
		if (offHandItem != null && offHandItem.getType() != Material.AIR) offHandItem.setAmount(1);
		helmet = Util.getItemStackFromString(getConfigString("helmet", ""));
		if (helmet != null && helmet.getType() != Material.AIR) helmet.setAmount(1);
		chestplate = Util.getItemStackFromString(getConfigString("chestplate", ""));
		if (chestplate != null && chestplate.getType() != Material.AIR) chestplate.setAmount(1);
		leggings = Util.getItemStackFromString(getConfigString("leggings", ""));
		if (leggings != null && leggings.getType() != Material.AIR) leggings.setAmount(1);
		boots = Util.getItemStackFromString(getConfigString("boots", ""));
		if (boots != null && boots.getType() != Material.AIR) boots.setAmount(1);

		// Minion target list
		minionTargetList = new ValidTargetList(this, getConfigStringList("minion-targets", null));

		mainHandItemDropChance = getConfigFloat("main-hand-drop-chance", 0) / 100F;
		offHandItemDropChance = getConfigFloat("off-hand-drop-chance", 0) / 100F;
		helmetDropChance = getConfigFloat("helmet-drop-chance", 0) / 100F;
		chestplateDropChance = getConfigFloat("chestplate-drop-chance", 0) / 100F;
		leggingsDropChance = getConfigFloat("leggings-drop-chance", 0) / 100F;
		bootsDropChance = getConfigFloat("boots-drop-chance", 0) / 100F;

		spawnSpellName = getConfigString("spell-on-spawn", "");
		attackSpellName = getConfigString("spell-on-attack", "");
		deathSpellName = getConfigString("spell-on-death", "");

		spawnOffset = getConfigVector("spawn-offset", "1,0,0");
		followRange = getConfigDouble("follow-range",  1.5) * -1;
		followSpeed = getConfigFloat("follow-speed", 1F);
		maxDistance = getConfigFloat("max-distance", 30F);
		powerAffectsHealth = getConfigBoolean("power-affects-health", false);
		powerHealthFactor = getConfigDouble("power-health-factor", 1);
		maxHealth = getConfigDouble("max-health", 20);
		health = getConfigDouble("health", 20);
		minionName = getConfigString("minion-name", "");
		gravity = getConfigBoolean("gravity", true);
		baby = getConfigBoolean("baby", false);
		preventCombust = getConfigBoolean("prevent-sun-burn", true);
	}

	@Override
	public void initialize() {
		super.initialize();

		spawnSpell = new Subspell(spawnSpellName);
		if (!spawnSpell.process() && !spawnSpellName.isEmpty()) {
			MagicSpells.error("MinionSpell '" + internalName + "' has an invalid spell-on-spawn defined!");
			spawnSpell = null;
		}

		attackSpell = new Subspell(attackSpellName);
		if (!attackSpell.process() && !attackSpellName.isEmpty()) {
			MagicSpells.error("MinionSpell '" + internalName + "' has an invalid spell-on-attack defined!");
			attackSpell = null;
		}

		deathSpell = new Subspell(deathSpellName);
		if (!deathSpell.process() && !deathSpellName.isEmpty()) {
			MagicSpells.error("MinionSpell '" + internalName + "' has an invalid spell-on-death defined!");
			deathSpell = null;
		}
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		// Selecting the mob
		EntityType creatureType = null;
		int num = random.nextInt(100);
		int n = 0;
		for (int i = 0; i < creatureTypes.length; i++) {
			if (num < chances[i] + n) {
				creatureType = creatureTypes[i];
				break;
			} else {
				n += chances[i];
			}
		}
		if (creatureType == null) return false;

		// Spawn location
		Location loc = player.getLocation().clone();
		Vector startDir = loc.clone().getDirection().setY(0).normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0, startDir.getX()).normalize();
		loc.add(horizOffset.multiply(spawnOffset.getZ())).getBlock().getLocation();
		loc.add(startDir.clone().multiply(spawnOffset.getX()));
		loc.setY(loc.getY() + spawnOffset.getY());

		// Spawn creature
		LivingEntity minion = (LivingEntity)player.getWorld().spawnEntity(loc, creatureType);
		if (!(minion instanceof Creature)) {
			minion.remove();
			MagicSpells.error("MinionSpell '" + internalName + "' Can only summon creatures!");
			return false;
		}

		if (minion instanceof Zombie) ((Zombie)minion).setBaby(baby);
		MagicSpells.getVolatileCodeHandler().setGravity(minion, gravity);
		minion.setCustomName(ChatColor.translateAlternateColorCodes('&', minionName.replace("%c", player.getName())));
		minion.setCustomNameVisible(true);
		if (powerAffectsHealth) {
			minion.setMaxHealth(maxHealth * power * powerHealthFactor);
			minion.setHealth(health * power * powerHealthFactor);
		} else {
			minion.setMaxHealth(maxHealth);
			minion.setHealth(health);
		}

		if (spawnSpell != null) {
			if (spawnSpell.isTargetedLocationSpell()) {
				spawnSpell.castAtLocation(player, minion.getLocation(), power);
			} else if (spawnSpell.isTargetedEntityFromLocationSpell()) {
				spawnSpell.castAtEntityFromLocation(player, minion.getLocation(), minion, power);
			} else if (spawnSpell.isTargetedEntitySpell()) {
				spawnSpell.castAtEntity(player, minion, power);
			}
		}

		// Apply potion effects and attributes
		if (potionEffects != null) minion.addPotionEffects(potionEffects);
		if (attributeTypes != null && attributeTypes.length > 0) {
			for (int i = 0; i < attributeTypes.length; i++) {
				if (attributeTypes[i] != null) {
					MagicSpells.getVolatileCodeHandler().addEntityAttribute(minion, attributeTypes[i], attributeValues[i], attributeOperations[i]);
				}
			}
		}

		// Equip the minion
		final EntityEquipment eq = minion.getEquipment();
		if (mainHandItem != null) eq.setItemInMainHand(mainHandItem.clone());
		if (offHandItem != null) eq.setItemInOffHand(offHandItem.clone());
		if (helmet != null) eq.setHelmet(helmet.clone());
		if (chestplate != null) eq.setChestplate(chestplate.clone());
		if (leggings != null) eq.setLeggings(leggings.clone());
		if (boots != null) eq.setBoots(boots.clone());

		// Equipment drop chance
		eq.setItemInMainHandDropChance(mainHandItemDropChance);
		eq.setItemInOffHandDropChance(offHandItemDropChance);
		eq.setHelmetDropChance(helmetDropChance);
		eq.setChestplateDropChance(chestplateDropChance);
		eq.setLeggingsDropChance(leggingsDropChance);
		eq.setBootsDropChance(bootsDropChance);

		minions.put(player.getUniqueId(), minion);
		players.put(minion, player.getUniqueId());
		return true;
	}

	@EventHandler
	public void onEntityTarget(EntityTargetEvent e) {
		if (minions.isEmpty() || e.getTarget() == null) return;
		if (!(e.getEntity() instanceof LivingEntity)) return;
		if (!isMinion(e.getEntity())) return;

		LivingEntity minion = (LivingEntity) e.getEntity();
		Player pl = Bukkit.getPlayer(players.get(minion));
		if (targets.get(pl.getUniqueId()) == null || !targets.containsKey(pl.getUniqueId()) || !targets.get(pl.getUniqueId()).isValid()) {
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
		MagicSpells.getVolatileCodeHandler().setTarget(minion, target);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageByEntityEvent e) {
		Entity entity = e.getEntity();
		Entity damager = e.getDamager();
		if (damager == null || entity == null) return;
		if (damager.isDead() || !damager.isValid()) return;
		if (entity.isDead() || !entity.isValid()) return;
		if (!(entity instanceof LivingEntity)) return;
		// Check if the damaged entity is a player
		if (entity instanceof Player) {
			Player pl = (Player) e.getEntity();
			if (!isActive(pl)) return;
			// If a Minion tries to attack his owner, cancel the damage and stop the minion
			if (minions.get(pl.getUniqueId()).equals(damager)) {
				targets.remove(pl.getUniqueId());
				MagicSpells.getVolatileCodeHandler().setTarget(minions.get(pl.getUniqueId()), null);
				e.setCancelled(true);
				return;
			}
			// Check if the player was damaged by a projectile
			if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity) {
				// Check if the shooter is alive
				Entity shooter = (LivingEntity) ((Projectile) damager).getShooter();
				if (shooter.isValid() && !shooter.isDead()) damager = shooter;
			}

			// If distance between previous target and the player is less than between the new target, the minion will keep focusing the previous target
			LivingEntity previousTarget = targets.get(pl.getUniqueId());
			if (previousTarget != null && pl.getLocation().distanceSquared(previousTarget.getLocation()) < pl.getLocation().distanceSquared(damager.getLocation())) return;

			targets.put(pl.getUniqueId(), (LivingEntity) damager);
			MagicSpells.getVolatileCodeHandler().setTarget(minions.get(pl.getUniqueId()), (LivingEntity) damager);
			return;
		}

		// Check if the damaged entity is a minion
		if (isMinion(entity)) {
			LivingEntity minion = (LivingEntity) entity;
			Player owner = Bukkit.getPlayer(players.get(minion));
			if (owner == null || !owner.isOnline() || !owner.isValid()) return;
			// Owner cant damage his minion
			if (damager.equals(owner)) {
				e.setCancelled(true);
				return;
			}

			if (((LivingEntity) entity).getHealth() - e.getFinalDamage() <= 0 && deathSpell != null) {
				if (deathSpell.isTargetedLocationSpell()) {
					deathSpell.castAtLocation(owner, minion.getLocation(), 1);
				} else if (deathSpell.isTargetedEntityFromLocationSpell()) {
					deathSpell.castAtEntityFromLocation(owner, minion.getLocation(), minion, 1);
				} else if (deathSpell.isTargetedEntitySpell()) {
					deathSpell.castAtEntity(owner, minion, 1);
				}
			}

			// If the minion is far away from the owner, forget about attacking
			if (owner.getLocation().distanceSquared(minion.getLocation()) > maxDistance * maxDistance) return;

			// If the owner has no targets and someone will attack the minion, he will strike back
			if (targets.get(owner.getUniqueId()) == null || targets.get(owner.getUniqueId()).isDead() || !targets.get(owner.getUniqueId()).isValid()) {
				// Check if the minion damager is an arrow, if so, get the shooter, otherwise get the living damager
				LivingEntity minionDamager = null;
				if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity) {
					// Check if the shooter is alive
					LivingEntity shooter = (LivingEntity) ((Projectile) damager).getShooter();
					if (shooter.isValid() && !shooter.isDead()) minionDamager = shooter;

				} else if (damager instanceof LivingEntity) {
					minionDamager = (LivingEntity) damager;
				}
				if (minionDamager != null) {
					targets.put(owner.getUniqueId(), minionDamager);
					MagicSpells.getVolatileCodeHandler().setTarget(minion, minionDamager);
				}
			}
		}

		if (damager instanceof Player) {
			// Check if player's damaged target is his minion, if its not, make him attack your target
			Player pl = (Player) damager;
			if (!isActive(pl)) return;
			for (BuffSpell buff : MagicSpells.getBuffManager().getActiveBuffs(pl)) {
				if (!(buff instanceof MinionSpell)) continue;
				if (entity.equals(((MinionSpell)buff).minions.get(pl.getUniqueId()))) {
					e.setCancelled(true);
					return;
				}
			}

			if (isMinion(entity) && minions.get(pl.getUniqueId()).equals(entity)) {
				e.setCancelled(true);
				return;
			}

			// Check if the entity can be targeted by the minion
			if (!minionTargetList.canTarget(entity)) return;

			addUseAndChargeCost(pl);
			targets.put(pl.getUniqueId(), (LivingEntity) entity);
			MagicSpells.getVolatileCodeHandler().setTarget(minions.get(pl.getUniqueId()), (LivingEntity) entity);

		}

		if (isMinion(damager)) {
			// Minion is the damager
			LivingEntity minion = (LivingEntity) damager;
			Player owner = Bukkit.getPlayer(players.get(minion));
			if (owner == null || !owner.isOnline() || !owner.isValid()) return;

			if (attackSpell != null) {
				if (attackSpell.isTargetedLocationSpell()) {
					attackSpell.castAtLocation(owner, minion.getLocation(), 1);
				} else if (attackSpell.isTargetedEntityFromLocationSpell()) {
					attackSpell.castAtEntityFromLocation(owner, minion.getLocation(), (LivingEntity) entity, 1);
				} else if (attackSpell.isTargetedEntitySpell()) {
					attackSpell.castAtEntity(owner, (LivingEntity) entity, 1);
				}
			}
		}

		// The target died, the minion will follow his owner
		if (targets.containsValue(entity) && ((LivingEntity) entity).getHealth() - e.getFinalDamage() <= 0) {
			for (UUID id : targets.keySet()) {
				if (!targets.get(id).equals(entity)) continue;
				Player pl = Bukkit.getPlayer(id);

				if (pl == null || !pl.isValid() || !pl.isOnline()) continue;

				targets.remove(id);
				MagicSpells.getVolatileCodeHandler().setTarget(minions.get(id), null);

				Location loc = pl.getLocation().clone();
				loc.add(loc.getDirection().setY(0).normalize().multiply(followRange));
				MagicSpells.getVolatileCodeHandler().creaturePathToLoc((Creature) minions.get(pl.getUniqueId()), loc, followSpeed);
			}
		}
	}

	// Owner cant damage his minion with spells
	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent e) {
		if (!(e.getSpell() instanceof SpellDamageSpell)) return;
		if (!isActive(e.getCaster())) return;
		if (e.getTarget().equals(minions.get(e.getCaster().getUniqueId()))) e.setCancelled(true);
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		if (!isMinion(e.getEntity())) return;
		EntityEquipment eq = e.getEntity().getEquipment();
		List<ItemStack> newDrops = new ArrayList<>();
		for (ItemStack drop : e.getDrops()) {
			for (int i = 0; i < eq.getArmorContents().length; i++) {
				if (drop.equals(eq.getArmorContents()[i])) newDrops.add(drop);
			}
			if (drop.equals(mainHandItem) || drop.equals(offHandItem)) newDrops.add(drop);
		}

		// Clear all the regular drops
		e.getDrops().clear();
		e.setDroppedExp(0);

		// Apply new drops
		for (ItemStack item : newDrops) {
			e.getDrops().add(item);
		}
		Player pl = Bukkit.getPlayer(players.get(e.getEntity()));
		if (pl == null || !pl.isValid() || !pl.isOnline()) return;
		turnOffBuff(pl);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		if (e.getFrom().getBlock().equals(e.getTo().getBlock())) return;
		Player pl = e.getPlayer();
		if (!isActive(pl)) return;
		LivingEntity minion = minions.get(pl.getUniqueId());

		if (pl.getLocation().distanceSquared(minion.getLocation()) > maxDistance * maxDistance || targets.get(pl.getUniqueId()) == null || !targets.containsKey(pl.getUniqueId())) {

			// The minion has a target but he is far away from his owner, remove his current target
			if (targets.get(pl.getUniqueId()) != null) {
				targets.remove(pl.getUniqueId());
				MagicSpells.getVolatileCodeHandler().setTarget(minion, null);
			}

			// The distance between minion and his owner is greater that the defined max distance or the minion has no targets, he will follow his owner
			Location loc = pl.getLocation().clone();
			loc.add(loc.getDirection().setY(0).normalize().multiply(followRange));
			MagicSpells.getVolatileCodeHandler().creaturePathToLoc((Creature) minions.get(pl.getUniqueId()), loc, followSpeed);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent e) {
		if (!preventCombust || !isMinion(e.getEntity())) return;
		e.setCancelled(true);
	}

	@Override
	public void turnOffBuff(Player player) {
		LivingEntity minion = minions.remove(player.getUniqueId());
		if (minion != null && !minion.isDead()) minion.remove();

		players.remove(minion);
		targets.remove(player.getUniqueId());
	}

	@Override
	protected void turnOff() {
		Util.forEachValueOrdered(minions, Entity::remove);
		minions.clear();
		players.clear();
		targets.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return minions.containsKey(player.getUniqueId());
	}

	public boolean isMinion(Entity entity) {
		return minions.containsValue(entity);
	}

}
