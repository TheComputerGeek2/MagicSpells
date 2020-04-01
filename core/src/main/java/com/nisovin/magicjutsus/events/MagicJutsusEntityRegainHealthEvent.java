package com.nisovin.magicjutsus.events;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class MagicJutsusEntityRegainHealthEvent extends EntityRegainHealthEvent implements IMagicJutsusCompatEvent {

	public MagicJutsusEntityRegainHealthEvent(Entity entity, double amount, RegainReason regainReason) {
		super(entity, amount, regainReason);
	}

}
