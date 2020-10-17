package com.nisovin.magicspells.spelleffects;

import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.trackers.BuffTracker;
import com.nisovin.magicspells.spelleffects.trackers.OrbitTracker;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.util.VectorUtils;

public abstract class SpellEffect {

	private final Random random = new Random();

	private int delay;
	private double chance;
	private double zOffset;
	private double heightOffset;
	private double forwardOffset;

	private Vector offset;
	private Vector relativeOffset;
	
	// for line
	private double maxDistance;
	private double distanceBetween;

	// for buff/orbit
	private float orbitXAxis;
	private float orbitYAxis;
	private float orbitZAxis;
	private float orbitRadius;
	private float orbitYOffset;
	private float horizOffset;
	private float horizExpandRadius;
	private float vertExpandRadius;
	private float ticksPerSecond;
	private float distancePerTick;
	private float secondsPerRevolution;

	private int ticksPerRevolution;
	private int horizExpandDelay;
	private int vertExpandDelay;
	private int effectInterval;

	private boolean counterClockwise;

	private List<String> modifiersList;
	private List<String> locationModifiersList;
	private ModifierSet modifiers;
	private ModifierSet locationModifiers;

	public void loadFromString(String string) {
		MagicSpells.plugin.getLogger().warning("Warning: single line effects are being removed, usage encountered: " + string);
	}
	
