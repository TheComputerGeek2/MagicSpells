package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.DeprecationNotice;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("gametestclearmarkers")
public class GameTestClearMarkersEffect extends SpellEffect {

	private static final DeprecationNotice DEPRECATION_NOTICE = new DeprecationNotice(
		"The 'gametestclearmarkers' spell effect has stopped functioning.",
		"No alternative.",
		"https://github.com/TheComputerGeek2/MagicSpells/wiki/Deprecations#spell-effect-gametestclearmarkers"
	);

	private ConfigData<Boolean> broadcast;
	private ConfigData<MarkerViewer> viewer;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		MagicSpells.getDeprecationManager().addDeprecation(DEPRECATION_NOTICE);

		viewer = ConfigDataUtil.getEnum(config, "viewer", MarkerViewer.class, MarkerViewer.POSITION);
		broadcast = ConfigDataUtil.getBoolean(config, "broadcast", false);
	}

	@Override
	protected Runnable playEffectLocation(Location location, SpellData data) {
		if (broadcast.get(data)) {
//			for (Player viewer : location.getWorld().getPlayers())
//				MagicSpells.getVolatileCodeHandler().clearGameTestMarkers(viewer);

			return null;
		}

		Player viewer = switch (this.viewer.get(data)) {
			case CASTER -> data.caster() instanceof Player p ? p : null;
			case TARGET -> data.target() instanceof Player p ? p : null;
			case POSITION -> null;
		};
		if (viewer == null) return null;

//		MagicSpells.getVolatileCodeHandler().clearGameTestMarkers(viewer);

		return null;
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if (broadcast.get(data)) {
//			for (Player viewer : entity.getWorld().getPlayers())
//				MagicSpells.getVolatileCodeHandler().clearGameTestMarkers(viewer);

			return null;
		}

		Player viewer = switch (this.viewer.get(data)) {
			case CASTER -> data.caster() instanceof Player p ? p : null;
			case TARGET -> data.target() instanceof Player p ? p : null;
			case POSITION -> entity instanceof Player p ? p : null;
		};
		if (viewer == null) return null;

//		MagicSpells.getVolatileCodeHandler().clearGameTestMarkers(viewer);

		return null;
	}

	private enum MarkerViewer {

		CASTER,
		TARGET,
		POSITION

	}

}
