package com.nisovin.magicspells.spells.command;

import java.io.File;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.itemreader.alternative.AlternativeReaderManager;

// TODO find a good way of configuring which items to serialize
// TODO make the serialization and io processing async
// TODO reconsider the way of determining file name
// TODO produce a better way of naming the items
// TODO allow a configurable yaml header with variable substitution
// TODO allow configurable messages
// WARNING: THIS SPELL IS SUBJECT TO BREAKING CHANGES
// DO NOT USE CURRENTLY IF EXPECTING LONG TERM UNCHANGING BEHAVIOR
public class ItemSerializeSpell extends CommandSpell {

	private File dataFolder;

	private final ConfigData<Integer> indentation;

	private final ConfigData<String> serializerKey;

	public ItemSerializeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		indentation = getConfigDataInt("indentation", 4);

		serializerKey = getConfigDataString("serializer-key", "external::spigot");
	}

	@Override
	protected void initialize() {
		// Setup data folder
		dataFolder = new File(MagicSpells.getInstance().getDataFolder(), "items");
		if (!dataFolder.exists()) dataFolder.mkdirs();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		ItemStack item = caster.getInventory().getItemInMainHand();
		if (item.getType().isAir()) {
			sendMessage("You must be holding an item in your hand", caster);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		ConfigurationSection section = AlternativeReaderManager.serialize(serializerKey.get(data), item);
		if (section == null) {
			sendMessage("Unable to serialize item.", caster);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		YamlConfiguration config = new YamlConfiguration();
		config.set("magic-items." + System.currentTimeMillis(), section);
		config.options().indent(indentation.get(data));

		try {
			config.save(new File(dataFolder, System.currentTimeMillis() + ".yml"));
		} catch (IOException e) {
			sendMessage("Unable to serialize item.", caster);
			e.printStackTrace();

			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	public File getDataFolder() {
		return dataFolder;
	}

	public void setDataFolder(File dataFolder) {
		this.dataFolder = dataFolder;
	}

}
