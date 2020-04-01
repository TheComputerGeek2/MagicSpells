package com.nisovin.magicjutsus.jutsus.targeted;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.data.DataEntity;
import com.nisovin.magicjutsus.jutsus.TargetedJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class DataJutsu extends TargetedJutsu implements TargetedEntityJutsu {

	private String variableName;
	private Function<? super LivingEntity, String> dataElement;
	
	public DataJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		variableName = getConfigString("variable-name", "");
		dataElement = DataEntity.getDataFunction(getConfigString("data-element", "uuid"));
	}
	
	@Override
	public void initialize() {
		if (variableName.isEmpty() || MagicJutsus.getVariableManager().getVariable(variableName) == null) {
			MagicJutsus.error("DataJutsu '" + internalName + "' has an invalid variable-name defined!");
			return;
		}

		if (dataElement == null) MagicJutsus.error("DataJutsu '" + internalName + "' has an invalid option defined for data-element!");

	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
			if (targetInfo == null) return noTarget(player);
			LivingEntity target = targetInfo.getTarget();
			if (target == null) return noTarget(player);

			playJutsuEffects(player, target);
			String value = dataElement.apply(target);
			MagicJutsus.getVariableManager().set(variableName, player, value);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(caster instanceof Player)) return false;
		playJutsuEffects(caster, target);
		String value = dataElement.apply(target);
		MagicJutsus.getVariableManager().set(variableName, (Player) caster, value);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
