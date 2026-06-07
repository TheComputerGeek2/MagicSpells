package com.nisovin.magicspells.variables.meta;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class AbsorptionVariable extends MetaVariable {

	private static final NamespacedKey KEY = new NamespacedKey(MagicSpells.getInstance(), "meta_absorption");

	@Override
	public double getValue(String p) {
		Player player = Bukkit.getPlayerExact(p);
		if (player == null) return 0D;
		return player.getAbsorptionAmount();
	}

	@Override
	public void set(String p, double amount) {
		Player player = Bukkit.getPlayerExact(p);
		if (player == null) return;

		AttributeInstance attribute = player.getAttribute(Attribute.MAX_ABSORPTION);
		if (attribute == null) return;
		attribute.removeModifier(KEY);
		attribute.addModifier(new AttributeModifier(KEY, amount, AttributeModifier.Operation.ADD_NUMBER));

		player.setAbsorptionAmount(amount);
	}

}
