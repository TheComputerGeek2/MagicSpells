package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell {

	private static final DeprecationNotice DAMAGE_TYPE_DEPRECATION_NOTICE = new DeprecationNotice(
		"The 'damage-type' option of '.targeted.PainSpell' does not function properly.",
		"Use '.targeted.DamageSpell', which has proper damage type support.",
		"https://github.com/TheComputerGeek2/MagicSpells/wiki/Deprecations#targetedpainspell-damage-type"
	);

	private final ConfigData<String> spellDamageType;
	private final ConfigData<DamageCause> damageType;

	private final ConfigData<Double> damage;

	private final ConfigData<Boolean> ignoreArmor;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> powerAffectsDamage;
	private final ConfigData<Boolean> avoidDamageModification;
	private final ConfigData<Boolean> tryAvoidingAntiCheatPlugins;

	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellDamageType = getConfigDataString("spell-damage-type", "");

		damageType = getConfigDataEnum("damage-type", DamageCause.class, DamageCause.ENTITY_ATTACK);

		damage = getConfigDataDouble("damage", 4);

		ignoreArmor = getConfigDataBoolean("ignore-armor", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		powerAffectsDamage = getConfigDataBoolean("power-affects-damage", true);
		avoidDamageModification = getConfigDataBoolean("avoid-damage-modification", true);
		tryAvoidingAntiCheatPlugins = getConfigDataBoolean("try-avoiding-anticheat-plugins", false);

		MagicSpells.getDeprecationManager().addDeprecation(this, DAMAGE_TYPE_DEPRECATION_NOTICE,
			!damageType.isConstant() || damageType.get() != DamageCause.ENTITY_ATTACK
		);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		if (data.caster() instanceof Player caster)
			return CompatBasics.exemptAction(() -> castAtEntity(info.spellData()), caster, CompatBasics.activeExemptionAssistant.getPainExemptions());

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.target().isValid()) return noTarget(data);

		double damage = this.damage.get(data);
		if (powerAffectsDamage.get(data)) damage *= data.power();

		DamageCause damageType = this.damageType.get(data);
		String spellDamageType = this.spellDamageType.get(data);

		boolean checkPlugins = this.checkPlugins.get(data);
		boolean avoidDamageModification = this.avoidDamageModification.get(data);

		if (checkPlugins && data.hasCaster() && damageType != DamageCause.ENTITY_ATTACK) {
			MagicSpellsEntityDamageByEntityEvent event = new MagicSpellsEntityDamageByEntityEvent(data.caster(), data.target(), damageType, damage, this);
			if (!event.callEvent()) return noTarget(data);

			if (!avoidDamageModification) damage = event.getDamage();
			data.target().setLastDamageCause(event);
		}

		boolean ignoreArmor = this.ignoreArmor.get(data);

		if (ignoreArmor && checkPlugins && data.hasCaster()) {
			EntityDamageEvent damageEvent = createFakeDamageEvent(data.caster(), data.target(), DamageCause.ENTITY_ATTACK, damage);
			if (!damageEvent.callEvent()) return noTarget(data);

			if (!avoidDamageModification) damage = damageEvent.getDamage();
		}

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, data.caster(), data.target(), damage, damageType, spellDamageType);
		event.callEvent();
		damage = event.getFinalDamage();

		if (ignoreArmor) {
			double maxHealth = Util.getMaxHealth(data.target());

			double health = Math.min(data.target().getHealth(), maxHealth);
			health = Math.max(Math.min(health - damage, maxHealth), 0);

			if (health == 0 && data.caster() instanceof Player player) data.target().setKiller(player);
			data.target().setHealth(health);
			data.target().setLastDamage(damage);
			Util.playHurtEffect(data.target(), data.caster());
		}
		else {
			if (tryAvoidingAntiCheatPlugins.get(data)) data.target().damage(damage);
			else data.target().damage(damage, data.caster());
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
