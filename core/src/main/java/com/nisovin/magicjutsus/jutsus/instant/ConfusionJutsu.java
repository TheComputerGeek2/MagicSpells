package com.nisovin.magicjutsus.jutsus.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class ConfusionJutsu extends InstantJutsu implements TargetedLocationJutsu {

	private double radius;
	
	public ConfusionJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		radius = getConfigDouble("radius", 10);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			confuse(livingEntity, livingEntity.getLocation(), power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		confuse(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private void confuse(LivingEntity caster, Location location, float power) {
		double castingRange = Math.round(radius * power);
		Collection<Entity> entities = location.getWorld().getNearbyEntities(location, castingRange, castingRange, castingRange);
		List<LivingEntity> monsters = new ArrayList<>();

		for (Entity e : entities) {
			if (!(e instanceof LivingEntity)) continue;
			if (!validTargetList.canTarget(caster, e)) continue;
			monsters.add((LivingEntity) e);
		}

		for (int i = 0; i < monsters.size(); i++) {
			int next = i + 1;
			if (next >= monsters.size()) next = 0;
			MagicJutsus.getVolatileCodeHandler().setTarget(monsters.get(i), monsters.get(next));
			playJutsuEffects(EffectPosition.TARGET, monsters.get(i));
			playJutsuEffectsTrail(caster.getLocation(), monsters.get(i).getLocation());
		}
		playJutsuEffects(EffectPosition.CASTER, caster);
	}

}
