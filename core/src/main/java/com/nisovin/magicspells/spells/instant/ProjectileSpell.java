package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.trackers.ProjectileTracker;
import com.nisovin.magicspells.util.projectile.ProjectileManager;
import com.nisovin.magicspells.util.projectile.ProjectileManagers;

public class ProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private static Set<ProjectileTracker> trackerSet;

	private NoMagicZoneManager zoneManager;

	private ProjectileManager projectileManager;

	private Vector relativeOffset;

	private ConfigData<Integer> tickInterval;
	private ConfigData<Integer> tickSpellInterval;
	private ConfigData<Integer> specialEffectInterval;

	private ConfigData<Float> rotation;
	private ConfigData<Float> velocity;
	private ConfigData<Float> hitRadius;
	private ConfigData<Float> vertSpread;
	private ConfigData<Float> horizSpread;
	private ConfigData<Float> verticalHitRadius;

	private boolean gravity;
	private boolean charged;
	private boolean incendiary;
	private boolean checkPlugins;
	private boolean stopOnModifierFail;

	private ConfigData<Double> maxDuration;

	private final String hitSpellName;
	private final String tickSpellName;
	private final String groundSpellName;
	private final String modifierSpellName;
	private final String durationSpellName;
	private final String entityLocationSpellName;

	private String projectileName;

	private Subspell hitSpell;
	private Subspell tickSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;
	private Subspell entityLocationSpell;

	private ModifierSet projectileModifiers;
	private List<String> projectileModifiersStrings;

	public ProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		trackerSet = new HashSet<>();

		projectileManager = ProjectileManagers.getManager(getConfigString("projectile-type",  "arrow"));

		relativeOffset = getConfigVector("relative-offset", "0,1.5,0");

		tickInterval = getConfigDataInt("tick-interval", 1);
		tickSpellInterval = getConfigDataInt("spell-interval", 20);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 0);

		rotation = getConfigDataFloat("rotation", 0F);
		velocity = getConfigDataFloat("velocity", 1F);
		hitRadius = getConfigDataFloat("hit-radius", 2F);
		vertSpread = getConfigDataFloat("vertical-spread", 0F);
		horizSpread = getConfigDataFloat("horizontal-spread", 0F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", 2F);

		gravity = getConfigBoolean("gravity", true);
		charged = getConfigBoolean("charged", false);
		incendiary = getConfigBoolean("incendiary", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		stopOnModifierFail = getConfigBoolean("stop-on-modifier-fail", true);

		maxDuration = getConfigDataDouble("max-duration", 10);

		hitSpellName = getConfigString("spell", "");
		tickSpellName = getConfigString("spell-on-tick", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		durationSpellName = getConfigString("spell-after-duration", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");

		projectileName = Util.colorize(getConfigString("projectile-name", ""));

		projectileModifiersStrings = getConfigStringList("projectile-modifiers", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (projectileModifiersStrings != null && !projectileModifiersStrings.isEmpty()) {
			projectileModifiers = new ModifierSet(projectileModifiersStrings, this);
			projectileModifiersStrings = null;
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		hitSpell = new Subspell(hitSpellName);
		if (!hitSpell.process()) {
			hitSpell = null;
			if (!hitSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell defined!");
		}

		groundSpell = new Subspell(groundSpellName);
		if (!groundSpell.process() || !groundSpell.isTargetedLocationSpell()) {
			groundSpell = null;
			if (!groundSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-hit-ground defined!");
		}

		tickSpell = new Subspell(tickSpellName);
		if (!tickSpell.process() || !tickSpell.isTargetedLocationSpell()) {
			tickSpell = null;
			if (!tickSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-tick defined!");
		}

		durationSpell = new Subspell(durationSpellName);
		if (!durationSpell.process() || !durationSpell.isTargetedLocationSpell()) {
			durationSpell = null;
			if (!durationSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-after-duration defined!");
		}

		modifierSpell = new Subspell(modifierSpellName);
		if (!modifierSpell.process() || !modifierSpell.isTargetedLocationSpell()) {
			if (!modifierSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-modifier-fail defined!");
			modifierSpell = null;
		}

		entityLocationSpell = new Subspell(entityLocationSpellName);
		if (!entityLocationSpell.process() || !entityLocationSpell.isTargetedLocationSpell()) {
			if (!entityLocationSpellName.isEmpty()) MagicSpells.error("ProjectileSpell '" + internalName + "' has an invalid spell-on-entity-location defined!");
			entityLocationSpell = null;
		}

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public void turnOff() {
		for (ProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			ProjectileTracker tracker = new ProjectileTracker(caster, caster.getLocation(), power, args);
			setupTracker(tracker, caster, power, args);
			tracker.start();
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity livingEntity, Location target, float power, String[] args) {
		ProjectileTracker tracker = new ProjectileTracker(livingEntity, target, power, args);
		setupTracker(tracker, livingEntity, power, args);
		tracker.start();
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private void setupTracker(ProjectileTracker tracker, LivingEntity caster, float power, String[] args) {
		tracker.setSpell(this);

		tracker.setProjectileManager(projectileManager);
		tracker.setRelativeOffset(relativeOffset);

		tracker.setTickInterval(tickInterval.get(caster, null, power, args));
		tracker.setTickSpellInterval(tickSpellInterval.get(caster, null, power, args));
		tracker.setSpecialEffectInterval(specialEffectInterval.get(caster, null, power, args));

		tracker.setRotation(rotation.get(caster, null, power, args));
		tracker.setVelocity(velocity.get(caster, null, power, args));
		tracker.setHitRadius(hitRadius.get(caster, null, power, args));
		tracker.setVertSpread(vertSpread.get(caster, null, power, args));
		tracker.setHorizSpread(horizSpread.get(caster, null, power, args));
		tracker.setVerticalHitRadius(verticalHitRadius.get(caster, null, power, args));

		tracker.setGravity(gravity);
		tracker.setCharged(charged);
		tracker.setIncendiary(incendiary);
		tracker.setCallEvents(checkPlugins);
		tracker.setStopOnModifierFail(stopOnModifierFail);

		tracker.setMaxDuration(maxDuration.get(caster, null, power, args) * TimeUtil.MILLISECONDS_PER_SECOND);

		tracker.setProjectileName(projectileName);

		tracker.setHitSpell(hitSpell);
		tracker.setTickSpell(tickSpell);
		tracker.setGroundSpell(groundSpell);
		tracker.setModifierSpell(modifierSpell);
		tracker.setDurationSpell(durationSpell);

		tracker.setProjectileModifiers(projectileModifiers);

		tracker.setTargetList(validTargetList);
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof WitherSkull)) return;
		Projectile projectile = (Projectile) entity;

		Iterator<ProjectileTracker> iterator = trackerSet.iterator();
		while (iterator.hasNext()) {
			ProjectileTracker tracker = iterator.next();
			if (tracker.getProjectile() == null) continue;
			if (!tracker.getProjectile().equals(projectile)) continue;

			event.setCancelled(true);
			tracker.stop(false);
			iterator.remove();
			break;
		}
	}

	@EventHandler
	public void onProjectileHit(EntityDamageByEntityEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;
		if (!(event.getEntity() instanceof LivingEntity entity)) return;

		Entity damagerEntity = event.getDamager();
		if (!(damagerEntity instanceof Projectile projectile)) return;

		Iterator<ProjectileTracker> iterator = trackerSet.iterator();
		while (iterator.hasNext()) {
			ProjectileTracker tracker = iterator.next();
			if (tracker.getProjectile() == null) continue;
			if (!tracker.getProjectile().equals(projectile)) continue;

			if (tracker.getHitSpell() != null) {
				if (tracker.getHitSpell().isTargetedEntitySpell()) tracker.getHitSpell().castAtEntity(tracker.getCaster(), entity, tracker.getPower());
				else if (tracker.getHitSpell().isTargetedLocationSpell()) tracker.getHitSpell().castAtLocation(tracker.getCaster(), entity.getLocation(), tracker.getPower());
			}

			playSpellEffects(EffectPosition.TARGET, entity);
			event.setCancelled(true);
			event.setDamage(0);
			tracker.stop(false);
			iterator.remove();
			break;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEnderTeleport(PlayerTeleportEvent event) {
		if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
		for (ProjectileTracker tracker : trackerSet) {
			if (tracker.getProjectile() == null) continue;
			if (!locationsEqual(tracker.getProjectile().getLocation(), event.getTo())) continue;
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPotionSplash(PotionSplashEvent event) {
		for (ProjectileTracker tracker : trackerSet) {
			if (tracker.getProjectile() == null) continue;
			if (!tracker.getProjectile().equals(event.getPotion())) continue;
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.EGG) return;
		for (ProjectileTracker tracker : trackerSet) {
			if (tracker.getProjectile() == null) continue;
			if (!locationsEqual(tracker.getProjectile().getLocation(), event.getLocation())) continue;
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onProjectileBlockHit(ProjectileHitEvent e) {
		Projectile projectile = e.getEntity();
		Block block = e.getHitBlock();
		if (block == null) return;
		Iterator<ProjectileTracker> iterator = trackerSet.iterator();
		while (iterator.hasNext()) {
			ProjectileTracker tracker = iterator.next();
			if (tracker.getProjectile() == null) continue;
			if (!tracker.getProjectile().equals(projectile)) continue;

			if (tracker.getCaster() != null && tracker.getGroundSpell() != null) {
				tracker.getGroundSpell().castAtLocation(tracker.getCaster(), projectile.getLocation(), tracker.getPower());
			}
			tracker.stop(false);
			iterator.remove();
		}
	}

	private boolean locationsEqual(Location loc1, Location loc2) {
		return Math.abs(loc1.getX() - loc2.getX()) < 0.1
				&& Math.abs(loc1.getY() - loc2.getY()) < 0.1
				&& Math.abs(loc1.getZ() - loc2.getZ()) < 0.1;
	}

	public void playEffects(EffectPosition position, Location loc) {
		playSpellEffects(position, loc);
	}

	public void playEffects(EffectPosition position, Entity entity) {
		playSpellEffects(position, entity);
	}

	public void playTrackingLineEffects(EffectPosition position, Location startLocation, Location location, LivingEntity caster, Projectile projectile) {
		playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), caster, projectile);
	}

	public static Set<ProjectileTracker> getProjectileTrackers() {
		return trackerSet;
	}

	public NoMagicZoneManager getZoneManager() {
		return zoneManager;
	}

	public void setZoneManager(NoMagicZoneManager zoneManager) {
		this.zoneManager = zoneManager;
	}

	public ProjectileManager getProjectileManager() {
		return projectileManager;
	}

	public void setProjectileManager(ProjectileManager projectileManager) {
		this.projectileManager = projectileManager;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public void setRelativeOffset(Vector relativeOffset) {
		this.relativeOffset = relativeOffset;
	}

	public boolean hasGravity() {
		return gravity;
	}

	public void setGravity(boolean gravity) {
		this.gravity = gravity;
	}

	public boolean isCharged() {
		return charged;
	}

	public void setCharged(boolean charged) {
		this.charged = charged;
	}

	public boolean isIncendiary() {
		return incendiary;
	}

	public void setIncendiary(boolean incendiary) {
		this.incendiary = incendiary;
	}

	public boolean shouldStopOnModifierFail() {
		return stopOnModifierFail;
	}

	public void setStopOnModifierFail(boolean stopOnModifierFail) {
		this.stopOnModifierFail = stopOnModifierFail;
	}

	public String getProjectileName() {
		return projectileName;
	}

	public void setProjectileName(String projectileName) {
		this.projectileName = projectileName;
	}

	public Subspell getHitSpell() {
		return hitSpell;
	}

	public void setHitSpell(Subspell hitSpell) {
		this.hitSpell = hitSpell;
	}

	public Subspell getTickSpell() {
		return tickSpell;
	}

	public void setTickSpell(Subspell tickSpell) {
		this.tickSpell = tickSpell;
	}

	public Subspell getGroundSpell() {
		return groundSpell;
	}

	public void setGroundSpell(Subspell groundSpell) {
		this.groundSpell = groundSpell;
	}

	public Subspell getModifierSpell() {
		return modifierSpell;
	}

	public void setModifierSpell(Subspell modifierSpell) {
		this.modifierSpell = modifierSpell;
	}

	public Subspell getDurationSpell() {
		return durationSpell;
	}

	public void setDurationSpell(Subspell durationSpell) {
		this.durationSpell = durationSpell;
	}

	public Subspell getEntityLocationSpell() {
		return entityLocationSpell;
	}

	public void setEntityLocationSpell(Subspell entityLocationSpell) {
		this.entityLocationSpell = entityLocationSpell;
	}

	public ModifierSet getProjectileModifiers() {
		return projectileModifiers;
	}

	public void setProjectileModifiers(ModifierSet projectileModifiers) {
		this.projectileModifiers = projectileModifiers;
	}

	public boolean shouldCheckPlugins() {
		return checkPlugins;
	}

	public void setCheckPlugins(boolean checkPlugins) {
		this.checkPlugins = checkPlugins;
	}

}
