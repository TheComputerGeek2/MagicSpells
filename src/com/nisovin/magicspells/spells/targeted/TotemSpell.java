package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.LocationUtil;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class TotemSpell extends TargetedSpell implements TargetedLocationSpell {

    Set<Totem> totems;
    private PulserTicker ticker;

    private int yOffset;
    private int totalPulses;
    private int interval;
    private int capPerPlayer;
    private double maxDistanceSquared;
    private boolean onlyCountOnSuccess;
    private boolean gravity;
    private boolean visibility;
    private boolean targetable;
    private String strAtCap;
    private String totemName;

    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack hand;
    private ItemStack mainHand;

    private List<String> spellNames;
    List<TargetedLocationSpell> spells;
    private String spellNameOnBreak;
    TargetedLocationSpell spellOnBreak;

    public TotemSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        helmet = Util.getItemStackFromString(getConfigString("helmet", "0"));
        chestplate = Util.getItemStackFromString(getConfigString("chestplate", "0"));
        leggings = Util.getItemStackFromString(getConfigString("leggings", "0"));
        boots = Util.getItemStackFromString(getConfigString("boots", "0"));
        hand = Util.getItemStackFromString(getConfigString("hand", "0"));
        mainHand = Util.getItemStackFromString(getConfigString("main-hand", "0"));

        if (helmet != null && helmet.getType() != Material.AIR) helmet.setAmount(1);
        if (chestplate != null && chestplate.getType() != Material.AIR) chestplate.setAmount(1);
        if (leggings != null && leggings.getType() != Material.AIR) leggings.setAmount(1);
        if (boots != null && boots.getType() != Material.AIR) boots.setAmount(1);
        if (hand != null && hand.getType() != Material.AIR) hand.setAmount(1);
        if (mainHand != null && mainHand.getType() != Material.AIR) mainHand.setAmount(1);

        gravity = getConfigBoolean("gravity", false);
        yOffset = getConfigInt("y-offset", 0);
        totalPulses = getConfigInt("total-pulses", 5);
        interval = getConfigInt("interval", 30);
        capPerPlayer = getConfigInt("cap-per-player", 10);
        maxDistanceSquared = getConfigDouble("max-distance", 30);
        maxDistanceSquared *= maxDistanceSquared;
        onlyCountOnSuccess = getConfigBoolean("only-count-on-success", false);
        spellNames = getConfigStringList("spells", null);
        spellNameOnBreak = getConfigString("spell-on-break", null);
        visibility = getConfigBoolean("visible", true);
        targetable = getConfigBoolean("targetable", true);
        totemName = getConfigString("totem-name", null);

        strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");

        totems = new HashSet<>();
        ticker = new PulserTicker();
    }

    @Override
    public void initialize() {
        super.initialize();
        spells = new ArrayList<>();
        if (spellNames != null && !spellNames.isEmpty()) {
            for (String spellName : spellNames) {
                Spell spell = MagicSpells.getSpellByInternalName(spellName);
                if (spell instanceof TargetedLocationSpell) {
                    spells.add((TargetedLocationSpell) spell);
                }
            }
        }
        if (spellNameOnBreak != null) {
            Spell spell = MagicSpells.getSpellByInternalName(spellNameOnBreak);
            if (spell instanceof TargetedLocationSpell) {
                spellOnBreak = (TargetedLocationSpell)spell;
            } else {
                MagicSpells.error("Totem spell '" + internalName + "' has an invalid spell-on-break spell defined");
            }
        }
        if (spells.isEmpty()) {
            MagicSpells.error("Totem spell '" + internalName + "' has no spells defined!");
        }
    }

    @Override
    public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
        if (state == SpellCastState.NORMAL) {
            if (capPerPlayer > 0) {
                int count = 0;
                for (Totem pulser : totems) {
                    if (!pulser.caster.equals(player)) continue;

                    count++;
                    if (count >= capPerPlayer) {
                        sendMessage(strAtCap, player, args);
                        return PostCastAction.ALREADY_HANDLED;
                    }
                }
            }

            List<Block> lastTwo = getLastTwoTargetedBlocks(player, power);
            Block target = null;
            if (lastTwo != null && lastTwo.size() == 2) target = lastTwo.get(0);
            if (target == null) return noTarget(player);
            if (yOffset > 0) {
                target = target.getRelative(BlockFace.UP, yOffset);
            } else if (yOffset < 0) {
                target = target.getRelative(BlockFace.DOWN, yOffset);
            }
            if (target.getType() != Material.AIR && target.getType() != Material.SNOW && target.getType() != Material.LONG_GRASS) return noTarget(player);
            if (target != null) {
                SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, target.getLocation(), power);
                EventUtil.call(event);
                if (event.isCancelled()) return noTarget(player);
                target = event.getTargetLocation().getBlock();
                power = event.getPower();
            }
            createTotem(player, target.getLocation(), power);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

    private void createTotem(Player caster, Location loc, float power) {
        totems.add(new Totem(caster, loc, power));
        ticker.start();
        if (caster != null) {
            playSpellEffects(caster, loc);
        } else {
            playSpellEffects(EffectPosition.TARGET, loc);
        }
    }

    @Override
    public boolean castAtLocation(Player caster, Location target, float power) {
        Block block = target.getBlock();
        if (yOffset > 0) {
            block = block.getRelative(BlockFace.UP, yOffset);
        } else if (yOffset < 0) {
            block = block.getRelative(BlockFace.DOWN, yOffset);
        }
        if (block.getType() == Material.AIR || block.getType() == Material.SNOW || block.getType() == Material.LONG_GRASS) {
            createTotem(caster, block.getLocation(), power);
            return true;
        }
        block = block.getRelative(BlockFace.UP);
        if (block.getType() == Material.AIR || block.getType() == Material.SNOW || block.getType() == Material.LONG_GRASS) {
            createTotem(caster, block.getLocation(), power);
            return true;
        }
        return false;
    }

    @Override
    public boolean castAtLocation(Location target, float power) {
        return castAtLocation(null, target, power);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (totems.isEmpty()) return;
        Player player = event.getEntity();
        Iterator<Totem> iter = totems.iterator();
        while (iter.hasNext()) {
            Totem pulser = iter.next();
            if (pulser.caster == null) continue;
            if (!pulser.caster.equals(player)) continue;
            pulser.stop();
            iter.remove();
        }
    }

    @EventHandler
    public void onSpellTarget(SpellTargetEvent e) {
        LivingEntity target = e.getTarget();
        if (totems.isEmpty()) return;
        for (Totem t : totems) {
            if (target.equals(t.armorStand) && !targetable) {
                e.setCancelled(true);
            } else if (e.getCaster().equals(t.caster) && target.equals(t.armorStand)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        if (totems.isEmpty()) return;
        for (Totem t : totems) {
            if (t.armorStand.equals(e.getRightClicked())) e.setCancelled(true);
        }
    }

    @Override
    public void turnOff() {
        for (Totem t : new HashSet<>(totems)) {
            t.stop();
        }
        totems.clear();
        ticker.stop();
    }

    public class Totem {

        Player caster;
        LivingEntity armorStand;
        EntityEquipment totemEquipment;
        Location totemLocation;
        float power;
        int pulseCount;

        public Totem(Player caster, Location loc, float power) {
            this.caster = caster;
            this.power = power;
            this.pulseCount = 0;
            loc.setYaw(caster.getLocation().getYaw());
            this.armorStand = (LivingEntity)loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            if (totemName != null && !totemName.isEmpty()) {
                this.armorStand.setCustomName(ChatColor.translateAlternateColorCodes('&',totemName));
                this.armorStand.setCustomNameVisible(true);
            }
            this.totemEquipment = this.armorStand.getEquipment();
            this.armorStand.setGravity(gravity);
            this.totemEquipment.setItemInMainHand(mainHand);
            this.totemEquipment.setItemInOffHand(hand);
            this.totemEquipment.setHelmet(helmet);
            this.totemEquipment.setChestplate(chestplate);
            this.totemEquipment.setLeggings(leggings);
            this.totemEquipment.setBoots(boots);
            ((ArmorStand)this.armorStand).setVisible(visibility);
            this.armorStand.setInvulnerable(true);
            this.totemLocation = armorStand.getLocation();
        }

        public boolean pulse() {
            totemLocation = armorStand.getLocation();
            if (caster == null) {
                if (!armorStand.isDead()) return activate();
                stop();
                return true;
            } else if (caster.isValid() && caster.isOnline() && !armorStand.isDead() && totemLocation.getChunk().isLoaded()) {
                if (maxDistanceSquared > 0 && (!LocationUtil.isSameWorld(totemLocation, caster) || totemLocation.distanceSquared(caster.getLocation()) > maxDistanceSquared)) {
                    stop();
                    return true;
                }
                return activate();
            }
            stop();
            return true;
        }

        private boolean activate() {
            boolean activated = false;
            for (TargetedLocationSpell spell : spells) {
                if (caster != null) {
                    activated = spell.castAtLocation(caster, totemLocation, power) || activated;
                } else {
                    activated = spell.castAtLocation(totemLocation, power) || activated;
                }
            }
            playSpellEffects(EffectPosition.SPECIAL, totemLocation);
            if (totalPulses > 0 && (activated || !onlyCountOnSuccess)) {
                pulseCount += 1;
                if (pulseCount >= totalPulses) {
                    stop();
                    return true;
                }
            }
            return false;
        }

        public void stop() {
            if (!totemLocation.getChunk().isLoaded()) totemLocation.getChunk().load();
            armorStand.remove();
            playSpellEffects(EffectPosition.DISABLED, totemLocation);
            if (spellOnBreak != null) {
                if (caster == null) {
                    spellOnBreak.castAtLocation(totemLocation, power);
                } else if (caster.isValid()) {
                    spellOnBreak.castAtLocation(caster, totemLocation, power);
                }
            }
        }

    }

    public class PulserTicker implements Runnable {

        private int taskId = -1;

        public void start() {
            if (taskId < 0) {
                taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 0, interval);
            }
        }

        public void stop() {
            if (taskId > 0) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = -1;
            }
        }

        @Override
        public void run() {
            for (Totem p : new HashSet<>(totems)) {
                boolean remove = p.pulse();
                if (remove) totems.remove(p);
            }
            if (totems.isEmpty()) stop();
        }

    }

}