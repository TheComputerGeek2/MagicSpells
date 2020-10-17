package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;

public class VariableCastSpell extends InstantSpell {
	
	private String variableName;
	private String strDoesntContainSpell;
	
	public VariableCastSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		variableName = getConfigString("variable-name", null);
		strDoesntContainSpell = getConfigString("str-doesnt-contain-spell", "You do not have a valid spell in memory");
	}
	
	@Override
	public void initializeVariables() {
		super.initializeVariables();
		
		if (MagicSpells.getVariableManager().getVariable(variableName) == null) {
			MagicSpells.error("VariableCastSpell '" + internalName + "' has an invalid variable-name defined!");
		}
	}
	
	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (variableName == null) return PostCastAction.HANDLE_NORMALLY;
			String strValue = MagicSpells.getVariableManager().getVariable(variableName).getStringValue(player);
			Spell toCast = MagicSpells.getSpellByInternalName(strValue);
			if (toCast == null) {
				sendMessage(player, strDoesntContainSpell, args);
				return PostCastAction.NO_MESSAGES;
			}
			toCast.cast(player, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
}
