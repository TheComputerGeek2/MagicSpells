package com.nisovin.magicspells.spells.targeted;

import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.util.Util;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class ForcetossSpell extends TargetedSpell implements TargetedEntitySpell {

	private int damage;
	private float hForce;
	private float vForce;
	private float rotation;
	private boolean checkPlugins;
	private boolean powerAffectsForce;
	private boolean avoidDamageModification;

	public ForcetossSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damage = getConfigInt("damage", 0);
		hForce = getConfigFloat("horizontal-force", 20) / 10.0F;
		vForce = getConfigFloat("vertical-force", 10) / 10.0F;
		rotation = getConfigFloat("rotation", 0);
		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsForce = getConfigBoolean("power-affects-force", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// Get target
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
			if (targetInfo == null) return noTarget(player);

			// Throw target
			toss(player, targetInfo.getTarget(), targetInfo.getPower());

			sendMessages(player, targetInfo.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void toss(Player player, LivingEntity target, float power) {
		if (!powerAffectsForce) power = 1f;

		// Deal damage
		if (damage > 0) {
			double damage = this.damage * power;
			if (target instanceof Player && checkPlugins) {
				MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, damage);
				EventUtil.call(event);
				if (!avoidDamageModification) damage = event.getDamage();
			}
			target.damage(damage);
		}

		Vector v;
		if (player.equals(target)) {
			v = player.getLocation().getDirection();
		} else {
			v = target.getLocation().toVector().subtract(player.getLocation().toVector());
		}
		if (v == null) throw new NullPointerException("v");
		v.setY(0).normalize().multiply(hForce * power).setY(vForce * power);
		if (rotation != 0) Util.rotateVector(v, rotation);
		target.setVelocity(v);
		playSpellEffects(player, target);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		toss(caster, target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
