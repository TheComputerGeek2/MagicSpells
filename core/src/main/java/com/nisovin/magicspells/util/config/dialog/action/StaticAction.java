package com.nisovin.magicspells.util.config.dialog.action;

import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.event.ClickEvent;

import org.apache.commons.lang3.function.TriFunction;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.action.DialogAction;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

public class StaticAction extends BuildContext<DialogAction> {

	private final ConfigurationSection config;
	private final String path;

	protected static NullConfigData<DialogAction> create(Debugger debugger, ConfigurationSection config, String basePath, String subPath) {
		ConfigurationSection section = config.getConfigurationSection(subPath);
		if (section == null) {
			debugger.config("Missing '" + subPath + "' section at '" + basePath + "'.");
			return _ -> null;
		}

		String path = basePath + "." + subPath;
		return new StaticAction(debugger, section, path).build();
	}

	private StaticAction(Debugger debugger, ConfigurationSection config, String path) {
		super(debugger);
		this.config = config;
		this.path = path;
	}

	@Override
	protected @NotNull NullConfigData<DialogAction> build() {
		ConfigData<ClickEvent<?>> eventData = switch (config.getString("type")) {
			case "open_url" -> clickEventString(ClickEvent::openUrl, config, path, "url");
			case "run_command" -> clickEventString(ClickEvent::runCommand, config, path, "command");
			case "suggest_command" -> clickEventString(ClickEvent::suggestCommand, config, path, "command");
			case "copy_to_clipboard" -> clickEventString(ClickEvent::copyToClipboard, config, path, "text");
			//case "callback" -> ClickEvent#callback; // same as "custom_click" DialogAction
			//case "custom" -> ClickEvent#custom; // same as "custom_click" DialogAction
			case "show_dialog" -> clickEvent(this::configDataDialog, ClickEvent::showDialog, config, path, "dialog");
			case null, default -> {
				debugger.config("Invalid 'type' defined in '" + path + "'.");
				yield _ -> null;
			}
		};

		return data -> {
			ClickEvent<?> event = eventData.get(data);
			if (event == null) return null;

			return DialogAction.staticAction(event);
		};
	}

	private ConfigData<Dialog> configDataDialog(ConfigurationSection config, String path, Dialog def) {
		return ConfigDataUtil.getRegistryEntry(config, path, RegistryKey.DIALOG, def);
	}

	private NullConfigData<ClickEvent<?>> clickEventString(
		Function<String, ClickEvent<?>> clickEventFun,
		ConfigurationSection config,
		String basePath,
		String subPath
	) {
		return clickEvent(ConfigDataUtil::getString, clickEventFun, config, basePath, subPath);
	}

	private <T> NullConfigData<ClickEvent<?>> clickEvent(
		TriFunction<ConfigurationSection, String, T, ConfigData<T>> configDataFun,
		Function<T, ClickEvent<?>> clickEventFun,
		ConfigurationSection config,
		String basePath,
		String subPath
	) {
		ConfigData<T> valData = required(configDataFun, config, basePath, subPath);

		return data -> {
			T val = valData.get(data);
			if (val == null) return null;
			return clickEventFun.apply(val);
		};
	}

}
