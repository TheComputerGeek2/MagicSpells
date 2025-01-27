package com.nisovin.magicspells.spells.targeted;

import java.util.List;
import java.util.ArrayList;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class ChainSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final ConfigData<Integer> bounces;
	private final ConfigData<Integer> interval;

	private final ConfigData<Double> bounceRange;

	private String spellToCastName;
	private Subspell spellToCast;

	public ChainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		bounces = getConfigDataInt("bounces", 3);
		interval = getConfigDataInt("interval", 10);

		bounceRange = getConfigDataDouble("bounce-range", 8);

		spellToCastName = getConfigString("spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		spellToCast = initSubspell(spellToCastName,
				"ChainSpell '" + internalName + "' has an invalid spell defined!");
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		chain(data.caster().getLocation(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		chain(data.hasCaster() ? data.caster().getLocation() : null, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		chain(data.location(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void chain(Location start, SpellData data) {
		List<LivingEntity> targets = new ArrayList<>();
		List<Float> targetPowers = new ArrayList<>();

		targets.add(data.target());
		targetPowers.add(data.power());

		data = data.location(start);

		int bounces = this.bounces.get(data);
		int interval = this.interval.get(data);
		double bounceRange = Math.min(this.bounceRange.get(data), MagicSpells.getGlobalRadius());

		// Get targets
		LivingEntity current = data.target();
		int attempts = 0;
		while (targets.size() < bounces && attempts++ < bounces << 1) {
			List<Entity> entities = current.getNearbyEntities(bounceRange, bounceRange, bounceRange);
			for (Entity entity : entities) {
				if (!(entity instanceof LivingEntity target) || targets.contains(target) || !validTargetList.canTarget(data.caster(), target))
					continue;

				float subPower = data.power();
				if (data.hasCaster()) {
					SpellTargetEvent event = new SpellTargetEvent(this, data, target);
					if (!event.callEvent()) continue;

					subPower = event.getPower();
					target = event.getTarget();
				}

				targets.add(target);
				targetPowers.add(subPower);
				current = target;

				break;
			}
		}

		// Cast spell at targets
		if (data.hasCaster()) playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		else if (start != null) playSpellEffects(EffectPosition.CASTER, start, data);

		if (interval <= 0) {
			for (int i = 0; i < targets.size(); i++) {
				Location from = i == 0 ? start : targets.get(i - 1).getLocation();
				LivingEntity target = targets.get(i);
				float power = targetPowers.get(i);

				SpellData subData = data.builder().target(target).location(from).power(power).build();
				if (spellToCast != null) spellToCast.subcast(subData);

				if (from != null) playSpellEffectsTrail(from, target.getLocation(), subData);
				playSpellEffects(EffectPosition.TARGET, target, subData);
			}
		} else new ChainBouncer(start, data, targets, targetPowers, interval);
	}

	private class ChainBouncer implements Runnable {

		private final SpellData data;
		private final Location start;
		private final ScheduledTask task;

		private final List<LivingEntity> targets;
		private final List<Float> targetPowers;

		private int current = 0;

		private ChainBouncer(Location start, SpellData data, List<LivingEntity> targets, List<Float> targetPowers, int interval) {
			this.start = start;
			this.data = data;

			this.targetPowers = targetPowers;
			this.targets = targets;

			task = MagicSpells.scheduleRepeatingTask(this, 0, interval, start);
		}

		@Override
		public void run() {
			Location from = current == 0 ? start : targets.get(current - 1).getLocation();
			LivingEntity target = targets.get(current);
			float power = targetPowers.get(current);

			SpellData subData = data.builder().target(target).location(from).power(power).build();
			if (spellToCast != null) spellToCast.subcast(subData);

			if (from != null) playSpellEffectsTrail(from.add(0, 0.5, 0), target.getLocation().add(0, 0.5, 0), subData);
			playSpellEffects(EffectPosition.TARGET, target, subData);

			current++;
			if (current >= targets.size()) MagicSpells.cancelTask(task);
		}

	}

}
