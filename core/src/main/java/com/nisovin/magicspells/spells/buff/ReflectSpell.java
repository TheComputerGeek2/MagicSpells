package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellPreImpactEvent;

// NO API CHANGES - NEEDS TOTAL REWORK
public class ReflectSpell extends BuffSpell {

	private Map<UUID, SpellData> reflectors;
	private Set<String> shieldBreakerNames;
	private Set<String> delayedReflectionSpells;

	private ConfigData<Float> reflectedSpellPowerMultiplier;

	private boolean spellPowerAffectsReflectedPower;
	private boolean delayedReflectionSpellsUsePayloadShieldBreaker;

	public ReflectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		reflectors = new HashMap<>();
		shieldBreakerNames = new HashSet<>();
		delayedReflectionSpells = new HashSet<>();

		shieldBreakerNames.addAll(getConfigStringList("shield-breakers", new ArrayList<>()));
		delayedReflectionSpells.addAll(getConfigStringList("delayed-reflection-spells", new ArrayList<>()));

		reflectedSpellPowerMultiplier = getConfigDataFloat("reflected-spell-power-multiplier", 1F);

		spellPowerAffectsReflectedPower = getConfigBoolean("spell-power-affects-reflected-power", false);
		delayedReflectionSpellsUsePayloadShieldBreaker = getConfigBoolean("delayed-reflection-spells-use-payload-shield-breaker", true);
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		reflectors.put(entity.getUniqueId(), new SpellData(power, args));
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return reflectors.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		reflectors.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		reflectors.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent event) {
		LivingEntity target = event.getTarget();
		if (target == null) return;
		if (!target.isValid()) return;
		if (!isActive(target)) return;

		if (shieldBreakerNames != null && shieldBreakerNames.contains(event.getSpell().getInternalName())) {
			turnOff(target);
			return;
		}
		if (delayedReflectionSpells != null && delayedReflectionSpells.contains(event.getSpell().getInternalName())) {
			// Let the delayed reflection spells target the reflector so the animations run
			// It will get reflected later
			return;
		}

		if (!chargeUseCost(target)) return;

		addUse(target);
		event.setTarget(event.getCaster());

		SpellData data = reflectors.get(target.getUniqueId());
		event.setPower(event.getPower() * reflectedSpellPowerMultiplier.get(target, event.getCaster(), data.power(), data.args()) * (spellPowerAffectsReflectedPower ? data.power() : 1));
	}

	@EventHandler
	public void onSpellPreImpact(SpellPreImpactEvent event) {
		LivingEntity target = event.getTarget();

		if (target == null) {
			MagicSpells.plugin.getLogger().warning("Spell preimpact event had a null target, the spell cannot be reflected.");
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("Spell preimpact event had a null target");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}

		if (event.getCaster() == null) {
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("SpellPreImpactEvent had a null caster!");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}

		if (!isActive(target)) return;
		if (delayedReflectionSpellsUsePayloadShieldBreaker && (event.getSpell() != null && shieldBreakerNames.contains(event.getSpell().getInternalName()))) {
			turnOff(target);
			return;
		}

		addUse(target);
		event.setRedirected(true);

		SpellData data = reflectors.get(target.getUniqueId());
		event.setPower(event.getPower() * reflectedSpellPowerMultiplier.get(target, event.getCaster(), data.power(), data.args()) * (spellPowerAffectsReflectedPower ? data.power() : 1));
	}

}
