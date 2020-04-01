package com.nisovin.magicjutsus.variables.meta;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.util.compat.CompatBasics;
import com.nisovin.magicjutsus.variables.MetaVariable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class AttributeBaseValueVariable extends MetaVariable {
	
	private boolean safeHere = false;
	private boolean safetyChecked = false;
	
	private Attribute targetAttribute = null;
	
	private String attributeName = null;
	
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
	
	@Override
	public boolean modify(String player, double amount) {
		if (!safetyChecked) {
			safeHere = calculateIsSafeHere();
			safetyChecked = true;
		}
		
		// Not usable here
		if (!safeHere) return false;
		
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p == null) return false;
		
		AttributeInstance attributeInstance = p.getAttribute(targetAttribute);
		attributeInstance.setBaseValue(attributeInstance.getBaseValue() + amount);
		return true;
	}

	private boolean calculateIsSafeHere() {
		boolean safe = CompatBasics.runsWithoutError(() -> targetAttribute = Attribute.valueOf(attributeName));
		return safe && targetAttribute != null;
	}
	
}
