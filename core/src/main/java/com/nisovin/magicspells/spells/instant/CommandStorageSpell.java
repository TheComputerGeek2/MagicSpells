package com.nisovin.magicspells.spells.instant;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.managers.VariableManager;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.variables.variabletypes.PlayerStringVariable;

public class CommandStorageSpell extends InstantSpell {

	private final ConfigData<String> tag;
	private final ConfigData<String> variable;
	private final ConfigData<NamespacedKey> containerId;

	public CommandStorageSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		tag = getConfigDataString("tag", null);
		variable = getConfigDataString("variable", null);

		containerId = getConfigDataNamespacedKey("container-id", null);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		VariableManager manager = MagicSpells.getVariableManager();

		String tag = this.tag.get(data);
		NamespacedKey containerId = this.containerId.get(data);
		Variable variable = manager.getVariable(this.variable.get(data));

		if (tag == null || containerId == null || variable == null)
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable) {
			String value = MagicSpells.getVolatileCodeHandler().getCommandStorageString(containerId, tag);
			if (value != null) manager.set(variable, caster, value);
		} else {
			Double value = MagicSpells.getVolatileCodeHandler().getCommandStorageDouble(containerId, tag);
			if (value != null) manager.set(variable, caster, value);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
