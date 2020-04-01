package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuFilter;
import com.nisovin.magicjutsus.events.JutsuCastEvent;

public class JutsuHasteJutsu extends BuffJutsu {

	private Map<UUID, Float> jutsuTimers;

	private float castTimeModAmt;
	private float cooldownModAmt;

	private JutsuFilter filter;

	public JutsuHasteJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
	
		castTimeModAmt = getConfigInt("cast-time-mod-amt", -25) / 100F;
		cooldownModAmt = getConfigInt("cooldown-mod-amt", -25) / 100F;
	
		jutsuTimers = new HashMap<>();

		List<String> jutsus = getConfigStringList("jutsus", null);
		List<String> deniedJutsus = getConfigStringList("denied-jutsus", null);
		List<String> tagList = getConfigStringList("jutsu-tags", null);
		List<String> deniedTagList = getConfigStringList("denied-jutsu-tags", null);
	
		filter = new JutsuFilter(jutsus, deniedJutsus, tagList, deniedTagList);
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		jutsuTimers.put(entity.getUniqueId(), power);
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return jutsuTimers.containsKey(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		jutsuTimers.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		jutsuTimers.clear();
	}

	@EventHandler (priority=EventPriority.MONITOR)
	public void onJutsuSpeedCast(JutsuCastEvent event) {
		if (!filter.check(event.getJutsu())) return;
		if (!isActive(event.getCaster())) return;
		
		Float power = jutsuTimers.get(event.getCaster().getUniqueId());
		if (power == null) return;

		if (castTimeModAmt != 0) {
			int ct = event.getCastTime();
			float newCT = ct + (castTimeModAmt * power * ct);
			if (newCT < 0) newCT = 0;
			event.setCastTime(Math.round(newCT));
		}

		if (cooldownModAmt != 0) {
			float cd = event.getCooldown();
			float newCD = cd + (cooldownModAmt * power * cd);
			if (newCD < 0) newCD = 0;
			event.setCooldown(newCD);
		}

	}

}
