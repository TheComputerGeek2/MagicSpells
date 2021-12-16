package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.MagicSpells;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.DamageSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class ResistSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private final Set<String> spellDamageTypes;

	private final Set<DamageCause> normalDamageTypes;

	private ConfigData<Float> multiplier;

	private boolean powerAffectsMultiplier;

	public ResistSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		multiplier = getConfigDataFloat("multiplier", 0.5F);

		powerAffectsMultiplier = getConfigBoolean("power-affects-multiplier", true);

		normalDamageTypes = new HashSet<>();
		List<String> causes = getConfigStringList("normal-damage-types", null);
		if (causes != null) {
			for (String cause : causes) {
				try {
					DamageCause damageCause = DamageCause.valueOf(cause.replace(" ", "_").replace("-", "_").toUpperCase());
					normalDamageTypes.add(damageCause);
				} catch (IllegalArgumentException e) {
					MagicSpells.error("ResistSpell '" + internalName + "' has an invalid damage cause defined '" + cause + "'!");
				}
			}
		}

		spellDamageTypes = new HashSet<>();
		causes = getConfigStringList("spell-damage-types", null);
		if (causes != null) spellDamageTypes.addAll(causes);

		entities = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		entities.put(entity.getUniqueId(), new SpellData(power, args));
		return true;
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
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler
	public void onSpellDamage(SpellApplyDamageEvent event) {
		if (spellDamageTypes.isEmpty()) return;
		if (!(event.getSpell() instanceof DamageSpell spell)) return;
		if (!isActive(event.getTarget())) return;

		String spellDamageType = spell.getSpellDamageType();
		if (spellDamageType == null) return;
		if (!spellDamageTypes.contains(spellDamageType)) return;

		LivingEntity caster = event.getTarget();
		SpellData data = entities.get(caster.getUniqueId());

		float modifier = multiplier.get(caster, event.getCaster(), data.power(), data.args());
		if (powerAffectsMultiplier) {
			if (modifier < 1) modifier /= data.power();
			else if (modifier > 1) modifier *= data.power();
		}

		addUseAndChargeCost(caster);
		event.applyDamageModifier(modifier);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (normalDamageTypes.isEmpty()) return;
		if (!normalDamageTypes.contains(event.getCause())) return;

		Entity entity = event.getEntity();
		if (!(entity instanceof LivingEntity caster)) return;
		if (!isActive(caster)) return;

		LivingEntity target = null;
		if (event instanceof EntityDamageByEntityEvent e)
			if (e.getDamager() instanceof LivingEntity damager)
				target = damager;

		SpellData data = entities.get(caster.getUniqueId());

		float modifier = multiplier.get(caster, target, data.power(), data.args());
		if (powerAffectsMultiplier) {
			if (modifier < 1) modifier /= data.power();
			else if (modifier > 1) modifier *= data.power();
		}

		addUseAndChargeCost(caster);
		event.setDamage(event.getDamage() * modifier);
	}

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

	public Set<String> getSpellDamageTypes() {
		return spellDamageTypes;
	}

	public Set<DamageCause> getNormalDamageTypes() {
		return normalDamageTypes;
	}

}
