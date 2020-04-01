package com.nisovin.magicjutsus.jutsus.targeted.ext;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;

import me.clip.placeholderapi.PlaceholderAPI;

// NOTE: PLACEHOLDERAPI IS REQUIRED FOR THIS
public class PlaceholderAPIDataJutsu extends TargetedJutsu {
	
	private String variableName;
	private String placeholderAPITemplate;
	
	public PlaceholderAPIDataJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		variableName = getConfigString("variable-name", null);
		placeholderAPITemplate = getConfigString("placeholderapi-template", "An admin forgot to set placeholderapi-template");
	}
	
	@Override
	public void initialize() {
		if (variableName == null) {
			MagicJutsus.error("PlaceholderAPIDataJutsu '" + internalName + "' has an invalid variable-name defined!");
			MagicJutsus.error("In most cases, this should be set to the name of a string variable, but non string variables may work depending on values.");
			return;
		}
		
		if (placeholderAPITemplate == null) {
			MagicJutsus.error("PlaceholderAPIDataJutsu '" + internalName + "' has an invalid placeholderapi-template defined!");
			MagicJutsus.error("This was probably because you put something similar to \"placeholderapi-template\" and did not specify a value.");
		}
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity caster, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && caster instanceof Player) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power);
			if (targetInfo == null) return noTarget(caster);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(caster);
			
			String value = PlaceholderAPI.setPlaceholders(target, placeholderAPITemplate);
			MagicJutsus.getVariableManager().set(variableName, (Player) caster, value);
			playJutsuEffects(caster, target);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
}
