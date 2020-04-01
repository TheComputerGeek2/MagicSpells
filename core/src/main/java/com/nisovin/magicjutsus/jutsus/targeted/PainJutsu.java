package com.nisovin.magicjutsus.jutsus.targeted;

import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.jutsus.JutsuDamageJutsu;
import com.nisovin.magicjutsus.util.compat.CompatBasics;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.events.JutsuApplyDamageEvent;
import com.nisovin.magicjutsus.events.MagicJutsusEntityDamageByEntityEvent;

public class PainJutsu extends TargetedJutsu implements TargetedEntityJutsu, JutsuDamageJutsu {

	private String jutsuDamageType;
	private DamageCause damageType;

	private double damage;

	private boolean ignoreArmor;
	private boolean checkPlugins;
	private boolean avoidDamageModification;
	private boolean tryAvoidingAntiCheatPlugins;
	
	public PainJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		jutsuDamageType = getConfigString("jutsu-damage-type", "");
		String type = getConfigString("damage-type", "ENTITY_ATTACK");
		for (DamageCause cause : DamageCause.values()) {
			if (cause.name().equalsIgnoreCase(type)) {
				damageType = cause;
				break;
			}
		}
		if (damageType == null) {
			DebugHandler.debugBadEnumValue(DamageCause.class, type);
			damageType = DamageCause.ENTITY_ATTACK;
		}

		damage = getConfigFloat("damage", 4);

		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		avoidDamageModification = getConfigBoolean("avoid-damage-modification", false);
		tryAvoidingAntiCheatPlugins = getConfigBoolean("try-avoiding-anticheat-plugins", false);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(livingEntity, power);
			if (target == null) return noTarget(livingEntity);

			boolean done;
			if (livingEntity instanceof Player) done = CompatBasics.exemptAction(() -> causePain(livingEntity, target.getTarget(), target.getPower()), (Player) livingEntity, CompatBasics.activeExemptionAssistant.getPainExemptions());
			else done = causePain(livingEntity, target.getTarget(), target.getPower());
			if (!done) return noTarget(livingEntity);
			
			sendMessages(livingEntity, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) return false;
		return causePain(caster, target, power);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) return false;
		return causePain(null, target, power);
	}

	@Override
	public String getJutsuDamageType() {
		return jutsuDamageType;
	}
	
	private boolean causePain(LivingEntity caster, LivingEntity target, float power) {
		if (target == null) return false;
		if (target.isDead()) return false;
		double localDamage = damage * power;

		if (checkPlugins) {
			MagicJutsusEntityDamageByEntityEvent event = new MagicJutsusEntityDamageByEntityEvent(caster, target, damageType, localDamage);
			EventUtil.call(event);
			if (event.isCancelled()) return false;
			if (!avoidDamageModification) localDamage = event.getDamage();
			target.setLastDamageCause(event);
		}

		JutsuApplyDamageEvent event = new JutsuApplyDamageEvent(this, caster, target, localDamage, damageType, jutsuDamageType);
		EventUtil.call(event);
		localDamage = event.getFinalDamage();

		if (ignoreArmor) {
			double health = target.getHealth();
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			health = health - localDamage;
			if (health < 0) health = 0;
			if (health > Util.getMaxHealth(target)) health = Util.getMaxHealth(target);
			if (health == 0 && caster instanceof Player) MagicJutsus.getVolatileCodeHandler().setKiller(target, (Player) caster);

			target.setHealth(health);
			playJutsuEffects(caster, target);
			target.playEffect(EntityEffect.HURT);
			return true;
		}

		if (tryAvoidingAntiCheatPlugins) target.damage(localDamage);
		else target.damage(localDamage, caster);
		playJutsuEffects(caster, target);
		return true;
	}

}
