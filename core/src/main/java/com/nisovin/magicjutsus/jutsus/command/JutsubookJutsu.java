package com.nisovin.magicjutsus.jutsus.command;

import java.io.File;
import java.util.List;
import java.util.Scanner;
import java.io.FileWriter;
import java.util.ArrayList;
import java.io.BufferedWriter;
import java.util.regex.Pattern;
import java.io.FileNotFoundException;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicjutsus.Perm;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.MagicLocation;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.JutsuLearnEvent.LearnSource;

// Advanced perm is for being able to destroy jutsubooks
// Op is currently required for using the reload

public class JutsubookJutsu extends CommandJutsu {
	
	private static final Pattern PATTERN_CAST_ARG_USAGE = Pattern.compile("^[0-9]+$");

	private ArrayList<String> bookJutsus;
	private ArrayList<Integer> bookUses;
	private ArrayList<MagicLocation> bookLocations;

	private Material jutsubookBlock;
	private String jutsubookBlockName;

	private int defaultUses;

	private boolean destroyBookcase;

	private String strUsage;
	private String strNoJutsu;
	private String strLearned;
	private String strNoTarget;
	private String strCantTeach;
	private String strCantLearn;
	private String strLearnError;
	private String strCantDestroy;
	private String strHasJutsubook;
	private String strAlreadyKnown;
	
