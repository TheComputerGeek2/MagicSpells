package com.nisovin.magicspells.util.config.dialog.base;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;

public class SingleOptionInput extends BuildContext<DialogInput> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogInput> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new SingleOptionInput(debugger, config, basePath).build();
	}

	private SingleOptionInput(Debugger debugger, ConfigurationSection config, String basePath) {
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

		ConfigData<List<SingleOptionDialogInput.OptionEntry>> entriesData = configDataList(config, basePath, "entries", (section, basePath) -> {
			ConfigData<String> idData = required(ConfigDataUtil::getString, section, basePath, "id");
			ConfigData<Component> display = ConfigDataUtil.getComponent(section, "display", null);
			ConfigData<Boolean> initial = ConfigDataUtil.getBoolean(section, "initial", false);

			return data -> {
				String id = idData.get(data);
				if (id == null) return null;

				return SingleOptionDialogInput.OptionEntry.create(id, display.get(data), initial.get(data));
			};
		});

		return data -> {
			String key = keyData.get(data);
			Component label = labelData.get(data);
			if (key == null || label == null) return null;

			List<SingleOptionDialogInput.OptionEntry> entries = entriesData.get(data);
			if (entries.isEmpty()) {
				debugger.cast("Empty 'entries' list found at '" + basePath + "'.");
				return null;
			}

			try {
				return DialogInput.singleOption(key, width.get(data), entries, label, labelVisible.get(data));
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

}
