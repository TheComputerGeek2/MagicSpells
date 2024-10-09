package com.nisovin.magicspells.spells.buff;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.function.Supplier;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class DamageEmpowerSpell extends BuffSpell {

	private final Map<UUID, DamageSuppliers> entities;

	private SpellFilter filter;

	private final ConfigData<Double> flatModifier;

	private final ConfigData<Float> damageMultiplier;

	private final ConfigData<Boolean> constantFlatModifier;
	private final ConfigData<Boolean> constantDamageMultiplier;


	public DamageEmpowerSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		flatModifier = getConfigDataDouble("flat-modifier", 0D);
		damageMultiplier = getConfigDataFloat("damage-multiplier", 1.5F);
		constantFlatModifier = getConfigDataBoolean("constant-flat-modifier", true);
		constantDamageMultiplier = getConfigDataBoolean("constant-damage-multiplier", true);

		entities = new HashMap<>();
	}

	@Override
	public void initialize() {
		super.initialize();

		filter = getConfigSpellFilter();
	}

	@Override
	public boolean castBuff(SpellData data) {
		Supplier<Float> multiplierSupplier;
		if (constantDamageMultiplier.get(data)) {
			float damageMultiplier = this.damageMultiplier.get(data);
			multiplierSupplier = () -> damageMultiplier;
		} else multiplierSupplier = () -> damageMultiplier.get(data);

		Supplier<Double> flatModifierSupplier;
		if (constantFlatModifier.get(data)) {
			double flatModifier = this.flatModifier.get(data);
			flatModifierSupplier = () -> flatModifier;
		} else flatModifierSupplier = () -> flatModifier.get(data);

		DamageSuppliers record = new DamageSuppliers(multiplierSupplier, flatModifierSupplier);

		entities.put(data.target().getUniqueId(), record);

		return true;
	}

	@Override
	public boolean recastBuff(SpellData data) {
		stopEffects(data.target());
		return castBuff(data);
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return entities.containsKey(entity.getUniqueId());
	}

	@Override
	protected void turnOffBuff(LivingEntity entity) {
		entities.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		entities.clear();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpellApplyDamage(SpellApplyDamageEvent event) {
		LivingEntity caster = event.getCaster();
		if (!isActive(caster)) return;
		if (!filter.check(event.getSpell())) return;

		DamageSuppliers suppliers = entities.get(caster.getUniqueId());

		double flatModifier = suppliers.flatModifier().get();
		float damageMultiplier = suppliers.damageMultiplier().get();
		event.applyFlatDamageModifier(flatModifier);
		event.applyDamageModifier(damageMultiplier);

		addUseAndChargeCost(caster);
	}

	public Map<UUID, DamageSuppliers> getEntities() {
		return entities;
	}

	public SpellFilter getFilter() {
		return filter;
	}

	public void setFilter(SpellFilter filter) {
		this.filter = filter;
	}

	public record DamageSuppliers(Supplier<Float> damageMultiplier, Supplier<Double> flatModifier) {}

}
