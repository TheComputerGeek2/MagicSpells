package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.events.SpellTargetEvent;

public class ImpactRecordSpell extends BuffSpell {
	
	private Set<UUID> recorders;

	private String variableName;
	private SpellFilter recordFilter;

	private boolean recordCancelled;
	
	public ImpactRecordSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		variableName = getConfigString("variable-name", null);
		recordFilter = SpellFilter.fromConfig(config, "spells." + internalName + ".filter");
		recordCancelled = getConfigBoolean("record-cancelled", false);

		recorders = new HashSet<>();
	}
	
	@Override
	public void initializeVariables() {
		super.initializeVariables();
		
		if (variableName == null || MagicSpells.getVariableManager().getVariable(variableName) == null) {
			MagicSpells.error("ImpactRecordSpell '" + internalName + "' has an invalid variable-name defined!");
			variableName = null;
		}
	}

	@Override
	public boolean castBuff(LivingEntity entity, float power, String[] args) {
		recorders.add(entity.getUniqueId());
		return true;
	}

	@Override
	public boolean isActive(LivingEntity entity) {
		return recorders.contains(entity.getUniqueId());
	}

	@Override
	public void turnOffBuff(LivingEntity entity) {
		recorders.remove(entity.getUniqueId());
	}

	@Override
	protected void turnOff() {
		recorders.clear();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.isCancelled() && !recordCancelled) return;
		
		LivingEntity target = event.getTarget();
		if (!(target instanceof Player)) return;

		Player playerTarget = (Player) target;
		if (!isActive(playerTarget)) return;
		
		Spell spell = event.getSpell();
		if (!recordFilter.check(spell)) return;
		
		addUseAndChargeCost(playerTarget);
		MagicSpells.getVariableManager().set(variableName, playerTarget, spell.getInternalName());
	}

}
