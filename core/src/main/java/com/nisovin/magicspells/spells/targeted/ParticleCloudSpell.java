package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.Particle.DustTransition;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class ParticleCloudSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {

	private final ConfigData<Vector> relativeOffset;

	private final ConfigData<Component> customName;

	private final ConfigData<Color> color;
	private final ConfigData<ItemStack> item;
	private final ConfigData<BlockData> blockData;
	private final ConfigData<DustOptions> dustOptions;
	private final ConfigData<Particle.Spell> spellOptions;
	private final ConfigData<DustTransition> dustTransition;

	private final ConfigData<Particle> particle;

	private final ConfigData<Integer> waitTime;
	private final ConfigData<Integer> shriekDelay;
	private final ConfigData<Integer> ticksDuration;
	private final ConfigData<Integer> durationOnUse;
	private final ConfigData<Integer> reapplicationDelay;

	private final ConfigData<Float> radius;
	private final ConfigData<Float> radiusOnUse;
	private final ConfigData<Float> radiusPerTick;
	private final ConfigData<Float> dragonBreathPower;
	private final ConfigData<Float> sculkChargeRotation;

	private final ConfigData<Boolean> useGravity;
	private final ConfigData<Boolean> canTargetEntities;
	private final ConfigData<Boolean> canTargetLocation;

	private final List<ConfigData<PotionEffect>> potionEffects;

	public ParticleCloudSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0.5, 0));

		customName = getConfigDataComponent("custom-name", null);

		ConfigData<Particle> particle = ConfigDataUtil.getParticle(config.getMainConfig(), internalKey + "particle", Particle.POOF);
		this.particle = ConfigDataUtil.getParticle(config.getMainConfig(), internalKey + "particle-name", null).orDefault(particle);

		blockData = getConfigDataBlockData("material", null);
		dustOptions = ConfigDataUtil.getDustOptions(config.getMainConfig(), internalKey + "dust-color", internalKey + "size", new DustOptions(Color.RED, 1));
		dustTransition = ConfigDataUtil.getDustTransition(
				config.getMainConfig(),
				internalKey + "dust-transition.color",
				internalKey + "dust-transition.to-color",
				internalKey + "dust-transition.size",
				new DustTransition(Color.RED, Color.BLACK, 1)
		);
		spellOptions = ConfigDataUtil.getSpellOptions(config.getMainConfig(), internalKey + "spell.color", internalKey + "spell.power", null);

		ConfigData<Material> material = getConfigDataMaterial("material", null);
		if (material.isConstant()) {
			Material mat = material.get();

			ItemStack stack = mat != null && mat.isItem() ? new ItemStack(mat) : null;
			item = data -> stack;
		} else {
			item = data -> {
				Material mat = material.get(data);
				return mat != null && mat.isItem() ? new ItemStack(mat) : null;
			};
		}

		shriekDelay = getConfigDataInt("shriek-delay", 0);

		dragonBreathPower = getConfigDataFloat("dragon-breath-power", 1);
		sculkChargeRotation = getConfigDataFloat("sculk-charge-rotation", 0);

		ConfigData<Integer> colorInt = getConfigDataInt("color", 0xFF0000);
		color = ConfigDataUtil.getARGBColor(config.getMainConfig(), internalKey + "argb-color", null)
				.orDefault(data -> Color.fromRGB(colorInt.get(data)));

		waitTime = getConfigDataInt("wait-time-ticks", 10);
		ticksDuration = getConfigDataInt("duration-ticks", 3 * TimeUtil.TICKS_PER_SECOND);
		durationOnUse = getConfigDataInt("duration-ticks-on-use", 0);
		reapplicationDelay = getConfigDataInt("reapplication-delay-ticks", 3 * TimeUtil.TICKS_PER_SECOND);

		radius = getConfigDataFloat("radius", 5F);
		radiusOnUse = getConfigDataFloat("radius-on-use", 0F);
		radiusPerTick = getConfigDataFloat("radius-per-tick", 0F);

		useGravity = getConfigDataBoolean("use-gravity", false);
		canTargetEntities = getConfigDataBoolean("can-target-entities", true);
		canTargetLocation = getConfigDataBoolean("can-target-location", true);

		potionEffects = Util.getPotionEffects(getConfigList("potion-effects", null), internalName);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (canTargetEntities.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.cancelled()) return noTarget(info);

			if (!info.noTarget()) {
				Location location = info.target().getLocation();
				location.setDirection(data.caster().getLocation().getDirection());
				return spawnCloud(info.spellData().location(location));
			}
		}

		if (canTargetLocation.get(data)) {
			TargetInfo<Location> info = getTargetedBlockLocation(data, 0.5, 1, 0.5, false);
			if (info.noTarget()) return noTarget(info);
			return spawnCloud(info.spellData());
		}

		return noTarget(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		return spawnCloud(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		return spawnCloud(data.location(data.target().getLocation()));
	}

	private CastResult spawnCloud(SpellData data) {
		Location location = data.location();

		//apply relative offset
		Vector relativeOffset = this.relativeOffset.get(data);
		location.add(0, relativeOffset.getY(), 0);
		Util.applyRelativeOffset(location, relativeOffset.setY(0));

		data = data.location(location);

		SpellData finalData = data;
		location.getWorld().spawn(location, AreaEffectCloud.class, cloud -> {
			cloud.setSource(finalData.caster());

			cloud.setRadius(radius.get(finalData));
			cloud.setGravity(useGravity.get(finalData));
			cloud.setWaitTime(waitTime.get(finalData));
			cloud.setDuration(ticksDuration.get(finalData));
			cloud.setDurationOnUse(durationOnUse.get(finalData));
			cloud.setRadiusOnUse(radiusOnUse.get(finalData));
			cloud.setRadiusPerTick(radiusPerTick.get(finalData));
			cloud.setReapplicationDelay(reapplicationDelay.get(finalData));

			if (potionEffects != null)
				for (ConfigData<PotionEffect> eff : potionEffects)
					cloud.addCustomEffect(eff.get(finalData), true);

			Particle particle = this.particle.get(finalData);
			cloud.setParticle(particle, getParticleData(particle, finalData));

			Component customName = this.customName.get(finalData);
			if (customName != null) {
				cloud.customName(customName);
				cloud.setCustomNameVisible(true);
			}
		});

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private Object getParticleData(@NotNull Particle particle, @NotNull SpellData data) {
		Class<?> type = particle.getDataType();

		if (type == Color.class) return color.get(data);
		if (type == ItemStack.class) return item.get(data);
		if (type == BlockData.class) return blockData.get(data);
		if (type == DustOptions.class) return dustOptions.get(data);
		if (type == Particle.Spell.class) return spellOptions.get(data);
		if (type == DustTransition.class) return dustTransition.get(data);

		return switch (particle) {
			case SHRIEK -> shriekDelay.get(data);
			case DRAGON_BREATH -> dragonBreathPower.get(data);
			case SCULK_CHARGE -> sculkChargeRotation.get(data);
			default -> null;
		};
	}

}
