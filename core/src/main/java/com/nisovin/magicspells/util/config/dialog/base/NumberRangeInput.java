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

public class NumberRangeInput extends BuildContext<DialogInput> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogInput> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new NumberRangeInput(debugger, config, basePath).build();
	}

	private NumberRangeInput(Debugger debugger, ConfigurationSection config, String basePath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
	}

	@Override
	protected @NotNull NullConfigData<DialogInput> build() {
		ConfigData<String> keyData = required(ConfigDataUtil::getString, config, basePath, "key");
		ConfigData<Component> labelData = required(ConfigDataUtil::getComponent, config, basePath, "label");

		ConfigData<Integer> width = ConfigDataUtil.getInteger(config, "width", 200);
		ConfigData<String> labelFormat = ConfigDataUtil.getString(config, "label-format", "options.generic_value");
		ConfigData<Float> start = ConfigDataUtil.getFloat(config, "start", 0);
		ConfigData<Float> end = ConfigDataUtil.getFloat(config, "end", 0);
		ConfigData<Float> initial = ConfigDataUtil.getFloat(config, "initial", 0);
		ConfigData<Float> step = ConfigDataUtil.getFloat(config, "step", 1);

		return data -> {
			String key = keyData.get(data);
			Component label = labelData.get(data);
			if (key == null || label == null) return null;

			try {
				return DialogInput.numberRange(
					key,
					width.get(data),
					label,
					labelFormat.get(data),
					start.get(data),
					end.get(data),
					initial.get(data),
					step.get(data)
				);
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

}
