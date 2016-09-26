package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.Util_1_9;

public class ParticleCloudSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {

	
	private int color = 0xFF0000;
	private int ticksDuration = 60;
	private int durationOnUse = 0;
	private float radius = 5F;
	private float radiusOnUse = 0F;
	private float radiusPerTick = 0F;
	private int reapplicationDelay = 60;
	private int waitTime = 10;
	private Particle particle;
	private Set<PotionEffect> effects;
	private boolean useGravity = false;
	private boolean canTargetEntities = true;
	private boolean canTargetLocation = true;
	
	
	
	public ParticleCloudSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		color = getConfigInt("color", color);
		ticksDuration = getConfigInt("duration-ticks", ticksDuration);
		durationOnUse = getConfigInt("duration-ticks-on-use", durationOnUse);
		radius = getConfigFloat("radius", radius);
		radiusOnUse = getConfigFloat("radius-on-use", radiusOnUse);
		radiusPerTick = getConfigFloat("radius-per-tick", radiusPerTick);
		reapplicationDelay = getConfigInt("reapplication-delay-ticks", reapplicationDelay);
		waitTime = getConfigInt("wait-time-ticks", waitTime);
		useGravity = getConfigBoolean("use-gravity", useGravity);
		String particleName = getConfigString("particle", "fireworksspark");
		canTargetEntities = getConfigBoolean("can-target-entities", canTargetEntities);
		canTargetLocation = getConfigBoolean("can-target-location", canTargetLocation);
		particle = Util_1_9.getParticleFromName(particleName);
		if (particle == null) {
			MagicSpells.error("could not determine particle from '" + particleName + "'");
		}
		
		List<String> potionEffectStrings = getConfigStringList("potion-effects", null);
		if (potionEffectStrings == null) potionEffectStrings = new ArrayList<String>();
		
		effects = new HashSet<PotionEffect>();
		
		for (String effect: potionEffectStrings) {
			effects.add(getPotionEffectFromString(effect));
		}
		
		
	}
	
	private static PotionEffect getPotionEffectFromString(String s) {
		//type durationTicks amplifier ambient particles? hexColor
		String[] splits = s.split(" ");
		PotionEffectType type = Util.getPotionEffectType(splits[0]);
		int durationTicks = Integer.parseInt(splits[1]);
		int amplifier = Integer.parseInt(splits[2]);
		boolean ambient = Boolean.parseBoolean(splits[3]);
		boolean particles = Boolean.parseBoolean(splits[4]);
		int color = Integer.parseInt(splits[5], 16);
		return new PotionEffect(type, durationTicks, amplifier, ambient, particles, Color.fromRGB(color));
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state,
			float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location locToSpawn = null;
			if (canTargetEntities) {
				TargetInfo<LivingEntity> targetEntityInfo = getTargetedEntity(player, power);
				if (targetEntityInfo.getTarget() != null) {
					locToSpawn = targetEntityInfo.getTarget().getLocation();
				}
			}
			if (canTargetLocation && locToSpawn == null) {
				Block targetBlock = getTargetedBlock(player, power);
				if (targetBlock != null) {
					locToSpawn = targetBlock.getLocation();
				}
			}
			
			if (locToSpawn == null) {
				return noTarget(player);
			}
			
			AreaEffectCloud cloud = spawnCloud(locToSpawn);
			cloud.setSource(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		if (!canTargetLocation) return false;
		AreaEffectCloud cloud = spawnCloud(target);
		cloud.setSource(caster);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power);
	}
	
	private AreaEffectCloud spawnCloud(Location loc) {
		AreaEffectCloud cloud = loc.getWorld().spawn(loc, AreaEffectCloud.class);
		cloud.setColor(Color.fromRGB(color));
		cloud.setDuration(ticksDuration); //ticks
		cloud.setDurationOnUse(durationOnUse); //ticks
		cloud.setParticle(null); //particle
		cloud.setRadius(radius);
		cloud.setRadiusOnUse(radiusOnUse);
		cloud.setRadiusPerTick(radiusPerTick);
		cloud.setReapplicationDelay(reapplicationDelay); //ticks
		cloud.setWaitTime(waitTime); //ticks
		for (PotionEffect eff: effects) {
			cloud.addCustomEffect(eff, true);
		}
		cloud.setGravity(useGravity);
		return cloud;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!canTargetEntities) return false;
		AreaEffectCloud cloud = spawnCloud(target.getLocation());
		cloud.setSource(caster);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(null, target, power);
	}

}
