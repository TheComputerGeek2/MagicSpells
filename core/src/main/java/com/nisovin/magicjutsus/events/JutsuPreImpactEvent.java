package com.nisovin.magicjutsus.events;

import java.util.Arrays;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;

public class JutsuPreImpactEvent extends JutsuEvent implements Cancellable {
	
	private static final HandlerList handlers = new HandlerList();

	private LivingEntity target;
	private float power;
	private Jutsu deliveryJutsu;
	private boolean redirect;
	private boolean cancelled;

	public JutsuPreImpactEvent(Jutsu jutsuPayload, Jutsu deliveryJutsu, LivingEntity caster, LivingEntity target, float power) {
		super(jutsuPayload, caster);
		this.target = target;
		this.power = power;
		this.deliveryJutsu = deliveryJutsu;
		redirect = false;
		cancelled = false;
		if (DebugHandler.isJutsuPreImpactEventCheckEnabled()) MagicJutsus.plugin.getLogger().info(toString());
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public LivingEntity getTarget() {
		return target;
	}
	
	public boolean getRedirected() {
		return redirect;
	}
	
	public void setRedirected(boolean redirect) {
		this.redirect = redirect;
	}
	
	public float getPower() {
		return power;
	}
	
	public void setPower(float power) {
		this.power = power;
	}
	
	public Jutsu getDeliveryJutsu() {
		return deliveryJutsu;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	@Override
	public String toString() {
		String casterLabel = "Caster: " + (caster == null ? "null" : caster.toString());
		String targetLabel = "Target: " + (target == null ? "null" : target.toString());
		String jutsuLabel = "JutsuPayload: " + (jutsu == null ? "null" : jutsu.toString());
		String payloadJutsuLabel = "Delivery Jutsu: " + (deliveryJutsu == null ? "null" : deliveryJutsu.toString());
		return Arrays.deepToString(new String[]{ casterLabel, targetLabel, jutsuLabel, payloadJutsuLabel });
	}
	
}
