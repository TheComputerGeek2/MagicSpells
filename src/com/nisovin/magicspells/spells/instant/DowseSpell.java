package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class DowseSpell extends InstantSpell {
    
    private final int radius;
    private final boolean rotatePlayer;
    private final boolean setCompass;
    private final String strNotFound;
    private final boolean getDistance;
    private MagicMaterial material;
    private EntityType entityType;
    private String playerName;
    
    public DowseSpell(final MagicConfig config, final String spellName) {
        super(config, spellName);
        
        final String blockName = getConfigString("block-type", "");
        if(!blockName.isEmpty()) {
            material = MagicSpells.getItemNameResolver().resolveBlock(blockName);
        }
        final String entityName = getConfigString("entity-type", "");
        if(!entityName.isEmpty()) {
            if(entityName.equalsIgnoreCase("player")) {
                entityType = EntityType.PLAYER;
            } else if(entityName.toLowerCase().startsWith("player:")) {
                entityType = EntityType.PLAYER;
                playerName = entityName.split(":")[1];
            } else {
                entityType = Util.getEntityType(entityName);
            }
        }
        
        radius = getConfigInt("radius", 4);
        rotatePlayer = getConfigBoolean("rotate-player", true);
        setCompass = getConfigBoolean("set-compass", true);
        strNotFound = getConfigString("str-not-found", "No dowsing target found.");
        
        getDistance = strCastSelf != null && strCastSelf.contains("%d");
        
        if(material == null && entityType == null) {
            MagicSpells.error("DowseSpell '" + internalName + "' has no dowse target (block or entity) defined");
        }
    }
    
    @Override
    public PostCastAction castSpell(final Player player, final SpellCastState state, final float power, final String[] args) {
        if(state == SpellCastState.NORMAL) {
            
            int distance = -1;
            
            if(material != null) {
                
                Block foundBlock = null;
                
                final Location loc = player.getLocation();
                final World world = player.getWorld();
                final int cx = loc.getBlockX();
                final int cy = loc.getBlockY();
                final int cz = loc.getBlockZ();
                for(int r = 1; r <= Math.round(radius * power); r++) {
                    for(int x = -r; x <= r; x++) {
                        for(int y = -r; y <= r; y++) {
                            for(int z = -r; z <= r; z++) {
                                if(x == r || y == r || z == r || -x == r || -y == r || -z == r) {
                                    final Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                                    if(material.equals(block)) {
                                        foundBlock = block;
                                        break;
                                    }
                                }
                            }
                            if(foundBlock != null) {
                                break;
                            }
                        }
                        if(foundBlock != null) {
                            break;
                        }
                    }
                    if(foundBlock != null) {
                        break;
                    }
                }
                
                if(foundBlock == null) {
                    sendMessage(strNotFound, player, args);
                    return PostCastAction.ALREADY_HANDLED;
                } else {
                    if(rotatePlayer) {
                        final Vector v = foundBlock.getLocation().add(.5, .5, .5).subtract(player.getEyeLocation()).toVector().normalize();
                        Util.setFacing(player, v);
                    }
                    if(setCompass) {
                        player.setCompassTarget(foundBlock.getLocation());
                    }
                    if(getDistance) {
                        distance = (int) Math.round(player.getLocation().distance(foundBlock.getLocation()));
                    }
                }
            } else if(entityType != null) {
                
                // find entity
                Entity foundEntity = null;
                final double distanceSq = radius * radius;
                if(entityType == EntityType.PLAYER && playerName != null) {
                    // find specific player
                    foundEntity = PlayerNameUtils.getPlayerExact(playerName);
                    if(foundEntity != null) {
                        if(!foundEntity.getWorld().equals(player.getWorld())) {
                            foundEntity = null;
                        } else if(radius > 0 && player.getLocation().distanceSquared(foundEntity.getLocation()) > distanceSq) {
                            foundEntity = null;
                        }
                    }
                } else {
                    // find nearest entity
                    final List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
                    final Location playerLoc = player.getLocation();
                    final Collection<NearbyEntity> ordered = new TreeSet<>();
                    for(final Entity e : nearby) {
                        if(e.getType() == entityType) {
                            final double d = e.getLocation().distanceSquared(playerLoc);
                            if(d < distanceSq) {
                                ordered.add(new NearbyEntity(e, d));
                            }
                        }
                    }
                    if(!ordered.isEmpty()) {
                        for(final NearbyEntity ne : ordered) {
                            if(ne.entity instanceof LivingEntity) {
                                final SpellTargetEvent event = new SpellTargetEvent(this, player, (LivingEntity) ne.entity, power);
                                Bukkit.getPluginManager().callEvent(event);
                                if(!event.isCancelled()) {
                                    foundEntity = ne.entity;
                                    break;
                                }
                            } else {
                                foundEntity = ne.entity;
                                break;
                            }
                        }
                    }
                }
                
                if(foundEntity == null) {
                    sendMessage(strNotFound, player, args);
                    return PostCastAction.ALREADY_HANDLED;
                } else {
                    if(rotatePlayer) {
                        final Location l = foundEntity instanceof LivingEntity ? ((LivingEntity) foundEntity).getEyeLocation() : foundEntity.getLocation();
                        final Vector v = l.subtract(player.getEyeLocation()).toVector().normalize();
                        Util.setFacing(player, v);
                    }
                    if(setCompass) {
                        player.setCompassTarget(foundEntity.getLocation());
                    }
                    if(getDistance) {
                        distance = (int) Math.round(player.getLocation().distance(foundEntity.getLocation()));
                    }
                }
            }
            
            playSpellEffects(EffectPosition.CASTER, player);
            if(getDistance) {
                sendMessage(formatMessage(strCastSelf, "%d", distance + ""), player, args);
                sendMessageNear(player, strCastOthers);
                return PostCastAction.NO_MESSAGES;
            }
        }
        
        return PostCastAction.HANDLE_NORMALLY;
    }
    
    class NearbyEntity implements Comparable<NearbyEntity> {
        
        Entity entity;
        double distanceSquared;
        
        NearbyEntity(final Entity entity, final double distanceSquared) {
            this.entity = entity;
            this.distanceSquared = distanceSquared;
        }
        
        @Override
        public int compareTo(@SuppressWarnings("NullableProblems") final NearbyEntity e) {
            if(e.distanceSquared < distanceSquared) {
                return -1;
            } else if(e.distanceSquared > distanceSquared) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
