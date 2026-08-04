package com.nisovin.magicspells.events;

import java.util.Arrays;

import org.bukkit.event.Cancellable;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.handlers.DebugHandler;

public class SpellPreImpactEvent extends SpellEvent implements Cancellable {

	private final Spell deliverySpell;

	private SpellData spellData;

	private boolean redirect;
	private boolean cancelled;

	@Deprecated
	public SpellPreImpactEvent(Spell spellPayload, Spell deliverySpell, LivingEntity caster, LivingEntity target, float power) {
		this(spellPayload, deliverySpell, new SpellData(caster, target, power));
	}

	public SpellPreImpactEvent(Spell spellPayload, Spell deliverySpell, SpellData spellData) {
		super(spellPayload, spellData.caster());
		this.deliverySpell = deliverySpell;
		this.spellData = spellData;
		if (DebugHandler.isSpellPreImpactEventCheckEnabled()) MagicSpells.plugin.getLogger().info(toString());
	}

	@Override
	public LivingEntity getCaster() {
		return spellData.caster();
	}

	public LivingEntity getTarget() {
		return spellData.target();
	}
	
	public boolean getRedirected() {
		return redirect;
	}

	public void setRedirected(boolean redirect) {
		this.redirect = redirect;
	}

	public float getPower() {
		return spellData.power();
	}

	public void setPower(float power) {
		spellData = spellData.power(power);
	}

	public SpellData getSpellData() {
		return spellData;
	}

	public Spell getDeliverySpell() {
		return deliverySpell;
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
		String casterLabel = "Caster: " + caster;
		String targetLabel = "Target: " + spellData.target();
		String spellLabel = "SpellPayload: " + (spell == null ? "null" : spell.getInternalName());
		String payloadSpellLabel = "Delivery Spell: " + (deliverySpell == null ? "null" : deliverySpell.getInternalName());
		return Arrays.deepToString(new String[]{ casterLabel, targetLabel, spellLabel, payloadSpellLabel });
	}

}
