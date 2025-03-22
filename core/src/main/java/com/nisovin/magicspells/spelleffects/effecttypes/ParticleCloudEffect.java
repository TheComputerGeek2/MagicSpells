package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("particlecloud")
public class ParticleCloudEffect extends ParticlesEffect {

	private ConfigData<Integer> color;
	private ConfigData<Integer> duration;

	private ConfigData<Float> radius;
	private ConfigData<Float> yOffset;
	private ConfigData<Float> radiusPerTick;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		super.loadFromConfig(config);

		color = ConfigDataUtil.getInteger(config, "color", 0xFF0000);
		duration = ConfigDataUtil.getInteger(config, "duration", 60);

		radius = ConfigDataUtil.getFloat(config, "radius", 5);
		yOffset = ConfigDataUtil.getFloat(config, "y-offset", 0);
		radiusPerTick = ConfigDataUtil.getFloat(config, "radius-per-tick", 0);
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		Particle particle = this.particle.get(data);
		if (particle == null) return null;

		location.getWorld().spawn(location.clone().add(0, yOffset.get(data), 0), AreaEffectCloud.class, cloud -> {
			cloud.setColor(Color.fromRGB(color.get(data)));
			cloud.setRadius(radius.get(data));
			cloud.setDuration(duration.get(data));
			cloud.setRadiusPerTick(radiusPerTick.get(data));

			cloud.setParticle(particle, getParticleData(particle, null, location, data));
		});

		return null;
	}

}
