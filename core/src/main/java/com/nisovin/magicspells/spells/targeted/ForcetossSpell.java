package com.nisovin.magicspells.spells.targeted;

import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class ForcetossSpell extends TargetedSpell implements TargetedEntitySpell {

	private int damage;

	private float vForce;
	private float hForce;
	private float rotation;

	private boolean checkPlugins;
	private boolean powerAffectsForce;
	private boolean addVelocityInstead;
	private boolean avoidDamageModification;

	public ForcetossSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damage = getConfigInt("damage", 0);

		vForce = getConfigFloat("vertical-force", 10) / 10.0F;
		hForce = getConfigFloat("horizontal-force", 20) / 10.0F;
		rotation = getConfigFloat("rotation", 0);

		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsForce = getConfigBoolean("power-affects-force", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", true);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(caster, power);
			if (targetInfo == null) return noTarget(caster);

			toss(caster, targetInfo.getTarget(), targetInfo.getPower(), args);
			sendMessages(caster, targetInfo.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		toss(caster, target, power, args);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void toss(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (target == null) return;
		if (caster == null) return;
		if (!caster.getLocation().getWorld().equals(target.getLocation().getWorld())) return;

		if (!powerAffectsForce) power = 1f;

		if (damage > 0) {
			double dmg = damage * power;
			if (checkPlugins) {
				MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, DamageCause.ENTITY_ATTACK, damage, this);
				EventUtil.call(event);
				if (!avoidDamageModification) dmg = event.getDamage();
			}
			target.damage(dmg);
		}

		Vector v;
		if (caster.equals(target)) v = caster.getLocation().getDirection();
		else v = target.getLocation().toVector().subtract(caster.getLocation().toVector());

		if (v == null) throw new NullPointerException("v");
		v.setY(0).normalize().multiply(hForce * power).setY(vForce * power);
		if (rotation != 0) Util.rotateVector(v, rotation);
		v = Util.makeFinite(v);
		if (addVelocityInstead) target.setVelocity(target.getVelocity().add(v));
		else target.setVelocity(v);

		playSpellEffects(caster, target);
	}

}
