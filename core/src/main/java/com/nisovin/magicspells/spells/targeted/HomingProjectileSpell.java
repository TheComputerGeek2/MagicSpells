package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.projectile.ProjectileManager;
import com.nisovin.magicspells.util.projectile.ProjectileManagers;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.util.projectile.ProjectileManagerThrownPotion;

public class HomingProjectileSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private NoMagicZoneManager zoneManager;

	private final List<HomingProjectileMonitor> monitors;

	private final ConfigData<String> projectileType;

	private final ConfigData<Vector> relativeOffset;
	private final ConfigData<Vector> targetRelativeOffset;

	private final ConfigData<Integer> tickInterval;
	private final ConfigData<Integer> airSpellInterval;
	private final ConfigData<Integer> specialEffectInterval;
	private final ConfigData<Integer> intermediateSpecialEffects;

	private final ConfigData<Float> velocity;
	private final ConfigData<Float> hitRadius;
	private final ConfigData<Float> verticalHitRadius;

	private final ConfigData<Boolean> visible;
	private final ConfigData<Boolean> charged;
	private final ConfigData<Boolean> incendiary;
	private final ConfigData<Boolean> stopOnModifierFail;
	private final ConfigData<Boolean> powerAffectsVelocity;

	private final ConfigData<Double> maxDuration;

	private final ConfigData<Component> projectileName;

	private final ConfigData<Color> arrowColor;

	private final String hitSpellName;
	private final String airSpellName;
	private final String groundSpellName;
	private final String modifierSpellName;
	private final String durationSpellName;

	private Subspell hitSpell;
	private Subspell airSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;

	private ModifierSet homingModifiers;
	private List<String> homingModifiersStrings;

	public HomingProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		monitors = new ArrayList<>();

		projectileType = getConfigDataString("projectile-type", "arrow");

		arrowColor = ConfigDataUtil.getColor(config.getMainConfig(), internalKey + "arrow-color", null);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(0.5, 0.5, 0));
		targetRelativeOffset = getConfigDataVector("target-relative-offset", new Vector(0, 0.5, 0));

		tickInterval = getConfigDataInt("tick-interval", 1);
		airSpellInterval = getConfigDataInt("spell-interval", 20);
		specialEffectInterval = getConfigDataInt("special-effect-interval", 0);
		intermediateSpecialEffects = getConfigDataInt("intermediate-special-effect-locations", 0);

		velocity = getConfigDataFloat("velocity", 1F);
		hitRadius = getConfigDataFloat("hit-radius", 2F);
		verticalHitRadius = getConfigDataFloat("vertical-hit-radius", 2F);

		visible = getConfigDataBoolean("visible", true);
		charged = getConfigDataBoolean("charged", false);
		incendiary = getConfigDataBoolean("incendiary", false);
		stopOnModifierFail = getConfigDataBoolean("stop-on-modifier-fail", true);
		powerAffectsVelocity = getConfigDataBoolean("power-affects-velocity", true);

		maxDuration = getConfigDataDouble("max-duration", 10);

		hitSpellName = getConfigString("spell", "");
		airSpellName = getConfigString("spell-on-hit-air", "");
		projectileName = getConfigDataComponent("projectile-name", Component.empty());
		groundSpellName = getConfigString("spell-on-hit-ground", "");
		modifierSpellName = getConfigString("spell-on-modifier-fail", "");
		durationSpellName = getConfigString("spell-after-duration", "");

		homingModifiersStrings = getConfigStringList("homing-modifiers", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (homingModifiersStrings != null && !homingModifiersStrings.isEmpty()) {
			homingModifiers = new ModifierSet(homingModifiersStrings, this);
			homingModifiersStrings = null;
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "HomingProjectileSpell '" + internalName + "' has an invalid '%s' defined!";
		hitSpell = initSubspell(hitSpellName,
				error.formatted("spell"),
				true);
		groundSpell = initSubspell(groundSpellName,
				error.formatted("spell-on-hit-ground"),
				true);
		airSpell = initSubspell(airSpellName,
				error.formatted("spell-on-hit-air"),
				true);
		durationSpell = initSubspell(durationSpellName,
				error.formatted("spell-after-duration"),
				true);
		modifierSpell = initSubspell(modifierSpellName,
				error.formatted("spell-on-modifier-fail"),
				true);

		zoneManager = MagicSpells.getNoMagicZoneManager();
	}

	@Override
	public void turnOff() {
		for (HomingProjectileMonitor monitor : monitors) monitor.stop(false);
		monitors.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData().location(data.caster().getLocation());

		new HomingProjectileMonitor(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		data = data.location(data.caster().getLocation());

		new HomingProjectileMonitor(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		new HomingProjectileMonitor(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler
	public void onProjectileHit(EntityDamageByEntityEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) return;
		if (!(event.getEntity() instanceof LivingEntity entity)) return;
		Entity damagerEntity = event.getDamager();
		if (!(damagerEntity instanceof Projectile projectile)) return;

		for (HomingProjectileMonitor monitor : monitors) {
			if (!monitor.projectile.equals(projectile)) continue;
			if (!entity.equals(monitor.data.target())) continue;

			if (hitSpell != null) hitSpell.subcast(monitor.data.noLocation());
			playSpellEffects(EffectPosition.TARGET, entity, monitor.data);

			event.setCancelled(true);
			monitor.stop();

			break;
		}
	}

	@EventHandler
	public void onProjectileBlockHit(ProjectileHitEvent e) {
		Projectile projectile = e.getEntity();
		Block block = e.getHitBlock();
		if (block == null) return;
		for (HomingProjectileMonitor monitor : monitors) {
			if (!projectile.equals(monitor.projectile)) continue;

			Location location = projectile.getLocation();
			SpellData data = monitor.data.builder().target(null).location(location).build();

			if (groundSpell != null) groundSpell.subcast(data);
			playSpellEffects(EffectPosition.TARGET, location, data);

			monitor.stop();
			break;
		}

	}

	private class HomingProjectileMonitor implements Runnable {

		private Vector currentVelocity;
		private SpellData data;

		private final Location currentLocation;
		private final Projectile projectile;
		private final BoundingBox hitBox;
		private final long startTime;
		private final ScheduledTask task;

		private final boolean stopOnModifierFail;

		private final int airSpellInterval;
		private final int specialEffectInterval;
		private final int intermediateSpecialEffects;

		private final float velocity;

		private final double maxDuration;
		private final double targetYOffset;

		private final Vector targetRelativeOffset;

		private int counter = 0;

		private HomingProjectileMonitor(SpellData data) {
			startTime = System.currentTimeMillis();

			currentLocation = data.location();

			Vector relativeOffset = HomingProjectileSpell.this.relativeOffset.get(data);
			currentLocation.add(0, relativeOffset.getY(), 0);
			Util.applyRelativeOffset(currentLocation, relativeOffset.setY(0));

			data = data.location(currentLocation);
			this.data = data;

			ProjectileManager manager = ProjectileManagers.getManager(projectileType.get(data));
			SpellData finalData = data;
			projectile = currentLocation.getWorld().spawn(currentLocation, manager.getProjectileClass(), proj -> {
				proj.setShooter(finalData.caster());
				proj.setVisibleByDefault(visible.get(finalData));

				Component projectileName = HomingProjectileSpell.this.projectileName.get(finalData);
				if (projectileName != null && !Util.getPlainString(projectileName).isEmpty()) {
					proj.customName(projectileName);
					proj.setCustomNameVisible(true);
				}
				if (proj instanceof Arrow arrow) arrow.setColor(arrowColor.get(finalData));
				if (proj instanceof WitherSkull witherSkull) witherSkull.setCharged(charged.get(finalData));
				if (proj instanceof Explosive explosive) explosive.setIsIncendiary(incendiary.get(finalData));
				if (proj instanceof ProjectileManagerThrownPotion potion) {
					((ThrownPotion) proj).setItem(potion.getItem());
				}
			});

			float hitRadius = HomingProjectileSpell.this.hitRadius.get(data);
			float verticalHitRadius = HomingProjectileSpell.this.verticalHitRadius.get(data);
			hitBox = new BoundingBox(currentLocation, hitRadius, verticalHitRadius);

			stopOnModifierFail = HomingProjectileSpell.this.stopOnModifierFail.get(data);

			airSpellInterval = HomingProjectileSpell.this.airSpellInterval.get(data);
			specialEffectInterval = HomingProjectileSpell.this.specialEffectInterval.get(data);

			intermediateSpecialEffects = Math.min(HomingProjectileSpell.this.intermediateSpecialEffects.get(data), 0);

			float velocity = HomingProjectileSpell.this.velocity.get(data);
			if (powerAffectsVelocity.get(data)) velocity *= data.power();
			this.velocity = velocity;

			maxDuration = HomingProjectileSpell.this.maxDuration.get(data) * TimeUtil.MILLISECONDS_PER_SECOND;

			targetRelativeOffset = HomingProjectileSpell.this.targetRelativeOffset.get(data);
			targetYOffset = targetRelativeOffset.getY();
			targetRelativeOffset.setY(0);

			currentVelocity = data.target().getLocation().add(0, 0.75, 0).subtract(projectile.getLocation()).toVector().normalize();
			currentVelocity.multiply(velocity);
			currentVelocity.setY(currentVelocity.getY() + 0.15);
			projectile.setVelocity(currentVelocity);

			monitors.add(this);

			int tickInterval = HomingProjectileSpell.this.tickInterval.get(data);
			task = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval, projectile);

			playSpellEffects(EffectPosition.CASTER, currentLocation, data);
			playSpellEffects(EffectPosition.PROJECTILE, projectile, data);
			playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, currentLocation, projectile.getLocation(), data.caster(), projectile, data);
		}

		@Override
		public void run() {
			if (!data.caster().isValid() || !data.target().isValid()) {
				stop();
				return;
			}

			if (!projectile.isValid()) {
				stop();
				return;
			}

			if (!projectile.getLocation().getWorld().equals(data.target().getWorld())) {
				stop();
				return;
			}

			if (zoneManager.willFizzle(currentLocation, HomingProjectileSpell.this)) {
				stop();
				return;
			}

			if (homingModifiers != null) {
				ModifierResult result = homingModifiers.apply(data.caster(), data);
				data = result.data();

				if (!result.check()) {
					if (modifierSpell != null) modifierSpell.subcast(data.noTarget());

					if (stopOnModifierFail) stop();
					return;
				}
			}

			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				if (durationSpell != null) durationSpell.subcast(data.noTarget());
				stop();
				return;
			}

			Location previousLocation = projectile.getLocation();

			Vector oldVelocity = new Vector(currentVelocity.getX(), currentVelocity.getY(), currentVelocity.getZ());

			Location targetLoc = data.target().getLocation();
			Util.applyRelativeOffset(targetLoc, targetRelativeOffset);
			targetLoc.add(0, targetYOffset, 0);

			currentVelocity = targetLoc.toVector().subtract(projectile.getLocation().toVector()).normalize();
			currentVelocity.multiply(velocity);
			currentVelocity.setY(currentVelocity.getY() + 0.15);
			projectile.setVelocity(currentVelocity);
			projectile.getLocation(currentLocation);
			data = data.location(currentLocation);

			if (counter % airSpellInterval == 0 && airSpell != null) airSpell.subcast(data.noTarget());

			if (intermediateSpecialEffects > 0) playIntermediateEffectLocations(previousLocation, oldVelocity);

			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0)
				playSpellEffects(EffectPosition.SPECIAL, currentLocation, data);

			counter++;

			hitBox.setCenter(currentLocation);
			if (hitBox.contains(targetLoc)) {
				SpellTargetEvent targetEvent = new SpellTargetEvent(HomingProjectileSpell.this, data);
				if (!targetEvent.callEvent()) return;
				data = targetEvent.getSpellData();

				if (hitSpell != null) hitSpell.subcast(data.noLocation());
				playSpellEffects(EffectPosition.TARGET, data.target(), data);

				stop();
			}
		}

		private void playIntermediateEffectLocations(Location old, Vector movement) {
			int divideFactor = intermediateSpecialEffects + 1;
			movement.setX(movement.getX() / divideFactor);
			movement.setY(movement.getY() / divideFactor);
			movement.setZ(movement.getZ() / divideFactor);
			for (int i = 0; i < intermediateSpecialEffects; i++) {
				old = old.add(movement).setDirection(movement);
				playSpellEffects(EffectPosition.SPECIAL, old, data);
			}
		}

		private void stop() {
			stop(true);
		}

		private void stop(boolean remove) {
			playSpellEffects(EffectPosition.DELAYED, currentLocation, data);
			MagicSpells.cancelTask(task);
			projectile.remove();
			if (remove) monitors.remove(this);
		}

	}

}
