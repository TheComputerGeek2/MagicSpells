package com.nisovin.magicspells.spelleffects.effecttypes;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.Name;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.DeprecationNotice;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

@Name("gametestaddmarker")
public class GameTestAddMarkerEffect extends SpellEffect {

	private static final DeprecationNotice DEPRECATION_NOTICE = new DeprecationNotice(
		"The 'gametestaddmarker' spell effect has stopped functioning.",
		"No alternative.",
		"https://github.com/TheComputerGeek2/MagicSpells/wiki/Deprecations#spell-effect-gametestaddmarker"
	);

	private ConfigData<Color> color;
	private ConfigData<String> name;
	private ConfigData<Integer> lifetime;
	private ConfigData<Boolean> broadcast;
	private ConfigData<MarkerViewer> viewer;
	private ConfigData<Boolean> useViewerAsTarget;
	private ConfigData<Boolean> useViewerAsDefault;

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		MagicSpells.getDeprecationManager().addDeprecation(DEPRECATION_NOTICE);

		name = ConfigDataUtil.getString(config, "name", "");
		color = ConfigDataUtil.getARGBColor(config, "color", Color.BLACK);
		viewer = ConfigDataUtil.getEnum(config, "viewer", MarkerViewer.class, MarkerViewer.POSITION);
		lifetime = ConfigDataUtil.getInteger(config, "lifetime", 1000);
		broadcast = ConfigDataUtil.getBoolean(config, "broadcast", false);
		useViewerAsTarget = ConfigDataUtil.getBoolean(config, "use-viewer-as-target", false);
		useViewerAsDefault = ConfigDataUtil.getBoolean(config, "use-viewer-as-default", true);
	}

	@Override
	protected Runnable playEffectLocation(Location location, SpellData data) {
		if (broadcast.get(data)) {
			broadcast(location, data);
			return null;
		}

		Player viewer = switch (this.viewer.get(data)) {
			case CASTER -> data.caster() instanceof Player p ? p : null;
			case TARGET -> data.target() instanceof Player p ? p : null;
			case POSITION -> null;
		};
		if (viewer == null) return null;

		String name = this.name.get(data);
		Color color = this.color.get(data);
		int lifetime = this.lifetime.get(data);
//		MagicSpells.getVolatileCodeHandler().addGameTestMarker(viewer, location, color.asARGB(), name, lifetime);

		return null;
	}

	@Override
	protected Runnable playEffectEntity(Entity entity, SpellData data) {
		if (broadcast.get(data)) {
			broadcast(entity.getLocation(), data);
			return null;
		}

		Player viewer = switch (this.viewer.get(data)) {
			case CASTER -> data.caster() instanceof Player p ? p : null;
			case TARGET -> data.target() instanceof Player p ? p : null;
			case POSITION -> entity instanceof Player p ? p : null;
		};
		if (viewer == null) return null;

		String name = this.name.get(data);
		Color color = this.color.get(data);
		int lifetime = this.lifetime.get(data);
//		MagicSpells.getVolatileCodeHandler().addGameTestMarker(viewer, entity.getLocation(), color.asARGB(), name, lifetime);

		return null;
	}

	private void broadcast(Location location, SpellData data) {
		boolean useViewerAsTarget = this.useViewerAsTarget.get(data);
		boolean useViewerAsDefault = this.useViewerAsDefault.get(data);

		String name = null;
		Color color = null;
		int lifetime = 0;

		if (!useViewerAsTarget && !useViewerAsDefault) {
			name = this.name.get(data);
			color = this.color.get(data);
			lifetime = this.lifetime.get(data);
		}

		for (Player viewer : location.getWorld().getPlayers()) {
			if (useViewerAsTarget || useViewerAsDefault) {
				SpellData spellData = data;
				if (useViewerAsTarget) spellData = spellData.target(viewer);
				if (useViewerAsDefault) spellData = spellData.recipient(viewer);

				name = this.name.get(spellData);
				color = this.color.get(spellData);
				lifetime = this.lifetime.get(spellData);
			}

//			MagicSpells.getVolatileCodeHandler().addGameTestMarker(viewer, location, color.asARGB(), name, lifetime);
		}
	}

	private enum MarkerViewer {

		CASTER,
		TARGET,
		POSITION

	}

}
