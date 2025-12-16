package com.nisovin.magicspells.spelleffects.effecttypes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Vibration;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.Particle.Trail;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Vibration.Destination;
import org.bukkit.Particle.DustTransition;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

import static org.bukkit.Vibration.Destination.*;

@Name("particles")
public class ParticlesEffect extends SpellEffect {

	protected ConfigData<Particle> particle;

	protected ConfigData<Color> argbColor;
	protected ConfigData<Material> material;
	protected ConfigData<BlockData> blockData;
	protected ConfigData<DustOptions> dustOptions;
	protected ConfigData<Particle.Spell> spellOptions;
	protected ConfigData<DustTransition> dustTransition;

	protected ConfigData<Vector> vibrationOffset;
	protected ConfigData<Vector> vibrationRelativeOffset;
	protected ConfigData<ParticlePosition> vibrationOrigin;
	protected ConfigData<ParticlePosition> vibrationDestination;

	protected ConfigData<Color> trailColor;
	protected ConfigData<Integer> trailDuration;
	protected ConfigData<Vector> trailTargetOffset;
	protected ConfigData<ParticlePosition> trailOrigin;
	protected ConfigData<ParticlePosition> trailTarget;
	protected ConfigData<Vector> trailTargetRelativeOffset;

	protected ConfigData<Integer> count;
	protected ConfigData<Integer> radius;
	protected ConfigData<Integer> arrivalTime;
	protected ConfigData<Integer> shriekDelay;

	protected ConfigData<Float> speed;
	protected ConfigData<Float> xSpread;
	protected ConfigData<Float> ySpread;
	protected ConfigData<Float> zSpread;
	protected ConfigData<Float> dragonBreathPower;
	protected ConfigData<Float> sculkChargeRotation;

	protected ConfigData<Boolean> force;
	protected ConfigData<Boolean> staticDestination;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		particle = ConfigDataUtil.getParticle(config, "particle-name", Particle.POOF);

		argbColor = ConfigDataUtil.getARGBColor(config, "argb-color", null);
		material = ConfigDataUtil.getMaterial(config, "material", null);
		blockData = ConfigDataUtil.getBlockData(config, "material", null);
		dustOptions = ConfigDataUtil.getDustOptions(config, "color", "size", new DustOptions(Color.RED, 1));
		dustTransition = ConfigDataUtil.getDustTransition(config, "color", "to-color", "size", new DustTransition(Color.RED, Color.BLACK, 1));
		spellOptions = ConfigDataUtil.getSpellOptions(config, "spell.color", "spell.power", null);

		vibrationOffset = ConfigDataUtil.getVector(config, "vibration-offset", new Vector());
		vibrationOrigin = ConfigDataUtil.getEnum(config, "vibration-origin", ParticlePosition.class, ParticlePosition.POSITION);
		vibrationDestination = ConfigDataUtil.getEnum(config, "vibration-destination", ParticlePosition.class, ParticlePosition.POSITION);
		vibrationRelativeOffset = ConfigDataUtil.getVector(config, "vibration-relative-offset", new Vector());

		trailColor = ConfigDataUtil.getColor(config, "trail.color", null);
		trailOrigin = ConfigDataUtil.getEnum(config, "trail.origin", ParticlePosition.class, ParticlePosition.POSITION);
		trailTarget = ConfigDataUtil.getEnum(config, "trail.target", ParticlePosition.class, null);
		trailDuration = ConfigDataUtil.getInteger(config, "trail.duration");
		trailTargetOffset = ConfigDataUtil.getVector(config, "trail.target-offset", new Vector());
		trailTargetRelativeOffset = ConfigDataUtil.getVector(config, "trail.target-relative-offset", new Vector());

		count = ConfigDataUtil.getInteger(config, "count", 5);
		radius = ConfigDataUtil.getInteger(config, "radius", 50);
		arrivalTime = ConfigDataUtil.getInteger(config, "arrival-time", -1);
		shriekDelay = ConfigDataUtil.getInteger(config, "shriek-delay", 0);

		dragonBreathPower = ConfigDataUtil.getFloat(config, "dragon-breath-power", 1);
		sculkChargeRotation = ConfigDataUtil.getFloat(config, "sculk-charge-rotation", 0);

		speed = ConfigDataUtil.getFloat(config, "speed", 0.2f);

		ConfigData<Float> horizSpread = ConfigDataUtil.getFloat(config, "horiz-spread", 0.2f);
		xSpread = ConfigDataUtil.getFloat(config, "x-spread", horizSpread);
		zSpread = ConfigDataUtil.getFloat(config, "z-spread", horizSpread);

		ConfigData<Float> vertSpread = ConfigDataUtil.getFloat(config, "vert-spread", 0.2f);
		ySpread = ConfigDataUtil.getFloat(config, "y-spread", vertSpread);

