package com.nisovin.magicjutsus.events;

import org.bukkit.entity.Explosive;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class MagicJutsusExplosionPrimeEvent extends ExplosionPrimeEvent implements IMagicJutsusCompatEvent {

	public MagicJutsusExplosionPrimeEvent(Explosive explosive) {
		super(explosive);
		// TODO Auto-generated constructor stub
	}

}
