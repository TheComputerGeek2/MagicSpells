package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.spells.passive.PassiveListener;
import com.nisovin.magicspells.spells.passive.PassiveTrigger;
import com.nisovin.magicspells.util.OverridePriority;

public class MoveListener extends PassiveListener {

    List<PassiveSpell> spells = new ArrayList<>();

    @Override
    public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
        if (spell != null) spells.add(spell);
    }

    @OverridePriority
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer() == null) return;
        Location locto = event.getTo();
        Location locfrom = event.getFrom();
        Location playerloc = event.getPlayer().getLocation();

        if (locto.getX() != locfrom.getX() || locto.getY() != locfrom.getY() || locto.getZ() != locfrom.getZ()) {
            Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
            for (PassiveSpell spell : spells) {
                if (!isCancelStateOk(spell, event.isCancelled())) continue;
                if (spellbook.hasSpell(spell, false)) {
                    boolean casted = spell.activate(event.getPlayer());
                    if (PassiveListener.cancelDefaultAction(spell, casted)) {
                        event.setCancelled(true);
                        playerloc.setYaw(locto.getYaw());
                        playerloc.setPitch(locto.getPitch());
                    }
                }
            }
        }
    }

}
