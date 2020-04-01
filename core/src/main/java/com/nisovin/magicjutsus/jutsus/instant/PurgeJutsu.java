package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class PurgeJutsu extends InstantJutsu implements TargetedLocationJutsu {
	
	private double radius;

	private List<EntityType> entities;
	
	public PurgeJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		radius = getConfigDouble("radius", 15);
		
		List<String> list = getConfigStringList("entities", null);
		if (list != null && !list.isEmpty()) {
			entities = new ArrayList<>();
			for (String s : list) {
				EntityType t = Util.getEntityType(s);
				if (t != null) entities.add(t);
				else MagicJutsus.error("PurgeJutsu '" + internalName + "' has an invalid entity defined: " + s);
			}
			if (entities.isEmpty()) entities = null;
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			boolean killed = purge(livingEntity.getLocation(), power);
			if (killed) playJutsuEffects(EffectPosition.CASTER, livingEntity);
			else return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		boolean killed = purge(target, power);
		if (killed) playJutsuEffects(EffectPosition.CASTER, caster);
		return killed;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}

	private boolean purge(Location loc, float power) {
		double castingRange = radius * power;
		Collection<Entity> entitiesNearby = loc.getWorld().getNearbyEntities(loc, castingRange, castingRange, castingRange);
		boolean killed = false;
		for (Entity entity : entitiesNearby) {
			if (!(entity instanceof LivingEntity)) continue;
			if (entity instanceof Player) continue;
			if (entities != null && !entities.contains(entity.getType())) continue;
			playJutsuEffectsTrail(loc, entity.getLocation());
			playJutsuEffects(EffectPosition.TARGET, entity);
			((LivingEntity) entity).setHealth(0);
			killed = true;
		}
		return killed;
	}

}
