package com.nisovin.magicspells.util;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;

public class BossBarManager_V1_8 implements BossBarManager, Listener {
    
    Map<String, String> bossBarTitles = new HashMap<>();
    Map<String, Double> bossBarValues = new HashMap<>();
    
    public BossBarManager_V1_8() {
        MagicSpells.registerEvents(this);
        
        MagicSpells.scheduleRepeatingTask(() -> {
            for(final String name : bossBarTitles.keySet()) {
                final Player player = Bukkit.getPlayerExact(name);
                if(player != null) {
                    updateBar(player, null, 0);
                }
            }
        }, 8, 8);
    }
    
    @Override
    public void setPlayerBar(final Player player, final String title, final double percent) {
        final boolean alreadyShowing = bossBarTitles.containsKey(player.getName());
        bossBarTitles.put(player.getName(), title);
        bossBarValues.put(player.getName(), percent);
        
        if(alreadyShowing) {
            updateBar(player, title, percent);
        } else {
            showBar(player);
        }
    }
    
    @Override
    public void removePlayerBar(final Player player) {
        if(bossBarTitles.remove(player.getName()) != null) {
            bossBarValues.remove(player.getName());
            MagicSpells.getVolatileCodeHandler().removeBossBar(player);
        }
    }
    
    private void showBar(final Player player) {
        try {
            if(player != null && player.isValid()) {
                MagicSpells.getVolatileCodeHandler().setBossBar(player, bossBarTitles.get(player.getName()), bossBarValues.get(player.getName()));
            }
        } catch(final Exception e) {
            System.out.println("BOSS BAR EXCEPTION: " + e.getMessage());
        }
    }
    
    private void updateBar(final Player player, final String title, final double val) {
        MagicSpells.getVolatileCodeHandler().updateBossBar(player, title, val);
    }
    
    @EventHandler
    public void onRespawn(final PlayerRespawnEvent event) {
        if(bossBarTitles.containsKey(event.getPlayer().getName())) {
            MagicSpells.scheduleDelayedTask(() -> showBar(event.getPlayer()), 10);
        }
    }
    
    @EventHandler
    public void onTeleport(final PlayerTeleportEvent event) {
        if(bossBarTitles.containsKey(event.getPlayer().getName())) {
            MagicSpells.scheduleDelayedTask(() -> showBar(event.getPlayer()), 10);
        }
    }
    
    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        bossBarTitles.remove(event.getPlayer().getName());
        bossBarValues.remove(event.getPlayer().getName());
    }
    
    @Override
    public void turnOff() {
        for(final Player player : Bukkit.getOnlinePlayers()) {
            if(bossBarTitles.containsKey(player.getName())) {
                MagicSpells.getVolatileCodeHandler().removeBossBar(player);
            }
        }
        bossBarTitles.clear();
        bossBarValues.clear();
    }
}
