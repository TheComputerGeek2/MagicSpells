package com.nisovin.magicspells.spelleffects.trackers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

import java.util.concurrent.TimeUnit;

public class AsyncEffectTracker implements Runnable {

	protected Entity entity;
	protected BuffSpell buffSpell;
	protected SpellEffect effect;
	protected SpellEffectActiveChecker checker;

	protected SpellData data;

	protected ScheduledTask effectTask;

	public AsyncEffectTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		this.entity = entity;
		this.checker = checker;
		this.effect = effect;
		this.data = data;

		int interval = effect.getEffectInterval().get(data);
		effectTask = Bukkit.getAsyncScheduler().runAtFixedRate(MagicSpells.getInstance(), t -> run(), 0, interval * 50L, TimeUnit.MILLISECONDS);
	}

	public Entity getEntity() {
		return entity;
	}

	public BuffSpell getBuffSpell() {
		return buffSpell;
	}

	public SpellEffect getEffect() {
		return effect;
	}

	public SpellEffectActiveChecker getChecker() {
		return checker;
	}

	public ScheduledTask getEffectTrackerTask() {
		return effectTask;
	}

	public SpellData getData() {
		return data;
	}

	public void setBuffSpell(BuffSpell spell) {
		buffSpell = spell;
	}

	@Override
	public void run() {

	}

	public void stop() {
		effectTask.cancel();
		entity = null;
	}

	public void unregister() {
		if (buffSpell != null) buffSpell.getAsyncEffectTrackers().remove(this);
	}

}
