package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ConfusionSpell extends InstantSpell implements TargetedLocationSpell {

	private double radius;
	
	public ConfusionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigDouble("radius", 10);
		if (radius > MagicSpells.getGlobalRadius()) radius = MagicSpells.getGlobalRadius();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			confuse(caster, caster.getLocation(), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		confuse(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		confuse(caster, target, power, null);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private void confuse(LivingEntity caster, Location location, float power, String[] args) {
		double castingRange = Math.round(radius * power);
		Collection<Entity> entities = location.getWorld().getNearbyEntities(location, castingRange, castingRange, castingRange);
		List<LivingEntity> monsters = new ArrayList<>();

		for (Entity e : entities) {
			if (!(e instanceof LivingEntity livingEntity)) continue;
			if (!validTargetList.canTarget(caster, e)) continue;
			monsters.add(livingEntity);
		}

		for (int i = 0; i < monsters.size(); i++) {
			int next = i + 1;
			if (next >= monsters.size()) next = 0;
			MobUtil.setTarget(monsters.get(i), monsters.get(next));
			playSpellEffects(EffectPosition.TARGET, monsters.get(i));
			playSpellEffectsTrail(caster.getLocation(), monsters.get(i).getLocation());
		}
		playSpellEffects(EffectPosition.CASTER, caster);
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

}
