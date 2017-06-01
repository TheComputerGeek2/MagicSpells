package com.nisovin.magicspells.util;

import org.bukkit.entity.Player;

public interface BossBarManager {
    
    void setPlayerBar(Player player, String title, double percent);
    
    void removePlayerBar(Player player);
    
    void turnOff();
}
