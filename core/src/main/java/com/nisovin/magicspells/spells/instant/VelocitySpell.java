package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class VelocitySpell extends InstantSpell {

	private final Set<UUID> jumping;

	private ConfigData<Double> speed;

	private boolean cancelDamage;
	private boolean powerAffectsSpeed;
	private boolean addVelocityInstead;

	public VelocitySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		jumping = new HashSet<>();

		speed = getConfigDataDouble("speed", 40);

		cancelDamage = getConfigBoolean("cancel-damage", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);
		powerAffectsSpeed = getConfigBoolean("power-affects-speed", true);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			double speed = this.speed.get(caster, null, power, args) / 10;
			if (powerAffectsSpeed) speed *= power;

			Vector v = caster.getEyeLocation().getDirection().normalize().multiply(speed);
			if (addVelocityInstead) caster.setVelocity(caster.getVelocity().add(v));
			else caster.setVelocity(v);

			jumping.add(caster.getUniqueId());
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
		LivingEntity livingEntity = (LivingEntity) event.getEntity();
		if (!jumping.remove(livingEntity.getUniqueId())) return;
		playSpellEffects(EffectPosition.TARGET, livingEntity.getLocation());
		if (cancelDamage) event.setCancelled(true);
	}

	public Set<UUID> getJumping() {
		return jumping;
	}

	public boolean shouldCancelDamage() {
		return cancelDamage;
	}

	public void setCancelDamage(boolean cancelDamage) {
		this.cancelDamage = cancelDamage;
	}

	public boolean shouldAddVelocityInstead() {
		return addVelocityInstead;
	}

	public void setAddVelocityInstead(boolean addVelocityInstead) {
		this.addVelocityInstead = addVelocityInstead;
	}

}
