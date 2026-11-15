package com.nisovin.magicspells.util.config.dialog.types;

import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;
import com.nisovin.magicspells.util.config.dialog.action.ActionButtonBuilder;

public class NoticeBuilder extends BuildContext<DialogType> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogType> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new NoticeBuilder(debugger, config, basePath).build();
	}

	private NoticeBuilder(Debugger debugger, ConfigurationSection config, String basePath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
	}

	@Override
	protected @NotNull NullConfigData<DialogType> build() {
		ConfigurationSection buttonSection = config.getConfigurationSection("button");
		NullConfigData<ActionButton> buttonData = ActionButtonBuilder.createOptional(debugger, buttonSection, basePath, "button");

		return data -> {
			ActionButton button = buttonData.get(data);
			return button == null ? DialogType.notice() : DialogType.notice(button);
		};
	}

}
