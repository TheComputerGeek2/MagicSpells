package com.nisovin.magicspells.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BossBarManager_V1_9 implements BossBarManager {
    
    Map<String, BossBar> bars = new HashMap<>();
    
    @Override
    public void setPlayerBar(final Player player, final String title, final double percent) {
        final BossBar bar = bars.computeIfAbsent(player.getName(), k -> Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', title), BarColor.PURPLE, BarStyle.SOLID));
        bar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
        bar.setProgress(percent);
        bar.addPlayer(player);
    }
    
    @Override
    public void removePlayerBar(final Player player) {
        final BossBar bar = bars.remove(player.getName());
        if(bar != null) {
            bar.removeAll();
        }
    }
    
    @Override
    public void turnOff() {
        for(final BossBar bar : bars.values()) {
            bar.removeAll();
        }
        bars.clear();
    }
}
