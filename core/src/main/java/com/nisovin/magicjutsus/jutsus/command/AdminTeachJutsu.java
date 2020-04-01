package com.nisovin.magicjutsus.jutsus.command;

import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.JutsuUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;

// NOTE: THIS DOES NOT PERFORM MANY SAFETY CHECKS, IT IS MEANT TO BE FAST FOR ADMINS
// NOTE: THIS CURRENTLY ONLY CASTS FROM CONSOLE

public class AdminTeachJutsu extends CommandJutsu {
	
	public AdminTeachJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		// FIXME add more descriptive messages
		// Need at least name and one node
		if (args.length < 2) return false;
		Player targetPlayer = Bukkit.getPlayer(args[0]);
		if (targetPlayer == null) return false;
		
		Jutsubook jutsubook = MagicJutsus.getJutsubook(targetPlayer);
		if (jutsubook == null) return false;
		// No target, TODO add messages

		String[] nodes = Arrays.copyOfRange(args, 1, args.length);
		Set<String> nodeSet = new HashSet<>(Arrays.asList(nodes));
		
		new AdminTeachTask(sender, jutsubook, nodeSet).runTaskAsynchronously(MagicJutsus.plugin);
		
		// Format should be <target> <node> <node> <...>
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
	private static class AdminTeachTask extends BukkitRunnable {
		
		private final CommandSender sender;
		private final Jutsubook jutsubook;
		private final Set<String> nodeSet;
		
		private AdminTeachTask(CommandSender sender, Jutsubook jutsubook, Set<String> nodeSet) {
			this.sender = sender;
			this.jutsubook = jutsubook;
			this.nodeSet = nodeSet;
		}
		
		@Override
		public void run() {
			// TODO can the retrieval of Magicjutsus::jutsus be done async or does that need to be done sync?
			final Collection<Jutsu> jutsuCollection = JutsuUtil.getJutsusByPermissionNames(MagicJutsus.jutsus(), nodeSet);
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicJutsus.plugin, () -> {
				if (jutsubook.getJutsus() == null) {
					sender.sendMessage("Target jutsubook was destroyed before changes could be applied.");
					return;
				}
				Util.forEachOrdered(jutsuCollection, jutsubook::addJutsu);
				jutsubook.save();
				sender.sendMessage("Jutsu granting complete");
			});
		}
		
	}
	
}
