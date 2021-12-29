package com.nisovin.magicspells.spells.targeted;

import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.DamageSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell, DamageSpell {

	private String spellDamageType;
	private DamageCause damageType;

	private ConfigData<Double> damage;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean powerAffectsDamage;
	private boolean avoidDamageModification;
	private boolean tryAvoidingAntiCheatPlugins;
	
	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellDamageType = getConfigString("spell-damage-type", "");
		String damageTypeName = getConfigString("damage-type", "ENTITY_ATTACK");
		try {
			damageType = DamageCause.valueOf(damageTypeName.toUpperCase());
		} catch (IllegalArgumentException ignored) {
			DebugHandler.debugBadEnumValue(DamageCause.class, damageTypeName);
			damageType = DamageCause.ENTITY_ATTACK;
		}

		damage = getConfigDataDouble("damage", 4);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsDamage = getConfigBoolean("power-affects-damage", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", true);
		tryAvoidingAntiCheatPlugins = getConfigBoolean("try-avoiding-anticheat-plugins", false);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power, args);
			if (target == null) return noTarget(caster);

			boolean done;
			if (caster instanceof Player) done = CompatBasics.exemptAction(() -> causePain(caster, target.getTarget(), target.getPower(), args), (Player) caster, CompatBasics.activeExemptionAssistant.getPainExemptions());
			else done = causePain(caster, target.getTarget(), target.getPower(), args);
			if (!done) return noTarget(caster);
			
			sendMessages(caster, target.getTarget(), args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return causePain(caster, target, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		return castAtEntity(caster, target, power, null);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power, String[] args) {
		if (!validTargetList.canTarget(target)) return false;
		return causePain(null, target, power, args);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return castAtEntity(target, power, null);
	}

	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}
	
	private boolean causePain(LivingEntity caster, LivingEntity target, float power, String[] args) {
		if (target == null) return false;
		if (target.isDead()) return false;

		double localDamage = damage.get(caster, target, power, args);
		if (powerAffectsDamage) localDamage *= power;

		if (checkPlugins) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(caster, target, damageType, localDamage, this);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) localDamage = event.getDamage();
			target.setLastDamageCause(event);
		}

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, caster, target, localDamage, damageType, spellDamageType);
		EventUtil.call(event);
		localDamage = event.getFinalDamage();

		if (ignoreArmor) {
			double health = target.getHealth();
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			health = health - localDamage;
			if (health < 0) health = 0;
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			if (health == 0 && caster instanceof Player) target.setKiller((Player) caster);

			target.setHealth(health);
			playSpellEffects(caster, target);
			target.playEffect(EntityEffect.HURT);
			return true;
		}

		if (tryAvoidingAntiCheatPlugins) target.damage(localDamage);
		else target.damage(localDamage, caster);
		playSpellEffects(caster, target);
		return true;
	}

}
