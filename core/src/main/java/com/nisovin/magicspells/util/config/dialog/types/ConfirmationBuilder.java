package com.nisovin.magicspells.util.config.dialog.types;

import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;
import com.nisovin.magicspells.util.config.dialog.action.ActionButtonBuilder;

public class ConfirmationBuilder extends BuildContext<DialogType> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogType> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new ConfirmationBuilder(debugger, config, basePath).build();
	}

	private ConfirmationBuilder(Debugger debugger, ConfigurationSection config, String basePath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
	}

	@Override
	protected @NotNull NullConfigData<DialogType> build() {
		ConfigurationSection yesSection = config.getConfigurationSection("button-yes");
		ConfigurationSection noSection = config.getConfigurationSection("button-no");

		ConfigData<ActionButton> yesData = ActionButtonBuilder.createRequired(debugger, yesSection, basePath, "button-yes");
		ConfigData<ActionButton> noData = ActionButtonBuilder.createRequired(debugger, noSection, basePath, "button-no");

		return data -> {
			ActionButton yes = yesData.get(data);
			ActionButton no = noData.get(data);
			if (yes == null || no == null) return null;

			return DialogType.confirmation(yes, no);
		};
	}

}
