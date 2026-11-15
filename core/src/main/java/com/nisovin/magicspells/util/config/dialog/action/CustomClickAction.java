package com.nisovin.magicspells.util.config.dialog.action;

import java.util.List;
import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.nbt.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.ClickCallback;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.registry.data.dialog.action.DialogAction;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.config.dialog.Debugger;
import com.nisovin.magicspells.util.config.dialog.BuildContext;
import com.nisovin.magicspells.util.config.dialog.NullConfigData;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;
import com.nisovin.magicspells.variables.variabletypes.PlayerStringVariable;

public class CustomClickAction extends BuildContext<DialogAction> {

	private static final ClickCallback.Options CLICK_OPTIONS = ClickCallback.Options.builder()
		.uses(ClickCallback.UNLIMITED_USES)
		.build();

	private final ConfigurationSection config;
	private final String path;

	protected static NullConfigData<DialogAction> create(Debugger debugger, ConfigurationSection config, String basePath, String subPath) {
		ConfigurationSection section = config.getConfigurationSection(subPath);
		if (section == null) {
			debugger.config("Missing '" + subPath + "' section at '" + basePath + "'.");
			return _ -> null;
		}

		String path = basePath + "." + subPath;
		return new CustomClickAction(debugger, section, path).build();
	}

	private CustomClickAction(Debugger debugger, ConfigurationSection config, String path) {
		super(debugger);
		this.config = config;
		this.path = path;
	}

	@Override
	protected @NotNull NullConfigData<DialogAction> build() {
		return switch (config.getString("type")) {
			case "custom" -> customPayload();
			case "spell" -> customSpell();
			case null, default -> {
				debugger.config("Invalid 'type' defined in '" + path + "'.");
				yield _ -> null;
			}
		};
	}

	private NullConfigData<DialogAction> customPayload() {
		ConfigData<NamespacedKey> idData = required(ConfigDataUtil::getNamespacedKey, config, path, "id");
		ConfigData<String> additionsData = ConfigDataUtil.getString(config, "additions", null);

		return data -> {
			Key id = idData.get(data);
			if (id == null) return null;

			String additionsString = additionsData.get(data);
			BinaryTagHolder additions = null;
			if (additionsString != null) additions = BinaryTagHolder.binaryTagHolder(additionsString);

			return DialogAction.customClick(id, additions);
		};
	}

	private NullConfigData<DialogAction> customSpell() {
		String spellName = config.getString("spell");
		String error = "Invalid 'spell' defined at '" + path + "': '" + spellName + "'.";
		if (spellName == null) {
			debugger.config(error);
			return _ -> null;
		}
		Subspell spell = new Subspell(spellName);
		if (!spell.process()) {
			debugger.config(error);
			return _ -> null;
		}

		ConfigData<List<VariableAction>> actionsData = configDataList(config, path, "variables", (config, path) -> {
			ConfigData<String> keyData = required(ConfigDataUtil::getString, config, path, "key");
			ConfigData<String> variableData = required(ConfigDataUtil::getString, config, path, "variable");

			return data -> {
				String key = keyData.get(data);
				String varName = variableData.get(data);
				if (key == null || varName == null) return null;

				Variable variable = MagicSpells.getVariableManager().getVariable(varName);
				if (variable == null) {
					debugger.cast("Invalid 'variable' defined at '" + path + "'.");
					return null;
				}

				return new VariableAction(debugger, variable, varName, key);
			};
		});

		return data -> DialogAction.customClick((view, audience) -> {
			if (!(audience instanceof Player player)) return;

			List<VariableAction> actions = actionsData.get(data);
			if (!actions.isEmpty()) {
				CompoundBinaryTag compoundTag;
				try {
					compoundTag = TagStringIO.tagStringIO().asCompound(view.payload().string());
				} catch (IOException _) {
					return;
				}

				for (VariableAction action : actions) action.set(compoundTag, player);
			}

			spell.subcast(new SpellData(player));
		}, CLICK_OPTIONS);
	}

	private record VariableAction(Debugger debugger, Variable variable, String variableName, String key) {

		private void set(CompoundBinaryTag compoundTag, Player player) {
			BinaryTag tag = compoundTag.get(key);
			if (tag == null) {
				debugger.cast("'variables' key '" + key + "' not found under 'inputs'.");
				return;
			}

			if (variable instanceof GlobalStringVariable || variable instanceof PlayerStringVariable) {
				MagicSpells.getVariableManager().set(variable, player, switch (tag) {
					case StringBinaryTag stringTag -> stringTag.value();
					case ByteBinaryTag byteTag -> String.valueOf(byteTag.value() != 0);
					case FloatBinaryTag floatTag -> String.valueOf(floatTag.value());
					default -> throw new UnsupportedOperationException("Not supported yet.");
				});
			} else {
				MagicSpells.getVariableManager().set(variable, player, switch (tag) {
					case StringBinaryTag stringTag -> NumberConversions.toDouble(stringTag.value());
					case NumberBinaryTag numTag -> numTag.doubleValue();
					default -> throw new UnsupportedOperationException("Not supported yet.");
				});
			}
		}

	}

}
