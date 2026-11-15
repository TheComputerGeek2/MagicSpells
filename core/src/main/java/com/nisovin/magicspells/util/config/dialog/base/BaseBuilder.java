package com.nisovin.magicspells.util.config.dialog.base;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;

import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;

import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction;

public class BaseBuilder extends BuildContext<DialogBase> {

	private static final String PATH = "base";

	private final ConfigurationSection config;

	@NotNull
	public static NullConfigData<DialogBase> create(Debugger debugger, ConfigurationSection config) {
		ConfigurationSection section = config.getConfigurationSection(PATH);

		if (section == null) {
			debugger.config("Missing '" + PATH + "'.");
			return _ -> null;
		}

		return new BaseBuilder(debugger, section).build();
	}

	private BaseBuilder(Debugger debugger, ConfigurationSection config) {
		super(debugger);
		this.config = config;
	}

	@Override
	protected @NotNull NullConfigData<DialogBase> build() {
		ConfigData<Component> titleData = required(ConfigDataUtil::getComponent, config, PATH, "title");

		ConfigData<Boolean> canCloseWithEscape = ConfigDataUtil.getBoolean(config, "can-close-with-escape", true);
		ConfigData<DialogAfterAction> afterAction = ConfigDataUtil.getEnum(config, "after-action", DialogAfterAction.class, DialogAfterAction.CLOSE);

		ConfigData<List<DialogBody>> body = configDataList(config, PATH, "body", (config, path) ->
			switch (config.getString("type")) {
				case "item" -> itemBody(config, path);
				case "message" -> plainMessage(config, path);
				case null, default -> {
					debugger.config("Invalid 'type' defined at '" + path + "'.");
					yield null;
				}
			}
		);

		ConfigData<List<DialogInput>> inputs = configDataList(config, PATH, "inputs", (config, path) ->
			switch (config.getString("type")) {
				case "bool" -> BooleanInput.create(debugger, config, path);
				case "number_range" -> NumberRangeInput.create(debugger, config, path);
				case "single_option" -> SingleOptionInput.create(debugger, config, path);
				case "text" -> TextInput.create(debugger, config, path);
				case null, default -> {
					debugger.config("Invalid 'type' defined at '" + path + "'.");
					yield null;
				}
			}
		);

		return data -> {
			Component title = titleData.get(data);
			if (title == null) return null;

			return DialogBase.builder(title)
				//.externalTitle() // Unused until you can add these dialogs to registry.
				.canCloseWithEscape(canCloseWithEscape.get(data))
				.pause(false)
				.afterAction(afterAction.get(data))
				.body(body.get(data))
				.inputs(inputs.get(data))
				.build();
		};
	}

	private NullConfigData<PlainMessageDialogBody> plainMessage(ConfigurationSection config, String path) {
		ConfigData<Component> messageData = required(ConfigDataUtil::getComponent, config, path, "message");
		ConfigData<Integer> width = ConfigDataUtil.getInteger(config, "width", 200);

		return data -> {
			Component message = messageData.get(data);
			if (message == null) return null;

			try {
				return DialogBody.plainMessage(message, width.get(data));
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid 'width' defined at '" + path + "': " + e.getMessage());
				return null;
			}
		};
	}

	private NullConfigData<ItemDialogBody> itemBody(ConfigurationSection config, String path) {
		ConfigData<ItemStack> itemData = required(ConfigDataUtil::getItemStack, config, path, "item");

		ConfigurationSection descSection = config.getConfigurationSection("description");
		NullConfigData<PlainMessageDialogBody> description;
		if (descSection != null) description = plainMessage(descSection, path);
		else description = _ -> null;

		ConfigData<Boolean> showDecorations = ConfigDataUtil.getBoolean(config, "show-decorations", true);
		ConfigData<Boolean> showTooltip = ConfigDataUtil.getBoolean(config, "show-tooltip", true);
		ConfigData<Integer> width = ConfigDataUtil.getInteger(config, "width", 16);
		ConfigData<Integer> height = ConfigDataUtil.getInteger(config, "height", 16);

		return data -> {
			ItemStack item = itemData.get(data);
			if (item == null) return null;

			try {
				return DialogBody.item(item)
					.description(description.get(data))
					.showDecorations(showDecorations.get(data))
					.showTooltip(showTooltip.get(data))
					.width(width.get(data))
					.height(height.get(data))
					.build();
			} catch (IllegalArgumentException e) {
				debugger.cast("Invalid '" + path + "' defined: " + e.getMessage());
				return null;
			}
		};
	}

}
