package com.nisovin.magicspells.spelleffects;

import java.util.List;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.util.TimeUtil;
import com.nisovin.magicspells.util.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.expression.Expression;

/**
 * Represents a graphical effect that can be used with the 'effects' option of a spell.<p>
 */
public abstract class SpellEffect {
	
	// for normal/line
	@ConfigData(field="height-offset", dataType="double", defaultValue="0")
	double heightOffset = 0;
	
	Expression heightOffsetExpression = null;
	
	@ConfigData(field="forward-offset", dataType="double", defaultValue="0")
	double forwardOffset = 0;
	
	Expression forwardOffsetExpression = null;
	
	@ConfigData(field="delay", dataType="int", defaultValue="0")
	int delay = 0;
	
	// for line
	@ConfigData(field="distance-between", dataType="double", defaultValue="1")
	double distanceBetween = 1;
	
	// for buff
	@ConfigData(field="effect-interval", dataType="int", defaultValue="20")
	int effectInterval = TimeUtil.TICKS_PER_SECOND;

	// for orbit
	@ConfigData(field="orbit-radius", dataType="double", defaultValue="1")
	float orbitRadius = 1;
	
	@ConfigData(field="orbit-seconds-per-revolution", dataType="double", defaultValue="3")
	float secondsPerRevolution = 3;
	
	@ConfigData(field="orbit-counter-clockwise", dataType="boolean", defaultValue="false")
	boolean counterClockwise = false;
	
	@ConfigData(field="orbit-tick-interval", dataType="int", defaultValue="2")
	int tickInterval = 2;

	@ConfigData(field="orbit-horiz-offset", dataType="double", defaultValue="0")
	float horizOffset = 0;

	@ConfigData(field="orbit-horiz-expand-radius", dataType="double", defaultValue="0")
	float horizExpandRadius = 0;

	@ConfigData(field="orbit-vert-expand-radius", dataType="double", defaultValue="0")
	float vertExpandRadius = 0;

	@ConfigData(field="orbit-horiz-expand-delay", dataType="double", defaultValue="0")
	int horizExpandDelay = 0;

	@ConfigData(field="orbit-vert-expand-delay", dataType="double", defaultValue="0")
	int vertExpandDelay = 0;

	float ticksPerSecond;
	float distancePerTick;
	int ticksPerRevolution;
	
	@ConfigData(field="orbit-y-offset", dataType="double", defaultValue="0")
	float orbitYOffset = 0;
	
	ModifierSet modifiers = null;
	
	int taskId = -1;
	
	public abstract void loadFromString(String string);
	
	public final void loadFromConfiguration(ConfigurationSection config) {
		heightOffset = config.getDouble("height-offset", heightOffset);
		String heightOffsetExpressionString = config.getString("height-offset-expression", null);
		if (heightOffsetExpressionString == null) {
			heightOffsetExpression = new Expression("0 + " + heightOffset);
		} else {
			heightOffsetExpression = new Expression(heightOffsetExpressionString);
		}
		
		forwardOffset = config.getDouble("forward-offset", forwardOffset);
		String forwardOffsetExpressionString = config.getString("forward-offset-expression", null);
		if (forwardOffsetExpressionString == null) {
			forwardOffsetExpression = new Expression("0 + " + forwardOffset);
		} else {
			forwardOffsetExpression = new Expression(forwardOffsetExpressionString);
		}
		delay = config.getInt("delay", delay);
		
		distanceBetween = config.getDouble("distance-between", distanceBetween);
		effectInterval = config.getInt("effect-interval", effectInterval);

		horizExpandRadius = (float)config.getDouble("orbit-horiz-expand-radius", horizExpandRadius);
		horizExpandDelay = config.getInt("orbit-horiz-expand-delay", horizExpandDelay);
		vertExpandRadius = (float)config.getDouble("orbit-vert-expand-radius", vertExpandRadius);
		vertExpandDelay = config.getInt("orbit-vert-expand-delay", vertExpandDelay);
		horizOffset = (float)config.getDouble("orbit-horiz-offset", horizOffset);
		orbitRadius = (float)config.getDouble("orbit-radius", orbitRadius);
		secondsPerRevolution = (float)config.getDouble("orbit-seconds-per-revolution", secondsPerRevolution);
		counterClockwise = config.getBoolean("orbit-counter-clockwise", counterClockwise);
		tickInterval = config.getInt("orbit-tick-interval", tickInterval);
		ticksPerSecond = 20F / (float)tickInterval;
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);
		ticksPerRevolution = Math.round(ticksPerSecond * secondsPerRevolution);
		orbitYOffset = (float)config.getDouble("orbit-y-offset", orbitYOffset);
		
