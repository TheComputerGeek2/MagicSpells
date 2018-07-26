package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.MagicConfig;

public class SpawnMonsterSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private String location;
	private EntityData entityData;
	private boolean allowSpawnInMidair;
	private boolean baby;
	private boolean tamed;
	
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
	private List<PotionEffect> potionEffects;
	private int fireTicks;
	private int duration;
	private String nameplateText;
	private boolean useCasterName;
	private boolean removeAI;
	private boolean noAI;
	private boolean addLookAtPlayerAI;
	
	private String[] attributeTypes;
	private double[] attributeValues;
	private int[] attributeOperations;
	
	private Subspell attackSpell;
	private double retargetRange;
	private int targetInterval;
	private double targetRange;
	
	private boolean gravity;
	
	private Random random = new Random();
	
	// DEBUG INFO: level 2, invalid potion effect on internalname spell data
	public SpawnMonsterSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		gravity = getConfigBoolean("gravity", true);
		
		location = getConfigString("location", "target");
		entityData = new EntityData(getConfigString("entity-type", "wolf"));
		allowSpawnInMidair = getConfigBoolean("allow-spawn-in-midair", false);
		baby = getConfigBoolean("baby", false);
		tamed = getConfigBoolean("tamed", false);
		holding = Util.getItemStackFromString(getConfigString("holding", "air"));
		if (holding != null && holding.getType() != Material.AIR) holding.setAmount(1);
		helmet = Util.getItemStackFromString(getConfigString("helmet", "air"));
		if (helmet != null && helmet.getType() != Material.AIR) helmet.setAmount(1);
		chestplate = Util.getItemStackFromString(getConfigString("chestplate", "air"));
		if (chestplate != null && chestplate.getType() != Material.AIR) chestplate.setAmount(1);
		leggings = Util.getItemStackFromString(getConfigString("leggings", "air"));
		if (leggings != null && leggings.getType() != Material.AIR) leggings.setAmount(1);
		boots = Util.getItemStackFromString(getConfigString("boots", "air"));
		if (boots != null && boots.getType() != Material.AIR) boots.setAmount(1);
		holdingDropChance = getConfigFloat("holding-drop-chance", 0) / 100F;
		helmetDropChance = getConfigFloat("helmet-drop-chance", 0) / 100F;
		chestplateDropChance = getConfigFloat("chestplate-drop-chance", 0) / 100F;
		leggingsDropChance = getConfigFloat("leggings-drop-chance", 0) / 100F;
		bootsDropChance = getConfigFloat("boots-drop-chance", 0) / 100F;
		
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
					MagicSpells.debug("Invalid potion effect string on '" + internalName + "' spell: " + data);
				}
			}
		}
		
		fireTicks = getConfigInt("fire-ticks", 0);
		duration = getConfigInt("duration", 0);
		
		boolean nameplateUsesFormatting = getConfigBoolean("nameplate-formatting", false);
		nameplateText = getConfigString("nameplate-text", "");
		
		if (nameplateUsesFormatting) nameplateText = ChatColor.translateAlternateColorCodes('&', nameplateText);
		
		removeAI = getConfigBoolean("remove-ai", false);
		noAI = getConfigBoolean("no-ai", false);
		addLookAtPlayerAI = getConfigBoolean("add-look-at-player-ai", false);
		
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
					MagicSpells.error("Invalid attribute on '" + spellName + "' spell: " + s);
				}
			}
		}
		
		String attackSpellName = getConfigString("attack-spell", null);
		if (attackSpellName != null && !attackSpellName.isEmpty()) attackSpell = new Subspell(attackSpellName);
		retargetRange = getConfigDouble("retarget-range", 50);
		targetInterval = getConfigInt("target-interval", -1);
		targetRange = getConfigDouble("target-range", 20);
		
		if (entityData.getType() == null || !entityData.getType().isAlive()) {
			MagicSpells.error("SpawnMonster spell '" + spellName + "' has an invalid entity-type!");
		}
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (attackSpell != null) {
			if (!attackSpell.process()) {
				MagicSpells.error("SpawnMonsterSpell '" + internalName + "' has invalid attack-spell");
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			LivingEntity target = null;
			
			if (location.equalsIgnoreCase("focus")) {
				loc = getRandomLocationFrom(player.getLocation(), 3);
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
				if (targetInfo == null) return noTarget(player);
				target = targetInfo.getTarget();
				power = targetInfo.getPower();
			} else if (location.equalsIgnoreCase("target")) {
				Block block = getTargetedBlock(player, power);
				if (block != null && block.getType() != Material.AIR) { 
					if (BlockUtils.isPathable(block)) {
						loc = block.getLocation();
					} else if (BlockUtils.isPathable(block.getRelative(BlockFace.UP))) {
						loc = block.getLocation().add(0D, 1D, 0D);
					}
				}
			} else if (location.equalsIgnoreCase("caster")) {
				loc = player.getLocation();
			} else if (location.equalsIgnoreCase("random")) {
				loc = getRandomLocationFrom(player.getLocation(), getRange(power));
			} else if (location.startsWith("casteroffset:")) {
				String[] split = location.split(":");
				float y = Float.parseFloat(split[1]);
				loc = player.getLocation().add(0D, y, 0D);
				loc.setPitch(0F);
			}
			
			if (loc == null) return noTarget(player);
			
			spawnMob(player, player.getLocation(), loc, target, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private Location getRandomLocationFrom(Location location, int range) {
		World world = location.getWorld();
		int attempts = 0;
		int x;
		int y;
		int z;
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
					if (BlockUtils.isPathable(block2)) {
						block = block2;
					} else {
						return block.getLocation();
					}
					c++;
				}
			}
			
			attempts++;
		}
		return null;
	}
	
	private void spawnMob(final Player player, Location source, Location loc, LivingEntity target, float power) {
		if (entityData.getType() == null) return;
		
		// Spawn it
		loc.setYaw((float) (Math.random() * 360));
		final Entity entity = entityData.spawn(loc.add(.5, .1, .5));
		
		// Prep
		prepMob(player, entity);
		
		// Add potion effects
		if (potionEffects != null) ((LivingEntity)entity).addPotionEffects(potionEffects);
		
		// Set on fire
		if (fireTicks > 0) entity.setFireTicks(fireTicks);
		
		// Add attributes
		if (attributeTypes != null && attributeTypes.length > 0) {
			for (int i = 0; i < attributeTypes.length; i++) {
				if (attributeTypes[i] == null) continue;
				MagicSpells.getVolatileCodeHandler().addEntityAttribute((LivingEntity)entity, attributeTypes[i], attributeValues[i], attributeOperations[i]);
			}
		}
		
		// Set AI
		if (removeAI) {
			MagicSpells.getVolatileCodeHandler().removeAI((LivingEntity)entity);
			if (addLookAtPlayerAI) MagicSpells.getVolatileCodeHandler().addAILookAtPlayer((LivingEntity)entity, 10);
		}
		if (noAI) MagicSpells.getVolatileCodeHandler().setNoAIFlag((LivingEntity)entity);
		
		// Set target
		if (target != null) MagicSpells.getVolatileCodeHandler().setTarget((LivingEntity)entity, target);
		if (targetInterval > 0) new Targeter(player, (LivingEntity)entity);
		
		// Setup attack spell
		if (attackSpell != null) {
			final AttackMonitor monitor = new AttackMonitor(player, (LivingEntity)entity, target, power);
			MagicSpells.registerEvents(monitor);
			MagicSpells.scheduleDelayedTask(() -> HandlerList.unregisterAll(monitor), duration > 0 ? duration : 12000);
		}
		
		// Play effects
		if (player != null) {
			playSpellEffects(player, entity);
		} else {
			playSpellEffects(source, entity);
		}
		
		// Schedule removal
		if (duration > 0) MagicSpells.scheduleDelayedTask(entity::remove, duration);
	}
	
	void prepMob(Player player, Entity entity) {
		MagicSpells.getVolatileCodeHandler().setGravity(entity, gravity);
		
		// Set as tamed
		if (tamed && entity instanceof Tameable && player != null) {
			((Tameable)entity).setTamed(true);
			((Tameable)entity).setOwner(player);
		}
		
		// Set as baby
		if (baby) {
			if (entity instanceof Ageable) ((Ageable)entity).setBaby();
		}
		if (entity instanceof Zombie) ((Zombie)entity).setBaby(baby);
		
		// Set held item
		if (holding != null && holding.getType() != Material.AIR) {
			if (entity instanceof Enderman) {
				((Enderman)entity).setCarriedMaterial(holding.getData());
			} else if (entity instanceof Skeleton || entity instanceof Zombie) {
				final EntityEquipment equip = ((LivingEntity)entity).getEquipment();
				HandHandler.setItemInMainHand(equip, holding.clone());
				HandHandler.setItemInMainHandDropChance(equip, holdingDropChance);
			}
		}
		
		// Set armor
		final EntityEquipment equip = ((LivingEntity)entity).getEquipment();
		equip.setHelmet(helmet);
		equip.setChestplate(chestplate);
		equip.setLeggings(leggings);
		equip.setBoots(boots);
		equip.setHelmetDropChance(helmetDropChance);
		equip.setChestplateDropChance(chestplateDropChance);
		equip.setLeggingsDropChance(leggingsDropChance);
		equip.setBootsDropChance(bootsDropChance);
		
		// Set nameplate text
		if (useCasterName && player != null) {
			entity.setCustomName(player.getDisplayName());
			entity.setCustomNameVisible(true);
		} else if (nameplateText != null && !nameplateText.isEmpty()) {
			entity.setCustomName(nameplateText);
			entity.setCustomNameVisible(true);
		}
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		if (location.equalsIgnoreCase("target")) {
			spawnMob(caster, caster.getLocation(), target, null, power);
		} else if (location.equalsIgnoreCase("caster")) {
			spawnMob(caster, caster.getLocation(), caster.getLocation(), null, power);
		} else if (location.equalsIgnoreCase("random")) {
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
		if (location.equalsIgnoreCase("target")) {
			spawnMob(null, target, target, null, power);
		} else if (location.equalsIgnoreCase("caster")) {
			spawnMob(null, target, target, null, power);
		} else if (location.equalsIgnoreCase("random")) {
			Location loc = getRandomLocationFrom(target, getRange(power));
			if (loc != null) {
				spawnMob(null, target, loc, null, power);
			}
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
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		if (location.equals("focus")) {
			spawnMob(caster, from, from, target, power);
		} else {
			castAtLocation(caster, from, power);
		}
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		if (location.equals("focus")) {
			spawnMob(null, from, from, target, power);
		} else {
			castAtLocation(from, power);
		}
		return true;
	}
	
	class AttackMonitor implements Listener {
		
		Player caster;
		LivingEntity monster;
		LivingEntity target;
		float power;
		
		public AttackMonitor(Player caster, LivingEntity monster, LivingEntity target, float power) {
			this.caster = caster;
			this.monster = monster;
			this.target = target;
			this.power = power;
		}
		
		@EventHandler(ignoreCancelled = true)
		void onDamage(EntityDamageByEntityEvent event) {
			if (attackSpell.getSpell().onCooldown(caster)) {
				return;
			}
			Entity damager = event.getDamager();
			if (damager instanceof Projectile) {
				if (((Projectile)damager).getShooter() != null && ((Projectile)damager).getShooter() instanceof Entity) {
					damager = (Entity)((Projectile)damager).getShooter();
				}
			}
			if (event.getEntity() instanceof LivingEntity && damager == monster) {
				if (attackSpell.isTargetedEntityFromLocationSpell()) {
					attackSpell.castAtEntityFromLocation(caster, monster.getLocation(), (LivingEntity)event.getEntity(), power);
				} else if (attackSpell.isTargetedEntitySpell()) {
					attackSpell.castAtEntity(caster, (LivingEntity)event.getEntity(), power);
				} else if (attackSpell.isTargetedLocationSpell()) {
					attackSpell.castAtLocation(caster, event.getEntity().getLocation(), power);
				} else {
					attackSpell.cast(caster, power);
				}
				event.setCancelled(true);
			}
		}
		
		@EventHandler
		void onTarget(EntityTargetEvent event) {
			if (event.getEntity() == monster && event.getTarget() == caster) {
				event.setCancelled(true);
			} else if (event.getTarget() == null) {
				retarget(null);
			} else if (target != null && event.getTarget() != target) {
				event.setTarget(target);
			}
		}
		
		@EventHandler
		void onDeath(EntityDeathEvent event) {
			if (event.getEntity() != target) return;
			target = null;
			retarget(event.getEntity());
		}
		
		void retarget(LivingEntity ignore) {
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
				int rr = (int)monster.getLocation().distanceSquared(e.getLocation());
				if (rr < r) {
					r = rr;
					t = (LivingEntity)e;
					if (r < 25) break;
				}
			}
			target = t;
			MagicSpells.getVolatileCodeHandler().setTarget(monster, t);
		}
		
	}
	
	class Targeter implements Runnable {
		
		Player caster;
		LivingEntity entity;
		int taskId;
		
		public Targeter(Player caster, LivingEntity entity) {
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
			if (!targetable.isEmpty()) {
				LivingEntity target = targetable.get(random.nextInt(targetable.size()));
				MagicSpells.getVolatileCodeHandler().setTarget(entity, target);
			}
		}
		
	}
	
}
