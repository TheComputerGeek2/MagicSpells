package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MobUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class RoarSpell extends InstantSpell {

	private ConfigData<Double> radius;

	private String strNoTarget;

	private boolean cancelIfNoTargets;

	public RoarSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		radius = getConfigDataDouble("radius", 8F);

		strNoTarget = getConfigString("str-no-target", "No targets found.");

		cancelIfNoTargets = getConfigBoolean("cancel-if-no-targets", true);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			double radius = Math.min(this.radius.get(caster, null, power, args), MagicSpells.getGlobalRadius());
			List<Entity> entities = caster.getNearbyEntities(radius, radius, radius);

			int count = 0;
			for (Entity entity : entities) {
				if (!(entity instanceof LivingEntity livingEntity)) continue;
				if (entity instanceof Player) continue;
				if (!validTargetList.canTarget(caster, entity)) continue;
				MobUtil.setTarget(livingEntity, caster);
				playSpellEffectsTrail(caster.getLocation(), entity.getLocation());
				playSpellEffects(EffectPosition.TARGET, entity);
				count++;
			}

			if (cancelIfNoTargets && count == 0) {
				sendMessage(strNoTarget, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public String getStrNoTarget() {
		return strNoTarget;
	}

	public void setStrNoTarget(String strNoTarget) {
		this.strNoTarget = strNoTarget;
	}

	public boolean shouldCancelIfNoTargets() {
		return cancelIfNoTargets;
	}

	public void setCancelIfNoTargets(boolean cancelIfNoTargets) {
		this.cancelIfNoTargets = cancelIfNoTargets;
	}

}
