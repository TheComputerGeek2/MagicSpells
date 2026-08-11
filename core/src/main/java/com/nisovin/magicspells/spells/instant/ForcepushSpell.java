package com.nisovin.magicspells.spells.instant;

import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ForcepushSpell extends InstantSpell {

	private final ConfigData<Double> force;
	private final ConfigData<Double> radius;
	private final ConfigData<Double> yForce;
	private final ConfigData<Double> maxYForce;

	private final ConfigData<Boolean> powerAffectsForce;
	private final ConfigData<Boolean> addVelocityInstead;

	public ForcepushSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		force = getConfigDataDouble("pushback-force", 30);
		radius = getConfigDataDouble("radius", 3);
		yForce = getConfigDataDouble("additional-vertical-force", 15);
		maxYForce = getConfigDataDouble("max-vertical-force", 20);

		powerAffectsForce = getConfigDataBoolean("power-affects-force", true);
		addVelocityInstead = getConfigDataBoolean("add-velocity-instead", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		double radius = Math.min(this.radius.get(data), MagicSpells.getGlobalRadius());

		Vector casterLoc = data.caster().getLocation().toVector();
		for (Entity entity : data.caster().getNearbyEntities(radius, radius, radius)) {
			if (!(entity instanceof LivingEntity target)) continue;
			if (!validTargetList.canTarget(data.caster(), target)) continue;

			SpellTargetEvent event = new SpellTargetEvent(this, data, target);
			if (!event.callEvent()) continue;

			SpellData subData = event.getSpellData();
			target = event.getTarget();

			double force = this.force.get(subData) / 10;
			if (powerAffectsForce.get(subData)) force *= subData.power();

			Vector velocity = target.getLocation().toVector().subtract(casterLoc).normalize().multiply(force);

			double yForce = this.yForce.get(subData) / 10;
			if (powerAffectsForce.get(subData)) yForce *= subData.power();

			velocity.setY(Math.min(velocity.getY() + yForce, maxYForce.get(subData) / 10));
			velocity = Util.makeFinite(velocity);

			if (addVelocityInstead.get(subData)) target.setVelocity(target.getVelocity().add(velocity));
			else target.setVelocity(velocity);

			playSpellEffects(EffectPosition.TARGET, target, subData);
			playSpellEffectsTrail(subData.caster().getLocation(), target.getLocation(), subData);
		}

		playSpellEffects(EffectPosition.CASTER, data.caster(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
