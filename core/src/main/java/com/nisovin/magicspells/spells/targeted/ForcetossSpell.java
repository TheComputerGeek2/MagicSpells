package com.nisovin.magicspells.spells.targeted;

import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class ForcetossSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Double> damage;

	private final ConfigData<Double> vForce;
	private final ConfigData<Double> hForce;
	private final ConfigData<Double> rotation;

	private final ConfigData<Boolean> powerAffectsForce;
	private final ConfigData<Boolean> powerAffectsDamage;
	private final ConfigData<Boolean> addVelocityInstead;

	public ForcetossSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		damage = getConfigDataDouble("damage", 0);

		vForce = getConfigDataDouble("vertical-force", 10);
		hForce = getConfigDataDouble("horizontal-force", 20);
		rotation = getConfigDataDouble("rotation", 0);

		powerAffectsForce = getConfigDataBoolean("power-affects-force", true);
		powerAffectsDamage = getConfigDataBoolean("power-affects-damage", true);
		addVelocityInstead = getConfigDataBoolean("add-velocity-instead", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		if (!caster.getWorld().equals(target.getWorld())) return noTarget(data);

		double damage = this.damage.get(data);
		if (powerAffectsDamage.get(data)) damage *= data.power();

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, caster, target, damage, "");
		event.callEvent();
		damage = event.getFinalDamage();

		if (damage > 0) target.damage(damage, caster);

		Vector v;
		if (caster.equals(target)) v = caster.getLocation().getDirection();
		else v = target.getLocation().toVector().subtract(caster.getLocation().toVector());

		double hForce = this.hForce.get(data) / 10;
		double vForce = this.vForce.get(data) / 10;
		if (powerAffectsForce.get(data)) {
			hForce *= data.power();
			vForce *= data.power();
		}
		v.setY(0).normalize().multiply(hForce).setY(vForce);

		double rotation = this.rotation.get(data);
		if (rotation != 0) Util.rotateVector(v, rotation);

		v = Util.makeFinite(v);

		if (addVelocityInstead.get(data)) target.setVelocity(target.getVelocity().add(v));
		else target.setVelocity(v);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
