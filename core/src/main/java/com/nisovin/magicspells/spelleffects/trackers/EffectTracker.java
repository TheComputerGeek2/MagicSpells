package com.nisovin.magicspells.spelleffects.trackers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.DelayableEntity;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.effecttypes.EntityEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

public class EffectTracker implements Runnable {

	protected Entity entity;
	protected BuffSpell buffSpell;
	protected SpellEffect effect;
	protected SpellEffectActiveChecker checker;

	protected SpellData data;

	protected ScheduledTask effectTrackerTask;

	protected boolean isEntityEffect;

	protected DelayableEntity<Entity> effectEntity;

	public EffectTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		this.entity = entity;
		this.checker = checker;
		this.effect = effect;
		this.data = data;

		isEntityEffect = effect instanceof EntityEffect;

		int interval = effect.getEffectInterval().get(data);
		effectTrackerTask = MagicSpells.scheduleRepeatingTask(this, 0, interval, entity);
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
		return effectTrackerTask;
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
		MagicSpells.cancelTask(effectTrackerTask);
		entity = null;
		if (effectEntity != null) effectEntity.remove();
	}

	public void unregister() {
		if (buffSpell != null) buffSpell.getEffectTrackers().remove(this);
	}

}
