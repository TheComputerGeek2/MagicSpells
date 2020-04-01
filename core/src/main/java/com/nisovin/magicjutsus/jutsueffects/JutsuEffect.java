package com.nisovin.magicjutsus.jutsueffects;

import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;

public abstract class JutsuEffect {

	private static Map<String, Class<? extends JutsuEffect>> effects = new HashMap<>();

	private int delay;
	private double chance;
	private double zOffset;
	private double heightOffset;
	private double forwardOffset;
	
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
	private int effectInterval = TimeUtil.TICKS_PER_SECOND;

	private boolean counterClockwise;

	private ModifierSet modifiers;
	private Random random = new Random();

	public void loadFromString(String string) {
		MagicJutsus.plugin.getLogger().warning("Warning: single line effects are being removed, usage encountered: " + string);
	}
	
	public final void loadFromConfiguration(ConfigurationSection config) {
		delay = config.getInt("delay", 0);
		chance = config.getDouble("chance", -1) / 100;
		zOffset = config.getDouble("z-offset", 0);
		heightOffset = config.getDouble("height-offset", 0);
		forwardOffset = config.getDouble("forward-offset", 0);

		maxDistance = config.getDouble("max-distance", 100);
		distanceBetween = config.getDouble("distance-between", 1);

		orbitXAxis = (float) config.getDouble("orbit-x-axis", 0F);
		orbitYAxis = (float) config.getDouble("orbit-y-axis", 0F);
		orbitZAxis = (float) config.getDouble("orbit-z-axis", 0F);
		orbitRadius = (float) config.getDouble("orbit-radius", 1F);
		orbitYOffset = (float) config.getDouble("orbit-y-offset", 0F);
		horizOffset = (float) config.getDouble("orbit-horiz-offset", 0F);
		horizExpandRadius = (float) config.getDouble("orbit-horiz-expand-radius", 0);
		vertExpandRadius = (float) config.getDouble("orbit-vert-expand-radius", 0);
		secondsPerRevolution = (float) config.getDouble("orbit-seconds-per-revolution", 3);

		horizExpandDelay = config.getInt("orbit-horiz-expand-delay", 0);
		vertExpandDelay = config.getInt("orbit-vert-expand-delay", 0);
		effectInterval = config.getInt("effect-interval", effectInterval);

		counterClockwise = config.getBoolean("orbit-counter-clockwise", false);
		
		List<String> list = config.getStringList("modifiers");
		if (list != null) modifiers = new ModifierSet(list);

		maxDistance *= maxDistance;
		ticksPerSecond = 20F / (float) effectInterval;
		ticksPerRevolution = Math.round(ticksPerSecond * secondsPerRevolution);
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);
		loadFromConfig(config);
	}
	
	protected abstract void loadFromConfig(ConfigurationSection config);
	
	/**
	 * Plays an effect on the specified entity.
	 * @param entity the entity to play the effect on
	 */
	public Runnable playEffect(final Entity entity) {
		if (chance > 0 && chance < 1 && random.nextDouble() > chance) return null;
		if (delay <= 0) return playEffectEntity(entity);
		MagicJutsus.scheduleDelayedTask(() -> playEffectEntity(entity), delay);
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
		if (delay <= 0) return playEffectLocationReal(location);
		MagicJutsus.scheduleDelayedTask(() -> playEffectLocationReal(location), delay);
		return null;
	}
	
	private Runnable playEffectLocationReal(Location location) {
		if (location == null) return playEffectLocation(null);
		Location loc = location.clone();
		if (zOffset != 0) {
			Vector locDirection = loc.getDirection().normalize();
			Vector horizOffset = new Vector(-locDirection.getZ(), 0.0, locDirection.getX()).normalize();
			loc.add(horizOffset.multiply(zOffset)).getBlock().getLocation();
		}
		if (heightOffset != 0) loc.setY(loc.getY() + heightOffset);
		if (forwardOffset != 0) loc.add(loc.getDirection().setY(0).normalize().multiply(forwardOffset));
		return playEffectLocation(loc);
	}
	
	protected Runnable playEffectLocation(Location location) {
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
	
	public BuffTracker playEffectWhileActiveOnEntity(final Entity entity, final JutsuEffectActiveChecker checker) {
		return new BuffTracker(entity, checker, this);
	}
	
	public OrbitTracker playEffectWhileActiveOrbit(final Entity entity, final JutsuEffectActiveChecker checker) {
		return new OrbitTracker(entity, checker, this);
	}
	
	@FunctionalInterface
	public interface JutsuEffectActiveChecker {
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

	/**
	 * Gets the GraphicalEffect by the provided name.
	 * @param name the name of the effect
	 * @return
	 */
	public static JutsuEffect createNewEffectByName(String name) {
		Class<? extends JutsuEffect> clazz = effects.get(name.toLowerCase());
		if (clazz == null) return null;
		try {
			return clazz.newInstance();
		} catch (Exception e) {
			DebugHandler.debugGeneral(e);
			return null;
		}
	}
	
	public void playTrackingLinePatterns(Location origin, Location target, Entity originEntity, Entity targetEntity) {
		// no op, effects should override this with their own behavior
	}
	
	/**
	 * Adds an effect with the provided name to the list of available effects.
	 * This will replace an existing effect if the same name is used.
	 * @param name the name of the effect
	 * @param effect the effect to add
	 */
	public static void addEffect(String name, Class<? extends JutsuEffect> effect) {
		effects.put(name.toLowerCase(), effect);
	}
	
	static {
		effects.put("actionbartext", ActionBarTextEffect.class);
		effects.put("bossbar", BossBarEffect.class);
		effects.put("broadcast", BroadcastEffect.class);
		effects.put("cloud", CloudEffect.class);
		effects.put("dragondeath", DragonDeathEffect.class);
		effects.put("ender", EnderSignalEffect.class);
		effects.put("explosion", ExplosionEffect.class);
		effects.put("fireworks", FireworksEffect.class);
		effects.put("itemcooldown", ItemCooldownEffect.class);
		effects.put("itemspray", ItemSprayEffect.class);
		effects.put("lightning", LightningEffect.class);
		effects.put("nova", NovaEffect.class);
		effects.put("particles", ParticlesEffect.class);
		effects.put("particlespersonal", ParticlesPersonalEffect.class);
		effects.put("particlecloud", ParticleCloudEffect.class);
		effects.put("potion", PotionEffect.class);
		effects.put("smokeswirl", SmokeSwirlEffect.class);
		effects.put("smoketrail", SmokeTrailEffect.class);
		effects.put("sound", SoundEffect.class);
		effects.put("soundpersonal", SoundPersonalEffect.class);
		effects.put("spawn", MobSpawnerEffect.class);
		effects.put("splash", SplashPotionEffect.class);
		effects.put("title", TitleEffect.class);
		effects.put("effectlib", EffectLibEffect.class);
		effects.put("effectlibline", EffectLibLineEffect.class);
		effects.put("effectlibentity", EffectLibEntityEffect.class);
	}
	
}
