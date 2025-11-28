package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.io.IOException;
import java.util.Collections;

import com.google.common.collect.Iterables;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;

public class KeybindSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private final Map<String, Keybinds> playerKeybinds;

	private ItemStack wandItem;
	private ItemStack defaultSpellIcon;

	public KeybindSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		playerKeybinds = new HashMap<>();

		MagicItem magicWandItem = MagicItems.getMagicItemFromString(getConfigString("wand-item", "blaze_rod"));
		if (magicWandItem != null) wandItem = magicWandItem.getItemStack();

		MagicItem magicIconItem = MagicItems.getMagicItemFromString(getConfigString("default-spell-icon", "redstone"));
		if (magicIconItem != null) defaultSpellIcon = magicIconItem.getItemStack();
	}

	@Override
	protected void initialize() {
		super.initialize();

		Util.forEachPlayerOnline(this::loadKeybinds);
	}

	private void loadKeybinds(Player player) {
		File file = new File(MagicSpells.plugin.getDataFolder(), "spellbooks" + File.separator + "keybinds-" + player.getName().toLowerCase() + ".txt");
		if (!file.exists()) return;
		try {
			Keybinds keybinds = new Keybinds(player);
			YamlConfiguration conf = new YamlConfiguration();
			conf.load(file);
			for (String key : conf.getKeys(false)) {
				int slot = Integer.parseInt(key);
				String spellName = conf.getString(key, "");
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null) keybinds.setKeybind(slot, spell);
			}
			playerKeybinds.put(player.getName(), keybinds);
		} catch (Exception e) {
			MagicSpells.plugin.getLogger().severe("Failed to load player keybinds for " + player.getName());
			e.printStackTrace();
		}

	}

	private void saveKeybinds(Keybinds keybinds) {
		File file = new File(MagicSpells.plugin.getDataFolder(), "spellbooks" + File.separator + "keybinds-" + keybinds.player.getName().toLowerCase() + ".txt");
		YamlConfiguration conf = new YamlConfiguration();
		Spell[] binds = keybinds.keybinds;
		for (int i = 0; i < binds.length; i++) {
			if (binds[i] == null) continue;
			conf.set(i + "", binds[i].getInternalName());
		}
		try {
			conf.save(file);
		} catch (IOException e) {
			MagicSpells.plugin.getLogger().severe("Failed to save keybinds for " + keybinds.player.getName());
			e.printStackTrace();
		}
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (data.args().length != 1) {
			sendMessage("Invalid args.", caster);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Keybinds keybinds = playerKeybinds.computeIfAbsent(caster.getName(), name -> new Keybinds(caster));

		int slot = caster.getInventory().getHeldItemSlot();
		ItemStack item = caster.getEquipment().getItemInMainHand();

		if (data.args()[0].equalsIgnoreCase("clear")) {
			keybinds.clearKeybind(slot);
			saveKeybinds(keybinds);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		if (data.args()[0].equalsIgnoreCase("clearall")) {
			keybinds.clearKeybinds();
			saveKeybinds(keybinds);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		if (!item.getType().isAir()) {
			caster.sendMessage("Not empty.");
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spell spell = MagicSpells.getSpellByName(data.args()[0]);
		if (spell == null || !MagicSpells.getSpellbook(caster).hasSpell(spell)) {
			caster.sendMessage("No spell.");
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		keybinds.setKeybind(slot, spell);
		keybinds.select(slot);
		saveKeybinds(keybinds);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
		if (!(executor instanceof Player caster)) return Collections.emptyList();

		return Iterables.concat(OwnedSpellParser.suggest(caster), List.of("clear", "clearall"));
	}

	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		keybinds.deselect(event.getPreviousSlot());
		keybinds.select(event.getNewSlot());
	}

	@EventHandler
	public void onAnimate(PlayerAnimationEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		boolean casted = keybinds.castKeybind(event.getPlayer().getInventory().getHeldItemSlot());
		if (casted) event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;

		if (keybinds.hasKeybind(event.getPlayer().getInventory().getHeldItemSlot())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		loadKeybinds(event.getPlayer());
	}

	public ItemStack getWandItem() {
		return wandItem;
	}

	public void setWandItem(ItemStack wandItem) {
		this.wandItem = wandItem;
	}

	public ItemStack getDefaultSpellIcon() {
		return defaultSpellIcon;
	}

	public void setDefaultSpellIcon(ItemStack defaultSpellIcon) {
		this.defaultSpellIcon = defaultSpellIcon;
	}

	private class Keybinds {

		private Player player;
		private Spell[] keybinds;

		private Keybinds(Player player) {
			this.player = player;
			this.keybinds = new Spell[10];
		}

		private void deselect(int slot) {
			Spell spell = keybinds[slot];
			if (spell == null) return;
			ItemStack spellIcon = spell.getSpellIcon();
			if (spellIcon == null && defaultSpellIcon != null) spellIcon = defaultSpellIcon;
			MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, spellIcon);
		}

		private void select(int slot) {
			Spell spell = keybinds[slot];
			if (spell == null) return;
			if (wandItem == null) return;
			MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, wandItem);
		}

		private boolean hasKeybind(int slot) {
			return keybinds[slot] != null;
		}

		private boolean castKeybind(int slot) {
			Spell spell = keybinds[slot];
			if (spell == null) return false;
			spell.hardCast(new SpellData(player));
			return true;
		}

		private void setKeybind(int slot, Spell spell) {
			keybinds[slot] = spell;
		}

		private void clearKeybind(int slot) {
			keybinds[slot] = null;
			MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, null);
		}

		private void clearKeybinds() {
			for (int i = 0; i < keybinds.length; i++) {
				if (keybinds[i] == null) continue;
				keybinds[i] = null;
				MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, i, null);
			}
		}

	}

}
