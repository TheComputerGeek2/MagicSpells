package com.nisovin.magicspells.util.config.dialog;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.key.Key;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;

import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.base.BaseBuilder;
import com.nisovin.magicspells.util.config.dialog.types.TypeBuilder;

public class DialogBuilder extends BuildContext<Dialog> {

	private final ConfigurationSection config;

	public static NullConfigData<Dialog> create(ConfigurationSection config, Consumer<String> debugConfig, Consumer<String> debugCast) {
		Debugger debugger = new Debugger(debugConfig, debugCast);
		return new DialogBuilder(debugger, config).build();
	}

	private DialogBuilder(Debugger debugger, ConfigurationSection config) {
		super(debugger);
		this.config = config;
	}

	@Override
	protected @NotNull NullConfigData<Dialog> build() {
		ConfigData<NamespacedKey> copyFromData = ConfigDataUtil.getNamespacedKey(config, "copy-from", null);

		ConfigData<DialogBase> baseData = BaseBuilder.create(debugger, config);
		ConfigData<DialogType> typeData = TypeBuilder.create(debugger, config);

		return data -> {
			DialogBase base = baseData.get(data);
			DialogType type = typeData.get(data);
			if (base == null || type == null) return null;

			Key copyFrom = copyFromData.get(data);

			return Dialog.create(factory -> {
				DialogRegistryEntry.Builder builder;

				if (copyFrom == null) builder = factory.empty();
				else {
					try {
						builder = factory.copyFrom(TypedKey.create(RegistryKey.DIALOG, copyFrom));
					} catch (IllegalArgumentException e) {
						debugger.cast("Invalid 'copy-from': " + e.getMessage());
						return;
					}
				}

				builder.base(base).type(type);
			});
		};
	}

}
