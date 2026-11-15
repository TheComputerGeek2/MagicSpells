package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("particlespersonal")
public class ParticlesPersonalEffect extends ParticlesEffect {

	protected ConfigData<ParticlePosition> target;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		super.loadFromConfig(config);

		target = ConfigDataUtil.getEnum(config, "target", ParticlePosition.class, ParticlePosition.POSITION);
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		Player player = getTarget(entity, data);
		if (player == null) return null;

		Location location = applyOffsets(entity.getLocation(), data);
		Particle particle = this.particle.get(data);

		Location spawnLocation = getSpawnLocation(particle, location, data);
		if (spawnLocation == null) return null;

		particle.builder()
				.location(location)
				.count(count.get(data))
				.offset(xSpread.get(data), ySpread.get(data), zSpread.get(data))
				.speed(speed.get(data))
				.data(getParticleData(particle, entity, location, data))
				.force(force.get(data))
				.receivers(player)
				.spawn();

		return null;
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		Player player = getTarget(null, data);
		if (player == null) return null;

		Particle particle = this.particle.get(data);

		Location spawnLocation = getSpawnLocation(particle, location, data);
		if (spawnLocation == null) return null;

		particle.builder()
				.location(location)
				.count(count.get(data))
				.offset(xSpread.get(data), ySpread.get(data), zSpread.get(data))
				.speed(speed.get(data))
				.data(getParticleData(particle, null, location, data))
				.force(force.get(data))
				.receivers(player)
				.spawn();

		return null;
	}

	private Player getTarget(Entity entity, SpellData data) {
		return switch (target.get(data)) {
			case CASTER -> {
				LivingEntity caster = data.caster();
				yield caster instanceof Player player ? player : null;
			}
			case TARGET -> {
				LivingEntity target = data.target();
				yield target instanceof Player player ? player : null;
			}
			case POSITION -> entity instanceof Player player ? player : null;
		};
	}

}
