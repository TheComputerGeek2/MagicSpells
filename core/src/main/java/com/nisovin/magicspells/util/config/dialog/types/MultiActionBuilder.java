package com.nisovin.magicspells.util.config.dialog.types;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;
import com.nisovin.magicspells.util.config.dialog.action.ActionButtonBuilder;

public class MultiActionBuilder extends BuildContext<DialogType> {

	private final ConfigurationSection config;
	private final String basePath;

	protected static NullConfigData<DialogType> create(Debugger debugger, ConfigurationSection config, String basePath) {
		return new MultiActionBuilder(debugger, config, basePath).build();
	}

	private MultiActionBuilder(Debugger debugger, ConfigurationSection config, String basePath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
	}

	@Override
	protected @NotNull NullConfigData<DialogType> build() {
		ConfigData<List<ActionButton>> actionsData = configDataList(config, basePath, "actions", (config, path) -> {
			List<String> modifierList = config.getStringList("modifiers");
			ModifierSet modifiers = modifierList.isEmpty() ? null : new ModifierSet(modifierList);

			NullConfigData<ActionButton> button = ActionButtonBuilder.createRequired(debugger, config, basePath, path);

			return data -> {
				if (modifiers != null && !modifiers.apply(data.caster(), data).check()) return null;
				return button.get(data);
			};
		});

		ConfigurationSection exitSection = config.getConfigurationSection("exit");
		NullConfigData<ActionButton> exit = ActionButtonBuilder.createOptional(debugger, exitSection, basePath, "exit");

		ConfigData<Integer> columns = ConfigDataUtil.getInteger(config, "columns", 2);

		return data -> {
			List<ActionButton> actions = actionsData.get(data);
			if (actions.isEmpty()) {
				debugger.cast("Empty 'actions' list found at '" + basePath + "'.");
				return null;
			}

			try {
				return DialogType.multiAction(actions, exit.get(data), columns.get(data));
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

}