	public JutsubookJutsu(MagicConfig config, String jutsuName) {
		super(config,jutsuName);

		bookJutsus = new ArrayList<>();
		bookUses = new ArrayList<>();
		bookLocations = new ArrayList<>();

		jutsubookBlockName = getConfigString("jutsubook-block", "bookshelf").toUpperCase();
		jutsubookBlock = Material.getMaterial(jutsubookBlockName);
		if (jutsubookBlock == null || !jutsubookBlock.isBlock()) {
			MagicJutsus.error("JutsubookJutsu '" + internalName + "' has an invalid jutsubook-block defined!");
			jutsubookBlock = null;
		}

		defaultUses = getConfigInt("default-uses", -1);

		destroyBookcase = getConfigBoolean("destroy-when-used-up", false);

		strUsage = getConfigString("str-usage", "Usage: /cast jutsubook <jutsu> [uses]");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu by that name.");
		strLearned = getConfigString("str-learned", "You have learned the %s jutsu!");
		strNoTarget = getConfigString("str-no-target", "You must target a bookcase to create a jutsubook.");
		strCantTeach = getConfigString("str-cant-teach", "You can't create a jutsubook with that jutsu.");
		strCantLearn = getConfigString("str-cant-learn", "You cannot learn the jutsu in this jutsubook.");
		strLearnError = getConfigString("str-learn-error", "");
		strCantDestroy = getConfigString("str-cant-destroy", "You cannot destroy a bookcase with a jutsubook.");
		strHasJutsubook = getConfigString("str-has-jutsubook", "That bookcase already has a jutsubook.");
		strAlreadyKnown = getConfigString("str-already-known", "You already know the %s jutsu.");

		loadJutsubooks();
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length < 1 || args.length > 2 || (args.length == 2 && !RegexUtil.matches(PATTERN_CAST_ARG_USAGE, args[1]))) {
				sendMessage(strUsage, player, args);
				return PostCastAction.HANDLE_NORMALLY;
			}
			if (player.isOp() && args[0].equalsIgnoreCase("reload")) {
				bookLocations = new ArrayList<>();
				bookJutsus = new ArrayList<>();
				bookUses = new ArrayList<>();
				loadJutsubooks();
				player.sendMessage("Jutsubook file reloaded.");
				return PostCastAction.ALREADY_HANDLED;
			}

			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[0]);
			if (jutsubook == null || jutsu == null || !jutsubook.hasJutsu(jutsu)) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!MagicJutsus.getJutsubook(player).canTeach(jutsu)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Block target = getTargetedBlock(player, 10);
			if (target == null || !jutsubookBlock.equals(target.getType())) {
				sendMessage(strNoTarget, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (bookLocations.contains(new MagicLocation(target.getLocation()))) {
				sendMessage(strHasJutsubook, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			bookLocations.add(new MagicLocation(target.getLocation()));
			bookJutsus.add(jutsu.getInternalName());
			if (args.length == 1) bookUses.add(defaultUses);
			else bookUses.add(Integer.parseInt(args[1]));

			saveJutsubooks();
			sendMessage(formatMessage(strCastSelf, "%s", jutsu.getName()), player, args);
			playJutsuEffects(player, target.getLocation());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (sender.isOp() && args != null && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			bookLocations = new ArrayList<>();
			bookJutsus = new ArrayList<>();
			bookUses = new ArrayList<>();
			loadJutsubooks();
			sender.sendMessage("Jutsubook file reloaded.");
			return true;
		}
		return false;
	}
	
	private void removeJutsubook(int index) {
		bookLocations.remove(index);
		bookJutsus.remove(index);
		bookUses.remove(index);
		saveJutsubooks();
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (jutsubookBlock == null) return;
		if (!event.hasBlock() || !jutsubookBlock.equals(event.getClickedBlock().getType()) || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (event.getHand().equals(EquipmentSlot.OFF_HAND)) return;
		MagicLocation loc = new MagicLocation(event.getClickedBlock().getLocation());
		if (!bookLocations.contains(loc)) return;

		event.setCancelled(true);
		Player player = event.getPlayer();
		int i = bookLocations.indexOf(loc);
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(bookJutsus.get(i));
		if (jutsubook == null || jutsu == null) {
			sendMessage(strLearnError, player, MagicJutsus.NULL_ARGS);
			return;
		}
		if (!jutsubook.canLearn(jutsu)) {
			sendMessage(formatMessage(strCantLearn, "%s", jutsu.getName()), player, MagicJutsus.NULL_ARGS);
			return;
		}
		if (jutsubook.hasJutsu(jutsu)) {
			sendMessage(formatMessage(strAlreadyKnown, "%s", jutsu.getName()), player, MagicJutsus.NULL_ARGS);
			return;
		}
		JutsuLearnEvent learnEvent = new JutsuLearnEvent(jutsu, player, LearnSource.JUTSUBOOK, event.getClickedBlock());
		EventUtil.call(learnEvent);
		if (learnEvent.isCancelled()) {
			sendMessage(formatMessage(strCantLearn, "%s", jutsu.getName()), player, MagicJutsus.NULL_ARGS);
			return;
		}
		jutsubook.addJutsu(jutsu);
		jutsubook.save();
		sendMessage(formatMessage(strLearned, "%s", jutsu.getName()), player, MagicJutsus.NULL_ARGS);
		playJutsuEffects(EffectPosition.DELAYED, player);

		int uses = bookUses.get(i);
		if (uses <= 0) return;

		uses--;
		if (uses == 0) {
			if (destroyBookcase) bookLocations.get(i).getLocation().getBlock().setType(Material.AIR);
			removeJutsubook(i);
		} else bookUses.set(i, uses);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (jutsubookBlock == null || !jutsubookBlock.equals(event.getBlock().getType())) return;
		MagicLocation loc = new MagicLocation(event.getBlock().getLocation());
		if (!bookLocations.contains(loc)) return;
		Player pl = event.getPlayer();
		if (pl.isOp() || Perm.ADVANCEDJUTSUBOOK.has(pl)) {
			int i = bookLocations.indexOf(loc);
			removeJutsubook(i);
			return;
		}

		event.setCancelled(true);
		sendMessage(strCantDestroy, pl, MagicJutsus.NULL_ARGS);
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (sender instanceof Player && !partial.contains(" ")) return tabCompleteJutsuName(sender, partial);
		return null;
	}
	
	private void loadJutsubooks() {
		try {
			Scanner scanner = new Scanner(new File(MagicJutsus.plugin.getDataFolder(), "books.txt"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (line.isEmpty()) continue;
				try {
					String[] data = line.split(":");
					MagicLocation loc = new MagicLocation(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]));
					int uses = Integer.parseInt(data[5]);
					bookLocations.add(loc);
					bookJutsus.add(data[4]);
					bookUses.add(uses);
				} catch (Exception e) {
					MagicJutsus.plugin.getServer().getLogger().severe("MagicJutsus: Failed to load jutsubook: " + line);
				}
			}
		} catch (FileNotFoundException e) {
			//DebugHandler.debugFileNotFoundException(e);
		} 
	}
	
	private void saveJutsubooks() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MagicJutsus.plugin.getDataFolder(), "books.txt"), false));
			MagicLocation loc;
			for (int i = 0; i < bookLocations.size(); i++) {
				loc = bookLocations.get(i);
				writer.write(loc.getWorld() + ':' + (int) loc.getX() + ':' + (int) loc.getY() + ':' + (int) loc.getZ() + ':');
				writer.write(bookJutsus.get(i) + ':' + bookUses.get(i));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			MagicJutsus.plugin.getServer().getLogger().severe("MagicJutsus: Error saving jutsubooks");
		}
	}
	
}
