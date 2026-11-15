package com.nisovin.magicspells.util.config.dialog.types;

import org.jetbrains.annotations.NotNull;

import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.type.DialogType;

import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

public class TypeBuilder extends BuildContext<DialogType> {

	private static final String PATH = "dialog-type";

	private final ConfigurationSection config;

	public static NullConfigData<DialogType> create(Debugger debugger, ConfigurationSection config) {
		ConfigurationSection section = config.getConfigurationSection(PATH);

		if (section == null) {
			debugger.config("Missing '" + PATH + "'.");
			return _ -> null;
		}

		return new TypeBuilder(debugger, section).build();
	}

	private TypeBuilder(Debugger debugger, ConfigurationSection config) {
		super(debugger);
		this.config = config;
	}

	@Override
	protected @NotNull NullConfigData<DialogType> build() {
		return switch (config.getString("type")) {
			case "confirmation" -> ConfirmationBuilder.create(debugger, config, PATH);
			case "dialog_list" -> DialogListBuilder.create(debugger, config, PATH);
			case "multi_action" -> MultiActionBuilder.create(debugger, config, PATH);
			case "notice" -> NoticeBuilder.create(debugger, config, PATH);
			case "server_links" -> ServerListTypeBuilder.create(debugger, config, PATH);
			case null, default -> {
				debugger.config("Invalid 'type' defined in '" + PATH + "'.");
				yield _ -> null;
			}
		};
	}

}
