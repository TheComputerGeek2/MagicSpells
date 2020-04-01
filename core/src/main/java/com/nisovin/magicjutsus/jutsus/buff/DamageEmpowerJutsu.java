package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.events.JutsuApplyDamageEvent;

public class DamageEmpowerJutsu extends BuffJutsu {

	private Set<UUID> empowered;

	private JutsuFilter filter;

	private float damageMultiplier;

	public DamageEmpowerJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		damageMultiplier = getConfigFloat("damage-multiplier", 1.5F);

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> tagList = getConfigStringList("jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-jutsu-tags", null);

		filter = new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);

		empowered = new HashSet<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		empowered.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return empowered.contains(entity.getUniqueId());
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		empowered.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		empowered.clear();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJutsuApplyDamage(JutsuApplyDamageEvent event) {
		LivingEntity caster = event.getCaster();
		if (!isActive(caster)) return;
		if (!filter.check(event.getJutsu())) return;

		addUseAndChargeCost(caster);
		event.applyDamageModifier(damageMultiplier);
	}

}
