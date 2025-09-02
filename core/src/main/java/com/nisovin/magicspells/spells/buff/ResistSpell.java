package com.nisovin.magicspells.spells.buff;

import java.util.*;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import org.bukkit.entity.Entity;
import org.bukkit.damage.DamageType;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import io.papermc.paper.registry.RegistryKey;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.DeprecationNotice;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

public class ResistSpell extends BuffSpell {

	private static final DeprecationNotice DEPRECATION_NOTICE = new DeprecationNotice(
		"The 'normal-damage-types' option does not function properly.",
		"Use the 'damage-types' option.",
		"https://github.com/TheComputerGeek2/MagicSpells/wiki/Deprecations#buffresistspell-normal-damage-types"
	);

	private final Map<UUID, ResistData> entities;

	private Predicate<DamageType> damageTypes;
	private final Set<String> spellDamageTypes;
	private final Set<DamageCause> normalDamageTypes;

	private final ConfigData<Double> flatModifier;

	private final ConfigData<Float> multiplier;

	private final ConfigData<Boolean> constantMultiplier;
	private final ConfigData<Boolean> constantFlatModifier;
	private final ConfigData<Boolean> powerAffectsMultiplier;
	private final ConfigData<Boolean> powerAffectsFlatModifier;

	private boolean damageWildcard;

	public ResistSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		flatModifier = getConfigDataDouble("flat-modifier", 0D);
		multiplier = getConfigDataFloat("multiplier", 0.5F);

		constantMultiplier = getConfigDataBoolean("constant-multiplier", true);
		constantFlatModifier = getConfigDataBoolean("constant-flat-modifier", true);
		powerAffectsMultiplier = getConfigDataBoolean("power-affects-multiplier", true);
		powerAffectsFlatModifier = getConfigDataBoolean("power-affects-flat-modifier", true);

		if (config.isBoolean(internalKey + "damage-types"))
			damageWildcard = getConfigBoolean("damage-types", false);
		else damageTypes = getConfigRegistryEntryPredicate("damage-types", RegistryKey.DAMAGE_TYPE);

		List<String> causes = getConfigStringList("normal-damage-types", null);
		if (causes != null) {
			normalDamageTypes = new HashSet<>();

			for (String cause : causes) {
				if (cause.equalsIgnoreCase("*")) {
					damageWildcard = true;
					break;
				}
				try {
					DamageCause damageCause = DamageCause.valueOf(cause.replace(" ", "_").replace("-", "_").toUpperCase());
					normalDamageTypes.add(damageCause);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ResistSpell '" + internalName + "' has an invalid damage cause defined '" + cause + "'!");
				}
			}

			MagicSpells.getDeprecationManager().addDeprecation(this, DEPRECATION_NOTICE);
		} else normalDamageTypes = null;

		spellDamageTypes = new HashSet<>();
		causes = getConfigStringList("spell-damage-types", null);
		if (causes != null) spellDamageTypes.addAll(causes);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(SpellData data) {
		boolean constantMultiplier = this.constantMultiplier.get(data);

		float multiplier = 0;
		if (constantMultiplier) {
			multiplier = this.multiplier.get(data);
			if (powerAffectsMultiplier.get(data)) {
				if (Math.abs(multiplier) < 1) multiplier /= data.power();
				else if (Math.abs(multiplier) > 1) multiplier *= data.power();
			}
		}
		boolean constantFlatDamage = this.constantFlatModifier.get(data);

		double flatModifier = 0;
		if (constantFlatDamage) {
			flatModifier = this.flatModifier.get(data);
			if (powerAffectsFlatModifier.get(data)) {
				if (Math.abs(flatModifier) < 1) flatModifier /= data.power();
				else if (Math.abs(flatModifier) > 1) flatModifier *= data.power();
			}
		}

		entities.put(data.target().getUniqueId(), new ResistData(data, multiplier, constantMultiplier, flatModifier, constantFlatDamage));
		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected @NotNull Collection<UUID> getActiveEntities() {
		return entities.keySet();
	}

	@EventHandler
	public void onSpellDamage(SpellApplyDamageEvent event) {
		if (spellDamageTypes.isEmpty()) return;
		if (!isActive(event.getTarget())) return;

		String spellDamageType = event.getSpellDamageType();
		if (spellDamageType == null) return;
		if (!spellDamageTypes.contains("*") && !spellDamageTypes.contains(spellDamageType)) return;

		LivingEntity caster = event.getTarget();
		ResistData data = entities.get(caster.getUniqueId());
		ResistCalculation calculation = calculateResist(data, caster, event.getCaster(), data.multiplier, data.flatModifier);

		event.applyDamageModifier(calculation.multiplier);
		event.applyFlatDamageModifier(calculation.flatModifier);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (!damageWildcard) {
			if (damageTypes == null && normalDamageTypes == null) return;

			if (damageTypes != null) {
				if (!damageTypes.test(event.getDamageSource().getDamageType())) return;
			} else if (!normalDamageTypes.contains(event.getCause())) return;
		}

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity caster)) return;
		if (!isActive(caster)) return;

		LivingEntity target = null;
		if (event instanceof EntityDamageByEntityEvent entityEvent &&
				entityEvent.getDamager() instanceof LivingEntity damager) target = damager;

		ResistData data = entities.get(caster.getUniqueId());
		ResistCalculation calculation = calculateResist(data, caster, target, data.multiplier, data.flatModifier);

		double finalDamage = (event.getDamage() * calculation.multiplier) - calculation.flatModifier;
		if (finalDamage < 0D) finalDamage = 0D;
		event.setDamage(finalDamage);
	}

	@EventHandler(ignoreCancelled = true)
	public void onLegacyDamage(MagicSpellsEntityDamageByEntityEvent event) {
		onEntityDamage(event);
	}

	private ResistCalculation calculateResist(ResistData data, LivingEntity caster, LivingEntity target, float multiplier, double flatModifier) {
		if (!data.constantMultiplier) {
			SpellData subData = data.spellData.target(target);

			multiplier = this.multiplier.get(subData);
			if (powerAffectsMultiplier.get(subData)) {
				if (Math.abs(multiplier) < 1) multiplier /= subData.power();
				else if (Math.abs(multiplier) > 1) multiplier *= subData.power();
			}
		}

		if (!data.constantFlatModifier) {
			SpellData subData = data.spellData.target(target);

			flatModifier = this.flatModifier.get(subData);
			if (powerAffectsFlatModifier.get(subData)) {
				if (Math.abs(flatModifier) < 1) flatModifier /= subData.power();
				else if (Math.abs(flatModifier) > 1) flatModifier *= subData.power();
			}
		}

		addUseAndChargeCost(caster);

		return new ResistCalculation(multiplier, flatModifier);
	}

	public Map<UUID, ResistData> getEntities() {
		return entities;
	}

	public Set<String> getSpellDamageTypes() {
		return spellDamageTypes;
	}

	public Set<DamageCause> getNormalDamageTypes() {
		return normalDamageTypes;
	}

	public record ResistCalculation(float multiplier, double flatModifier) {}

	public record ResistData(SpellData spellData, float multiplier, boolean constantMultiplier, double flatModifier, boolean constantFlatModifier) {}

}
