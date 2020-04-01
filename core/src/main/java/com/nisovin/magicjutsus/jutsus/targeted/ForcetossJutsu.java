package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class ForcetossJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private int damage;

	private float vForce;
	private float hForce;
	private float rotation;

	private boolean checkPlugins;
	private boolean powerAffectsForce;
	private boolean addVelocityInstead;
	private boolean avoidDamageModification;

	public ForcetossJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		damage = getConfigInt("damage", 0);

		vForce = getConfigFloat("vertical-force", 10) / 10.0F;
		hForce = getConfigFloat("horizontal-force", 20) / 10.0F;
		rotation = getConfigFloat("rotation", 0);

		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsForce = getConfigBoolean("power-affects-force", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);

			toss(livingEntity, targetInfo.getTarget(), targetInfo.getPower());
			sendMessages(livingEntity, targetInfo.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		toss(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private void toss(LivingEntity livingEntity, LivingEntity target, float power) {
		if (target == null) return;
		if (livingEntity == null) return;
		if (!livingEntity.getLocation().getWorld().equals(target.getLocation().getWorld())) return;

		if (!powerAffectsForce) power = 1f;

		if (damage > 0) {
			double dmg = damage * power;
			if (checkPlugins) {
				MagicJutsusEntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(livingEntity, target, DamageCause.ENTITY_ATTACK, damage);
				EventUtil.call(event);
				if (!avoidDamageModification) dmg = event.getDamage();
			}
			target.damage(dmg);
		}

		Vector v;
		if (livingEntity.equals(target)) v = livingEntity.getLocation().getDirection();
		else v = target.getLocation().toVector().subtract(livingEntity.getLocation().toVector());

		if (v == null) throw new NullPointerException("v");
		v.setY(0).normalize().multiply(hForce * power).setY(vForce * power);
		if (rotation != 0) Util.rotateVector(v, rotation);
		v = Util.makeFinite(v);
		if (addVelocityInstead) target.setVelocity(target.getVelocity().add(v));
		else target.setVelocity(v);

		playJutsuEffects(livingEntity, target);
	}

}
