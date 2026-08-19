package com.nisovin.magicspells.spells.instant;

import java.util.*;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.trackers.ProjectileTracker;
import com.nisovin.magicspells.util.projectile.ProjectileManager;
import com.nisovin.magicspells.util.projectile.ProjectileManagers;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;

public class ProjectileSpell extends InstantSpell implements TargetedLocationSpell {

	private static Set<ProjectileTracker> trackerSet;

	private final ConfigData<String> projectileType;

	private final ConfigData<Vector> relativeOffset;
	private final ConfigData<Vector> effectOffset;

	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> tickSpellInterval;
	private final ConfigData<Integer> specialEffectInterval;
	private final ConfigData<Integer> intermediateEffects;
	private final ConfigData<Integer> intermediateHitboxes;

	private final ConfigData<Float> rotation;
	private final ConfigData<Float> velocity;
	private final ConfigData<Float> hitRadius;
	private final ConfigData<Float> vertSpread;
	private final ConfigData<Float> horizSpread;
	private final ConfigData<Float> verticalHitRadius;

	private final ConfigData<Boolean> visible;
	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> charged;
	private final ConfigData<Boolean> incendiary;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> stopOnModifierFail;

	private final ConfigData<Double> maxDuration;

	private final String hitSpellName;
	private final String tickSpellName;
	private final String groundSpellName;
	private final String modifierSpellName;
	private final String durationSpellName;
	private final String entityLocationSpellName;

	private final ConfigData<Component> projectileName;

	private final ConfigData<Color> arrowColor;

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

		projectileType = getConfigDataString("projectile-type", "arrow");

		relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 1.5, 0));
		effectOffset = getConfigDataVector("effect-offset", new Vector());

		tickInterval = getConfigDataInt("tick-interval", 1);
		tickSpellInterval = getConfigDataInt("spell-interval", 20);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 0);
		intermediateEffects = getConfigDataInt("intermediate-effects", 0);
		intermediateHitboxes = getConfigDataInt("intermediate-hitboxes", 0);

		rotation = getConfigDataFloat("rotation", 0F);
		velocity = getConfigDataFloat("velocity", 1F);
		hitRadius = getConfigDataFloat("hit-radius", 2F);
		vertSpread = getConfigDataFloat("vertical-spread", 0F);
		horizSpread = getConfigDataFloat("horizontal-spread", 0F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", 2F);

		visible = getConfigDataBoolean("visible", true);
		gravity = getConfigDataBoolean("gravity", true);
		charged = getConfigDataBoolean("charged", false);
		incendiary = getConfigDataBoolean("incendiary", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		stopOnModifierFail = getConfigDataBoolean("stop-on-modifier-fail", true);

		maxDuration = getConfigDataDouble("max-duration", 10);

		hitSpellName = getConfigString("spell", "");
		tickSpellName = getConfigString("spell-on-tick", "");
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		durationSpellName = getConfigString("spell-after-duration", "");
		entityLocationSpellName = getConfigString("spell-on-entity-location", "");

		projectileName = getConfigDataComponent("projectile-name", null);

		arrowColor = ConfigDataUtil.getColor(config.getMainConfig(), internalKey + "arrow-color", null);

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

		String error = "ProjectileSpell '" + internalName + "' has an invalid '%s' defined!";
		hitSpell = initSubspell(hitSpellName,
				error.formatted("spell"),
				true);
		groundSpell = initSubspell(groundSpellName,
				error.formatted("spell-on-hit-ground"),
				true);
		tickSpell = initSubspell(tickSpellName,
				error.formatted("spell-on-tick"),
				true);
		durationSpell = initSubspell(durationSpellName,
				error.formatted("spell-after-duration"),
				true);
		modifierSpell = initSubspell(modifierSpellName,
				error.formatted("spell-on-modifier-fail"),
				true);
		entityLocationSpell = initSubspell(entityLocationSpellName,
				error.formatted("spell-on-entity-location"),
				true);
	}

	@Override
	public void turnOff() {
		for (ProjectileTracker tracker : trackerSet) {
			tracker.stop(false);
		}
		trackerSet.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		return castAtLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		ProjectileTracker tracker = new ProjectileTracker(data);
		setupTracker(tracker, data);
		tracker.start();

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void setupTracker(ProjectileTracker tracker, SpellData data) {
		tracker.setSpell(this);

		ProjectileManager manager = ProjectileManagers.getManager(projectileType.get(data));
		tracker.setProjectileManager(manager);

		tracker.setRelativeOffset(relativeOffset.get(data));
		tracker.setEffectOffset(effectOffset.get(data));

		tracker.setTickInterval(tickInterval.get(data));
		tracker.setTickSpellInterval(tickSpellInterval.get(data));
		tracker.setSpecialEffectInterval(specialEffectInterval.get(data));
		tracker.setIntermediateEffects(intermediateEffects.get(data));
		tracker.setIntermediateHitboxes(intermediateHitboxes.get(data));

		tracker.setRotation(rotation.get(data));
		tracker.setVelocity(velocity.get(data));
		tracker.setHitRadius(hitRadius.get(data));
		tracker.setVertSpread(vertSpread.get(data));
		tracker.setHorizSpread(horizSpread.get(data));
		tracker.setVerticalHitRadius(verticalHitRadius.get(data));

		tracker.setVisible(visible.get(data));
		tracker.setGravity(gravity.get(data));
		tracker.setCharged(charged.get(data));
		tracker.setIncendiary(incendiary.get(data));
		tracker.setCallEvents(checkPlugins.get(data));
		tracker.setStopOnModifierFail(stopOnModifierFail.get(data));

		tracker.setMaxDuration(maxDuration.get(data) * TimeUtil.MILLISECONDS_PER_SECOND);

		tracker.setProjectileName(projectileName.get(data));

		tracker.setArrowColor(arrowColor.get(data));

		tracker.setHitSpell(hitSpell);
		tracker.setTickSpell(tickSpell);
		tracker.setGroundSpell(groundSpell);
		tracker.setModifierSpell(modifierSpell);
		tracker.setDurationSpell(durationSpell);
		tracker.setEntityLocationSpell(entityLocationSpell);

		tracker.setProjectileModifiers(projectileModifiers);

		tracker.setTargetList(validTargetList);
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		Entity entity = event.getEntity();
		if (!(entity instanceof WitherSkull projectile)) return;

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
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();

		Set<ProjectileTracker> trackers = new HashSet<>(trackerSet);
		for (ProjectileTracker tracker : trackers) {
			if (!projectile.equals(tracker.getProjectile())) continue;

			SpellData subData = tracker.getSpellData();

			if (event.getHitBlock() != null) {
				subData = subData.location(projectile.getLocation());

				if (tracker.getGroundSpell() != null) tracker.getGroundSpell().subcast(subData);

				tracker.stop();
				break;
			}

			if (event.getHitEntity() instanceof LivingEntity target) {
				event.setCancelled(true);
				tracker.hitEntity(target, subData.target(target));
				break;
			}
		}
	}

	private boolean locationsEqual(Location loc1, Location loc2) {
		return Math.abs(loc1.getX() - loc2.getX()) < 0.1
			&& Math.abs(loc1.getY() - loc2.getY()) < 0.1
			&& Math.abs(loc1.getZ() - loc2.getZ()) < 0.1;
	}

	public void playEffects(EffectPosition position, Location loc, SpellData data) {
		playSpellEffects(position, loc, data);
	}

	public void playEffects(EffectPosition position, Entity entity, SpellData data) {
		playSpellEffects(position, entity, data);
	}

	public Set<EffectlibSpellEffect> playEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellEffectLibEffects(position, location, data);
	}

	public Map<SpellEffect, DelayableEntity<Entity>> playEntityEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellEntityEffects(position, location, data);
	}

	public Set<DelayableEntity<ArmorStand>> playArmorStandEffectsProjectile(EffectPosition position, Location location, SpellData data) {
		return playSpellArmorStandEffects(position, location, data);
	}

	public static Set<ProjectileTracker> getProjectileTrackers() {
		return trackerSet;
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

}
