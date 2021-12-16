package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.events.ManaChangeEvent;
import com.nisovin.magicspells.util.config.ConfigData;

public class ManaRegenSpell extends BuffSpell {

	private final Map<UUID, SpellData> entities;

	private ConfigData<Integer> regenModAmt;

	public ManaRegenSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		regenModAmt = getConfigDataInt("regen-mod-amt", 3);

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
	public void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onManaRegenTick(ManaChangeEvent event) {
		Player pl = event.getPlayer();
		if (isExpired(pl)) {
			turnOff(pl);
			return;
		}

		if (!isActive(pl)) return;
		if (!event.getReason().equals(ManaChangeReason.REGEN)) return;

		SpellData data = entities.get(pl.getUniqueId());

		int newAmt = event.getNewAmount() + regenModAmt.get(pl, null, data.power(), data.args());
		if (newAmt > event.getMaxMana()) newAmt = event.getMaxMana();
		else if (newAmt < 0) newAmt = 0;

		addUseAndChargeCost(pl);
		event.setNewAmount(newAmt);
	}

	public Map<UUID, SpellData> getEntities() {
		return entities;
	}

}
