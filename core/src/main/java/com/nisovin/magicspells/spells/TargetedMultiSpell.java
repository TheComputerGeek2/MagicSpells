package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public final class TargetedMultiSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell, TargetedEntityFromLocationSpell {

	private static final Pattern DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private List<String> spellList;
	private final List<Action> actions;

	private final ConfigData<Float> yOffset;

	private final ConfigData<Boolean> pointBlank;
	private final ConfigData<Boolean> stopOnFail;
	private final ConfigData<Boolean> passTargeting;
	private final ConfigData<Boolean> stopOnSuccess;
	private final ConfigData<Boolean> requireEntityTarget;
	private final ConfigData<Boolean> castRandomSpellInstead;

	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		actions = new ArrayList<>();
		spellList = getConfigStringList("spells", null);

		yOffset = getConfigDataFloat("y-offset", 0F);

		pointBlank = getConfigDataBoolean("point-blank", false);
		stopOnFail = getConfigDataBoolean("stop-on-fail", true);
		passTargeting = getConfigDataBoolean("pass-targeting", false);
		stopOnSuccess = getConfigDataBoolean("stop-on-success", false);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", false);
		castRandomSpellInstead = getConfigDataBoolean("cast-random-spell-instead", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (spellList == null) return;

		for (String s : spellList) {
			if (DELAY_PATTERN.asMatchPredicate().test(s)) {
				int delay = Integer.parseInt(s.split(" ")[1]);
				actions.add(new DelayAction(delay));
				continue;
			}

			Subspell spell = initSubspell(s, "TargetedMultiSpell '" + internalName + "' has an invalid spell '" + s + "' defined!");
			if (spell != null) actions.add(new SpellAction(spell));
		}

		spellList = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (requireEntityTarget.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);

			return runSpells(info.spellData());
		}

		if (pointBlank.get(data)) {
			SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
			if (!targetEvent.callEvent()) return noTarget(targetEvent);

			return runSpells(targetEvent.getSpellData());
		}

		TargetInfo<Location> info = getTargetedBlockLocation(data, 0.5, 0, 0.5, false);
		if (info.noTarget()) return noTarget(info);

		return runSpells(info.spellData());
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		return runSpells(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		return runSpells(data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		return runSpells(data);
	}

	private CastResult runSpells(SpellData data) {
		if (actions.isEmpty()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (data.hasLocation() && !data.hasTarget())
			data = data.location(data.location().add(0, yOffset.get(data), 0));

		boolean passTargeting = this.passTargeting.get(data);

		if (castRandomSpellInstead.get(data)) {
			Action action = actions.get(random.nextInt(actions.size()));

			boolean casted = action instanceof SpellAction(Subspell spell) && spell.subcast(data, passTargeting).success();
			if (!casted) return noTarget(data);

			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		boolean stopOnFail = this.stopOnFail.get(data);
		boolean stopOnSuccess = this.stopOnSuccess.get(data);

		DelayedSpells delayedSpells = new DelayedSpells(new IntArrayList(), data, passTargeting, stopOnFail, stopOnSuccess);
		int totalDelay = 0;

		boolean casted = false;
		loop:
		for (Action action : actions) {
			switch (action) {
				case DelayAction(int delay) -> totalDelay += delay;
				case SpellAction(Subspell subspell) -> {
					if (totalDelay > 0) {
						delayedSpells.scheduleSpell(subspell, totalDelay);
						casted = true;
						continue;
					}

					boolean success = subspell.subcast(data, passTargeting).success();
					casted |= success;

					if (stopOnSuccess && success || stopOnFail && !success) {
						delayedSpells.cancel();
						break loop;
					}
				}
			}
		}

		if (casted) {
			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		return noTarget(data);
	}

	private sealed interface Action permits DelayAction, SpellAction {}

	private record DelayAction(int delay) implements Action {}

	private record SpellAction(Subspell subspell) implements Action {}

	private record DelayedSpells(IntCollection tasks, SpellData data, boolean passTargeting, boolean stopOnFail, boolean stopOnSuccess) {

		private void cancel() {
			tasks.forEach(MagicSpells::cancelTask);
		}

		private void scheduleSpell(Subspell subspell, int delay) {
			tasks.add(MagicSpells.scheduleDelayedTask(() -> {
				if (!data.isValid()) {
					cancel();
					return;
				}

				boolean success = subspell.subcast(data, passTargeting).success();
				if (stopOnSuccess && success || stopOnFail && !success) cancel();
			}, delay));
		}

	}

}
