package com.nisovin.magicjutsus.jutsueffects;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;

public class PotionEffect extends JutsuEffect {

	private int color = 0xFF0000;

	private int duration;
	
	@Override
	public void loadFromConfig(ConfigurationSection config) {
		String c = config.getString("color", "");
		if (!c.isEmpty()) {
			try {
				color = Integer.parseInt(c, 16);
			} catch (NumberFormatException e) {
				DebugHandler.debugNumberFormat(e);
			}
		}
		duration = config.getInt("duration", 30);
	}

	@Override
	public Runnable playEffectEntity(Entity entity) {
		if (!(entity instanceof LivingEntity)) return null;
		MagicJutsus.getVolatileCodeHandler().addPotionGraphicalEffect((LivingEntity) entity, color, duration);
		return null;
	}
	
}
