package com.nisovin.magicspells.spells.buff;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellPreImpactEvent;

// NO API CHANGES - NEEDS TOTAL REWORK
public class ReflectSpell extends BuffSpell {

	private final Map<UUID, ReflectData> reflectors = new HashMap<>();
	private final Set<String> shieldBreakerNames = new HashSet<>();
	private final Set<String> delayedReflectionSpells = new HashSet<>();

	private final ConfigData<Float> reflectedSpellPowerMultiplier;

	private final ConfigData<Boolean> spellPowerAffectsReflectedPower;
	private final ConfigData<Boolean> constantReflectedSpellPowerMultiplier;
	private final ConfigData<Boolean> delayedReflectionSpellsUsePayloadShieldBreaker;

	public ReflectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		shieldBreakerNames.addAll(getConfigStringList("shield-breakers", new ArrayList<>()));
		delayedReflectionSpells.addAll(getConfigStringList("delayed-reflection-spells", new ArrayList<>()));

		reflectedSpellPowerMultiplier = getConfigDataFloat("reflected-spell-power-multiplier", 1F);

		spellPowerAffectsReflectedPower = getConfigDataBoolean("spell-power-affects-reflected-power", false);
		constantReflectedSpellPowerMultiplier = getConfigDataBoolean("constant-reflected-spell-power-multiplier", true);
		delayedReflectionSpellsUsePayloadShieldBreaker = getConfigDataBoolean("delayed-reflection-spells-use-payload-shield-breaker", true);
	}

	@Override
	public boolean castBuff(SpellData data) {
		boolean constantReflectedSpellPowerMultiplier = this.constantReflectedSpellPowerMultiplier.get(data);

		float reflectedSpellPowerMultiplier = 0;
		if (constantReflectedSpellPowerMultiplier) {
			reflectedSpellPowerMultiplier = this.reflectedSpellPowerMultiplier.get(data);
			if (spellPowerAffectsReflectedPower.get(data)) reflectedSpellPowerMultiplier *= data.power();
		}

		reflectors.put(data.target().getUniqueId(), new ReflectData(
			data,
			reflectedSpellPowerMultiplier,
			constantReflectedSpellPowerMultiplier,
			delayedReflectionSpellsUsePayloadShieldBreaker.get(data)
		));

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
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
	protected @NotNull Collection<UUID> getActiveEntities() {
		return reflectors.keySet();
	}

	@EventHandler(ignoreCancelled = true)
	public void onSpellTarget(SpellTargetEvent event) {
		LivingEntity target = event.getTarget();
		if (target == null) return;
		if (!target.isValid()) return;
		if (!isActive(target)) return;

		if (shieldBreakerNames.contains(event.getSpell().getInternalName())) {
			turnOff(target);
			return;
		}

		// Let the delayed reflection spells target the reflector so the reflection is handled on impact instead.
		if (delayedReflectionSpells.contains(event.getSpell().getInternalName())) return;

		event.setSpellData(event.getSpellData().invert());
		ReflectData data = reflectors.get(target.getUniqueId());

		addUseAndChargeCost(target);

		float reflectPower = data.reflectPower;
		if (!data.constantReflectPower) {
			SpellData subData = data.spellData.target(event.getCaster());

			reflectPower = reflectedSpellPowerMultiplier.get(subData);
			if (spellPowerAffectsReflectedPower.get(data.spellData)) reflectPower *= data.spellData.power();
		}

		event.setPower(event.getPower() * reflectPower);
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

		ReflectData data = reflectors.get(target.getUniqueId());
		if (data == null) return;

		if (data.delayedShieldBreaker && (event.getSpell() != null && shieldBreakerNames.contains(event.getSpell().getInternalName()))) {
			turnOff(target);
			return;
		}

		float reflectPower = data.reflectPower;
		if (!data.constantReflectPower) {
			SpellData subData = data.spellData.target(event.getCaster());

			reflectPower = reflectedSpellPowerMultiplier.get(subData);
			if (spellPowerAffectsReflectedPower.get(data.spellData)) reflectPower *= data.spellData.power();
		}

		addUseAndChargeCost(target);
		event.setRedirected(true);

		event.setPower(event.getPower() * reflectPower);
	}

	private record ReflectData(SpellData spellData, float reflectPower, boolean constantReflectPower, boolean delayedShieldBreaker) {
	}

}
