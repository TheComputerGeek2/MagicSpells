package com.nisovin.magicspells.util.trackers;

import java.util.Map;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Color;
import org.bukkit.entity.*;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import net.kyori.adventure.text.Component;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.ModifiedEffect;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.projectile.*;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.TrackerMoveEvent;
import com.nisovin.magicspells.zones.NoMagicZoneManager;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.instant.ProjectileSpell;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;

public class ProjectileTracker implements Runnable, Tracker {

	private final Random rand = ThreadLocalRandom.current();

	private Set<EffectlibSpellEffect> effectSet;
	private Map<SpellEffect, DelayableEntity<Entity>> entityMap;
	private Set<DelayableEntity<ArmorStand>> armorStandSet;

	private ProjectileSpell spell;

	private NoMagicZoneManager zoneManager;

	private ProjectileManager projectileManager;

	private Vector relativeOffset;
	private Vector effectOffset;

	private int tickInterval;
	private int tickSpellInterval;
	private int specialEffectInterval;
	private int intermediateEffects;
	private int intermediateHitboxes;

	private float rotation;
	private float velocity;
	private float hitRadius;
	private float vertSpread;
	private float horizSpread;
	private float verticalHitRadius;

	private boolean visible;
	private boolean gravity;
	private boolean charged;
	private boolean incendiary;
	private boolean callEvents;
	private boolean stopOnModifierFail;

	private double maxDuration;

	private Component projectileName;

	private Color arrowColor;

	private Subspell hitSpell;
	private Subspell tickSpell;
	private Subspell groundSpell;
	private Subspell modifierSpell;
	private Subspell durationSpell;
	private Subspell entityLocationSpell;

	private ModifierSet projectileModifiers;

	private Projectile projectile;
	private Location previousLocation;
	private Location currentLocation;
	private Location startLocation;
	private Vector currentVelocity;
	private SpellData data;
	private long startTime;

	private ValidTargetList targetList;

	private ScheduledTask task;
	private int counter = 0;

	private boolean stopped = false;

	public ProjectileTracker(SpellData data) {
		this.startLocation = data.location();
		this.data = data;
	}

	public void start() {
		initialize();
	}

	@Override
	public void initialize() {
		zoneManager = MagicSpells.getNoMagicZoneManager();
		startTime = System.currentTimeMillis();
		task = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval, projectile);

		startLocation.add(0, relativeOffset.getY(), 0);
		Util.applyRelativeOffset(startLocation, relativeOffset.setY(0));

		currentLocation = startLocation.clone();
		currentVelocity = startLocation.getDirection();
		currentVelocity.multiply(velocity * data.power());
		if (rotation != 0) Util.rotateVector(currentVelocity, rotation);
		if (horizSpread > 0 || vertSpread > 0) {
			float rx = -1 + rand.nextFloat() * 2;
			float ry = -1 + rand.nextFloat() * 2;
			float rz = -1 + rand.nextFloat() * 2;
			currentVelocity.add(new Vector(rx * horizSpread, ry * vertSpread, rz * horizSpread));
		}

		projectile = startLocation.getWorld().spawn(startLocation, projectileManager.getProjectileClass(), proj -> {
			proj.setVisibleByDefault(visible);
			proj.setVelocity(currentVelocity);
			proj.setShooter(data.caster());
			proj.setGravity(gravity);
			if (projectileName != null && !Util.getPlainString(projectileName).isEmpty()) {
				proj.customName(projectileName);
				proj.setCustomNameVisible(true);
			}
			if (proj instanceof Arrow arrow) arrow.setColor(arrowColor);
			if (proj instanceof WitherSkull witherSkull) witherSkull.setCharged(charged);
			if (proj instanceof Explosive explosive) explosive.setIsIncendiary(incendiary);
			if (projectileManager instanceof ProjectileManagerThrownPotion potion) {
				((ThrownPotion) proj).setItem(potion.getItem());
			}
		});

		if (spell != null) {
			spell.playEffects(EffectPosition.CASTER, startLocation, data);
			effectSet = spell.playEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			entityMap = spell.playEntityEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			armorStandSet = spell.playArmorStandEffectsProjectile(EffectPosition.PROJECTILE, currentLocation, data);
			spell.playTrackingLinePatterns(EffectPosition.DYNAMIC_CASTER_PROJECTILE_LINE, startLocation, projectile.getLocation(), data.caster(), projectile, data);
		}

