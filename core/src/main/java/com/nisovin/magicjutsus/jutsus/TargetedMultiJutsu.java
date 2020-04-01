package com.nisovin.magicjutsus.jutsus;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public final class TargetedMultiJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedLocationJutsu {

	private static final Pattern DELAY_PATTERN = Pattern.compile("DELAY [0-9]+");

	private Random random;

	private List<Action> actions;
	private List<String> jutsuList;

	private float yOffset;

	private boolean pointBlank;
	private boolean stopOnFail;
	private boolean requireEntityTarget;
	private boolean castRandomJutsuInstead;

	public TargetedMultiJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		random = new Random();

		actions = new ArrayList<>();
		jutsuList = getConfigStringList("jutsus", null);

		yOffset = getConfigFloat("y-offset", 0F);

		pointBlank = getConfigBoolean("point-blank", false);
		stopOnFail = getConfigBoolean("stop-on-fail", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		castRandomJutsuInstead = getConfigBoolean("cast-random-jutsu-instead", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (jutsuList != null) {
			for (String s : jutsuList) {
				if (RegexUtil.matches(DELAY_PATTERN, s)) {
					int delay = Integer.parseInt(s.split(" ")[1]);
					actions.add(new Action(delay));
				} else {
					Ninjutsu jutsu = new Ninjutsu(s);
					if (jutsu.process()) actions.add(new Action(jutsu));
					else MagicJutsus.error("TargetedMultiJutsu '" + internalName + "' has an invalid jutsu '" + s + "' defined!");
				}
			}
		}
		jutsuList = null;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location locTarget = null;
			LivingEntity entTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> info = getTargetedEntity(livingEntity, power);
				if (info != null) {
					entTarget = info.getTarget();
					power = info.getPower();
				}
			} else if (pointBlank) {
				locTarget = livingEntity.getLocation();
			} else {
				Block b;
				try {
					b = getTargetedBlock(livingEntity, power);
					if (b != null && !BlockUtils.isAir(b.getType())) {
						locTarget = b.getLocation();
						locTarget.add(0.5, 0, 0.5);
					}
				} catch (IllegalStateException e) {
					DebugHandler.debugIllegalState(e);
				}
			}
			if (locTarget == null && entTarget == null) return noTarget(livingEntity);
			if (locTarget != null) {
				locTarget.setY(locTarget.getY() + yOffset);
				locTarget.setDirection(livingEntity.getLocation().getDirection());
			}
			
			boolean somethingWasDone = runJutsus(livingEntity, entTarget, locTarget, power);
			if (!somethingWasDone) return noTarget(livingEntity);
			
			if (entTarget != null) {
				sendMessages(livingEntity, entTarget);
				return PostCastAction.NO_MESSAGES;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return runJutsus(caster, null, target.clone().add(0, yOffset, 0), power);
	}
	
	@Override
	public boolean castAtLocation(Location location, float power) {
		return runJutsus(null, null, location.clone().add(0, yOffset, 0), power);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return runJutsus(caster, target, null, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return runJutsus(null, target, null, power);
	}
	
	private boolean runJutsus(LivingEntity livingEntity, LivingEntity entTarget, Location locTarget, float power) {
		boolean somethingWasDone = false;
		if (!castRandomJutsuInstead) {
			int delay = 0;
			Ninjutsu jutsu;
			List<DelayedJutsu> delayedJutsus = new ArrayList<>();
			for (Action action : actions) {
				if (action.isDelay()) {
					delay += action.getDelay();
				} else if (action.isJutsu()) {
					jutsu = action.getJutsu();
					if (delay == 0) {
						boolean ok = castTargetedJutsu(jutsu, livingEntity, entTarget, locTarget, power);
						if (ok) somethingWasDone = true;
						else if (stopOnFail) break;
					} else {
						DelayedJutsu ds = new DelayedJutsu(jutsu, livingEntity, entTarget, locTarget, power, delayedJutsus);
						delayedJutsus.add(ds);
						MagicJutsus.scheduleDelayedTask(ds, delay);
						somethingWasDone = true;
					}
				}
			}
		} else {
			Action action = actions.get(random.nextInt(actions.size()));
			if (action.isJutsu()) somethingWasDone = castTargetedJutsu(action.getJutsu(), livingEntity, entTarget, locTarget, power);
			else somethingWasDone = false;
		}
		if (somethingWasDone) {
			if (livingEntity != null) {
				if (entTarget != null) playJutsuEffects(livingEntity, entTarget);
				else if (locTarget != null) playJutsuEffects(livingEntity, locTarget);
			} else {
				if (entTarget != null) playJutsuEffects(EffectPosition.TARGET, entTarget);
				else if (locTarget != null) playJutsuEffects(EffectPosition.TARGET, locTarget);
			}
		}
		return somethingWasDone;
	}
	
	private boolean castTargetedJutsu(Ninjutsu jutsu, LivingEntity caster, LivingEntity entTarget, Location locTarget, float power) {
		boolean success = false;
		if (jutsu.isTargetedEntityJutsu() && entTarget != null) {
			success = jutsu.castAtEntity(caster, entTarget, power);
		} else if (jutsu.isTargetedLocationJutsu()) {
			if (entTarget != null) success = jutsu.castAtLocation(caster, entTarget.getLocation(), power);
			else if (locTarget != null) success = jutsu.castAtLocation(caster, locTarget, power);
		} else {
			success = jutsu.cast(caster, power) == PostCastAction.HANDLE_NORMALLY;
		}
		return success;
	}
	
	private static class Action {
		
		private Ninjutsu jutsu;
		private int delay;
		
		Action(Ninjutsu jutsu) {
			this.jutsu = jutsu;
			delay = 0;
		}
		
		Action(int delay) {
			this.delay = delay;
			jutsu = null;
		}
		
		public boolean isJutsu() {
			return jutsu != null;
		}
		
		public Ninjutsu getJutsu() {
			return jutsu;
		}
		
		public boolean isDelay() {
			return delay > 0;
		}
		
		public int getDelay() {
			return delay;
		}
		
	}
	
	private class DelayedJutsu implements Runnable {
		
		private Ninjutsu jutsu;
		private LivingEntity caster;
		private LivingEntity entTarget;
		private Location locTarget;
		private float power;
		
		private List<DelayedJutsu> delayedJutsus;
		private boolean cancelled;
		
		DelayedJutsu(Ninjutsu jutsu, LivingEntity caster, LivingEntity entTarget, Location locTarget, float power, List<DelayedJutsu> delayedJutsus) {
			this.jutsu = jutsu;
			this.caster = caster;
			this.entTarget = entTarget;
			this.locTarget = locTarget;
			this.power = power;
			this.delayedJutsus = delayedJutsus;
			cancelled = false;
		}
		
		public void cancel() {
			cancelled = true;
			delayedJutsus = null;
		}
		
		public void cancelAll() {
			for (DelayedJutsu ds : delayedJutsus) {
				if (ds == this) continue;
				ds.cancel();
			}
			delayedJutsus.clear();
			cancel();
		}
		
		@Override
		public void run() {
			if (!cancelled) {
				if (caster == null || caster.isValid()) {
					boolean ok = castTargetedJutsu(jutsu, caster, entTarget, locTarget, power);
					delayedJutsus.remove(this);
					if (!ok && stopOnFail) cancelAll();
				} else cancelAll();
			}
			delayedJutsus = null;
		}
		
	}
	
}
