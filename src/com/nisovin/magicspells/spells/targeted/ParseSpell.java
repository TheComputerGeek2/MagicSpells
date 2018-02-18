package com.nisovin.magicspells.spells.targeted;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class ParseSpell extends TargetedSpell {

	private String variableToParse;
	private String expectedValue;
	private String parseToVariable;
	private String parseTo;

	public ParseSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		this.variableToParse = getConfigString("variable-to-parse", null);
		this.expectedValue = getConfigString("expected-value", null);
		this.parseToVariable = getConfigString("parse-to-variable", null);
		this.parseTo = getConfigString("parse-to", null);
	}

	@Override
	public void initialize() {
		// You can do it, I believe in you.
		if (variableToParse == null) {
			MagicSpells.error("You must define a variable to parse for ParseSpell");
			return;
		}

		if (expectedValue == null) {
			MagicSpells.error("You must define an expected variable for ParseSpell");
			return;
		}

		if (parseToVariable == null) {
			MagicSpells.error("You must define a variable to parse to for ParseSpell");
			return;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(player, power);
			if (targetInfo == null) return noTarget(player);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(player);

			// Change the actual variable to the requested value.
			String receivedValue = MagicSpells.getVariableManager().getStringValue(variableToParse, target);

		    // Do the values match?
			if (receivedValue.equals(expectedValue)) {
				MagicSpells.getVariableManager().set(parseToVariable, target, parseTo);

				playSpellEffects(player, target);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
}
