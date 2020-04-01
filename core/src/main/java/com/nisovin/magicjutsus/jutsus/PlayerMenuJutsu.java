package com.nisovin.magicjutsus.jutsus;

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

public class PlayerMenuJutsu extends TargetedJutsu implements TargetedEntityJutsu {

    private Map<UUID, Float> jutsuPower;

    private final int delay;
    private final String title;
    private final double radius;
    private final boolean stayOpen;
    private final boolean addOpener;
    private final String skullName;
    private final String skullNameOffline;
    private final String skullNameRadius;
    private final List<String> skullLore;
    private final String jutsuRangeName;
    private final String jutsuOfflineName;
    private final boolean castJutsusOnTarget;
    private final String jutsuOnLeftName;
    private final String jutsuOnRightName;
    private final String jutsuOnMiddleName;
    private final String jutsuOnSneakLeftName;
    private final String jutsuOnSneakRightName;
    private final List<String> playerModifiersStrings;
    private final String variableTarget;

    private Ninjutsu jutsuOffline;
    private Ninjutsu jutsuRange;
    private Ninjutsu jutsuOnLeft;
    private Ninjutsu jutsuOnRight;
    private Ninjutsu jutsuOnMiddle;
    private Ninjutsu jutsuOnSneakLeft;
    private Ninjutsu jutsuOnSneakRight;
    private ModifierSet playerModifiers;

    public PlayerMenuJutsu(MagicConfig config, String jutsuName) {
        super(config, jutsuName);
        delay = getConfigInt("delay", 0);
        title = getConfigString("title", "PlayerMenuJutsu '" + internalName + "'");
        radius = getConfigDouble("radius", 0);
        stayOpen = getConfigBoolean("stay-open", false);
        addOpener = getConfigBoolean("add-opener", false);
        skullName = getConfigString("skull-name", "&6%t");
        skullNameOffline = getConfigString("skull-name-offline", "&4%t");
        skullNameRadius = getConfigString("skull-name-radius", "&4%t &3out of radius.");
        skullLore = getConfigStringList("skull-lore", null);
        jutsuOfflineName = getConfigString("jutsu-offline", "");
        jutsuRangeName = getConfigString("jutsu-range", "");
        castJutsusOnTarget = getConfigBoolean("cast-jutsus-on-target", true);
        jutsuOnLeftName = getConfigString("jutsu-on-left", "");
        jutsuOnRightName = getConfigString("jutsu-on-right", "");
        jutsuOnMiddleName = getConfigString("jutsu-on-middle", "");
        jutsuOnSneakLeftName = getConfigString("jutsu-on-sneak-left", "");
        jutsuOnSneakRightName = getConfigString("jutsu-on-sneak-right", "");
        playerModifiersStrings = getConfigStringList("player-modifiers", null);
        variableTarget = getConfigString("variable-target", null);
    }

    @Override
    public void initialize() {
        super.initialize();
        jutsuPower = new HashMap<>();

        jutsuOffline = initNinjutsu(jutsuOfflineName, "PlayerMenuJutsu '" + internalName + "' has an invalid jutsu-offline defined!");
        jutsuRange = initNinjutsu(jutsuRangeName, "PlayerMenuJutsu '" + internalName + "' has an invalid jutsu-range defined!");
        jutsuOnLeft = initNinjutsu(jutsuOnLeftName, "PlayerMenuJutsu '" + internalName + "' has an invalid jutsu-on-left defined!");
        jutsuOnRight = initNinjutsu(jutsuOnRightName, "PlayerMenuJutsu '" + internalName + "' has an invalid jutsu-on-right defined!");
        jutsuOnMiddle = initNinjutsu(jutsuOnMiddleName, "PlayerMenuJutsu '" + internalName + "' has an invalid jutsu-on-middle defined!");
        jutsuOnSneakLeft = initNinjutsu(jutsuOnSneakLeftName, "PlayerMenuJutsu '" + internalName + "' has an invalid jutsu-on-sneak-left defined!");
        jutsuOnSneakRight = initNinjutsu(jutsuOnSneakRightName, "PlayerMenuJutsu '" + internalName + "' has an invalid jutsu-on-sneak-right defined!");

        if(playerModifiersStrings != null && !playerModifiersStrings.isEmpty()) playerModifiers = new ModifierSet(playerModifiersStrings);
    }

    @Override
    public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
        if(state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
            TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
            if(targetInfo == null) return noTarget(livingEntity);
            Player target = targetInfo.getTarget();
            if(target == null) return noTarget(livingEntity);
            openDelay(target, power);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

    @Override
    public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
        if(!(target instanceof Player)) return false;
        openDelay((Player) target, power);
        return true;
    }

    @Override
    public boolean castAtEntity(LivingEntity target, float power) {
        if(!(target instanceof Player)) return false;
        openDelay((Player) target, power);
        return true;
    }

