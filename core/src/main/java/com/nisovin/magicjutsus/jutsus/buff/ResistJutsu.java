package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.JutsuDamageJutsu;
import com.nisovin.magicjutsus.events.JutsuApplyDamageEvent;

public class ResistJutsu extends BuffJutsu {

	private Map<UUID, Float> buffed;

	private float multiplier;

	private List<String> jutsuDamageTypes;
	private List<DamageCause> normalDamageTypes;

	public ResistJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		multiplier = getConfigFloat("multiplier", 0.5F);

		jutsuDamageTypes = getConfigStringList("jutsu-damage-types", null);
		List<String> list = getConfigStringList("normal-damage-types", null);

		if (list != null) {
			normalDamageTypes = new ArrayList<>();
			for (String s : list) {
				for (DamageCause cause : DamageCause.values()) {
					if (!cause.name().equalsIgnoreCase(s)) continue;
					normalDamageTypes.add(cause);
					break;
				}
			}
			if (normalDamageTypes.isEmpty()) normalDamageTypes = null;
		}

		buffed = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		buffed.put(entity.getUniqueId(), power);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return buffed.containsKey(entity.getUniqueId());
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		buffed.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		buffed.clear();
	}
	
	@EventHandler
	public void onJutsuDamage(JutsuApplyDamageEvent event) {
		if (jutsuDamageTypes == null) return;
		if (!(event.getJutsu() instanceof JutsuDamageJutsu)) return;
		if (!isActive(event.getTarget())) return;

		JutsuDamageJutsu jutsu = (JutsuDamageJutsu) event.getJutsu();
		String jutsuDamageType = jutsu.getJutsuDamageType();
		if (jutsuDamageType == null) return;
		if (!jutsuDamageTypes.contains(jutsuDamageType)) return;

		LivingEntity entity = event.getTarget();

		float power = multiplier;
		if (multiplier < 1) power *= 1 / buffed.get(entity.getUniqueId());
		else if (multiplier > 1) power *= buffed.get(entity.getUniqueId());

		addUseAndChargeCost(entity);
		event.applyDamageModifier(power);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (normalDamageTypes == null) return;
		if (!normalDamageTypes.contains(event.getCause())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity)) return;
		if (!isActive((LivingEntity) entity)) return;

		float mult = multiplier;
		if (multiplier < 1) mult *= 1 / buffed.get(entity.getUniqueId());
		else if (multiplier > 1) mult *= buffed.get(entity.getUniqueId());

		addUseAndChargeCost((LivingEntity) entity);
		event.setDamage(event.getDamage() * mult);
	}

}
