package com.nisovin.magicspells.spelleffects.trackers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.ModifierResult;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.spelleffects.SpellEffect.SpellEffectActiveChecker;

public class BuffTracker extends EffectTracker implements Runnable {

	private final boolean dragEntity;

	public BuffTracker(Entity entity, SpellEffectActiveChecker checker, SpellEffect effect, SpellData data) {
		super(entity, checker, effect, data);

		dragEntity = effect.isDraggingEntity().get(data);
	}

	@Override
	public void run() {
		if (!checker.isActive(entity) || effect == null) {
			stop();
			return;
		}
		if (!entity.isValid()) {
			if (!(entity instanceof Player)) stop();
			return;
		}

		if (entity instanceof LivingEntity livingEntity && effect.getModifiers() != null) {
			ModifierResult result = effect.getModifiers().apply(livingEntity, data);
			data = result.data();

			if (!result.check()) return;
		}

		playEffects(entity, data);
	}

	private void playEffects(Entity entity, SpellData data) {
		if (!isEntityEffect) {
			effect.playEffect(entity, data);
			return;
		}

		if (!dragEntity) {
			effect.playEffect(entity, data);
			return;
		}

		if (effectEntity == null) {
			effectEntity = effect.playEntityEffect(entity.getLocation(), data);
			return;
		}

		effectEntity.teleport(effect.applyOffsets(entity.getLocation(), data));
	}

}