    @Override
    public boolean castFromConsole(CommandSender sender, String[] args) {
        if(args.length < 1) return false;
        Player player = Bukkit.getPlayer(args[0]);
        if(player == null) return false;
        openDelay(player, 1);
        return true;
    }

    private void openDelay(Player opener, float power) {
        if(delay > 0) MagicJutsus.scheduleDelayedTask(() -> open(opener), delay);
        else open(opener);
        jutsuPower.put(opener.getUniqueId(), power);
    }

    private String translate(Player player, Player target, String string) {
        if(target != null) string = string.replaceAll("%t", target.getName());
        string = string.replaceAll("%a", player.getName());
        string = MagicJutsus.doVariableReplacements(player, string);
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    private void processClickJutsu(Ninjutsu ninjutsu, Player caster, Player target, float power) {
        if(ninjutsu == null) return;
        if(castJutsusOnTarget && ninjutsu.isTargetedEntityJutsu()) {
            ninjutsu.castAtEntity(caster, target, power);
            return;
        }
        ninjutsu.cast(caster, power);
    }

    private void open(Player opener) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if(!addOpener) players.remove(opener);
        if(playerModifiers != null) players.removeIf(player -> !playerModifiers.check(player));
        if(radius > 0) players.removeIf(player -> opener.getLocation().distance(player.getLocation()) > radius);

        double rows = players.size()/9;
        if(Math.round(rows*10%10) < 5) rows += .5;
        Inventory inv = Bukkit.createInventory(opener, Math.toIntExact(Math.round(rows) * 9), translate(opener, null, title));

        for(int i = 0; i < players.size(); i++) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta itemMeta = head.getItemMeta();
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            if(skullMeta == null) continue;
            skullMeta.setOwningPlayer(players.get(i));
            itemMeta.setDisplayName(translate(opener, players.get(i), skullName));
            if(skullLore != null) {
                List<String> lore = new ArrayList<>();
                for(String loreLine : skullLore) lore.add(translate(opener, players.get(i), loreLine));
                itemMeta.setLore(lore);
            }
            head.setItemMeta(skullMeta);
            inv.setItem(i, head);
        }
        opener.openInventory(inv);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        jutsuPower.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String currentTitle = ChatColor.stripColor(event.getView().getTitle());
        String newTitle = ChatColor.stripColor(translate(player, null, title));
        if(!currentTitle.equals(newTitle)) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if(item == null) return;
        ItemMeta itemMeta = item.getItemMeta();
        SkullMeta skullMeta = (SkullMeta) itemMeta;
        if(skullMeta == null) return;
        OfflinePlayer target = skullMeta.getOwningPlayer();
        float power = jutsuPower.containsKey(player.getUniqueId()) ?  jutsuPower.get(player.getUniqueId()) : 1;
        if(target == null || !target.isOnline()) {
            itemMeta.setDisplayName(translate(player, null, skullNameOffline));
            if(jutsuOffline != null) jutsuOffline.cast(player, power);
            if(stayOpen) item.setItemMeta(itemMeta);
            else {
                player.closeInventory();
                jutsuPower.remove(player.getUniqueId());
            }
            return;
        }
        else {
            itemMeta.setDisplayName(translate(player, (Player) target, skullName));
            item.setItemMeta(itemMeta);
        }
        Player targetPlayer = (Player) target;
        if(radius > 0  && targetPlayer.getLocation().distance(player.getLocation()) > radius) {
            itemMeta.setDisplayName(translate(player, targetPlayer, skullNameRadius));
            if(jutsuRange != null) jutsuRange.cast(player, power);
            if(stayOpen) item.setItemMeta(itemMeta);
            else {
                player.closeInventory();
                jutsuPower.remove(player.getUniqueId());
            }
            return;
        }
        switch(event.getClick()) {
            case LEFT: processClickJutsu(jutsuOnLeft, player, targetPlayer, power); break;
            case RIGHT: processClickJutsu(jutsuOnRight, player, targetPlayer, power); break;
            case MIDDLE: processClickJutsu(jutsuOnMiddle, player, targetPlayer, power); break;
            case SHIFT_LEFT: processClickJutsu(jutsuOnSneakLeft, player, targetPlayer, power); break;
            case SHIFT_RIGHT: processClickJutsu(jutsuOnSneakRight, player, targetPlayer, power); break;
        }
        if(variableTarget != null && !variableTarget.isEmpty() && MagicJutsus.getVariableManager().getVariable(variableTarget) != null) {
            MagicJutsus.getVariableManager().set(variableTarget, player, target.getName());
        }
        if(stayOpen) openDelay(player, power);
        else {
            player.closeInventory();
            jutsuPower.remove(player.getUniqueId());
        }
    }
}

