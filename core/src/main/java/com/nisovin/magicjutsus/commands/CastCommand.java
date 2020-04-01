package com.nisovin.magicjutsus.commands;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.command.Command;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.command.BlockCommandSender;

import com.nisovin.magicjutsus.Perm;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.DebugHandler;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.variables.Variable;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.mana.ManaChangeReason;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsus.TargetedLocationJutsu;
import com.nisovin.magicjutsus.variables.PlayerStringVariable;

public class CastCommand implements CommandExecutor, TabCompleter {

    private static final Pattern LOOSE_PLAYER_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("^[^,]+,-?[0-9.]+,-?[0-9.]+,-?[0-9.]+(,-?[0-9.]+,-?[0-9.]+)?$");

    private MagicJutsus plugin;
    private boolean enableTabComplete;

    public CastCommand(MagicJutsus plugin, boolean enableTabComplete) {
        this.plugin = plugin;
        this.enableTabComplete = enableTabComplete;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!command.getName().equalsIgnoreCase("magicjutsucast")) return false;
            args = Util.splitParams(args);

            // command with no arguments
            if (args == null || args.length == 0) {
                if (sender instanceof Player) MagicJutsus.sendMessage(MagicJutsus.getStrCastUsage(), (Player) sender, MagicJutsus.NULL_ARGS);
                else sender.sendMessage(MagicJutsus.getTextColor() + MagicJutsus.getStrCastUsage());
                return true;
            }

