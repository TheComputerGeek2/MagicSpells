package com.nisovin.magicspells.util.trackers;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.util.ValidTargetList;
import com.nisovin.magicspells.util.ConfigReaderUtil;

import org.bukkit.configuration.ConfigurationSection;

public record Interaction(
		@NotNull SpellFilter interactsWith,
		@Nullable Subspell collisionSpell,
		boolean stopCausing,
		boolean stopWith,
		@Nullable ValidTargetList canInteractList
) {

	public Interaction(@NotNull SpellFilter interactWith, @Nullable Subspell collisionSpell) {
		this(interactWith, collisionSpell, true, true, null);
	}

	public static List<Interaction> read(Spell causing, List<?> objectList) {
		String logPrefix = causing.getClass().getSimpleName() + " '" + causing.getInternalName() + "' ";
		List<Interaction> interactions = new ArrayList<>();

		for (Object object : objectList) {
			if (object instanceof String string) {
				String[] splits = string.split(" ");

				SpellFilter interactsWith = SpellFilter.fromLegacyString(splits[0]);
				if (splits.length < 2) {
					interactions.add(new Interaction(interactsWith, null));
					continue;
				}

				String collisionName = splits[1];
				Subspell collisionSpell = new Subspell(collisionName);
				if (!collisionSpell.process()) {
					MagicSpells.error(logPrefix + "has an invalid interaction because the collision spell '" + collisionName + "' is not a valid spell!");
					continue;
				}

				interactions.add(new Interaction(interactsWith, collisionSpell));
				continue;
			}

			if (!(object instanceof Map<?, ?> map)) continue;
			ConfigurationSection config = ConfigReaderUtil.mapToSection(map);

			SpellFilter interactsWith;
			if (config.isConfigurationSection("with"))
				interactsWith = SpellFilter.fromConfig(config, "with");
			else interactsWith = SpellFilter.fromLegacyString(config.getString("with", ""));

			String collisionName = config.getString("collision-spell", "");
			Subspell collisionSpell = collisionName.isEmpty() ? null : new Subspell(collisionName);
			if (collisionSpell != null && !collisionSpell.process()) {
				MagicSpells.error(logPrefix + "has an invalid interaction because their collision spell '" + collisionName + "' is not a valid spell!");
				continue;
			}

			boolean stopCausing = config.getBoolean("stop-causing", true);
			boolean stopWith = config.getBoolean("stop-with", true);

			ValidTargetList canInteractList;
			if (config.isString("can-interact")) canInteractList = new ValidTargetList(causing, config.getString("can-interact"));
			else if (config.isList("can-interact")) canInteractList = new ValidTargetList(causing, config.getStringList("can-interact"));
			else canInteractList = null;

			interactions.add(new Interaction(interactsWith, collisionSpell, stopCausing, stopWith, canInteractList));
		}

		return interactions;
	}

}
