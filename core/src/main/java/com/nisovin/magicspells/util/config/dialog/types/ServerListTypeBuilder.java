package com.nisovin.magicspells.util.config.dialog.types;

import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;
import com.nisovin.magicspells.util.config.dialog.action.ActionButtonBuilder;

public class ServerListTypeBuilder extends BuildContext<DialogType> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogType> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new ServerListTypeBuilder(debugger, config, basePath).build();
	}

	private ServerListTypeBuilder(Debugger debugger, ConfigurationSection config, String basePath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
	}

	@Override
	protected @NotNull NullConfigData<DialogType> build() {
		ConfigurationSection exitSection = config.getConfigurationSection("exit");
		NullConfigData<ActionButton> exit = ActionButtonBuilder.createOptional(debugger, exitSection, basePath, "exit");

		ConfigData<Integer> columns = ConfigDataUtil.getInteger(config, "columns", 2);
		ConfigData<Integer> buttonWidth = ConfigDataUtil.getInteger(config, "button-width", 150);

		return data -> {
			try {
				return DialogType.serverLinks(exit.get(data), columns.get(data), buttonWidth.get(data));
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

}
