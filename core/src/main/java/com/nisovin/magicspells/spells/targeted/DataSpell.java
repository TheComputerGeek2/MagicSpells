package com.nisovin.magicspells.spells.targeted;

import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.data.DataLocation;
import com.nisovin.magicspells.util.data.DataLivingEntity;
import com.nisovin.magicspells.variables.variabletypes.GlobalVariable;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;

public class DataSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private final ConfigData<String> dataElement;
	private final ConfigData<String> variableName;

	public DataSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		variableName = getConfigDataString("variable-name", "");
		dataElement = getConfigDataString("data-element", "uuid");
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.cancelled()) return noTarget(info);
		if (!info.empty()) return castAtEntity(info.spellData());

		TargetInfo<Location> locationInfo = getTargetedBlockLocation(data);
		if (locationInfo.noTarget()) return noTarget(locationInfo);

		return castAtLocation(locationInfo.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		Function<? super LivingEntity, String> dataElement = DataLivingEntity.getDataFunction(this.dataElement.get(data));
		if (dataElement == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		return applyValue(data, dataElement.apply(data.target()));
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		Function<Location, String> dataElement = DataLocation.getDataFunction(this.dataElement.get(data));
		if (dataElement == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		return applyValue(data, dataElement.apply(data.location()));
	}

	private CastResult applyValue(SpellData data, String value) {
		Variable variable = MagicSpells.getVariableManager().getVariable(variableName.get(data));
		if (variable == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		Player caster = data.caster() instanceof Player player ? player : null;
		if (caster == null && !(variable instanceof GlobalVariable) && !(variable instanceof GlobalStringVariable))
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		MagicSpells.getVariableManager().set(variable, caster == null ? null : caster.getName(), value);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
