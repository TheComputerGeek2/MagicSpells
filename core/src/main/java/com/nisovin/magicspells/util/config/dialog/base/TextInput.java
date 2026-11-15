package com.nisovin.magicspells.util.config.dialog.base;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput.MultilineOptions;

public class TextInput extends BuildContext<DialogInput> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogInput> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new TextInput(debugger, config, basePath).build();
	}

	private TextInput(Debugger debugger, ConfigurationSection config, String basePath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
	}

	@Override
	protected @NotNull NullConfigData<DialogInput> build() {
		ConfigData<String> keyData = required(ConfigDataUtil::getString, config, basePath, "key");
		ConfigData<Component> labelData = required(ConfigDataUtil::getComponent, config, basePath, "label");

		ConfigData<Integer> width = ConfigDataUtil.getInteger(config, "width", 200);
		ConfigData<Boolean> labelVisible = ConfigDataUtil.getBoolean(config, "label-visible", true);
		ConfigData<String> initial = ConfigDataUtil.getString(config, "initial", "");
		ConfigData<Integer> maxLength = ConfigDataUtil.getInteger(config, "max-length", 32);

		ConfigData<MultilineOptions> multilineOptions = multiLineOptions();

		return data -> {
			String key = keyData.get(data);
			Component label = labelData.get(data);
			MultilineOptions options = multilineOptions.get(data);
			if (key == null || label == null || options == null) return null;

			try {
				return DialogInput.text(
					key,
					width.get(data),
					label,
					labelVisible.get(data),
					initial.get(data),
					maxLength.get(data),
					options
				);
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

	private @NotNull NullConfigData<MultilineOptions> multiLineOptions() {
		ConfigurationSection section = config.getConfigurationSection("multiline-options");
		if (section == null) return _ -> null;

		ConfigData<Integer> maxLines = ConfigDataUtil.getInteger(section, "max-lines", _ -> null);
		ConfigData<Integer> height = ConfigDataUtil.getInteger(section, "height", _ -> null);

		return data -> {
			try {
				return MultilineOptions.create(maxLines.get(data), height.get(data));
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid 'multiline-options' at '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

}
