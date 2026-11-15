package com.nisovin.magicspells.util.config.dialog.base;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.input.DialogInput;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

public class BooleanInput extends BuildContext<DialogInput> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogInput> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new BooleanInput(debugger, config, basePath).build();
	}

	private BooleanInput(Debugger debugger, ConfigurationSection config, String basePath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
	}

	@Override
	protected @NotNull NullConfigData<DialogInput> build() {
		ConfigData<String> keyData = required(ConfigDataUtil::getString, config, basePath, "key");
		ConfigData<Component> labelData = required(ConfigDataUtil::getComponent, config, basePath, "label");

		ConfigData<Boolean> initial = ConfigDataUtil.getBoolean(config, "initial", false);
		ConfigData<String> onTrue = ConfigDataUtil.getString(config, "on-true", "true");
		ConfigData<String> onFalse = ConfigDataUtil.getString(config, "on-false", "false");

		return data -> {
			String key = keyData.get(data);
			Component label = labelData.get(data);
			if (key == null || label == null) return null;

			try {
				return DialogInput.bool(key, label, initial.get(data), onTrue.get(data), onFalse.get(data));
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

}