            // forcecast
            if (Perm.FORCECAST.has(sender) && args[0].equals("forcecast")) {
                if (args.length < 3) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "The correct syntax is /c forcecast <playerName/mobUUID> <jutsuInGameName> [jutsuArgs]");
                    return true;
                }

                Player target = PlayerNameUtils.getPlayer(args[1]);
                Entity targetEntity = null;

                // if player is null, check for entity uuid
                if (target == null) {
                    targetEntity = Bukkit.getEntity(UUID.fromString(args[1]));
                    if (targetEntity == null || !(targetEntity instanceof LivingEntity)) {
                        sender.sendMessage(MagicJutsus.getTextColor() + "No matching player or living entity found!");
                        return true;
                    }
                }

                Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[2]);
                // jutsu not found
                if (jutsu == null) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "Jutsu with that in-game name doesn't exist!");
                    return true;
                }

                String[] jutsuArgs = null;
                if (args.length > 3) jutsuArgs = Arrays.copyOfRange(args, 3, args.length);
                if (target != null) {
                    jutsu.cast(target, jutsuArgs);
                    if (MagicJutsus.isDebug()) sender.sendMessage(MagicJutsus.getTextColor() + "Player " + target.getName() + " forced to cast " + jutsu.getName());
                } else if (targetEntity != null) {
                    jutsu.cast((LivingEntity) targetEntity, jutsuArgs);
                    if (MagicJutsus.isDebug()) sender.sendMessage(MagicJutsus.getTextColor() + "LivingEntity " + targetEntity.getName() + " forced to cast " + jutsu.getName());
                }

                return true;
            }

            // castat
            if (Perm.CAST_AT.has(sender) && args[0].equals("castat")) {
                if (args.length < 3) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "The correct syntax is /c castat <playerName/mobUUID> <jutsuInGameName> [power]");
                    return true;
                }

                // castat <playerName/mobUUID> <jutsuInGameName> [power]
                Player target = Bukkit.getServer().getPlayer(args[1]);
                Entity targetEntity = null;

                // if player is null, check for entity uuid
                if (target == null) {
                    targetEntity = Bukkit.getEntity(UUID.fromString(args[1]));
                    if (targetEntity == null || !(targetEntity instanceof LivingEntity)) {
                        sender.sendMessage(MagicJutsus.getTextColor() + "No matching player or living entity found!");
                        return true;
                    }
                }

                Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[2]);
                TargetedEntityJutsu tes = null;
                TargetedLocationJutsu tls = null;

                if (jutsu instanceof TargetedEntityJutsu) tes = (TargetedEntityJutsu) jutsu;
                else if (jutsu instanceof TargetedLocationJutsu) tls = (TargetedLocationJutsu) jutsu;
                else {
                    sender.sendMessage(MagicJutsus.getTextColor() + "You did not specify a targeted entity or targeted location jutsu");
                    return true;
                }

                float cPower = 1F;
                if (args.length == 4) cPower = Float.parseFloat(args[3]);

                if (tes != null) {
                    if (target != null) tes.castAtEntity(target, cPower);
                    else if (targetEntity != null) tes.castAtEntity((LivingEntity) targetEntity, cPower);
                } else if (tls != null) {
                    if (target != null) tls.castAtLocation(target.getLocation(), cPower);
                    else if (targetEntity != null) tls.castAtLocation(targetEntity.getLocation(), cPower);
                }

                return true;
            }

            // reload
            if (Perm.RELOAD.has(sender) && args[0].equals("reload")) {
                // reload the plugin if no player name is specified
                if (args.length == 1) {
                    plugin.unload();
                    plugin.load();
                    sender.sendMessage(MagicJutsus.getTextColor() + "MagicJutsus config reloaded.");
                    return true;
                }

                // reload player's jutsubook
                List<Player> players = plugin.getServer().matchPlayer(args[1]);
                if (players.size() != 1) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "Player not found.");
                    return true;
                }

                Player player = players.get(0);
                MagicJutsus.getJutsubooks().put(player.getName(), new Jutsubook(player, plugin));
                sender.sendMessage(MagicJutsus.getTextColor() + player.getName() + "'s jutsubook reloaded.");

                return true;
            }

            // resetcd
            if (Perm.RESET_COOLDOWN.has(sender) && args[0].equals("resetcd")) {
                Player p = null;
                if (args.length > 1) {
                    // check for player name
                    p = PlayerNameUtils.getPlayer(args[1]);
                    if (p == null) {
                        sender.sendMessage(MagicJutsus.getTextColor() + "No matching player found");
                        return true;
                    }
                }

                for (Jutsu jutsu : MagicJutsus.getJutsus().values()) {
                    if (p != null) jutsu.setCooldown(p, 0);
                    else jutsu.getCooldowns().clear();
                }
                sender.sendMessage(MagicJutsus.getTextColor() + "Cooldowns reset" + (p != null ? " for " + p.getName() : ""));

                return true;
            }

            // resetmana
            if (Perm.RESET_MANA.has(sender) && args[0].equals("resetmana") && args.length > 1 && MagicJutsus.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                MagicJutsus.getManaHandler().createManaBar(p);
                sender.sendMessage(MagicJutsus.getTextColor() + p.getName() + "'s mana reset.");

                return true;
            }

            // updatemanarank
            if (Perm.UPDATE_MANA_RANK.has(sender) && args[0].equals("updatemanarank") && args.length > 1 && MagicJutsus.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                boolean updated = MagicJutsus.getManaHandler().updateManaRankIfNecessary(p);
                MagicJutsus.getManaHandler().showMana(p);

                if (updated) sender.sendMessage(MagicJutsus.getTextColor() + p.getName() + "'s mana rank updated.");
                else sender.sendMessage(MagicJutsus.getTextColor() + p.getName() + "'s mana rank already correct.");

                return true;
            }

            // setmaxmana
            if (Perm.SET_MAX_MANA.has(sender) && args[0].equalsIgnoreCase("setmaxmana") && args.length == 3 && MagicJutsus.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                int amt = Integer.parseInt(args[2]);
                MagicJutsus.getManaHandler().setMaxMana(p, amt);
                sender.sendMessage(MagicJutsus.getTextColor() + p.getName() + "'s max mana set to " + amt + '.');

                return true;
            }

            // modifymana
            if (Perm.MODIFY_MANA.has(sender) && args[0].equalsIgnoreCase("modifymana") && args.length == 3 && MagicJutsus.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                int amt = Integer.parseInt(args[2]);
                MagicJutsus.getManaHandler().addMana(p, amt, ManaChangeReason.OTHER);
                sender.sendMessage(MagicJutsus.getTextColor() + p.getName() + "'s mana modified by " + amt + '.');

                return true;
            }

            // setmana
            if (Perm.SET_MANA.has(sender) && args[0].equalsIgnoreCase("setmana") && args.length == 3 && MagicJutsus.getManaHandler() != null) {
                Player p = PlayerNameUtils.getPlayer(args[1]);
                if (p == null) return true;

                int amt = Integer.parseInt(args[2]);
                MagicJutsus.getManaHandler().setMana(p, amt, ManaChangeReason.OTHER);
                sender.sendMessage(MagicJutsus.getTextColor() + p.getName() + "'s mana set to " + amt + '.');

                return true;
            }

            if (Perm.SHOW_VARIABLE.has(sender) && args[0].equals("showvariable")) {
                if (args.length != 3) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "The correct syntax is /c showvariable <variable> <player>");
                    return true;
                }

                String var = args[1];
                String player = args[2];
                sender.sendMessage(MagicJutsus.getTextColor() + MagicJutsus.getVariableManager().getStringValue(var, player));

                return true;
            }

            // modifyvariable <variable> <player> (+|-|*|/|=)<value>
            if (Perm.MODIFY_VARIABLE.has(sender) && args[0].equals("modifyvariable")) {
                if (args.length != 4) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "The correct syntax is /c modifyvariable <variable> <player> <[operator]value>");
                    return true;
                }

                String var = args[1];
                String player = args[2];
                boolean set = false;
                boolean multiply = false;
                boolean divide = false;
                double num = 0;
                String numString = args[3];
                String valueString = args[3];
                Variable variable = MagicJutsus.getVariableManager().getVariable(var);

                // Possible operations + - = * /

                if (numString.startsWith("*")) {
                    multiply = true;
                    numString = numString.substring(1);
                } else if (numString.startsWith("/")) {
                    divide = true;
                    numString = numString.substring(1);
                } else if (numString.startsWith("=")) {
                    set = true;
                    numString = numString.substring(1);
                    valueString = valueString.substring(1);
                } else if (numString.startsWith("+")) {
                    numString = numString.substring(1);
                }

                if (!RegexUtil.matches(RegexUtil.DOUBLE_PATTERN, numString)) {
                    boolean negate = false;
                    if (numString.startsWith("-")) {
                        negate = true;
                        numString = numString.substring(1);
                    }
                    String targetPlayerName = player;
                    if (numString.contains(":")) {
                        String[] targetVarData = numString.split(":");
                        targetPlayerName = targetVarData[0];
                        numString = targetVarData[1];
                    }
                    num = MagicJutsus.getVariableManager().getValue(numString, PlayerNameUtils.getPlayer(targetPlayerName));
                    if (negate) num *= -1;
                } else num = Double.parseDouble(numString);

                if (multiply) MagicJutsus.getVariableManager().multiplyBy(var, PlayerNameUtils.getPlayer(player), num);
                else if (divide) MagicJutsus.getVariableManager().divideBy(var, PlayerNameUtils.getPlayer(player), num);
                else if (set) {
                    if (variable instanceof PlayerStringVariable) MagicJutsus.getVariableManager().set(var, player, valueString);
                    else MagicJutsus.getVariableManager().set(var, player, num);
                }
                else MagicJutsus.getVariableManager().modify(var, player, num);

                return true;
            }

            // magicitem
            if (Perm.MAGICITEM.has(sender) && args[0].equals("magicitem")) {
                if (args.length < 3) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "The correct syntax is /c magicitem <playerName> <itemName> [amount]");
                    return true;
                }

                Player target = Bukkit.getServer().getPlayer(args[1]);
                if (target == null || !(target instanceof InventoryHolder)) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "Player not found.");
                    return true;
                }

                ItemStack item = Util.getItemStackFromString(args[2]);
                if (item == null) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "Item not found.");
                    return true;
                }

                // set item amount
                if (args.length > 3 && RegexUtil.matches(RegexUtil.SIMPLE_INT_PATTERN, args[3])) item.setAmount(Integer.parseInt(args[3]));
                target.getInventory().addItem(item);

                return true;
            }

            // download
            if (Perm.DOWNLOAD.has(sender) && args[0].equals("download")) {
                if (args.length != 3) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "The correct syntax is /c download <fileName> <downloadLink>");
                    return true;
                }

                File file = new File(plugin.getDataFolder(), "jutsus-" + args[1] + ".yml");
                if (file.exists()) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "ERROR: The file jutsus-" + args[1] + ".yml already exists!");
                    return true;
                }

                boolean downloaded = Util.downloadFile(args[2], file);
                if (downloaded) sender.sendMessage(MagicJutsus.getTextColor() + "SUCCESS! You will need to do a /cast reload to load the new jutsus.");
                else sender.sendMessage(MagicJutsus.getTextColor() + "ERROR: The file could not be downloaded.");

                return true;
            }

            // update
            if (Perm.UPDATE.has(sender) && args[0].equals("update")) {
                if (args.length != 3) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "The correct syntax is /c update <fileName> <downloadLink>");
                    return true;
                }

                File file = new File(plugin.getDataFolder(), "update-" + args[1] + ".yml");
                boolean downloaded = Util.downloadFile(args[2], file);
                boolean abort = false;
                if (!downloaded) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "Update file failed to download.");
                    return true;
                }

                sender.sendMessage(MagicJutsus.getTextColor() + "Update file successfully downloaded.");

                // Delete the existing file
                File old = new File(plugin.getDataFolder(), args[1] + ".yml");
                if (old.exists()) {
                    boolean deleteSuccess = old.delete();
                    if (deleteSuccess) sender.sendMessage(MagicJutsus.getTextColor() + "Old file successfully deleted.");
                    else {
                        sender.sendMessage(MagicJutsus.getTextColor() + "Old file could not be deleted.");
                        sender.sendMessage(MagicJutsus.getTextColor() + "Aborting update, please delete the update file: " + file.getName());
                        abort = true;
                    }
                } else sender.sendMessage(MagicJutsus.getTextColor() + "There was no old file to delete.");

                if (!abort) {
                    // Rename the update to the original file's name
                    boolean renamingSuccess = file.renameTo(new File(plugin.getDataFolder(), args[1] + ".yml"));
                    if (renamingSuccess) {
                        sender.sendMessage(MagicJutsus.getTextColor() + "Successfully renamed the update file to " + args[1] + ".yml");
                        sender.sendMessage(MagicJutsus.getTextColor() + "You will need to do a /cast reload to load the update.");
                    } else sender.sendMessage(MagicJutsus.getTextColor() + "Failed to rename the update file, update failed");
                }

                return true;
            }

            // saveskin
            if (Perm.SAVESKIN.has(sender) && args[0].equals("saveskin") && args.length == 3) {
                Player player = PlayerNameUtils.getPlayerExact(args[1]);
                if (player == null) return true;

                MagicJutsus.getVolatileCodeHandler().saveSkinData(player, args[2]);
                sender.sendMessage(MagicJutsus.getTextColor() + "Skin data for player " + player.getName() + " saved as " + args[2]);

                return true;
            }

            // profilereport
            if (Perm.PROFILE.has(sender) && args[0].equals("profilereport")) {
                sender.sendMessage(MagicJutsus.getTextColor() + "Creating profiling report");
                MagicJutsus.profilingReport();

                return true;
            }

            // debug
            if (Perm.DEBUG.has(sender) && args[0].equals("debug")) {
                MagicJutsus.setDebug(!MagicJutsus.isDebug());
                sender.sendMessage(MagicJutsus.getTextColor() + "[MagicJutsus]: Debug - " + (MagicJutsus.isDebug() ? "enabled" : "disabled"));

                return true;
            }

            // cast jutsu
            if (sender instanceof LivingEntity) {
                LivingEntity caster = (LivingEntity) sender;
                Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[0]);

                // if caster is a player
                if (caster instanceof Player) {
                    Jutsubook jutsubook = MagicJutsus.getJutsubook((Player) caster);
                    if (jutsu != null && (!jutsu.isHelperJutsu() || caster.isOp()) && jutsu.canCastByCommand() && jutsubook.hasJutsu(jutsu)) {
                        if (!jutsu.isValidItemForCastCommand(caster.getEquipment().getItemInMainHand())) {
                            MagicJutsus.sendMessage(jutsu.getStrWrongCastItem(), caster, null);
                            return true;
                        }

                        String[] jutsuArgs = null;
                        if (args.length > 1) {
                            jutsuArgs = new String[args.length - 1];
                            System.arraycopy(args, 1, jutsuArgs, 0, args.length - 1);
                        }
                        jutsu.cast(caster, jutsuArgs);

                        return true;
                    }
                    MagicJutsus.sendMessage(MagicJutsus.getStrUnknownJutsu(), caster, null);

                    return true;
                }

                // if caster is a living entity
                if (jutsu != null && (!jutsu.isHelperJutsu() || caster.isOp()) && jutsu.canCastByCommand()) {
                    if (!jutsu.isValidItemForCastCommand(caster.getEquipment().getItemInMainHand())) {
                        MagicJutsus.sendMessage(jutsu.getStrWrongCastItem(), caster, null);
                        return true;
                    }

                    String[] jutsuArgs = null;
                    if (args.length > 1) {
                        jutsuArgs = new String[args.length - 1];
                        System.arraycopy(args, 1, jutsuArgs, 0, args.length - 1);
                    }
                    jutsu.cast(caster, jutsuArgs);

                    return true;
                }

                MagicJutsus.sendMessage(MagicJutsus.getStrUnknownJutsu(), caster, null);

                return true;
            }

            // invalid jutsu
            Jutsu jutsu = MagicJutsus.getJutsuNames().get(args[0].toLowerCase());
            if (jutsu == null) {
                sender.sendMessage(MagicJutsus.getTextColor() + "Unknown jutsu.");
                return true;
            }

            String[] jutsuArgs = null;
            if (args.length > 1) {
                jutsuArgs = new String[args.length - 1];
                System.arraycopy(args, 1, jutsuArgs, 0, args.length - 1);
            }

            boolean casted = false;
            if (sender instanceof BlockCommandSender && jutsu instanceof TargetedLocationJutsu) {
                Location loc = ((BlockCommandSender) sender).getBlock().getLocation().add(0.5, 0.5, 0.5);
                if (jutsuArgs != null && jutsuArgs.length >= 3) {
                    try {
                        int x = Integer.parseInt(jutsuArgs[0]);
                        int y = Integer.parseInt(jutsuArgs[1]);
                        int z = Integer.parseInt(jutsuArgs[2]);
                        float yaw = 0;
                        float pitch = 0;
                        if (jutsuArgs.length > 3) yaw = Float.parseFloat(jutsuArgs[3]);
                        if (jutsuArgs.length > 4) pitch = Float.parseFloat(jutsuArgs[4]);
                        loc.add(x, y, z);
                        loc.setYaw(yaw);
                        loc.setPitch(pitch);
                    } catch (NumberFormatException e) {
                        DebugHandler.debugNumberFormat(e);
                    }
                }
                ((TargetedLocationJutsu) jutsu).castAtLocation(loc, 1.0F);
                casted = true;
            }

            if (casted) return true;

            // On to trying to handle as a non player entity, only for targeted location jutsus
            if (sender instanceof Entity && jutsu instanceof TargetedLocationJutsu) {
                Entity senderEntity = (Entity) sender;
                Location loc = senderEntity.getLocation();
                if (jutsuArgs != null && jutsuArgs.length >= 3) {
                    try {
                        int x = Integer.parseInt(jutsuArgs[0]);
                        int y = Integer.parseInt(jutsuArgs[1]);
                        int z = Integer.parseInt(jutsuArgs[2]);
                        float yaw = 0;
                        float pitch = 0;
                        if (jutsuArgs.length > 3) yaw = Float.parseFloat(jutsuArgs[3]);
                        if (jutsuArgs.length > 4) pitch = Float.parseFloat(jutsuArgs[4]);
                        loc.add(x, y, z);
                        loc.setYaw(yaw);
                        loc.setPitch(pitch);
                    } catch (NumberFormatException e) {
                        DebugHandler.debugNumberFormat(e);
                    }
                }
                ((TargetedLocationJutsu) jutsu).castAtLocation(loc, 1.0F);
                casted = true;
            }

            if (casted) return true;

            boolean ok = jutsu.castFromConsole(sender, jutsuArgs);
            if (!ok) return true;

            if ((jutsu instanceof TargetedEntityJutsu || jutsu instanceof TargetedLocationJutsu) && jutsuArgs != null && jutsuArgs.length == 1 && RegexUtil.matches(LOOSE_PLAYER_NAME_PATTERN, jutsuArgs[0])) {
                Player target = PlayerNameUtils.getPlayer(jutsuArgs[0]);
                if (target == null) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "Invalid target.");
                    return true;
                }

                if (jutsu instanceof TargetedEntityJutsu) ok = ((TargetedEntityJutsu) jutsu).castAtEntity(target, 1.0F);
                else if (jutsu instanceof TargetedLocationJutsu) ok = ((TargetedLocationJutsu) jutsu).castAtLocation(target.getLocation(), 1.0F);

                if (ok) sender.sendMessage(MagicJutsus.getTextColor() + "Jutsu casted!");
                else sender.sendMessage(MagicJutsus.getTextColor() + "Jutsu failed, probably can't be cast from console.");

                return true;
            }

            if (jutsu instanceof TargetedLocationJutsu && jutsuArgs != null && jutsuArgs.length == 1 && RegexUtil.matches(LOCATION_PATTERN, jutsuArgs[0])) {
                String[] locData = jutsuArgs[0].split(",");
                World world = Bukkit.getWorld(locData[0]);
                if (world == null) {
                    sender.sendMessage(MagicJutsus.getTextColor() + "No such world.");
                    return true;
                }

                Location loc = new Location(world, Float.parseFloat(locData[1]), Float.parseFloat(locData[2]), Float.parseFloat(locData[3]));
                if (locData.length > 4) loc.setYaw(Float.parseFloat(locData[4]));
                if (locData.length > 5) loc.setPitch(Float.parseFloat(locData[5]));
                ok = ((TargetedLocationJutsu) jutsu).castAtLocation(loc, 1.0F);

                if (ok) sender.sendMessage(MagicJutsus.getTextColor() + "Jutsu casted!");
                else sender.sendMessage(MagicJutsus.getTextColor() + "Jutsu failed, probably can't be cast from console.");

                return true;
            }

            sender.sendMessage(MagicJutsus.getTextColor() + "Cannot cast that jutsu from console.");

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
