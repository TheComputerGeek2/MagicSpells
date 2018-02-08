package com.nisovin.magicspells.spells.buff;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class WindglideSpell extends BuffSpell {

    Set<UUID> gliders;

    Subspell glideSpell;
    Subspell collisionSpell;
    String glideSpellName;
    String collisionSpellName;

    boolean cancelOnCollision;
    boolean blockCollisionDmg;

    float velocity;
    float height;
    int interval;

    private GlideMonitor monitor;

    public WindglideSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        glideSpellName = getConfigString("spell", "");
        collisionSpellName = getConfigString("collision-spell", "");

        blockCollisionDmg = getConfigBoolean("block-collision-dmg", true);
        cancelOnCollision = getConfigBoolean("cancel-on-collision", false);

        velocity = getConfigFloat("velocity", 20F);
        height = getConfigFloat("height", 0F);
        interval = getConfigInt("interval", 4);
        if (interval <= 0) interval = 4;

        gliders = new HashSet<>();
        monitor = new GlideMonitor();
    }

    @Override
    public void initialize() {
        super.initialize();

        Subspell s = new Subspell(glideSpellName);

        if (s.process()) {
            glideSpell = s;
        } else {
            if (!glideSpellName.equals("")) MagicSpells.error("WindglideSpell " + internalName + " has an invalid spell defined");
        }

        Subspell s2 = new Subspell(collisionSpellName);

        if (s2.process()) {
            collisionSpell = s2;
        } else {
            if (!collisionSpellName.equals("")) MagicSpells.error("WindglideSpell " + internalName + " has an invalid collision-spell defined");
        }

    }

    @Override
    public boolean castBuff(Player player, float power, String[] args) {
        gliders.add(player.getUniqueId());
        player.setGliding(true);
        return true;
    }

    @Override
    public boolean isActive(Player player) {
        return gliders.contains(player.getUniqueId());
    }

    @Override
    public void turnOffBuff(Player player) {
        gliders.remove(player.getUniqueId());
        player.setGliding(false);
    }

    @Override
    protected void turnOff() {
        for (EffectPosition pos: EffectPosition.values()) {
            cancelEffectForAllPlayers(pos);
        }

        for (UUID id : gliders) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null && pl.isValid()) {
                pl.setGliding(false);
                turnOffBuff(pl);
            }
        }

        gliders.clear();
    }

    @EventHandler
    public void onPlayerGlide(EntityToggleGlideEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) return;
        Player pl = (Player)entity;
        if (!gliders.contains(pl.getUniqueId())) return;
        if (pl.isGliding()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCollision(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) return;
        if (!(e.getEntity() instanceof Player)) return;
        Player pl = (Player)e.getEntity();
        if (!gliders.contains(pl.getUniqueId())) return;

        if (blockCollisionDmg) e.setCancelled(true);
        if (cancelOnCollision) turnOffBuff(pl);
        if (collisionSpell != null && collisionSpell.isTargetedLocationSpell()) collisionSpell.castAtLocation(pl, pl.getLocation(), 1);
    }

    public class GlideMonitor implements Runnable {

        int taskId;

        public GlideMonitor() {
            this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin,this, interval, interval);
        }

        @Override
        public void run() {
            for (UUID id : gliders) {
                Player pl = Bukkit.getPlayer(id);
                if (pl == null || !pl.isValid()) continue;

                Location pLoc = pl.getLocation();
                Vector v = pLoc.getDirection().normalize().multiply(velocity).add(new Vector(0,height,0));
                pl.setVelocity(v);

                if (glideSpell != null && glideSpell.isTargetedLocationSpell()) glideSpell.castAtLocation(pl, pLoc, 1);
                playSpellEffects(EffectPosition.SPECIAL, pLoc);
                addUseAndChargeCost(pl);
            }
        }
    }

}
