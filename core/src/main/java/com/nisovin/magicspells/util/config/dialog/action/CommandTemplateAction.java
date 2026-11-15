package com.nisovin.magicspells.util.config.dialog.action;

import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.action.DialogAction;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

public class CommandTemplateAction extends BuildContext<DialogAction> {

	private final ConfigurationSection config;
	private final String basePath;
	private final String subPath;

	protected static NullConfigData<DialogAction> create(Debugger debugger, ConfigurationSection config, String basePath, String subPath) {
		return new CommandTemplateAction(debugger, config, basePath, subPath).build();
	}

	private CommandTemplateAction(Debugger debugger, ConfigurationSection config, String basePath, String subPath) {
		super(debugger);
		this.config = config;
		this.basePath = basePath;
		this.subPath = subPath;
	}

	@Override
	protected @NotNull NullConfigData<DialogAction> build() {
		ConfigData<String> templateData = required(ConfigDataUtil::getString, config, basePath, subPath);

		return data -> {
			String template = templateData.get(data);
			if (template == null) return null;

			return DialogAction.commandTemplate(template);
		};
	}

}