		force = ConfigDataUtil.getBoolean(config, "force", false);
		staticDestination = ConfigDataUtil.getBoolean(config, "static-destination", false);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		Location location = applyOffsets(entity.getLocation(), data);
		Particle particle = this.particle.get(data);

		Location spawnLocation = getSpawnLocation(particle, location, data);
		if (spawnLocation == null) return null;

		particle.builder()
			.location(spawnLocation)
			.count(count.get(data))
			.offset(xSpread.get(data), ySpread.get(data), zSpread.get(data))
			.extra(speed.get(data))
			.data(getParticleData(particle, entity, location, data))
			.force(force.get(data))
			.receivers(radius.get(data), true)
			.spawn();

		return null;
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		Particle particle = this.particle.get(data);

		Location spawnLocation = getSpawnLocation(particle, location, data);
		if (spawnLocation == null) return null;

		particle.builder()
			.location(spawnLocation)
			.count(count.get(data))
			.offset(xSpread.get(data), ySpread.get(data), zSpread.get(data))
			.extra(speed.get(data))
			.data(getParticleData(particle, null, location, data))
			.force(force.get(data))
			.receivers(radius.get(data), true)
			.spawn();

		return null;
	}

	protected Object getParticleData(@NotNull Particle particle, @Nullable Entity entity, @NotNull Location location, @NotNull SpellData data) {
		Class<?> type = particle.getDataType();

		if (type == ItemStack.class) {
			Material material = this.material.get(data);
			return material == null ? null : new ItemStack(material);
		}

		if (type == Vibration.class) {
			Vector relativeOffset = vibrationRelativeOffset.get(data);
			Vector offset = vibrationOffset.get(data);

			boolean staticDestination = this.staticDestination.get(data);

			Destination destination = switch (vibrationDestination.get(data)) {
				case CASTER -> {
					LivingEntity caster = data.caster();
					if (caster == null) yield null;

					yield staticDestination ?
						new BlockDestination(applyOffsets(caster.getLocation(), offset, relativeOffset)) :
						new EntityDestination(caster);
				}
				case TARGET -> {
					LivingEntity target = data.target();
					if (target == null) yield null;

					yield staticDestination ?
						new BlockDestination(applyOffsets(target.getLocation(), offset, relativeOffset)) :
						new EntityDestination(target);
				}
				case POSITION -> entity == null || staticDestination ?
					new BlockDestination(applyOffsets(location, offset, relativeOffset)) :
					new EntityDestination(entity);
			};
			if (destination == null) return null;

			return new Vibration(destination, arrivalTime.get(data));
		}

		if (type == Trail.class) {
			Color color = trailColor.get(data);
			if (color == null) return null;

			Integer duration = trailDuration.get(data);
			if (duration == null) return null;

			Vector relativeOffset = trailTargetRelativeOffset.get(data);
			Vector offset = trailTargetOffset.get(data);

			Location target = switch (trailTarget.get(data)) {
				case CASTER -> data.hasCaster() ? data.caster().getLocation() : null;
				case TARGET -> data.hasTarget() ? data.target().getLocation() : null;
				case POSITION -> location.clone();
				case null -> null;
			};
			if (target == null) return null;

			return new Trail(applyOffsets(target, offset, relativeOffset), color, duration);
		}

		if (type == BlockData.class) return blockData.get(data);
		if (type == DustOptions.class) return dustOptions.get(data);
		if (type == Particle.Spell.class) return spellOptions.get(data);
		if (type == DustTransition.class) return dustTransition.get(data);
		if (type == Color.class) return argbColor.get(data);

		return switch (particle) {
			case SHRIEK -> shriekDelay.get(data);
			case DRAGON_BREATH -> dragonBreathPower.get(data);
			case SCULK_CHARGE -> sculkChargeRotation.get(data);
			default -> null;
		};
	}

	protected Location getSpawnLocation(@NotNull Particle particle, @NotNull Location position, @NotNull SpellData data) {
		return switch (particle) {
			case TRAIL -> getLocation(position, data, trailOrigin);
			case VIBRATION -> getLocation(position, data, vibrationOrigin);
			default -> position;
		};
	}

	protected Location getLocation(Location location, SpellData data, ConfigData<ParticlePosition> position) {
		return switch (position.get(data)) {
			case CASTER -> {
				LivingEntity caster = data.caster();
				yield caster == null ? null : caster.getLocation();
			}
			case TARGET -> {
				LivingEntity target = data.target();
				yield target == null ? null : target.getLocation();
			}
			case POSITION -> location;
		};
	}

	protected enum ParticlePosition {

		CASTER,
		TARGET,
		POSITION

	}

}
