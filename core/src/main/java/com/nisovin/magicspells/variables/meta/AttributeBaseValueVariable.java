package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class AttributeBaseValueVariable extends MetaVariable {

	private final String attributeName;

	private boolean safeHere = false;
	private boolean safetyChecked = false;
	
	private Attribute targetAttribute = null;

	public AttributeBaseValueVariable(String attributeName) {
		super();

		this.attributeName = attributeName;
	}
	
	@Override
	public double getValue(String player) {
		if (!safetyChecked) {
			safeHere = calculateIsSafeHere();
			safetyChecked = true;
		}
		// Not usable here
		if (!safeHere) return 0D;
		
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p == null) return 0D;
		
		return p.getAttribute(targetAttribute).getBaseValue();
	}
	
	@Override
	public void set(String player, double amount) {
		if (!safetyChecked) {
			safeHere = calculateIsSafeHere();
			safetyChecked = true;
		}
		
		// Not usable here
		if (!safeHere) return;
		
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p == null) return;
		
		p.getAttribute(targetAttribute).setBaseValue(amount);
	}

	private boolean calculateIsSafeHere() {
		boolean safe = CompatBasics.runsWithoutError(() -> targetAttribute = Attribute.valueOf(attributeName));
		return safe && targetAttribute != null;
	}
	
}
