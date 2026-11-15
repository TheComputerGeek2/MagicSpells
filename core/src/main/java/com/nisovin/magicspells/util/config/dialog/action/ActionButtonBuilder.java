package com.nisovin.magicspells.util.config.dialog.action;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

public class ActionButtonBuilder extends BuildContext<ActionButton> {

	private final ConfigurationSection config;
	private final String basePath;
	private final String subPath;

	public static NullConfigData<ActionButton> createOptional(Debugger debugger, ConfigurationSection config, String basePath, String subPath) {
		if (config == null) return _ -> null;
		return new ActionButtonBuilder(debugger, config, basePath, subPath).build();
	}

	public static NullConfigData<ActionButton> createRequired(Debugger debugger, ConfigurationSection config, String basePath, String subPath) {
		if (config == null) {
			debugger.config("Missing '" + subPath + "' section at '" + basePath + "'.");
			return _ -> null;
		}
		return new ActionButtonBuilder(debugger, config, basePath, subPath).build();
	}

	private ActionButtonBuilder(Debugger debugger, ConfigurationSection config, String basePath, String subPath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
		this.subPath = subPath;
	}

	@Override
	protected @NotNull NullConfigData<ActionButton> build() {
		String path = basePath + "." + subPath;

		NullConfigData<Component> labelData = required(ConfigDataUtil::getComponent, config, path, "label");
		ConfigData<Component> tooltip = ConfigDataUtil.getComponent(config, "tooltip", null);
		ConfigData<Integer> width = ConfigDataUtil.getInteger(config, "width", 150);
		ConfigData<DialogAction> action = dialogActionOptional(config, path, "action");

		return data -> {
			Component label = labelData.get(data);
			if (label == null) return null;

			try {
				return ActionButton.create(label, tooltip.get(data), width.get(data), action.get(data));
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + subPath + "' button defined in '" + basePath + "': " + e.getMessage());
				return null;
			}
		};
	}

	@NotNull
	private ConfigData<DialogAction> dialogActionOptional(ConfigurationSection config, String basePath, String subPath) {
		String path = basePath + "." + subPath;
		ConfigurationSection section = config.getConfigurationSection(subPath);
		if (section == null) return _ -> null;

		return switch (section.getString("type")) {
			case "command_template" -> CommandTemplateAction.create(debugger, section, path, "template");
			case "static_action" -> StaticAction.create(debugger, section, path, "static");
			case "custom_click" -> CustomClickAction.create(debugger, section, path, "click");
			case null, default -> {
				debugger.config("Invalid 'type' defined in '" + path + "'.");
				yield _ -> null;
			}
		};
	}

}
