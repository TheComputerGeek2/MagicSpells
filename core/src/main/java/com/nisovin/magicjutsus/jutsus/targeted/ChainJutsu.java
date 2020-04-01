package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.TargetedEntityFromLocationJutsu;

public class ChainJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedEntityFromLocationJutsu {

	private int bounces;
	private int interval;

	private double bounceRange;

	private String jutsuToCastName;
	private Ninjutsu jutsuToCast;

	public ChainJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		bounces = getConfigInt("bounces", 3);
		interval = getConfigInt("interval", 10);

		bounceRange = getConfigDouble("bounce-range", 8);

		jutsuToCastName = getConfigString("jutsu", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		jutsuToCast = new Ninjutsu(jutsuToCastName);
		if (!jutsuToCast.process()) {
			jutsuToCast = null;
			MagicJutsus.error("ChainJutsu '" + internalName + "' has an invalid jutsu defined!");
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);
			chain(livingEntity, livingEntity.getLocation(), target.getTarget(), target.getPower());
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		chain(caster, caster.getLocation(), target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		chain(null, null, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(LivingEntity caster, Location from, LivingEntity target, float power) {
		chain(caster, from, target, power);
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		chain(null, from, target, power);
		return true;
	}

	private void chain(LivingEntity livingEntity, Location start, LivingEntity target, float power) {
		List<LivingEntity> targets = new ArrayList<>();
		List<Float> targetPowers = new ArrayList<>();
		targets.add(target);
		targetPowers.add(power);

		// Get targets
		LivingEntity current = target;
		int attempts = 0;
		while (targets.size() < bounces && attempts++ < bounces << 1) {
			List<Entity> entities = current.getNearbyEntities(bounceRange, bounceRange, bounceRange);
			for (Entity e : entities) {
				if (!(e instanceof LivingEntity)) continue;
				if (targets.contains(e)) continue;

				if (!validTargetList.canTarget(livingEntity, target)) continue;

				float thisPower = power;
				if (livingEntity != null) {
					JutsuTargetEvent event = new JutsuTargetEvent(this, livingEntity, (LivingEntity) e, thisPower);
					EventUtil.call(event);
					if (event.isCancelled()) continue;
					thisPower = event.getPower();
				}

				targets.add((LivingEntity) e);
				targetPowers.add(thisPower);
				current = (LivingEntity) e;
				break;
			}
		}

		// Cast jutsu at targets
		if (livingEntity != null) playJutsuEffects(EffectPosition.CASTER, livingEntity);
		else if (start != null) playJutsuEffects(EffectPosition.CASTER, start);

		if (interval <= 0) {
			for (int i = 0; i < targets.size(); i++) {
				Location from;
				if (i == 0) from = start;
				else from = targets.get(i - 1).getLocation();

				castJutsuAt(livingEntity, from, targets.get(i), targetPowers.get(i));
				if (i > 0) playJutsuEffectsTrail(targets.get(i - 1).getLocation(), targets.get(i).getLocation());
				else if (i == 0 && livingEntity != null) playJutsuEffectsTrail(livingEntity.getLocation(), targets.get(i).getLocation());
				playJutsuEffects(EffectPosition.TARGET, targets.get(i));
			}
		} else new ChainBouncer(livingEntity, start, targets, power);
	}

	private boolean castJutsuAt(LivingEntity caster, Location from, LivingEntity target, float power) {
		if (jutsuToCast.isTargetedEntityFromLocationJutsu() && from != null) return jutsuToCast.castAtEntityFromLocation(caster, from, target, power);
		if (jutsuToCast.isTargetedEntityJutsu()) return jutsuToCast.castAtEntity(caster, target, power);
		if (jutsuToCast.isTargetedLocationJutsu()) return jutsuToCast.castAtLocation(caster, target.getLocation(), power);
		return true;
	}

	private class ChainBouncer implements Runnable {

		private LivingEntity caster;
		private Location start;
		private List<LivingEntity> targets;
		private float power;
		private int current = 0;
		private int taskId;

		private ChainBouncer(LivingEntity caster, Location start, List<LivingEntity> targets, float power) {
			this.caster = caster;
			this.start = start;
			this.targets = targets;
			this.power = power;
			taskId = MagicJutsus.scheduleRepeatingTask(this, 0, interval);
		}

		@Override
		public void run() {
			Location from;
			if (current == 0) from = start;
			else from = targets.get(current - 1).getLocation();

			castJutsuAt(caster, from, targets.get(current), power);
			if (current > 0) {
				playJutsuEffectsTrail(targets.get(current - 1).getLocation().add(0, 0.5, 0), targets.get(current).getLocation().add(0, 0.5, 0));
			} else if (current == 0 && caster != null) {
				playJutsuEffectsTrail(caster.getLocation().add(0, 0.5, 0), targets.get(current).getLocation().add(0, 0.5, 0));
			}

			playJutsuEffects(EffectPosition.TARGET, targets.get(current));
			current++;
			if (current >= targets.size()) MagicJutsus.cancelTask(taskId);
		}

	}

}
