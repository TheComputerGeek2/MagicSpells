package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;

import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;

public class StealthJutsu extends BuffJutsu {
	
	private Set<UUID> stealthy;
	
	public StealthJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		stealthy = new HashSet<>();
	}
	
	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		stealthy.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return stealthy.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		stealthy.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		stealthy.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if (!(event.getTarget() instanceof LivingEntity)) return;
		LivingEntity target = (LivingEntity) event.getTarget();
		if (!isActive(target)) return;
		if (isExpired(target)) {
			turnOff(target);
			return;
		}

		addUseAndChargeCost(target);
		event.setCancelled(true);
	}
	
}
