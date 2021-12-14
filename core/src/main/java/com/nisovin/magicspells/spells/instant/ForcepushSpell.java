package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class ForcepushSpell extends InstantSpell {

	private ConfigData<Double> force;
	private ConfigData<Double> radius;
	private ConfigData<Double> yForce;
	private ConfigData<Double> maxYForce;

	private boolean powerAffectsForce;
	private boolean addVelocityInstead;

	public ForcepushSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		force = getConfigDataDouble("pushback-force", 30);
		radius = getConfigDataDouble("radius", 3);
		yForce = getConfigDataDouble("additional-vertical-force", 15);
		maxYForce = getConfigDataDouble("max-vertical-force", 20);

		powerAffectsForce = getConfigBoolean("power-affects-force", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			knockback(caster, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void knockback(LivingEntity caster, float basePower, String[] args) {
		double radius = Math.min(this.radius.get(caster, null, basePower, args), MagicSpells.getGlobalRadius());

		List<Entity> entities = caster.getNearbyEntities(radius, radius, radius);
		Vector e;
		Vector v;
		Vector p = caster.getLocation().toVector();
		for (Entity entity : entities) {
			if (!(entity instanceof LivingEntity target)) continue;
			if (!validTargetList.canTarget(caster, entity)) continue;

			SpellTargetEvent event = new SpellTargetEvent(this, caster, target, basePower);
			EventUtil.call(event);
			if (event.isCancelled()) continue;

			float power = event.getPower();
			target = event.getTarget();

			double force = this.force.get(caster, target, power, args) / 10;
			if (powerAffectsForce) force *= power;

			e = target.getLocation().toVector();
			v = e.subtract(p).normalize().multiply(force);

			double yForce = this.yForce.get(caster, target, power, args) / 10;
			if (powerAffectsForce) yForce *= power;

			v.setY(Math.min(v.getY() + yForce, maxYForce.get(caster, target, power, args) / 10));
			v = Util.makeFinite(v);

			if (addVelocityInstead) target.setVelocity(target.getVelocity().add(v));
			else target.setVelocity(v);

			playSpellEffects(EffectPosition.TARGET, target);
			playSpellEffectsTrail(caster.getLocation(), target.getLocation());
		}

		playSpellEffects(EffectPosition.CASTER, caster);
	}

	public boolean shouldAddVelocityInstead() {
		return addVelocityInstead;
	}

	public void setAddVelocityInstead(boolean addVelocityInstead) {
		this.addVelocityInstead = addVelocityInstead;
	}

}
