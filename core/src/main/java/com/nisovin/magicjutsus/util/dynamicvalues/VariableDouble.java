package com.nisovin.magicjutsus.util.dynamicvalues;

import com.nisovin.magicjutsus.util.VariableMod;
import org.bukkit.entity.Player;

public class VariableDouble {
	
	private VariableMod primaryValue = null;
	private VariableMod secondaryValue = null;
	
	public VariableDouble(VariableMod primaryValue, VariableMod secondaryValue) {
		if (primaryValue == null) throw new IllegalArgumentException("Primary value cannot be null");
		this.primaryValue = primaryValue;
		this.secondaryValue = secondaryValue;
	}
	
	public VariableDouble(String raw) {
		if (raw == null) throw new IllegalArgumentException("VariableDouble cannot be created from null");
		if (raw.isEmpty()) throw new IllegalArgumentException("VariableDouble cannot");
		if (raw.contains(" ")) {
			String[] splits = raw.split(" ");
			this.secondaryValue = new VariableMod(splits[1]);
			raw = splits[0];
		}
		this.primaryValue = new VariableMod(raw);
	}
	
	public boolean hasSecondaryValue() {
		return this.secondaryValue != null;
	}
	
	public double getPrimaryValue(Player caster, Player target) {
		return this.primaryValue.getValue(caster, target);
	}
	
	public double getSecondaryValue(Player caster, Player target) {
		return this.secondaryValue.getValue(caster, target);
	}
	
	public double calculateValue(Player caster, Player target) {
		double ret = this.getPrimaryValue(caster, target);
		if (this.hasSecondaryValue()) {
			ret = this.secondaryValue.getValue(caster, target, ret);
		}
		return ret;
	}
	
}
