package com.nisovin.magicspells.commands;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;

import org.bukkit.*;
import org.bukkit.entity.Mob;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import co.aikar.commands.*;
import co.aikar.commands.annotation.*;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.profile.ProfileProperty;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.variables.Variable;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.mana.ManaChangeReason;
import com.nisovin.magicspells.handlers.MagicXpHandler;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellbookReloadEvent;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.variables.variabletypes.GlobalVariable;
import com.nisovin.magicspells.variables.variabletypes.GlobalStringVariable;

@CommandAlias("ms|magicspells")
public class MagicCommand extends BaseCommand {

	private static final File PLUGIN_FOLDER = MagicSpells.getInstance().getDataFolder();

	private static final Pattern QUOTATIONS_PATTERN = Pattern.compile("^[\"']|[\"']$");

	public MagicCommand() {
		PaperCommandManager commandManager = MagicSpells.getCommandManager();

		// Creating conditions.
		commandManager.getCommandConditions().addCondition("mana_is_enabled", context -> {
			if (!MagicSpells.isManaSystemEnabled()) throw new ConditionFailedException("The Mana system is not enabled.");
		});

		// Create command completions.
		commandManager.getCommandCompletions().registerAsyncCompletion("spells", context ->
			TxtUtil.tabCompleteSpellName(context.getSender())
		);
		commandManager.getCommandCompletions().registerAsyncCompletion("variables", context ->
			MagicSpells.getVariableManager().getVariables().keySet()
		);
		commandManager.getCommandCompletions().registerAsyncCompletion("magic_items", context ->
			MagicItems.getMagicItemKeys()
		);
		commandManager.getCommandCompletions().registerAsyncCompletion("looking_at", context -> {
			Player player = context.getPlayer();
			if (player == null) return Collections.emptySet();

			String config = context.getConfig();
			if (config == null || config.isEmpty()) return Collections.emptySet();

			RayTraceResult result = player.rayTraceBlocks(6, FluidCollisionMode.SOURCE_ONLY);
			if (result == null) return Collections.emptySet();

			Block block = result.getHitBlock();
			if (block == null) return Collections.emptySet();

			String value = switch (config.toLowerCase()) {
				case "x" -> String.valueOf(block.getX());
				case "y" -> String.valueOf(block.getY());
				case "z" -> String.valueOf(block.getZ());
				case "pitch" -> String.valueOf(player.getLocation().getPitch());
				case "yaw" -> String.valueOf(player.getLocation().getYaw());
				default -> "";
			};
			return Set.of(value);
		});
		commandManager.getCommandCompletions().registerAsyncCompletion("spell_target", context -> {
			CommandSender sender = context.getSender();
			Player player = context.getPlayer();
			if (player == null) return TxtUtil.tabCompletePlayerName(sender);
			RayTraceResult result = player.rayTraceEntities(6);

			Set<String> targets = new HashSet<>();
			// Add the targeted entity's uuid/username first.
			if (result != null && result.getHitEntity() instanceof LivingEntity entity) {
				targets.add(entity instanceof Player pl ? pl.getName() : entity.getUniqueId().toString());
			}
			targets.addAll(TxtUtil.tabCompletePlayerName(sender));
			return targets;
		});
		commandManager.getCommandCompletions().registerAsyncCompletion("target_uuid", context -> {
			Player player = context.getPlayer();
			if (player == null) return Collections.emptySet();

			RayTraceResult result = player.rayTraceEntities(6);
			if (result == null || !(result.getHitEntity() instanceof LivingEntity entity))
				return Collections.emptySet();
			return Set.of(entity.getUniqueId().toString());
		});
		commandManager.getCommandCompletions().registerAsyncCompletion("cast_data", context -> {
			String[] args = (String[]) context.getContextValue(String.class.arrayType());
			CommandSender sender = context.getSender();
			if (args.length == 1) return TxtUtil.tabCompleteSpellName(sender);

			Spell spell = getSpell(args[0]);
			if (spell == null) return null;
			if (sender instanceof Player player) {
				if (!MagicSpells.getSpellbook(player).hasSpell(spell) || !spell.canCastByCommand()) {
					return null;
				}
			} else if (!(sender instanceof ConsoleCommandSender)) return null;

			List<String> ret = new ArrayList<>();
			if (args.length == 2 && Perm.COMMAND_CAST_POWER.has(sender)) ret.add("-p:");
			List<String> completion = spell.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
			if (completion != null) ret.addAll(completion);
			return ret;
		});
	}

	private static Spell getSpell(String name) {
		return MagicSpells.getSpellByName(QUOTATIONS_PATTERN.matcher(name).replaceAll(""));
	}