		List<String> list = config.getStringList("modifiers");
		if (list != null) modifiers = new ModifierSet(list);
		
		loadFromConfig(config);
	}
	
	protected abstract void loadFromConfig(ConfigurationSection config);
	
	/**
	 * Plays an effect on the specified entity.
	 * @param entity the entity to play the effect on
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public Runnable playEffect(final Entity entity) {
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
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public final Runnable playEffect(final Location location) {
		if (delay <= 0) return playEffectLocationReal(location);
		MagicSpells.scheduleDelayedTask(() -> playEffectLocationReal(location), delay);
		return null;
	}
	
	private Runnable playEffectLocationReal(Location location) {
		if (location == null) return playEffectLocation(null);
		Location loc = location.clone();
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
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public Runnable playEffect(Location location1, Location location2) {
		Location loc1 = location1.clone();
		Location loc2 = location2.clone();
		//double localHeightOffset = heightOffsetExpression.resolveValue(null, null, location1, location2).doubleValue();
		//double localForwardOffset = forwardOffsetExpression.resolveValue(null, null, location1, location2).doubleValue();
		int c = (int)Math.ceil(loc1.distance(loc2) / distanceBetween) - 1;
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
	
	public void playEffectWhileActiveOnEntity(final Entity entity, final SpellEffectActiveChecker checker) {
		new EffectTracker(entity, checker);
	}
	
	public OrbitTracker playEffectWhileActiveOrbit(final Entity entity, final SpellEffectActiveChecker checker) {
		return new OrbitTracker(entity, checker);
	}
	
	@FunctionalInterface
	public interface SpellEffectActiveChecker {
		
		boolean isActive(Entity entity);
		
	}

	class EffectTracker implements Runnable {

		Entity entity;
		SpellEffectActiveChecker checker;
		int effectTrackerTaskId;

		public EffectTracker(Entity entity, SpellEffectActiveChecker checker) {
			this.entity = entity;
			this.checker = checker;
			this.effectTrackerTaskId = MagicSpells.scheduleRepeatingTask(this, 0, effectInterval);
		}

		@Override
		public void run() {
			// check for valid and alive caster
			if (!entity.isValid() || !checker.isActive(entity)) {
				stop();
				return;
			}

			playEffect(entity);

		}

		public void stop() {
			MagicSpells.cancelTask(effectTrackerTaskId);
			entity = null;
		}

	}

	class OrbitTracker implements Runnable {
		
		Entity entity;
		SpellEffectActiveChecker checker;
		Vector currentPosition;
		int orbitTrackerTaskId;
		int repeatingHorizTaskId;
		int repeatingVertTaskId;
		float orbRadius;
		float orbHeight;
		
		int counter = 0;
		
		public OrbitTracker(Entity entity, SpellEffectActiveChecker checker) {
			this.entity = entity;
			this.checker = checker;
			this.currentPosition = entity.getLocation().getDirection().setY(0);
			Util.rotateVector(this.currentPosition, horizOffset);
			this.orbRadius = orbitRadius;
			this.orbHeight = orbitYOffset;
			this.orbitTrackerTaskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			if (horizExpandDelay > 0 && horizExpandRadius != 0) {
				this.repeatingHorizTaskId = MagicSpells.scheduleRepeatingTask(() -> this.orbRadius += horizExpandRadius, horizExpandDelay, horizExpandDelay);
			}
			if (vertExpandDelay > 0 && vertExpandRadius != 0) {
				this.repeatingVertTaskId = MagicSpells.scheduleRepeatingTask(() -> this.orbHeight += vertExpandRadius, vertExpandDelay, vertExpandDelay);
			}
		}
		
		@Override
		public void run() {
			// check for valid and alive caster and target
			if (!entity.isValid()) {
				stop();
				return;
			}
			
			// check if duration is up
			if (counter++ % ticksPerRevolution == 0 && !checker.isActive(entity)) {
				stop();
				return;
			}
			
			// move projectile and calculate new vector
			Location loc = getLocation();
			
			// show effect
			playEffect(loc);
			
		}
		
		private Location getLocation() {
			Vector perp;
			if (counterClockwise) {
				perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
			} else {
				perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
			}
			currentPosition.add(perp.multiply(distancePerTick)).normalize();
			return entity.getLocation().add(0, orbHeight, 0).add(currentPosition.clone().multiply(orbRadius));
		}
		
		public void stop() {
			MagicSpells.cancelTask(orbitTrackerTaskId);
			MagicSpells.cancelTask(repeatingHorizTaskId);
			MagicSpells.cancelTask(repeatingVertTaskId);
			entity = null;
			currentPosition = null;
		}
		
	}
	
	private static HashMap<String, Class<? extends SpellEffect>> effects = new HashMap<>();
	
	/**
	 * Gets the GraphicalEffect by the provided name.
	 * @param name the name of the effect
	 * @return
	 */
	public static SpellEffect createNewEffectByName(String name) {
		Class<? extends SpellEffect> clazz = effects.get(name.toLowerCase());
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
	public static void addEffect(String name, Class<? extends SpellEffect> effect) {
		effects.put(name.toLowerCase(), effect);
	}
	
	static {
		effects.put("actionbartext", ActionBarTextEffect.class);
		effects.put("angry", AngryEffect.class);
		effects.put("bigsmoke", BigSmokeEffect.class);
		effects.put("blockbreak", BlockBreakEffect.class);
		effects.put("bluesparkle", BlueSparkleEffect.class);
		effects.put("broadcast", BroadcastEffect.class);
		effects.put("cloud", CloudEffect.class);
		effects.put("dragondeath", DragonDeathEffect.class);
		effects.put("ender", EnderSignalEffect.class);
		effects.put("explosion", ExplosionEffect.class);
		effects.put("fireworks", FireworksEffect.class);
		effects.put("greensparkle", GreenSparkleEffect.class);
		effects.put("hearts", HeartsEffect.class);
		effects.put("itemcooldown", ItemCooldownEffect.class);
		effects.put("itemspray", ItemSprayEffect.class);
		effects.put("lightning", LightningEffect.class);
		effects.put("nova", NovaEffect.class);
		effects.put("particles", ParticlesEffect.class);
		effects.put("particlecloud", ParticleCloudEffect.class);
		effects.put("particleline", ParticleLineEffect.class);
		effects.put("potion", PotionEffect.class);
		effects.put("smoke", SmokeEffect.class);
		effects.put("smokeswirl", SmokeSwirlEffect.class);
		effects.put("smoketrail", SmokeTrailEffect.class);
		effects.put("sound", SoundEffect.class);
		effects.put("soundpersonal", SoundPersonalEffect.class);
		effects.put("spawn", MobSpawnerEffect.class);
		effects.put("splash", SplashPotionEffect.class);
		effects.put("title", TitleEffect.class);
		effects.put("effectlib", EffectLibEffect.class);
		effects.put("effectlibparticles", EffectLibParticlesEffect.class);
		effects.put("effectlibline", EffectLibLineEffect.class);
		effects.put("effectlibentity", EffectLibEntityEffect.class);
	}
	
}
