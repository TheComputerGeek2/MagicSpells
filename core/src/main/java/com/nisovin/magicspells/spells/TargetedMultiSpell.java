package com.nisovin.magicspells.spells;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public final class TargetedMultiSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static final Pattern DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private final Random random;

	private List<Action> actions;
	private List<String> spellList;

	private ConfigData<Float> yOffset;

	private boolean pointBlank;
	private boolean stopOnFail;
	private boolean passTargeting;
	private boolean requireEntityTarget;
	private boolean castRandomSpellInstead;

	public TargetedMultiSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		random = ThreadLocalRandom.current();

		actions = new ArrayList<>();
		spellList = getConfigStringList("spells", null);

		yOffset = getConfigDataFloat("y-offset", 0F);

		pointBlank = getConfigBoolean("point-blank", false);
		stopOnFail = getConfigBoolean("stop-on-fail", true);
		passTargeting = getConfigBoolean("pass-targeting", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		castRandomSpellInstead = getConfigBoolean("cast-random-spell-instead", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (spellList == null) return;

		for (String s : spellList) {
			if (RegexUtil.matches(DELAY_PATTERN, s)) {
				int delay = Integer.parseInt(s.split(" ")[1]);
				actions.add(new Action(delay));
				continue;
			}

			Subspell spell = new Subspell(s);
			if (spell.process()) actions.add(new Action(spell));
			else MagicSpells.error("TargetedMultiSpell '" + internalName + "' has an invalid spell '" + s + "' defined!");
		}

		spellList = null;
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location locTarget = null;
			LivingEntity entTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> info = getTargetedEntity(caster, power, args);
				if (info != null) {
					entTarget = info.getTarget();
					power = info.getPower();
				}
			} else if (pointBlank) {
				locTarget = caster.getLocation();
			} else {
				Block b;
				try {
					b = getTargetedBlock(caster, power);
					if (b != null && !BlockUtils.isAir(b.getType())) {
						locTarget = b.getLocation();
						locTarget.add(0.5, 0, 0.5);
					}
				} catch (IllegalStateException e) {
					DebugHandler.debugIllegalState(e);
				}
			}
			if (locTarget == null && entTarget == null) return noTarget(caster);
			if (locTarget != null) {
				locTarget.setY(locTarget.getY() + yOffset.get(caster, null, power, args));
				locTarget.setDirection(caster.getLocation().getDirection());
			}
			
			boolean somethingWasDone = runSpells(caster, entTarget, locTarget, power);
			if (!somethingWasDone) return noTarget(caster);
			
			if (entTarget != null) {
				sendMessages(caster, entTarget, args);
				return PostCastAction.NO_MESSAGES;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		return runSpells(caster, null, target.clone().add(0, yOffset.get(caster, null, power, args), 0), power);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return runSpells(caster, null, target.clone().add(0, yOffset.get(caster, null, power, null), 0), power);
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return runSpells(null, null, target.clone().add(0, yOffset.get(null, null, power, args), 0), power);
	}

	@Override
	public boolean castAtLocation(Location location, float power) {
		return runSpells(null, null, location.clone().add(0, yOffset.get(null, null, power, null), 0), power);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return runSpells(caster, target, null, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return runSpells(null, target, null, power);
	}

	private boolean runSpells(LivingEntity livingEntity, LivingEntity entTarget, Location locTarget, float power) {
		boolean somethingWasDone = false;
		if (!castRandomSpellInstead) {
			int delay = 0;
			Subspell spell;
			List<DelayedSpell> delayedSpells = new ArrayList<>();
			for (Action action : actions) {
				if (action.isDelay()) {
					delay += action.getDelay();
					continue;
				}
				if (action.isSpell()) {
					spell = action.getSpell();
					if (delay == 0) {
						boolean ok = castTargetedSpell(spell, livingEntity, entTarget, locTarget, power);
						if (ok) somethingWasDone = true;
						else if (stopOnFail) break;
						continue;
					}

					DelayedSpell ds = new DelayedSpell(spell, livingEntity, entTarget, locTarget, power, delayedSpells);
					delayedSpells.add(ds);
					MagicSpells.scheduleDelayedTask(ds, delay);
					somethingWasDone = true;
				}
			}
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isSpell()) somethingWasDone = castTargetedSpell(action.getSpell(), livingEntity, entTarget, locTarget, power);
			else somethingWasDone = false;
		}
		if (somethingWasDone) {
			if (livingEntity != null) {
				if (entTarget != null) playSpellEffects(livingEntity, entTarget);
				else if (locTarget != null) playSpellEffects(livingEntity, locTarget);
			} else {
				if (entTarget != null) playSpellEffects(EffectPosition.TARGET, entTarget);
				else if (locTarget != null) playSpellEffects(EffectPosition.TARGET, locTarget);
			}
		}
		return somethingWasDone;
	}
	
	private boolean castTargetedSpell(Subspell spell, LivingEntity caster, LivingEntity entTarget, Location locTarget, float power) {
		if (spell.isTargetedEntitySpell() && entTarget != null) {
			return spell.castAtEntity(caster, entTarget, power, passTargeting);
		}

		if (spell.isTargetedLocationSpell()) {
			if (entTarget != null) return spell.castAtLocation(caster, entTarget.getLocation(), power);
			if (locTarget != null) return spell.castAtLocation(caster, locTarget, power);
		}

		PostCastAction action = spell.cast(caster, power);
		return action == PostCastAction.HANDLE_NORMALLY || action == PostCastAction.NO_MESSAGES;
	}
	
	private static class Action {
		
		private final Subspell spell;
		private final int delay;
		
		private Action(Subspell spell) {
			this.spell = spell;
			delay = 0;
		}

		private Action(int delay) {
			this.delay = delay;
			spell = null;
		}
		
		public boolean isSpell() {
			return spell != null;
		}
		
		public Subspell getSpell() {
			return spell;
		}
		
		public boolean isDelay() {
			return delay > 0;
		}
		
		public int getDelay() {
			return delay;
		}
		
	}
	
	private class DelayedSpell implements Runnable {
		
		private final Subspell spell;
		private final LivingEntity caster;
		private final LivingEntity entTarget;
		private final Location locTarget;
		private final float power;
		
		private List<DelayedSpell> delayedSpells;
		private boolean cancelled;
		
		private DelayedSpell(Subspell spell, LivingEntity caster, LivingEntity entTarget, Location locTarget, float power, List<DelayedSpell> delayedSpells) {
			this.spell = spell;
			this.caster = caster;
			this.entTarget = entTarget;
			this.locTarget = locTarget;
			this.power = power;
			this.delayedSpells = delayedSpells;
			cancelled = false;
		}
		
		public void cancel() {
			cancelled = true;
			delayedSpells = null;
		}
		
		public void cancelAll() {
			for (DelayedSpell ds : delayedSpells) {
				if (ds == this) continue;
				ds.cancel();
			}
			delayedSpells.clear();
			cancel();
		}
		
		@Override
		public void run() {
			if (cancelled) {
				delayedSpells = null;
				return;
			}

			if (caster == null || caster.isValid()) {
				boolean ok = castTargetedSpell(spell, caster, entTarget, locTarget, power);
				delayedSpells.remove(this);
				if (!ok && stopOnFail) cancelAll();
			} else cancelAll();

			delayedSpells = null;
		}
		
	}
	
}
