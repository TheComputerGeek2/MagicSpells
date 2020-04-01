package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TimeUtil;
import com.nisovin.magicjutsus.util.ColorUtil;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;

public class ParticleCloudJutsu extends TargetedJutsu implements TargetedLocationJutsu, TargetedEntityJutsu {

	private Particle particle;
	private String particleName;

	private Material material;
	private String materialName;

	private BlockData blockData;
	private ItemStack itemStack;

	private float dustSize;
	private String colorHex;
	private Color dustColor;
	private DustOptions dustOptions;

	private boolean none = true;
	private boolean item = false;
	private boolean dust = false;
	private boolean block = false;

	private int color;
	private int waitTime;
	private int ticksDuration;
	private int durationOnUse;
	private int reapplicationDelay;

	private float radius;
	private float radiusOnUse;
	private float radiusPerTick;

	private boolean useGravity;
	private boolean canTargetEntities;
	private boolean canTargetLocation;

	private Set<PotionEffect> potionEffects;

	public ParticleCloudJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		particleName = getConfigString("particle-name", "EXPLOSION_NORMAL");
		particle = Util.getParticle(particleName);

		materialName = getConfigString("material", "").toUpperCase();
		material = Material.getMaterial(materialName);

		dustSize = getConfigFloat("size", 1);
		colorHex = getConfigString("dust-color", "FF0000");
		dustColor = ColorUtil.getColorFromHexString(colorHex);
		if (dustColor != null) dustOptions = new DustOptions(dustColor, dustSize);

		if ((particle == Particle.BLOCK_CRACK || particle == Particle.BLOCK_DUST || particle == Particle.FALLING_DUST) && material != null && material.isBlock()) {
			block = true;
			blockData = material.createBlockData();
			none = false;
		} else if (particle == Particle.ITEM_CRACK && material != null && material.isItem()) {
			item = true;
			itemStack = new ItemStack(material);
			none = false;
		} else if (particle == Particle.REDSTONE && dustOptions != null) {
			dust = true;
			none = false;
		}

		if (particle == null) MagicJutsus.error("ParticleCloudJutsu '" + internalName + "' has a wrong particle-name defined! '" + particleName + "'");

		if ((particle == Particle.BLOCK_CRACK || particle == Particle.BLOCK_DUST || particle == Particle.FALLING_DUST) && (material == null || !material.isBlock())) {
			particle = null;
			MagicJutsus.error("ParticleCloudJutsu '" + internalName + "' has a wrong material defined! '" + materialName + "'");
		}

		if (particle == Particle.ITEM_CRACK && (material == null || !material.isItem())) {
			particle = null;
			MagicJutsus.error("ParticleCloudJutsu '" + internalName + "' has a wrong material defined! '" + materialName + "'");
		}

		if (particle == Particle.REDSTONE && dustColor == null) {
			particle = null;
			MagicJutsus.error("ParticleCloudJutsu '" + internalName + "' has a wrong dust-color defined! '" + colorHex + "'");
		}

		color = getConfigInt("color", 0xFF0000);
		waitTime = getConfigInt("wait-time-ticks", 10);
		ticksDuration = getConfigInt("duration-ticks", 3 * TimeUtil.TICKS_PER_SECOND);
		durationOnUse = getConfigInt("duration-ticks-on-use", 0);
		reapplicationDelay = getConfigInt("reapplication-delay-ticks", 3 * TimeUtil.TICKS_PER_SECOND);

		radius = getConfigFloat("radius", 5F);
		radiusOnUse = getConfigFloat("radius-on-use", 0F);
		radiusPerTick = getConfigFloat("radius-per-tick", 0F);

		useGravity = getConfigBoolean("use-gravity", false);
		canTargetEntities = getConfigBoolean("can-target-entities", true);
		canTargetLocation = getConfigBoolean("can-target-location", true);

		List<String> potionEffectStrings = getConfigStringList("potion-effects", null);
		if (potionEffectStrings == null) potionEffectStrings = new ArrayList<>();

		potionEffects = new HashSet<>();

		for (String effect: potionEffectStrings) {
			potionEffects.add(getPotionEffectFromString(effect));
		}
	}

	private static PotionEffect getPotionEffectFromString(String s) {
		String[] splits = s.split(" ");
		PotionEffectType type = Util.getPotionEffectType(splits[0]);
		int durationTicks = Integer.parseInt(splits[1]);
		int amplifier = Integer.parseInt(splits[2]);
		boolean ambient = Boolean.parseBoolean(splits[3]);
		boolean particles = Boolean.parseBoolean(splits[4]);
		boolean icon = Boolean.parseBoolean(splits[5]);
		return new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location locToSpawn = null;
			if (canTargetEntities) {
				TargetInfo<LivingEntity> targetEntityInfo = getTargetedEntity(livingEntity, power);
				if (targetEntityInfo != null && targetEntityInfo.getTarget() != null) locToSpawn = targetEntityInfo.getTarget().getLocation();
			}
			if (canTargetLocation && locToSpawn == null) {
				Block targetBlock = getTargetedBlock(livingEntity, power);
				if (targetBlock != null) locToSpawn = targetBlock.getLocation().add(0.5, 1, 0.5);
			}

			if (locToSpawn == null) return noTarget(livingEntity);

			AreaEffectCloud cloud = spawnCloud(locToSpawn);
			cloud.setSource(livingEntity);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (!canTargetLocation) return false;
		AreaEffectCloud cloud = spawnCloud(target.getBlock().getLocation().add(0.5, 1, 0.5));
		cloud.setSource(caster);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target.getBlock().getLocation().add(0.5, 1, 0.5), power);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!canTargetEntities) return false;
		AreaEffectCloud cloud = spawnCloud(target.getLocation());
		cloud.setSource(caster);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(null, target, power);
	}

	private AreaEffectCloud spawnCloud(Location loc) {
		AreaEffectCloud cloud = loc.getWorld().spawn(loc, AreaEffectCloud.class);
		if (block) cloud.setParticle(particle, blockData);
		else if (item) cloud.setParticle(particle, itemStack);
		else if (dust) cloud.setParticle(particle, dustOptions);
		else if (none) cloud.setParticle(particle);

		cloud.setColor(Color.fromRGB(color));
		cloud.setRadius(radius);
		cloud.setGravity(useGravity);
		cloud.setWaitTime(waitTime);
		cloud.setDuration(ticksDuration);
		cloud.setDurationOnUse(durationOnUse);
		cloud.setRadiusOnUse(radiusOnUse);
		cloud.setRadiusPerTick(radiusPerTick);
		cloud.setReapplicationDelay(reapplicationDelay);

		for (PotionEffect eff: this.potionEffects) {
			cloud.addCustomEffect(eff, true);
		}

		return cloud;
	}

}
