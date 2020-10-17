package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.EntityData;
import com.nisovin.magicspells.spelleffects.SpellEffect;

public class EntityEffect extends SpellEffect {

	public static final String ENTITY_TAG = "MS_ENTITY";

	private EntityData entityData;

	private boolean silent;
	private boolean gravity;
	private boolean enableAI;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		ConfigurationSection section = config.getConfigurationSection("entity");
		if (section == null) return;

		entityData = new EntityData(section);

		silent = section.getBoolean("silent", false);
		gravity = section.getBoolean("gravity", false);
		enableAI = section.getBoolean("ai", true);
	}

	@Override
	protected Entity playEntityEffectLocation(Location location) {
		Entity entity = entityData.spawn(location);
		entity.addScoreboardTag(ENTITY_TAG);
		entity.setGravity(gravity);
		entity.setSilent(silent);
		if (entity instanceof LivingEntity) ((LivingEntity) entity).setAI(enableAI);
		return entity;
	}

}
