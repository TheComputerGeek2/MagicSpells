package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.events.JutsuCastEvent;

public class EmpowerJutsu extends BuffJutsu {

	private Map<UUID, Float> empowered;

	private float maxPower;
	private float extraPower;

	private JutsuFilter filter;

	public EmpowerJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		maxPower = getConfigFloat("max-power-multiplier", 1.5F);
		extraPower = getConfigFloat("power-multiplier", 1.5F);
		
		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> tagList = getConfigStringList("jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-jutsu-tags", null);
		filter = new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);

		empowered = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		float p = power * extraPower;
		if (p > maxPower) p = maxPower;
		empowered.put(entity.getUniqueId(), p);
		return true;
	}

	@Override
	public boolean recastBuff(LivingEntity entity, float power, String[] args) {
		return castBuff(entity, power, args);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return empowered.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		empowered.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		empowered.clear();
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onJutsuCast(JutsuCastEvent event) {
		LivingEntity player = event.getCaster();
		if (player == null) return;
		if (!isActive(player)) return;
		if (!filter.check(event.getJutsu())) return;

		addUseAndChargeCost(player);
		event.increasePower(empowered.get(player.getUniqueId()));
	}

}
