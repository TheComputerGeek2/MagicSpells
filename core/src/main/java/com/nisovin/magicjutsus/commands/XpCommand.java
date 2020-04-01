package com.nisovin.magicjutsus.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.MagicXpHandler;

public class XpCommand implements CommandExecutor, TabCompleter {

    private MagicJutsus plugin;
    private boolean enableTabComplete;

    public XpCommand(MagicJutsus plugin, boolean enableTabComplete) {
        this.plugin = plugin;
        this.enableTabComplete = enableTabComplete;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!command.getName().equalsIgnoreCase("magicjutsuxp")) return false;
            if (!(sender instanceof Player)) return true;

            MagicXpHandler xpHandler = MagicJutsus.getMagicExpHandler();
            if (xpHandler != null) xpHandler.showXpInfo((Player) sender);

            return true;

        } catch (Exception ex) {
            MagicJutsus.handleException(ex);
            sender.sendMessage(ChatColor.RED + "An error has occured.");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!enableTabComplete || !(sender instanceof Player)) return null;

        Jutsubook jutsubook = MagicJutsus.getJutsubook((Player) sender);
        String partial = Util.arrayJoin(args, ' ');
        return jutsubook.tabComplete(partial);
    }

}