	public final void loadFromConfiguration(ConfigurationSection config) {
		delay = config.getInt("delay", 0);
		chance = config.getDouble("chance", -1) / 100;
		zOffset = config.getDouble("z-offset", 0);
		heightOffset = config.getDouble("height-offset", 0);
		forwardOffset = config.getDouble("forward-offset", 0);

		String[] offsetStr = config.getString("offset", "0,0,0").split(",");
		String[] relativeStr = config.getString("relative-offset", "0,0,0").split(",");
		offset = new Vector(Double.parseDouble(offsetStr[0]), Double.parseDouble(offsetStr[1]), Double.parseDouble(offsetStr[2]));
		relativeOffset = new Vector(Double.parseDouble(relativeStr[0]), Double.parseDouble(relativeStr[1]), Double.parseDouble(relativeStr[2]));

		maxDistance = config.getDouble("max-distance", 100);
		distanceBetween = config.getDouble("distance-between", 1);

		String path = "orbit-";
		orbitXAxis = (float) config.getDouble(path + "x-axis", 0F);
		orbitYAxis = (float) config.getDouble(path + "y-axis", 0F);
		orbitZAxis = (float) config.getDouble(path + "z-axis", 0F);
		orbitRadius = (float) config.getDouble(path + "radius", 1F);
		orbitYOffset = (float) config.getDouble(path + "y-offset", 0F);
		horizOffset = (float) config.getDouble(path + "horiz-offset", 0F);
		horizExpandRadius = (float) config.getDouble(path + "horiz-expand-radius", 0);
		vertExpandRadius = (float) config.getDouble(path + "vert-expand-radius", 0);
		secondsPerRevolution = (float) config.getDouble(path + "seconds-per-revolution", 3);

		horizExpandDelay = config.getInt(path + "horiz-expand-delay", 0);
		vertExpandDelay = config.getInt(path + "vert-expand-delay", 0);
		effectInterval = config.getInt("effect-interval", TimeUtil.TICKS_PER_SECOND);

		counterClockwise = config.getBoolean(path + "counter-clockwise", false);
		
		modifiersList = config.getStringList("modifiers");
		locationModifiersList = config.getStringList("location-modifiers");

		maxDistance *= maxDistance;
		ticksPerSecond = 20F / (float) effectInterval;
		ticksPerRevolution = Math.round(ticksPerSecond * secondsPerRevolution);
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);
		loadFromConfig(config);
	}

	public void initializeModifiers() {
		if (modifiersList != null) modifiers = new ModifierSet(modifiersList);
		if (locationModifiersList != null) locationModifiers = new ModifierSet(locationModifiersList);
	}
	
	protected abstract void loadFromConfig(ConfigurationSection config);

	private void applyOffsets(Location loc) {
		if (offset.getX() != 0 || offset.getY() != 0 || offset.getZ() != 0) loc.add(offset);
		if (relativeOffset.getX() != 0 || relativeOffset.getY() != 0 || relativeOffset.getZ() != 0) loc.add(VectorUtils.rotateVector(relativeOffset, loc));
		if (zOffset != 0) {
			Vector locDirection = loc.getDirection().normalize();
			Vector horizOffset = new Vector(-locDirection.getZ(), 0.0, locDirection.getX()).normalize();
			loc.add(horizOffset.multiply(zOffset)).getBlock().getLocation();
		}
		if (heightOffset != 0) loc.setY(loc.getY() + heightOffset);
		if (forwardOffset != 0) loc.add(loc.getDirection().setY(0).normalize().multiply(forwardOffset));
	}

	/**
	 * Plays an effect on the specified entity.
	 * @param entity the entity to play the effect on
	 */
	public Runnable playEffect(final Entity entity) {
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;
		if (entity instanceof LivingEntity && modifiers != null && !modifiers.check((LivingEntity) entity)) return null;
		if (delay <= 0) return playEffectEntity(entity);
		MagicSpells.scheduleDelayedTask(() -> playEffectEntity(entity), delay);
		return null;
	}
	
	protected Runnable playEffectEntity(Entity entity) {
		return playEffectLocationReal(entity == null ? null : entity.getLocation());
	}
	
	/**
	 * Plays an effect at the specified location.
	 * @param location location to play the effect at
	 */
	public final Runnable playEffect(final Location location) {
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;
		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;
		if (delay <= 0) return playEffectLocationReal(location);
		MagicSpells.scheduleDelayedTask(() -> playEffectLocationReal(location), delay);
		return null;
	}

	public final Effect playEffectLib(final Location location) {
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;
		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;
		if (delay <= 0) return playEffectLibLocationReal(location);
		MagicSpells.scheduleDelayedTask(() -> playEffectLibLocationReal(location), delay);
		return null;
	}

	public final Entity playEntityEffect(final Location location) {
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;
		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;
		if (delay <= 0) return playEntityEffectLocationReal(location);
		MagicSpells.scheduleDelayedTask(() -> playEffectLibLocationReal(location), delay);
		return null;
	}

	public final ArmorStand playArmorStandEffect(final Location location) {
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;
		if (locationModifiers != null && !locationModifiers.check(null, location)) return null;
		if (delay <= 0) return playArmorStandEffectLocationReal(location);
		MagicSpells.scheduleDelayedTask(() -> playEffectLibLocationReal(location), delay);
		return null;
	}
	
	private Runnable playEffectLocationReal(Location location) {
		if (location == null) return playEffectLocation(null);
		Location loc = location.clone();
		applyOffsets(loc);
		return playEffectLocation(loc);
	}

	private Effect playEffectLibLocationReal(Location location) {
		if (location == null) return playEffectLibLocation(null);
		Location loc = location.clone();
		applyOffsets(loc);
		return playEffectLibLocation(loc);
	}

	private Entity playEntityEffectLocationReal(Location location) {
		if (location == null) return playEntityEffectLocation(null);
		Location loc = location.clone();
		applyOffsets(loc);
		return playEntityEffectLocation(loc);
	}

	private ArmorStand playArmorStandEffectLocationReal(Location location) {
		if (location == null) return playArmorStandEffectLocation(null);
		Location loc = location.clone();
		applyOffsets(loc);
		return playArmorStandEffectLocation(loc);
	}
	
	protected Runnable playEffectLocation(Location location) {
		//expect to be overridden
		return null;
	}

	protected Effect playEffectLibLocation(Location location) {
		//expect to be overridden
		return null;
	}

	protected Entity playEntityEffectLocation(Location location) {
		//expect to be overridden
		return null;
	}

	protected ArmorStand playArmorStandEffectLocation(Location location) {
		//expect to be overridden
		return null;
	}
	
	/**
	 * Plays an effect between two locations (such as a smoke trail type effect).
	 * @param location1 the starting location
	 * @param location2 the ending location
	 */
	public Runnable playEffect(Location location1, Location location2) {
		if (location1.distanceSquared(location2) > maxDistance) return null;
		Location loc1 = location1.clone();
		Location loc2 = location2.clone();
		//double localHeightOffset = heightOffsetExpression.resolveValue(null, null, location1, location2).doubleValue();
		//double localForwardOffset = forwardOffsetExpression.resolveValue(null, null, location1, location2).doubleValue();
		int c = (int) Math.ceil(loc1.distance(loc2) / distanceBetween) - 1;
		if (c <= 0) return null;
		Vector v = loc2.toVector().subtract(loc1.toVector()).normalize().multiply(distanceBetween);
		Location l = loc1.clone();
		if (heightOffset != 0) l.setY(l.getY() + heightOffset);
		
		for (int i = 0; i < c; i++) {
			l.add(v);
			playEffect(l);
		}
		return null;
	}
	
	public BuffTracker playEffectWhileActiveOnEntity(final Entity entity, final SpellEffectActiveChecker checker) {
		return new BuffTracker(entity, checker, this);
	}
	
	public OrbitTracker playEffectWhileActiveOrbit(final Entity entity, final SpellEffectActiveChecker checker) {
		return new OrbitTracker(entity, checker, this);
	}
	
	@FunctionalInterface
	public interface SpellEffectActiveChecker {
		boolean isActive(Entity entity);
	}

	public int getDelay() {
		return delay;
	}

	public double getChance() {
		return chance;
	}

	public double getZOffset() {
		return zOffset;
	}

	public double getHeightOffset() {
		return heightOffset;
	}

	public double getForwardOffset() {
		return forwardOffset;
	}

	public Vector getOffset() {
		return offset;
	}

	public Vector getRelativeOffset() {
		return relativeOffset;
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public double getDistanceBetween() {
		return distanceBetween;
	}

	public float getOrbitXAxis() {
		return orbitXAxis;
	}

	public float getOrbitYAxis() {
		return orbitYAxis;
	}

	public float getOrbitZAxis() {
		return orbitZAxis;
	}

	public float getOrbitRadius() {
		return orbitRadius;
	}

	public float getOrbitYOffset() {
		return orbitYOffset;
	}

	public float getHorizOffset() {
		return horizOffset;
	}

	public float getHorizExpandRadius() {
		return horizExpandRadius;
	}

	public float getVertExpandRadius() {
		return vertExpandRadius;
	}

	public float getTicksPerSecond() {
		return ticksPerSecond;
	}

	public float getDistancePerTick() {
		return distancePerTick;
	}

	public float getSecondsPerRevolution() {
		return secondsPerRevolution;
	}

	public int getTicksPerRevolution() {
		return ticksPerRevolution;
	}

	public int getHorizExpandDelay() {
		return horizExpandDelay;
	}

	public int getVertExpandDelay() {
		return vertExpandDelay;
	}

	public int getEffectInterval() {
		return effectInterval;
	}

	public boolean isCounterClockwise() {
		return counterClockwise;
	}

	public ModifierSet getModifiers() {
		return modifiers;
	}
	
	public void playTrackingLinePatterns(Location origin, Location target, Entity originEntity, Entity targetEntity) {
		// no op, effects should override this with their own behavior
	}

}
