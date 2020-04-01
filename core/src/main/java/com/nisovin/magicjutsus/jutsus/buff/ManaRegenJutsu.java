package com.nisovin.magicjutsus.jutsus.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.jutsus.BuffJutsu;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.mana.ManaChangeReason;
import com.nisovin.magicjutsus.events.ManaChangeEvent;

public class ManaRegenJutsu extends BuffJutsu {

	private Set<UUID> regeners;

	private int regenModAmt;

	public ManaRegenJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		regenModAmt = getConfigInt("regen-mod-amt", 3);

		regeners = new HashSet<>();
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		regeners.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return regeners.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		regeners.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		regeners.clear();
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onManaRegenTick(ManaChangeEvent event) {
		Player pl = event.getPlayer();
		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		if (!isActive(pl)) return;
		if (!event.getReason().equals(ManaChangeReason.REGEN)) return;
		
		int newAmt = event.getNewAmount() + regenModAmt;
		if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
		else if (newAmt < 0) newAmt = 0;

		addUseAndChargeCost(pl);
		event.setNewAmount(newAmt);
	}

}