		ProjectileSpell.getProjectileTrackers().add(this);
	}

	@Override
	public void run() {
		if (data.hasCaster() && !data.caster().isValid()) {
			stop();
			return;
		}

		if (projectile == null || projectile.isDead()) {
			stop();
			return;
		}

		if (zoneManager.willFizzle(currentLocation, spell)) {
			stop();
			return;
		}

		if (projectileModifiers != null) {
			ModifierResult result = projectileModifiers.apply(data.caster(), data);
			data = result.data();

			if (!result.check()) {
				if (modifierSpell != null) modifierSpell.subcast(data);
				if (stopOnModifierFail) stop();
				return;
			}
		}

		if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
			if (durationSpell != null) durationSpell.subcast(data);
			stop();
			return;
		}

		previousLocation = currentLocation.clone();
		currentLocation = projectile.getLocation().clone();
		currentLocation.setDirection(projectile.getVelocity());

		if (callEvents) {
			TrackerMoveEvent trackerMoveEvent = new TrackerMoveEvent(this, previousLocation, currentLocation);
			EventUtil.call(trackerMoveEvent);
			if (stopped) return;
		}

		data = data.location(currentLocation);

		if (counter % tickSpellInterval == 0 && tickSpell != null) tickSpell.subcast(data);

		if (spell != null) {
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) spell.playEffects(EffectPosition.SPECIAL, currentLocation, data);
			if (intermediateEffects > 0) playIntermediateEffects();
		}

		if (effectSet != null) {
			Effect effect;
			Location effectLoc;
			for (EffectlibSpellEffect spellEffect : effectSet) {
				effect = spellEffect.getEffect();

				effectLoc = spellEffect.getSpellEffect().applyOffsets(currentLocation.clone(), data);
				effect.setLocation(effectLoc);

				if (effect instanceof ModifiedEffect mod) {
					Effect modifiedEffect = mod.getInnerEffect();
					if (modifiedEffect != null) modifiedEffect.setLocation(effectLoc);
				}
			}
		}

		if (entityMap != null || armorStandSet != null) {
			// Changing the effect location
			Location effectLoc = currentLocation.clone();
			Util.applyRelativeOffset(effectLoc, effectOffset.clone().setY(0));
			effectLoc.add(0, effectOffset.getY(), 0);

			if (entityMap != null) {
				for (var entry : entityMap.entrySet()) {
					entry.getValue().teleport(entry.getKey().applyOffsets(effectLoc.clone(), data));
				}
			}

			if (armorStandSet != null) {
				armorStandSet.forEach(stand -> stand.teleport(effectLoc));
			}
		}

		counter++;

		if (intermediateHitboxes > 0) checkIntermediateHitboxes();
		if (!stopped) checkHitbox(currentLocation);
	}

	public void playIntermediateEffects() {
		if (!(specialEffectInterval > 0 && counter % specialEffectInterval == 0))
			return;

		int divideFactor = intermediateEffects + 1;

		Vector v = new Vector(
			(currentLocation.getX() - previousLocation.getX()) / divideFactor,
			(currentLocation.getY() - previousLocation.getY()) / divideFactor,
			(currentLocation.getZ() - previousLocation.getZ()) / divideFactor
		);
		if (v.isZero()) return;

		Location old = previousLocation.clone();
		old.setDirection(v);

		for (int i = 0; i < intermediateEffects; i++)
			spell.playEffects(EffectPosition.SPECIAL, old.add(v), data.location(old));
	}

	public void checkIntermediateHitboxes() {
		int divideFactor = intermediateHitboxes + 1;

		Vector v = new Vector(
			(currentLocation.getX() - previousLocation.getX()) / divideFactor,
			(currentLocation.getY() - previousLocation.getY()) / divideFactor,
			(currentLocation.getZ() - previousLocation.getZ()) / divideFactor
		);
		if (v.isZero()) return;

		Location old = previousLocation.clone();
		old.setDirection(v);

		for (int i = 0; i < intermediateHitboxes; i++) {
			checkHitbox(old.add(v));
			if (stopped) return;
		}
	}

	public void checkHitbox(Location location) {
		if (location == null || !data.hasCaster()) return;
		if (projectile == null) return;

		SpellData data = this.data.location(location);

		for (LivingEntity entity : location.getNearbyLivingEntities(hitRadius, verticalHitRadius, hitRadius)) {
			if (!targetList.canTarget(data.caster(), entity)) continue;

			SpellTargetEvent event = new SpellTargetEvent(spell, data, entity);
			if (!event.callEvent()) continue;

			SpellData subData = event.getSpellData();
			if (hitSpell != null) hitSpell.subcast(subData.noLocation());
			if (entityLocationSpell != null) entityLocationSpell.subcast(subData.noTarget());

			stop();
			return;
		}
	}

	@Override
	public void stop() {
		stop(true);
	}

	public void stop(boolean removeTracker) {
		if (spell != null) {
			spell.playEffects(EffectPosition.DELAYED, currentLocation, data);
			if (removeTracker) ProjectileSpell.getProjectileTrackers().remove(this);
		}
		MagicSpells.cancelTask(task);
		if (effectSet != null) {
			for (EffectlibSpellEffect spellEffect : effectSet) {
				spellEffect.getEffect().cancel();
			}
			effectSet.clear();
		}
		if (entityMap != null) {
			entityMap.values().forEach(DelayableEntity::remove);
			entityMap.clear();
		}
		if (armorStandSet != null) {
			armorStandSet.forEach(DelayableEntity::remove);
			armorStandSet.clear();
		}
		currentLocation = null;
		if (projectile != null) projectile.remove();
		projectile = null;
		stopped = true;
	}

	public ProjectileSpell getSpell() {
		return spell;
	}

	public void setSpell(ProjectileSpell spell) {
		this.spell = spell;
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

	public Vector getEffectOffset() {
		return effectOffset;
	}

	public void setEffectOffset(Vector effectOffset) {
		this.effectOffset = effectOffset;
	}

	public int getTickInterval() {
		return tickInterval;
	}

	public void setTickInterval(int tickInterval) {
		this.tickInterval = tickInterval;
	}

	public int getTickSpellInterval() {
		return tickSpellInterval;
	}

	public void setTickSpellInterval(int tickSpellInterval) {
		this.tickSpellInterval = tickSpellInterval;
	}

	public int getSpecialEffectInterval() {
		return specialEffectInterval;
	}

	public void setSpecialEffectInterval(int specialEffectInterval) {
		this.specialEffectInterval = specialEffectInterval;
	}

	public void setIntermediateEffects(int intermediateEffects) {
		this.intermediateEffects = intermediateEffects;
	}

	public int getIntermediateEffects() {
		return intermediateEffects;
	}

	public void setIntermediateHitboxes(int intermediateHitboxes) {
		this.intermediateHitboxes = intermediateHitboxes;
	}

	public int getIntermediateHitboxes() {
		return intermediateHitboxes;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public float getVelocity() {
		return velocity;
	}

	public void setVelocity(float velocity) {
		this.velocity = velocity;
	}

	public float getHitRadius() {
		return hitRadius;
	}

	public void setHitRadius(float hitRadius) {
		this.hitRadius = hitRadius;
	}

	public float getVertSpread() {
		return vertSpread;
	}

	public void setVertSpread(float vertSpread) {
		this.vertSpread = vertSpread;
	}

	public float getHorizSpread() {
		return horizSpread;
	}

	public void setHorizSpread(float horizSpread) {
		this.horizSpread = horizSpread;
	}

	public float getVerticalHitRadius() {
		return verticalHitRadius;
	}

	public void setVerticalHitRadius(float verticalHitRadius) {
		this.verticalHitRadius = verticalHitRadius;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
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

	public Color getArrowColor() {
		return arrowColor;
	}

	public void setArrowColor(Color arrowColor) {
		this.arrowColor = arrowColor;
	}

	public boolean shouldStopOnModifierFail() {
		return stopOnModifierFail;
	}

	public void setStopOnModifierFail(boolean stopOnModifierFail) {
		this.stopOnModifierFail = stopOnModifierFail;
	}

	public double getMaxDuration() {
		return maxDuration;
	}

	public void setMaxDuration(double maxDuration) {
		this.maxDuration = maxDuration;
	}

	public Component getProjectileName() {
		return projectileName;
	}

	public void setProjectileName(Component projectileName) {
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

	public Projectile getProjectile() {
		return projectile;
	}

	public void setProjectile(Projectile projectile) {
		this.projectile = projectile;
	}

	public Location getPreviousLocation() {
		return previousLocation;
	}

	public void setPreviousLocation(Location previousLocation) {
		this.previousLocation = previousLocation;
	}

	public Location getCurrentLocation() {
		return currentLocation;
	}

	public void setCurrentLocation(Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	public Location getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Location startLocation) {
		this.startLocation = startLocation;
	}

	public LivingEntity getCaster() {
		return data.caster();
	}

	public void setCaster(LivingEntity caster) {
		data = data.caster(caster);
	}

	public Vector getCurrentVelocity() {
		return currentVelocity;
	}

	public void setCurrentVelocity(Vector currentVelocity) {
		this.currentVelocity = currentVelocity;
	}

	public float getPower() {
		return data.power();
	}

	public void setPower(float power) {
		data = data.power(power);
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public ValidTargetList getTargetList() {
		return targetList;
	}

	public void setTargetList(ValidTargetList targetList) {
		this.targetList = targetList;
	}

	public boolean shouldCallEvents() {
		return callEvents;
	}

	public void setCallEvents(boolean callEvents) {
		this.callEvents = callEvents;
	}

	public String[] getArgs() {
		return data.args();
	}

	public void setArgs(String[] args) {
		data = data.args(args);
	}

	public SpellData getSpellData() {
		return data;
	}

	public void setSpellData(SpellData data) {
		this.data = data;
	}

}