	private static Spell getSpell(CommandIssuer issuer, String name) {
		Spell spell = getSpell(name);
		if (spell == null) issuer.sendMessage(MagicSpells.getTextColor() + "No matching spell found: '" + name + "'");
		return spell;
	}

	private static Player getPlayerFromIssuer(CommandIssuer issuer) {
		if (issuer.isPlayer()) return issuer.getIssuer();
		issuer.sendMessage(MagicSpells.getTextColor() + "You must be a player in order to perform this command.");
		return null;
	}

	private static LivingEntity getEntity(String input) {
		if (input == null || input.isEmpty()) return null;
		// Try an ID.
		UUID uuid = null;
		try {
			uuid = UUID.fromString(input);
		} catch (IllegalArgumentException ignored) {}

		if (uuid != null) {
			Entity entity = Bukkit.getEntity(uuid);
			return entity instanceof LivingEntity ? (LivingEntity) entity : null;
		}
		// Try to find a player instead.
		Player player = Bukkit.getPlayer(input);
		return player != null && player.isOnline() ? player : null;
	}

	private static String[] getCustomArgs(String[] args, int fromIndex) {
		if (args.length <= fromIndex) return null;
		List<String> spellArgs = new ArrayList<>();
		for (String string : Arrays.copyOfRange(args, fromIndex, args.length)) {
			// Skip custom flags.
			if (string.startsWith("-p:")) continue;
			spellArgs.add(string);
		}
		return spellArgs.toArray(new String[0]);
	}

	private static boolean hasPowerArg(String[] args, int fromIndex) {
		if (args.length <= fromIndex) return false;
		for (String string : args) {
			if (!string.startsWith("-p:")) continue;
			return true;
		}
		return false;
	}

	private static float getPowerFromArgs(String[] args, int fromIndex) {
		if (args.length <= fromIndex) return 1F;
		for (String string : args) {
			if (!string.startsWith("-p:")) continue;
			return ACFUtil.parseFloat(string.substring(3), 1F);
		}
		return 1F;
	}

	private static boolean noPermission(CommandSender sender, Perm perm) {
		return noPermission(sender, perm, "You do not have permission to perform this command.");
	}

	private static boolean noPermission(CommandSender sender, Perm perm, String error) {
		if (perm.has(sender)) return false;
		sender.sendMessage(Util.getMiniMessage("&4Error: " + error));
		return true;
	}

	@HelpCommand
	@Syntax("[command]")
	@Description("Display command help.")
	public void doHelp(CommandSender sender, CommandHelp help) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(sender, Perm.COMMAND_HELP)) return;
		CommandHelpFilter.filter(sender, help);
		help.showHelp();
	}

	@Subcommand("reload")
	@CommandCompletion("@players @nothing")
	@Syntax("[player]")
	@Description("Reloads MagicSpells. If player is specified, then it reloads their spellbook.")
	@HelpPermission(permission = Perm.COMMAND_RELOAD)
	public static void onReload(CommandIssuer issuer, String[] args) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_RELOAD)) return;

		MagicSpells plugin = MagicSpells.getInstance();
		if (args.length == 0) {
			plugin.unload();
			plugin.load();
			issuer.sendMessage(MagicSpells.getTextColor() + "MagicSpells plugin reloaded.");
			return;
		}

		if (noPermission(issuer.getIssuer(), Perm.COMMAND_RELOAD_SPELLBOOK)) return;
		Player player = ACFBukkitUtil.findPlayerSmart(issuer, args[0]);
		if (player == null) return;

		// Remove old spellbook
		Spellbook spellbook = MagicSpells.getSpellbooks().get(player.getName());
		if (spellbook != null) spellbook.destroy();

		// Create new spellbook
		spellbook = new Spellbook(player);
		MagicSpells.getSpellbooks().put(player.getName(), spellbook);
		Bukkit.getPluginManager().callEvent(new SpellbookReloadEvent(player, spellbook));

		issuer.sendMessage(MagicSpells.getTextColor() + "Spellbook for player '" + TxtUtil.getPossessiveName(player.getName()) + "' has been reloaded.");

	}

	@Subcommand("reloadeffectlib")
	@Description("Reloads EffectLib, the shaded version inside MagicSpells.")
	@HelpPermission(permission = Perm.COMMAND_RELOAD_EFFECTLIB)
	public static void onReloadEffectLib(CommandIssuer issuer) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_RELOAD_EFFECTLIB)) return;
		MagicSpells.resetEffectlib();
		issuer.sendMessage(MagicSpells.getTextColor() + "Effectlib reloaded.");
	}

	@Subcommand("taskinfo")
	@Description("Displays information about tasks.")
	@HelpPermission(permission = Perm.COMMAND_TASKINFO)
	public static void onTask(CommandIssuer issuer) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_TASKINFO)) return;

		List<ScheduledTask> msTasks = new ArrayList<>();
