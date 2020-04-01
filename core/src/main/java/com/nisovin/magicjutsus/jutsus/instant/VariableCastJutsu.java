package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;

public class VariableCastJutsu extends InstantJutsu {
	
	private String variableName;
	private String strDoesntContainJutsu;
	
	public VariableCastJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		variableName = getConfigString("variable-name", null);
		strDoesntContainJutsu = getConfigString("str-doesnt-contain-jutsu", "You do not have a valid jutsu in memory");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (MagicJutsus.getVariableManager().getVariable(variableName) == null) {
			MagicJutsus.error("VariableCastJutsu '" + internalName + "' has an invalid variable-name defined!");
		}
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (variableName == null) return PostCastAction.HANDLE_NORMALLY;
			String strValue = MagicJutsus.getVariableManager().getVariable(variableName).getStringValue(player);
			Jutsu toCast = MagicJutsus.getJutsuByInternalName(strValue);
			if (toCast == null) {
				sendMessage(player, strDoesntContainJutsu, args);
				return PostCastAction.NO_MESSAGES;
			}
			toCast.cast(player, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
}
