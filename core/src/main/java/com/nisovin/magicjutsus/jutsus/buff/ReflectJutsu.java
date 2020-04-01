package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.events.JutsuPreImpactEvent;

public class ReflectJutsu extends BuffJutsu {

	private Map<UUID, Float> reflectors;
	private Set<String> shieldBreakerNames;
	private Set<String> delayedReflectionJutsus;

	private float reflectedJutsuPowerMultiplier;

	private boolean jutsuPowerAffectsReflectedPower;
	private boolean delayedReflectionJutsusUsePayloadShieldBreaker;

	public ReflectJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		reflectors = new HashMap<>();
		shieldBreakerNames = new HashSet<>();
		delayedReflectionJutsus = new HashSet<>();

		shieldBreakerNames.addAll(getConfigStringList("shield-breakers", new ArrayList<>()));
		delayedReflectionJutsus.addAll(getConfigStringList("delayed-reflection-jutsus", new ArrayList<>()));

		reflectedJutsuPowerMultiplier = (float) getConfigDouble("reflected-jutsu-power-multiplier", 1F);

		jutsuPowerAffectsReflectedPower = getConfigBoolean("jutsu-power-affects-reflected-power", false);
		delayedReflectionJutsusUsePayloadShieldBreaker = getConfigBoolean("delayed-reflection-jutsus-use-payload-shield-breaker", true);
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		reflectors.put(entity.getUniqueId(), power);
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
	public void onJutsuTarget(JutsuTargetEvent event) {
		LivingEntity target = event.getTarget();
		if (target == null) return;
		if (!target.isValid()) return;
		if (!isActive(target)) return;

		float power = reflectors.get(target.getUniqueId());
		if (shieldBreakerNames != null && shieldBreakerNames.contains(event.getJutsu().getInternalName())) {
			turnOff(target);
			return;
		}
		if (delayedReflectionJutsus != null && delayedReflectionJutsus.contains(event.getJutsu().getInternalName())) {
			// Let the delayed reflection jutsus target the reflector so the animations run
			// It will get reflected later
			return;
		}

		if (!chargeUseCost(target)) return;

		addUse(target);
		event.setTarget(event.getCaster());
		event.setPower(event.getPower() * reflectedJutsuPowerMultiplier * (jutsuPowerAffectsReflectedPower ? power : 1));
	}

	@EventHandler
	public void onJutsuPreImpact(JutsuPreImpactEvent event) {
		LivingEntity target = event.getTarget();

		if (event == null) {
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("JutsuPreImpactEvent was null!");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}
		if (target == null) {
			MagicJutsus.plugin.getLogger().warning("Jutsu preimpact event had a null target, the jutsu cannot be reflected.");
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("Jutsu preimpact event had a null target");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}
		if (event.getCaster() == null) {
			if (DebugHandler.isNullCheckEnabled()) {
				NullPointerException e = new NullPointerException("JutsuPreImpactEvent had a null caster!");
				e.fillInStackTrace();
				DebugHandler.nullCheck(e);
			}
			return;
		}

		if (!isActive(target)) return;
		if (delayedReflectionJutsusUsePayloadShieldBreaker && (event.getJutsu() != null && shieldBreakerNames.contains(event.getJutsu().getInternalName()))) {
			turnOff(target);
			return;
		}

		addUse(target);
		event.setRedirected(true);
		float powerMultiplier = 1.0F;
		powerMultiplier *= reflectedJutsuPowerMultiplier * (jutsuPowerAffectsReflectedPower ? (reflectors.get(target.getUniqueId()) == null ? 1.0: reflectors.get(target.getUniqueId())) : 1.0);
		event.setPower(event.getPower() * powerMultiplier);

	}

}