//		for (ScheduledTask task : /* api ? */ ) {
//			if (task == null) continue;
//			if (!task.getOwningPlugin().equals(MagicSpells.getInstance())) continue;
//			msTasks.add(task);
//		}

		issuer.sendMessage(MagicSpells.getTextColor() + "EffectLib effect instances - " + MagicSpells.getEffectManager().getEffects().size());
		issuer.sendMessage(MagicSpells.getTextColor() + "MagicSpells tasks - " + msTasks.size());
	}

	@Subcommand("resetcd")
	@CommandCompletion("*|@players *|@spells @nothing")
	@Syntax("[player/*] [spell/*]")
	@Description("Reset cooldown of all players or a player for a spell or all spells.")
	@HelpPermission(permission = Perm.COMMAND_RESET_COOLDOWN)
	public static void onResetCD(CommandIssuer issuer, String[] args) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_RESET_COOLDOWN)) return;
		args = Util.splitParams(args);
		Player player = null;
		Spell spell = null;

		if (args.length > 0 && !args[0].isEmpty()) {
			if (!args[0].equals("*")) {
				player = ACFBukkitUtil.findPlayerSmart(issuer, args[0]);
				if (player == null) return;
			}

			if (args.length > 1 && !args[1].equals("*")) {
				spell = getSpell(issuer, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
				if (spell == null) return;
			}
		}

		if (player == null && args[0].isEmpty() && issuer.isPlayer()) player = issuer.getIssuer();

		Set<Spell> spells = new HashSet<>();
		if (spell == null) spells.addAll(MagicSpells.getSpells().values());
		else spells.add(spell);
		for (Spell s : spells) {
			if (player == null) s.getCooldowns().clear();
			else s.setCooldown(player, 0, false);
		}
		issuer.sendMessage(MagicSpells.getTextColor() + "Cooldowns reset" + (player == null ? "" : " for " + player.getName()) + (spell == null ? "" : " for spell " + Util.getLegacyFromMiniMessage(spell.getName())));
	}

	@Subcommand("mana")
	@Conditions("mana_is_enabled")
	public class ManaCommands extends BaseCommand {

		@Subcommand("show")
		@CommandAlias("mana")
		@Description("Display your mana.")
		@HelpPermission(permission = Perm.COMMAND_MANA_SHOW)
		public void onShow(CommandIssuer issuer) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_MANA_SHOW)) return;
			Player player = getPlayerFromIssuer(issuer);
			if (player == null) return;
			MagicSpells.getManaHandler().showMana(player, true);
		}

		@Subcommand("reset")
		@CommandCompletion("@players @nothing")
		@Syntax("[player]")
		@Description("Reset mana of yourself or another player.")
		@HelpPermission(permission = Perm.COMMAND_MANA_RESET)
		public void onReset(CommandIssuer issuer, @Optional OnlinePlayer onlinePlayer) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_MANA_RESET)) return;
			Player player = onlinePlayer == null ? getPlayerFromIssuer(issuer) : onlinePlayer.getPlayer();
			if (player == null) return;
			MagicSpells.getManaHandler().createManaBar(player);
			issuer.sendMessage(MagicSpells.getTextColor() + TxtUtil.getPossessiveName(player.getName()) + " mana was reset.");
		}

		@Subcommand("setmax")
		@CommandCompletion("@players @nothing")
		@Syntax("[player] <amount>")
		@Description("Set the max mana of yourself or another player.")
		@HelpPermission(permission = Perm.COMMAND_MANA_SET_MAX)
		public void onSetMax(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_MANA_SET_MAX)) return;
			if (args.length < 1) throw new InvalidCommandArgument();

			int amount;
			Player player = null;
			if (ACFUtil.isInteger(args[0])) amount = Integer.parseInt(args[0]);
			else {
				if (args.length < 2) throw new InvalidCommandArgument();
				player = ACFBukkitUtil.findPlayerSmart(issuer, args[0]);
				if (player == null) throw new InvalidCommandArgument();

				if (!ACFUtil.isInteger(args[1])) throw new InvalidCommandArgument();
				amount = Integer.parseInt(args[1]);
			}

			if (player == null) player = getPlayerFromIssuer(issuer);
			if (player == null) return;

			MagicSpells.getManaHandler().setMaxMana(player, amount);
			issuer.sendMessage(MagicSpells.getTextColor() + TxtUtil.getPossessiveName(player.getName()) + " max mana set to " + amount + ".");
		}

		@Subcommand("add")
		@CommandCompletion("@players @nothing")
		@Syntax("[player] <amount>")
		@Description("Add mana to yourself or another player.")
		@HelpPermission(permission = Perm.COMMAND_MANA_ADD)
		public void onAdd(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_MANA_ADD)) return;
			if (args.length < 1) throw new InvalidCommandArgument();

			int amount;
			Player player = null;
			if (ACFUtil.isInteger(args[0])) amount = Integer.parseInt(args[0]);
			else {
				if (args.length < 2) throw new InvalidCommandArgument();
				player = ACFBukkitUtil.findPlayerSmart(issuer, args[0]);
				if (player == null) throw new InvalidCommandArgument();

				if (!ACFUtil.isInteger(args[1])) throw new InvalidCommandArgument();
				amount = Integer.parseInt(args[1]);
			}

			if (player == null) player = getPlayerFromIssuer(issuer);
			if (player == null) return;

			MagicSpells.getManaHandler().addMana(player, amount, ManaChangeReason.OTHER);
			issuer.sendMessage(MagicSpells.getTextColor() + TxtUtil.getPossessiveName(player.getName()) + " mana was modified by " + amount + ".");
		}

		@Subcommand("set")
		@CommandCompletion("@players @nothing")
		@Syntax("[player] <amount>")
		@Description("Set your or another player's mana to a new value.")
		@HelpPermission(permission = Perm.COMMAND_MANA_SET)
		public void onSet(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_MANA_SET)) return;
			if (args.length < 1) throw new InvalidCommandArgument();

			int amount;
			Player player = null;
			if (ACFUtil.isInteger(args[0])) amount = Integer.parseInt(args[0]);
			else {
				if (args.length < 2) throw new InvalidCommandArgument();
				player = ACFBukkitUtil.findPlayerSmart(issuer, args[0]);
				if (player == null) throw new InvalidCommandArgument();

				if (!ACFUtil.isInteger(args[1])) throw new InvalidCommandArgument();
				amount = Integer.parseInt(args[1]);
			}

			if (player == null) player = getPlayerFromIssuer(issuer);
			if (player == null) return;

			MagicSpells.getManaHandler().setMana(player, amount, ManaChangeReason.OTHER);
			issuer.sendMessage(MagicSpells.getTextColor() + TxtUtil.getPossessiveName(player.getName()) + " mana was set to " + amount + ".");
		}

		@Subcommand("updaterank")
		@CommandCompletion("@players @nothing")
		@Syntax("[player]")
		@Description("Update your or another player's mana rank.")
		@HelpPermission(permission = Perm.COMMAND_MANA_UPDATE_RANK)
		public void onUpdateManaRank(CommandIssuer issuer, @Optional OnlinePlayer onlinePlayer) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_MANA_UPDATE_RANK)) return;
			Player player = onlinePlayer == null ? getPlayerFromIssuer(issuer) : onlinePlayer.getPlayer();
			if (player == null) return;
			boolean updated = MagicSpells.getManaHandler().updateManaRankIfNecessary(player);
			MagicSpells.getManaHandler().showMana(player);
			String status = updated ? "updated" : "already correct";
			issuer.sendMessage(MagicSpells.getTextColor() + TxtUtil.getPossessiveName(player.getName()) + " mana rank was " + status + ".");
		}

	}

	@Subcommand("variable")
	public class VariableCommands extends BaseCommand {

		@Subcommand("show")
		@CommandCompletion("@variables @players @nothing")
		@Syntax("<variable> [player]")
		@Description("Display value of a variable.")
		@HelpPermission(permission = Perm.COMMAND_VARIABLE_SHOW)
		public void onShowVariable(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_VARIABLE_SHOW)) return;
			if (args.length == 0) throw new InvalidCommandArgument();

			Variable variable = MagicSpells.getVariableManager().getVariable(args[0]);
			if (variable == null) throw new ConditionFailedException("No matching variable found: '" + args[0] + "'");

			String name = null;
			if (!(variable instanceof GlobalVariable || variable instanceof GlobalStringVariable)) {
				Player player = args.length == 1 ? getPlayerFromIssuer(issuer) : ACFBukkitUtil.findPlayerSmart(issuer, args[1]);
				if (player == null) return;

				name = player.getName();
			}

			String message = name == null ? "Variable" : TxtUtil.getPossessiveName(name) + " variable";
			issuer.sendMessage(MagicSpells.getTextColor() + message + " value for " + args[0] + " is: " + variable.getStringValue(name));
		}

		@Subcommand("modify")
		@CommandCompletion("@variables @players @nothing")
		@Syntax("<variable> <player> <varMod>")
		@Description("Modify a variable's value.")
		@HelpPermission(permission = Perm.COMMAND_VARIABLE_MODIFY)
		public void onModifyVariable(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_VARIABLE_MODIFY)) return;
			if (args.length < 3) throw new InvalidCommandArgument();

			String variableName = args[0];
			Variable variable = MagicSpells.getVariableManager().getVariable(variableName);
			if (variable == null) throw new ConditionFailedException("No matching variable found: '" + variableName + "'");

			Player player = null;
			if (ACFBukkitUtil.isValidName(args[1])) {
				player = Bukkit.getPlayer(args[1]);
				if (player == null || !player.isOnline()) throw new ConditionFailedException("No matching player found: '" + args[1] + "'");
			}
			String playerName = player == null ? "-" : player.getName();

			VariableMod variableMod = new VariableMod(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
			String oldValue = MagicSpells.getVariableManager().getStringValue(variableName, playerName);
			MagicSpells.getVariableManager().processVariableMods(variableName, variableMod, player, new SpellData(player));

			String message = player == null ? "Value" : TxtUtil.getPossessiveName(playerName) + " value";
			issuer.sendMessage(MagicSpells.getTextColor() + message + " of '" + variableName + "' was modified: '" + oldValue + "' to '" + MagicSpells.getVariableManager().getStringValue(variableName, playerName) + "'.");
		}

	}

	@Subcommand("magicitem")
	@CommandCompletion("@magic_items @players @nothing")
	@Syntax("<magicItem> [amount] [player]")
	@Description("Give a user a Magic Item.")
	@HelpPermission(permission = Perm.COMMAND_MAGIC_ITEM)
	public static void onMagicItem(CommandIssuer issuer, String[] args) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_MAGIC_ITEM)) return;
		if (args.length == 0) throw new InvalidCommandArgument();
		MagicItem magicItem = MagicItems.getMagicItemByInternalName(args[0]);
		if (magicItem == null) throw new ConditionFailedException("No matching Magic Item found: '" + args[0] + "'");

		int amount = 1;
		Player player = null;
		if (args.length > 1) {
			if (ACFUtil.isInteger(args[1])) {
				amount = Integer.parseInt(args[1]);
			} else {
				player = ACFBukkitUtil.findPlayerSmart(issuer, args[1]);
				if (player == null) throw new InvalidCommandArgument();
			}

			if (args.length > 2) {
				player = ACFBukkitUtil.findPlayerSmart(issuer, args[2]);
				if (player == null) return;
			}
		}

		if (player == null) player = getPlayerFromIssuer(issuer);
		if (player == null) return;

		ItemStack item = magicItem.getItemStack().clone();
		item.setAmount(amount);
		player.getInventory().addItem(item);
		issuer.sendMessage(MagicSpells.getTextColor() + player.getName() + " received a magic item (" + args[0] + " x" + amount + ").");
	}

	@Subcommand("util")
	public class UtilCommands extends BaseCommand {

		@Subcommand("download")
		@Syntax("<url> <fileName>")
		@Description("Download a file from a specified URL and save it with the specified name. (The spell file prefix is not automatically added.)")
		@HelpPermission(permission = Perm.COMMAND_UTIL_DOWNLOAD)
		public void onDownload(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_UTIL_DOWNLOAD)) return;
			if (args.length < 2) throw new InvalidCommandArgument();
			String fileName = args[1] + ".yml";
			File file = new File(PLUGIN_FOLDER, fileName);
			if (file.exists()) throw new ConditionFailedException("The file '" + fileName + "' already exists!");
			boolean downloaded = Util.downloadFile(args[0], file);
			if (downloaded) issuer.sendMessage(MagicSpells.getTextColor() + "SUCCESS! You will need reload the plugin to load new spells.");
			else throw new ConditionFailedException("The file could not be downloaded.");
		}

		@Subcommand("update")
		@Syntax("<url> <fileName>")
		@Description("This behaves the same as the download command, except it can overwrite existing files.")
		@HelpPermission(permission = Perm.COMMAND_UTIL_UPDATE)
		public void onUpdate(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_UTIL_UPDATE)) return;
			if (args.length < 2) throw new InvalidCommandArgument();
			String fileName = args[1];
			File updateFile = new File(PLUGIN_FOLDER, "update-" + fileName + ".yml");
			boolean downloaded = Util.downloadFile(args[0], updateFile);
			if (!downloaded) throw new ConditionFailedException("Update file failed to download.");
			// Delete the existing file.
			File oldFile = new File(PLUGIN_FOLDER, fileName + ".yml");
			if (oldFile.exists()) {
				boolean deleted = oldFile.delete();
				if (!deleted) throw new ConditionFailedException("Old file could not be deleted. Aborting update, please delete the update file: '" + updateFile.getName() + "'");
				issuer.sendMessage(MagicSpells.getTextColor() + "Old file successfully deleted.");
			}
			// Rename the update to the original file's name.
			boolean renamed = updateFile.renameTo(new File(PLUGIN_FOLDER, fileName + ".yml"));
			if (!renamed) throw new ConditionFailedException("Failed to rename the update file, update failed");
			issuer.sendMessage(MagicSpells.getTextColor() + "Successfully renamed the update file to '" + fileName + ".yml'. You will need reload the plugin to load new spells.");
		}

		@Subcommand("saveskin")
		@CommandCompletion("@players @nothing")
		@Syntax("[player]")
		@Description("Save a player's current skin data to a readable file.")
		@HelpPermission(permission = Perm.COMMAND_UTIL_SAVE_SKIN)
		public void onSaveSkin(CommandIssuer issuer, @Optional Player player) {
			if (!MagicSpells.isLoaded()) return;

			if (noPermission(issuer.getIssuer(), Perm.COMMAND_UTIL_SAVE_SKIN)) return;
			if (player == null) player = getPlayerFromIssuer(issuer);
			if (player == null) return;

			ProfileProperty latestSkin = player.getPlayerProfile()
					.getProperties()
					.stream()
					.filter(prop -> prop.getName().equals("textures"))
					.toList()
					.getFirst();

			YamlConfiguration data = new YamlConfiguration();
			data.set("skin", latestSkin.getValue());
			data.set("signature", latestSkin.getSignature());

			File folder = new File(PLUGIN_FOLDER, "skins");
			if (!folder.exists()) folder.mkdir();
			try {
				data.save(new File(folder, System.currentTimeMillis() + ".yml"));
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			issuer.sendMessage(MagicSpells.getTextColor() + TxtUtil.getPossessiveName(player.getName()) + " skin was saved.");
		}

		@Subcommand("listgoals")
		@CommandCompletion("@target_uuid @nothing")
		@Syntax("[uuid]")
		@Description("List an entity's mob goals.")
		@HelpPermission(permission = Perm.COMMAND_UTIL_LIST_GOALS)
		public void onListGoals(CommandIssuer issuer, String uuidString) {
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_UTIL_LIST_GOALS)) return;

			UUID uuid;
			try {
				uuid = UUID.fromString(uuidString);
			} catch (IllegalArgumentException e) {
				throw new ConditionFailedException("Passed UUID argument is not a valid UUIO.");
			}
			if (!(Bukkit.getEntity(uuid) instanceof Mob mob))
				throw new ConditionFailedException("UUID did not match an entity of Mob type.");

			Collection<Goal<@NotNull Mob>> goals = Bukkit.getMobGoals().getAllGoals(mob);
			if (goals.isEmpty()) {
				issuer.sendMessage(MagicSpells.getTextColor() + "Entity '" + uuid + "' has no mob goals.");
				return;
			}

			issuer.sendMessage(MagicSpells.getTextColor() + "Mob goals of entity '" + uuid + "':");
			for (Goal<@NotNull Mob> goal : goals) {
				String entity = goal.getKey().getEntityClass().getSimpleName();
				String key = goal.getKey().getNamespacedKey().toString();
				issuer.sendMessage(MagicSpells.getTextColor() + "  - " + entity + ": " + key + " " + goal.getTypes());
			}
		}

	}

	@Subcommand("profilereport")
	@Description("Save profile report to a file.")
	@HelpPermission(permission = Perm.COMMAND_PROFILE_REPORT)
	public static void onProfiler(CommandIssuer issuer) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_PROFILE_REPORT)) return;
		MagicSpells.profilingReport();
		issuer.sendMessage(MagicSpells.getTextColor() + "Created profiling report.");
	}

	@Subcommand("debug")
	@Syntax("[level]")
	@Description("Toggle MagicSpells debug mode.")
	@HelpPermission(permission = Perm.COMMAND_DEBUG)
	public static void onDebug(CommandIssuer issuer, @Optional Integer level) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_DEBUG)) return;

		int levelFinal = MagicSpells.isDebug() || level == null ? MagicSpells.getDebugLevelOriginal() : level;
		MagicSpells.setDebugLevel(levelFinal);
		MagicSpells.setDebug(!MagicSpells.isDebug());

		issuer.sendMessage(MagicSpells.getTextColor() + "MagicSpells debug mode " + (MagicSpells.isDebug() ? "enabled (level: " + levelFinal + ")" : "disabled") + ".");
	}

	@Subcommand("magicxp")
	@CommandAlias("magicxp")
	@Description("Display your MagicXp.")
	@HelpPermission(permission = Perm.COMMAND_MAGICXP)
	public void onShow(CommandIssuer issuer) {
		if (!MagicSpells.isLoaded()) return;
		if (noPermission(issuer.getIssuer(), Perm.COMMAND_MAGICXP)) return;
		Player player = getPlayerFromIssuer(issuer);
		if (player == null) return;
		MagicXpHandler xpHandler = MagicSpells.getMagicXpHandler();
		if (xpHandler == null) throw new ConditionFailedException("The ManaXp system is not enabled.");
		xpHandler.showXpInfo(player);
	}

	@Subcommand("cast")
	public class CastCommands extends BaseCommand {

		@Subcommand("self")
		@CommandAlias("c|cast")
		@CommandCompletion("@cast_data")
		@Syntax("<spell> [-p:(power)] [spellArgs]")
		@Description("Cast a spell. (You can optionally define power: -p:1.0)")
		@HelpPermission(permission = Perm.COMMAND_CAST_SELF)
		public void onCastSelf(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_CAST_SELF)) return;
			args = Util.splitParams(args);
			if (args[0].isEmpty()) throw new InvalidCommandArgument();

			// This is an abstract way to preserve an old command alias for the "ms cast as" command. ("c forcecast")
			if (args[0].equals("forcecast") && args.length > 2) {
				onCastAs(issuer, args[1], Arrays.copyOfRange(args, 2, args.length));
				return;
			}

			Spell spell = getSpell(issuer, args[0]);
			if (spell == null) return;

			// Get spell power if the user has permission.
			if (hasPowerArg(args, 1)) {
				boolean noPowerPerm = noPermission(issuer.getIssuer(), Perm.COMMAND_CAST_POWER,"You do not have permission to use the power parameter.");
				if (noPowerPerm) return;
			}
			float power = getPowerFromArgs(args, 1);
			String[] spellArgs = getCustomArgs(args, 1);

			CommandSender sender = issuer.getIssuer();
			// Console
			if (sender instanceof ConsoleCommandSender) {
				boolean casted = spell.castFromConsole(issuer.getIssuer(), spellArgs);
				if (casted) issuer.sendMessage("Spell casted!");
				else issuer.sendMessage("Spell failed to cast.");
				return;
			}
			// Player
			if (sender instanceof Player) {
				Player player = ((Player) sender).getPlayer();
				if (player == null) return;
				if (!spell.canCastByCommand()) {
					MagicSpells.sendMessage(MagicSpells.getTextColor() + "You cannot cast this spell by commands.", player, null);
					return;
				}
				if (spell.isHelperSpell() && !Perm.COMMAND_CAST_SELF_HELPER.has(player) || !MagicSpells.getSpellbook(player).hasSpell(spell)) {
					MagicSpells.sendMessage(MagicSpells.getTextColor() + MagicSpells.getUnknownSpellMessage(), player, null);
					return;
				}
				if (!spell.isValidItemForCastCommand(player.getInventory().getItemInMainHand())) {
					MagicSpells.sendMessage(spell.getStrWrongCastItem(), player, null);
					return;
				}
				spell.hardCast(new SpellData(player, power, spellArgs));
				return;
			}
			// LivingEntity
			if (sender instanceof LivingEntity livingEntity) {
				if (!spell.canCastByCommand()) return;
				EntityEquipment equipment = livingEntity.getEquipment();
				if (equipment == null) return;
				if (!spell.isValidItemForCastCommand(equipment.getItemInMainHand())) return;
				spell.hardCast(new SpellData(livingEntity, power, spellArgs));
			}
		}

		@Subcommand("as")
		@CommandCompletion("@spell_target @cast_data")
		@Syntax("<player/UUID> <spell> (-p:[power]) [spellArgs]")
		@Description("Force a player to cast a spell. (You can optionally define power: -p:1.0)")
		@HelpPermission(permission = Perm.COMMAND_CAST_AS)
		public void onCastAs(CommandIssuer issuer, String targetName, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_CAST_AS)) return;

			LivingEntity target = getEntity(targetName);
			if (target == null) throw new ConditionFailedException("Entity not found.");

			args = Util.splitParams(args);
			Spell spell = getSpell(issuer, args[0]);
			if (spell == null) return;

			// Get spell power if the user has permission.
			if (hasPowerArg(args, 1)) {
				boolean noPowerPerm = noPermission(issuer.getIssuer(), Perm.COMMAND_CAST_POWER,"You do not have permission to use the power parameter.");
				if (noPowerPerm) return;
			}
			float power = getPowerFromArgs(args, 1);
			String[] spellArgs = getCustomArgs(args, 1);
			spell.hardCast(new SpellData(target, power, spellArgs));
		}

		@Subcommand("on")
		@CommandCompletion("@spell_target @cast_data")
		@Syntax("<player/UUID> <spell> (-p:[power]) [spellArgs]")
		@Description("Cast a spell on an entity. (You can optionally define power: -p:1.0)")
		@HelpPermission(permission = Perm.COMMAND_CAST_ON)
		public void onCastOn(CommandIssuer issuer, String targetName, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_CAST_ON)) return;

			LivingEntity target = getEntity(targetName);
			if (target == null) throw new ConditionFailedException("Entity not found.");

			args = Util.splitParams(args);
			Spell spell = getSpell(issuer, args[0]);
			if (spell == null) return;
			if (!(spell instanceof TargetedEntitySpell newSpell)) {
				throw new ConditionFailedException("Spell is not a targeted entity spell.");
			}

			// Get spell power if the user has permission.
			if (hasPowerArg(args, 1)) {
				boolean noPowerPerm = noPermission(issuer.getIssuer(), Perm.COMMAND_CAST_POWER,"You do not have permission to use the power parameter.");
				if (noPowerPerm) return;
			}
			float power = getPowerFromArgs(args, 1);
			String[] spellArgs = getCustomArgs(args, 1);

			LivingEntity caster = issuer.getIssuer() instanceof LivingEntity ? issuer.getIssuer() : null;
			CastResult result = newSpell.castAtEntity(new SpellData(caster, target, power, spellArgs));
			if (result.action() == PostCastAction.ALREADY_HANDLED)
				throw new ConditionFailedException("Spell probably cannot be cast from console.");
		}

		@Subcommand("at")
		@CommandCompletion("@spells @worlds @looking_at:X @looking_at:Y @looking_at:Z @looking_at:yaw @looking_at:pitch @nothing")
		@Syntax("<spell> [world] <x> <y> <z> [yaw] [pitch]")
		@Description("Cast a spell at a location.")
		@HelpPermission(permission = Perm.COMMAND_CAST_AT)
		public void onCastAt(CommandIssuer issuer, String[] args) {
			if (!MagicSpells.isLoaded()) return;
			if (noPermission(issuer.getIssuer(), Perm.COMMAND_CAST_AT)) return;

			args = Util.splitParams(args);
			if (args.length < 4) throw new InvalidCommandArgument();
			Spell spell = getSpell(issuer, args[0]);
			if (spell == null) return;
			if (!(spell instanceof TargetedLocationSpell newSpell)) {
				throw new ConditionFailedException("Spell is not a targeted location spell.");
			}

			World world = null;
			float yaw = 0, pitch = 0;
			int i = 0;
			// Has world parameter.
			if (!ACFUtil.isDouble(args[1])) {
				if (args.length < 5) throw new InvalidCommandArgument();
				world = Bukkit.getWorld(args[1]);
				i = 1;
			}
			// Use issuer's world.
			else if (issuer.getIssuer() instanceof LivingEntity) {
				Location location = ((LivingEntity) issuer.getIssuer()).getLocation();
				world = location.getWorld();
				// If only coordinates were specified.
				if (args.length < 5) {
					yaw = location.getYaw();
					pitch = location.getPitch();
				}
			}
			// This fails if the world wasn't specified and the issuer is console, or if the world was invalid.
			if (world == null) throw new ConditionFailedException("No world found.");

			// Parse coordinates.
			if (!ACFUtil.isDouble(args[1 + i])) throw new InvalidCommandArgument();
			if (!ACFUtil.isDouble(args[2 + i])) throw new InvalidCommandArgument();
			if (!ACFUtil.isDouble(args[3 + i])) throw new InvalidCommandArgument();
			double x = ACFUtil.parseDouble(args[1 + i]);
			double y = ACFUtil.parseDouble(args[2 + i]);
			double z = ACFUtil.parseDouble(args[3 + i]);
			if (args.length > 4 + i) {
				if (!ACFUtil.isFloat(args[4 + i])) throw new InvalidCommandArgument();
				yaw = ACFUtil.parseFloat(args[4 + i]);
			}
			if (args.length > 5 + i) {
				if (!ACFUtil.isFloat(args[5 + i])) throw new InvalidCommandArgument();
				pitch = ACFUtil.parseFloat(args[5 + i]);
			}
			Location location = new Location(world, x, y, z, yaw, pitch);

			// Handle with or without caster.
			SpellData data = new SpellData(issuer.getIssuer() instanceof LivingEntity le ? le : null, location);
			CastResult result = newSpell.castAtLocation(data);

			if (result.action() == PostCastAction.ALREADY_HANDLED)
				throw new ConditionFailedException("Spell probably cannot be cast from console.");
		}

	}

}
