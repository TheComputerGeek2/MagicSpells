package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastedEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisguiseSpell extends TargetedSpell implements TargetedEntitySpell {
    
    static IDisguiseManager manager;
    boolean undisguiseOnGiveDamage;
    boolean undisguiseOnTakeDamage;
    Map<String, Disguise> disguised = new HashMap<>();
    private final DisguiseSpell thisSpell;
    private EntityData entityData;
    private boolean showPlayerName;
    private String nameplateText = "";
    private String uuid = "";
    private String skin = "";
    private String skinSig = "";
    private boolean alwaysShowNameplate = true;
    private boolean preventPickups;
    private boolean friendlyMobs = true;
    private boolean ridingBoat;
    private boolean undisguiseOnDeath = true;
    private boolean undisguiseOnLogout;
    private boolean undisguiseOnCast;
    private boolean disguiseSelf;
    private int duration;
    private boolean toggle;
    private String strFade;
    
    public DisguiseSpell(final MagicConfig config, final String spellName) {
        super(config, spellName);
        thisSpell = this;
        
        if(manager == null) {
            try {
                manager = MagicSpells.getVolatileCodeHandler().getDisguiseManager(config);
            } catch(final Exception e) {
                manager = null;
            }
            if(manager == null) {
                MagicSpells.error("DisguiseManager could not be created!");
                return;
            }
        }
        manager.registerSpell(this);
        
        final String type = getConfigString("entity-type", "zombie");
        entityData = new EntityData(type);
        showPlayerName = getConfigBoolean("show-player-name", false);
        nameplateText = ChatColor.translateAlternateColorCodes('&', getConfigString("nameplate-text", ""));
        uuid = getConfigString("uuid", "");
        if(configKeyExists("skin")) {
            final String skinName = getConfigString("skin", "skin");
            final File folder = new File(MagicSpells.getInstance().getDataFolder(), "disguiseskins");
            if(folder.exists()) {
                try {
                    File file = new File(folder, skinName + ".skin.txt");
                    if(file.exists()) {
                        final BufferedReader reader = new BufferedReader(new FileReader(file));
                        skin = reader.readLine();
                        reader.close();
                    }
                    file = new File(folder, skinName + ".sig.txt");
                    if(file.exists()) {
                        final BufferedReader reader = new BufferedReader(new FileReader(file));
                        skinSig = reader.readLine();
                        reader.close();
                    }
                } catch(final Exception e) {
                    MagicSpells.handleException(e);
                }
            }
        }
        alwaysShowNameplate = getConfigBoolean("always-show-nameplate", true);
        preventPickups = getConfigBoolean("prevent-pickups", true);
        friendlyMobs = getConfigBoolean("friendly-mobs", true);
        ridingBoat = getConfigBoolean("riding-boat", false);
        undisguiseOnDeath = getConfigBoolean("undisguise-on-death", true);
        undisguiseOnLogout = getConfigBoolean("undisguise-on-logout", false);
        undisguiseOnCast = getConfigBoolean("undisguise-on-cast", false);
        undisguiseOnGiveDamage = getConfigBoolean("undisguise-on-give-damage", false);
        undisguiseOnTakeDamage = getConfigBoolean("undisguise-on-take-damage", false);
        disguiseSelf = getConfigBoolean("disguise-self", false);
        duration = getConfigInt("duration", 0);
        toggle = getConfigBoolean("toggle", false);
        targetSelf = getConfigBoolean("target-self", true);
        strFade = getConfigString("str-fade", "");
        
        if(entityData.getType() == null) {
            MagicSpells.error("Invalid entity-type specified for disguise spell '" + spellName + "': " + type);
        }
    }
    
    public static IDisguiseManager getDisguiseManager() {
        return manager;
    }
    
    @Override
    public void initialize() {
        if(manager == null) {
            return;
        }
        super.initialize();
        if(undisguiseOnCast) {
            registerEvents(new CastListener());
        }
        if(undisguiseOnGiveDamage || undisguiseOnTakeDamage) {
            registerEvents(new DamageListener());
        }
    }
    
    @Override
    public PostCastAction castSpell(final Player player, final SpellCastState state, final float power, final String[] args) {
        if(manager == null) {
            return PostCastAction.ALREADY_HANDLED;
        }
        if(state == SpellCastState.NORMAL) {
            final Disguise oldDisguise = disguised.remove(player.getName().toLowerCase());
            manager.removeDisguise(player);
            if(oldDisguise != null && toggle) {
                sendMessage(strFade, player, args);
                return PostCastAction.ALREADY_HANDLED;
            }
            final TargetInfo<Player> target = getTargetPlayer(player, power);
            if(target != null) {
                disguise(target.getTarget());
                sendMessages(player, target.getTarget());
                playSpellEffects(EffectPosition.CASTER, player);
                return PostCastAction.NO_MESSAGES;
            } else {
                return noTarget(player);
            }
        }
        return PostCastAction.HANDLE_NORMALLY;
    }
    
    private void disguise(final Player player) {
        String nameplate = nameplateText;
        if(showPlayerName) {
            nameplate = player.getDisplayName();
        }
        final PlayerDisguiseData playerDisguiseData = new PlayerDisguiseData(uuid.isEmpty() ? UUID.randomUUID().toString() : uuid, skin, skinSig);
        final Disguise disguise = new Disguise(player, entityData.getType(), nameplate, playerDisguiseData, alwaysShowNameplate, disguiseSelf, ridingBoat, entityData.getFlag(), entityData.getVar1(), entityData.getVar2(), entityData.getVar3(), duration, this);
        manager.addDisguise(player, disguise);
        disguised.put(player.getName().toLowerCase(), disguise);
        playSpellEffects(EffectPosition.TARGET, player);
    }
    
    public void undisguise(final Player player) {
        final Disguise disguise = disguised.remove(player.getName().toLowerCase());
        if(disguise != null) {
            disguise.cancelDuration();
            sendMessage(strFade, player, MagicSpells.NULL_ARGS);
            playSpellEffects(EffectPosition.DISABLED, player);
        }
    }
    
    @Override
    public boolean castAtEntity(@Nullable final Player player, final LivingEntity target, final float power) {
        if(target instanceof Player) {
            disguise((Player) target);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean castAtEntity(final LivingEntity target, final float power) {
        if(target instanceof Player) {
            disguise((Player) target);
            return true;
        }
        return false;
    }
    
    @EventHandler
    public void onPickup(final PlayerPickupItemEvent event) {
        if(preventPickups && disguised.containsKey(event.getPlayer().getName().toLowerCase())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onDeath(final PlayerDeathEvent event) {
        if(undisguiseOnDeath && disguised.containsKey(event.getEntity().getName().toLowerCase())) {
            manager.removeDisguise(event.getEntity(), entityData.getType() == EntityType.PLAYER);
        }
    }
    
    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        if(undisguiseOnLogout && disguised.containsKey(event.getPlayer().getName().toLowerCase())) {
            manager.removeDisguise(event.getPlayer(), entityData.getType() == EntityType.PLAYER);
        }
    }
    
    @EventHandler
    public void onTarget(final EntityTargetEvent event) {
        if(friendlyMobs && event.getTarget() != null && event.getTarget() instanceof Player && disguised.containsKey(event.getTarget().getName().toLowerCase())) {
            event.setCancelled(true);
        }
    }
    
    @Override
    public void turnOff() {
        if(manager != null) {
            for(final String name : new ArrayList<>(disguised.keySet())) {
                final Player player = PlayerNameUtils.getPlayerExact(name);
                if(player != null) {
                    manager.removeDisguise(player, false);
                }
            }
            manager.unregisterSpell(this);
            if(manager.registeredSpellsCount() == 0) {
                manager.destroy();
                manager = null;
            }
        }
    }
    
    class CastListener implements Listener {
        @EventHandler
        void onSpellCast(final SpellCastedEvent event) {
            if(event.getCaster() == null) {
                return;
            }
            if(event.getSpell() != thisSpell && disguised.containsKey(event.getCaster().getName().toLowerCase())) {
                manager.removeDisguise(event.getCaster());
            }
        }
    }
    
    class DamageListener implements Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        void onDamage(final EntityDamageEvent event) {
            if(undisguiseOnTakeDamage && event.getEntity() instanceof Player && disguised.containsKey(event.getEntity().getName().toLowerCase())) {
                manager.removeDisguise((Player) event.getEntity());
            }
            if(undisguiseOnGiveDamage && event instanceof EntityDamageByEntityEvent) {
                final Entity e = ((EntityDamageByEntityEvent) event).getDamager();
                if(e instanceof Player) {
                    if(disguised.containsKey(e.getName().toLowerCase())) {
                        manager.removeDisguise((Player) e);
                    }
                } else if(e instanceof Projectile && ((Projectile) e).getShooter() instanceof Player) {
                    final Player shooter = (Player) ((Projectile) e).getShooter();
                    if(disguised.containsKey(shooter.getName().toLowerCase())) {
                        manager.removeDisguise(shooter);
                    }
                }
            }
        }
    }
    
    public class Disguise {
        
        Player player;
        private final EntityType entityType;
        private final String nameplateText;
        private final PlayerDisguiseData playerDisguiseData;
        private final boolean alwaysShowNameplate;
        private final boolean disguiseSelf;
        private final boolean ridingBoat;
        private final boolean flag;
        private final int var1;
        private final int var2;
        private final int var3;
        private final DisguiseSpell spell;
        
        private int taskId;
        
        public Disguise(final Player player, final EntityType entityType, final String nameplateText, final PlayerDisguiseData playerDisguiseData, final boolean alwaysShowNameplate, final boolean disguiseSelf, final boolean ridingBoat, final boolean flag, final int var1, final int var2, final int var3, final int duration, final DisguiseSpell spell) {
            this.player = player;
            this.entityType = entityType;
            this.nameplateText = nameplateText;
            this.playerDisguiseData = playerDisguiseData;
            this.alwaysShowNameplate = alwaysShowNameplate;
            this.disguiseSelf = disguiseSelf;
            this.ridingBoat = ridingBoat;
            this.flag = flag;
            this.var1 = var1;
            this.var2 = var2;
            this.var3 = var3;
            if(duration > 0) {
                startDuration(duration);
            }
            this.spell = spell;
        }
        
        public Player getPlayer() {
            return player;
        }
        
        public EntityType getEntityType() {
            return entityType;
        }
        
        public String getNameplateText() {
            return nameplateText;
        }
        
        public PlayerDisguiseData getPlayerDisguiseData() {
            return playerDisguiseData;
        }
        
        public boolean alwaysShowNameplate() {
            return alwaysShowNameplate;
        }
        
        public boolean disguiseSelf() {
            return disguiseSelf;
        }
        
        public boolean isRidingBoat() {
            return ridingBoat;
        }
        
        public boolean getFlag() {
            return flag;
        }
        
        public int getVar1() {
            return var1;
        }
        
        public int getVar2() {
            return var2;
        }
        
        public int getVar3() {
            return var3;
        }
        
        private void startDuration(final int duration) {
            taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, () -> manager.removeDisguise(player), duration);
        }
        
        public void cancelDuration() {
            if(taskId > 0) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = 0;
            }
        }
        
        public DisguiseSpell getSpell() {
            return spell;
        }
    }
    
    public class PlayerDisguiseData {
        public String uuid;
        public String skin;
        public String sig;
        
        public PlayerDisguiseData(final String uuid, final String skin, final String sig) {
            this.uuid = uuid;
            this.skin = skin;
            this.sig = sig;
        }
        
        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public PlayerDisguiseData clone() {
            return new PlayerDisguiseData(uuid, skin, sig);
        }
    }
}
