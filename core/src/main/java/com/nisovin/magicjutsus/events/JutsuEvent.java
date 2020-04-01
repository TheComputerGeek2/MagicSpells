package com.nisovin.magicjutsus.events;

import org.bukkit.event.Event;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;

public abstract class JutsuEvent extends Event implements IMagicJutsusCompatEvent {

	protected Jutsu jutsu;
	protected LivingEntity caster;
	
	public JutsuEvent(Jutsu jutsu, LivingEntity caster) {
		this.jutsu = jutsu;
		this.caster = caster;
	}
	
	/**
	 * Gets the jutsu involved in the event.
	 * @return the jutsu
	 */
	public Jutsu getJutsu() {
		return jutsu;
	}
	
	/**
	 * Gets the player casting the jutsu.
	 * @return the casting player
	 */
	public LivingEntity getCaster() {
		return caster;
	}
	
}
