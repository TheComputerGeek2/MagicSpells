package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.events.JutsuCastEvent;

public class ClarityJutsu extends BuffJutsu {

	private Map<UUID, Float> buffed;

	private float multiplier;
	private JutsuFilter filter;

	public ClarityJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		multiplier = getConfigFloat("multiplier", 0.5F);

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> tagList = getConfigStringList("jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-jutsu-tags", null);
		filter = new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);

		buffed = new HashMap<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		buffed.put(entity.getUniqueId(), power);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return buffed.containsKey(entity.getUniqueId());
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		buffed.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		buffed.clear();
	}

	@EventHandler(ignoreCancelled = true)
	public void onJutsuCast(JutsuCastEvent event) {
		LivingEntity caster = event.getCaster();
		if (!isActive(caster)) return;
		if (!filter.check(event.getJutsu())) return;

		float mod = multiplier;
		float power = buffed.get(caster.getUniqueId());

		if (multiplier < 1) mod *= 1 / power;
		else if (multiplier > 1) mod *= power;

		JutsuReagents reagents = event.getReagents();
		if (reagents != null) event.setReagents(reagents.multiply(mod));
		
		addUseAndChargeCost(caster);
	}

}
