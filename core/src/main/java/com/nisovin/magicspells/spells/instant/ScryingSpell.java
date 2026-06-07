package com.nisovin.magicspells.spells.instant;

import java.util.*;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class ScryingSpell extends TargetedSpell implements TargetedEntitySpell {
    private final ConfigData<String> scryText;
    private final ConfigData<Integer> areaSize;
    private final ConfigData<Double> scale;
    private final ConfigData<Integer> period;
    private final ConfigData<Integer> iterations;
    private final ConfigData<Integer> durationTicks;
    private final ConfigData<Vector> relativeOffset;
    private final ConfigData<Boolean> snapToBlockCenter;

    public ScryingSpell(MagicConfig config, String spellName) {
        super(config, spellName);
        areaSize = getConfigDataInt("area-size", 10);
        scale = getConfigDataDouble("scale", 0.2);
        period = getConfigDataInt("period", 20); // ticks
        iterations = getConfigDataInt("iterations", 5);
        durationTicks = getConfigDataInt("duration-ticks", 100);
        relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0, 0));
        snapToBlockCenter = getConfigDataBoolean("snap-to-block-center", true);
        scryText = getConfigDataString("scry-text", null);
    }

    @Override
    public CastResult cast(SpellData data) {
        TargetInfo<LivingEntity> info = getTargetedEntity(data);
        if (info.noTarget()) return noTarget(info);

        return castAtEntity(info.spellData());
    }

    @Override
    public CastResult castAtEntity(SpellData data) {
        if (!data.hasCaster() || !data.hasTarget()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        LivingEntity caster = data.caster();
        LivingEntity target = data.target();
        Location visionOrigin = caster.getLocation().add(caster.getLocation().getDirection().multiply(3));
        // Set pitch and yaw to zero to avoid unintended rotation
        visionOrigin.setPitch(0f);
        visionOrigin.setYaw(0f);
        int area = areaSize.get(data);
        double scl = scale.get(data);
        int periodTicks = period.get(data);
        int maxIterations = iterations.get(data);
        int visionDuration = durationTicks.get(data);
        Vector relOffset = relativeOffset.get(data);
        boolean snap = snapToBlockCenter.get(data);
        String text = scryText.get(data);

        // Snap visionOrigin to the center of the block if enabled
        if (snap) {
            visionOrigin.setX(visionOrigin.getBlockX() + 0.5);
            visionOrigin.setZ(visionOrigin.getBlockZ() + 0.5);
        }
        // Align the bottom of the minimap with the targeted block
        // The minimap is centered on visionOrigin, so shift it UP by half the area (scaled)
        visionOrigin.setY(visionOrigin.getY() + (area * scl) / 2.0);

        // If targeting self, treat as minimap (show surroundings)
        LivingEntity visionTarget = (target.equals(caster)) ? caster : target;
        ScryingVision vision = new ScryingVision(visionTarget, visionOrigin, area, scl, visionDuration, periodTicks, maxIterations, relOffset, text);
        vision.start();
        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }

    private static class ScryingVision implements Runnable {
        // Helper to check if a block is transparent or a fluid
        private boolean isTransparentOrFluid(Material mat) {
            if (mat.isAir() || mat.isTransparent() || !mat.isOccluding()) return true;

            String name = mat.name();
            return name.contains("WATER") || name.contains("LAVA");
        }

        private Entity scryedTextDisplay = null;
        /**
         * Spawns a scaled-down copy of the target entity at the given location.
         * If the entity is a mob, spawns the same mob type, disables AI, scales it down, and tags it.
         * If not, spawns an ArmorStand as fallback.
         */

        private Entity spawnScryedEntity(Location loc, double scale) {
            World world = loc.getWorld();
            Entity spawnedEntity = null;
            try {
                // Try to spawn the same type if it's a mob
                if (target instanceof org.bukkit.entity.Mob) {
                    org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) target;
                    spawnedEntity = world.spawnEntity(loc, mob.getType());
                    if (spawnedEntity instanceof org.bukkit.entity.Mob) {
                        org.bukkit.entity.Mob spawnedMob = (org.bukkit.entity.Mob) spawnedEntity;
                        spawnedMob.setAI(false);
                        spawnedMob.setSilent(true);
                        spawnedMob.setCustomName(target.getName());
                        spawnedMob.setCustomNameVisible(true);
                        spawnedMob.addScoreboardTag("MS_ENTITY");
                        spawnedMob.addScoreboardTag("SCRYING_SPAWN");
                        // Try to scale down (if supported)
                        try {
                            spawnedMob.getAttribute(org.bukkit.attribute.Attribute.valueOf("SCALE")).setBaseValue(scale);
                        } catch (Throwable ignored) {}
                    }
                } else {
                    // Fallback: ArmorStand
                    ArmorStand stand = world.spawn(loc, ArmorStand.class, e -> {
                        e.setVisible(true);
                        e.setCustomName(target.getName());
                        e.setCustomNameVisible(true);
                        e.setAI(false);
                        e.setGravity(false);
                        e.setSmall(true);
                        e.addScoreboardTag("SCRYING_SPAWN");
                        e.setHelmet(target instanceof LivingEntity && ((LivingEntity)target).getEquipment() != null ? ((LivingEntity)target).getEquipment().getHelmet() : null);
                        try {
                            e.getAttribute(org.bukkit.attribute.Attribute.valueOf("SCALE")).setBaseValue(scale);
                        } catch (Throwable ignored) {}
                    });
                    spawnedEntity = stand;
                }
            } catch (Throwable t) {
                // Fallback: ArmorStand if anything fails
                ArmorStand stand = world.spawn(loc, ArmorStand.class, e -> {
                    e.setVisible(true);
                    e.setCustomName(target.getName());
                    e.setCustomNameVisible(true);
                    e.setAI(false);
                    e.setGravity(false);
                    e.setSmall(true);
                    e.setMarker(true); // No hitbox
                    e.addScoreboardTag("SCRYING_SPAWN");
                    e.addScoreboardTag("MS_ENTITY");
                    e.setHelmet(target instanceof LivingEntity && ((LivingEntity)target).getEquipment() != null ? ((LivingEntity)target).getEquipment().getHelmet() : null);
                    try {
                        e.getAttribute(org.bukkit.attribute.Attribute.valueOf("SCALE")).setBaseValue(scale);
                    } catch (Throwable ignored) {}
                });
                spawnedEntity = stand;
            }
            return spawnedEntity;
        }
        private final LivingEntity target;
        private final Location visionOrigin;
        private final int areaSize;
        private final double scale;
        private final int visionDuration;
        private final int periodTicks;
        private final int maxIterations;
        private int iteration = 0;
        private final List<Entity> spawned = new ArrayList<>();
        private Entity scryedEntity = null;
        private int taskId = -1;

        private final Vector relOffset;
        private final String scryText;

        public ScryingVision(LivingEntity target, Location visionOrigin, int areaSize, double scale, int visionDuration, int periodTicks, int maxIterations, Vector relOffset, String scryText) {
            this.target = target;
            this.visionOrigin = visionOrigin.clone();
            this.areaSize = areaSize;
            this.scale = scale;
            this.visionDuration = visionDuration;
            this.periodTicks = periodTicks;
            this.maxIterations = maxIterations;
            this.relOffset = relOffset;
            this.scryText = scryText;
        }

        public void start() {
            updateVision();
            spawnScryedEntityOnTopBlock();
            if (maxIterations > 1) {
                taskId = MagicSpells.scheduleRepeatingTask(this, periodTicks, periodTicks);
            }
        }

        @Override
        public void run() {
            iteration++;
            clearVision();
            updateVision();
            // Do not respawn scryed entity
            if (iteration >= maxIterations - 1) {
                MagicSpells.scheduleDelayedTask(this::clearVision, visionDuration);
                MagicSpells.cancelTask(taskId);
            }
        }

        private int highestCenterY = Integer.MIN_VALUE;
        private void updateVision() {
            // Block face offsets
            int[][] faces = { {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1} };
            Location targetLoc = target.getLocation().clone().add(relOffset);
            World world = targetLoc.getWorld();
            int half = areaSize / 2;
            highestCenterY = Integer.MIN_VALUE;
            // Copy all blocks from ground up in the area
                for (int x = -half; x < half; x++) {
                    for (int z = -half; z < half; z++) {
                        for (int y = -half; y < half; y++) {
                            Location blockLoc = targetLoc.clone().add(x, y, z);
                            Material mat = blockLoc.getBlock().getType();
                            if (mat.isAir()) continue;
                            boolean hasOpenFace = false;
                            for (int[] face : faces) {
                                Location neighbor = blockLoc.clone().add(face[0], face[1], face[2]);
                                Material neighborMat = neighbor.getBlock().getType();
                                if (isTransparentOrFluid(neighborMat)) {
                                    hasOpenFace = true;
                                    break;
                                }
                            }
                            if (!hasOpenFace) continue;
                            Vector offset = new Vector(x * scale, y * scale, z * scale);
                            Location displayLoc = visionOrigin.clone().add(offset);
                            BlockDisplay display = world.spawn(displayLoc, BlockDisplay.class, e -> {
                                e.setBlock(blockLoc.getBlock().getBlockData());
                                e.setTransformation(new Transformation(
                                    new Vector3f(0,0,0),
                                    new Quaternionf(0,0,0,1),
                                    new Vector3f((float)scale, (float)scale, (float)scale),
                                    new Quaternionf(0,0,0,1)
                                ));
                            });
                            spawned.add(display);
                            // Make this block display last 1 tick longer to prevent flicker
                            MagicSpells.scheduleDelayedTask(display::remove, periodTicks + 2);
                            // Track the highest Y at the center (x==0, z==0)
                            if (x == 0 && z == 0 && y > highestCenterY) {
                                highestCenterY = y;
                            }
                        }
                    }
                }
        }

        private void clearVision() {
            for (Entity e : spawned) e.remove();
            spawned.clear();
            // Only remove the scryed entity when vision ends (not every update)
            if (iteration >= maxIterations - 1 && scryedEntity != null) {
                scryedEntity.remove();
                scryedEntity = null;
            }
            if (iteration >= maxIterations - 1 && scryedTextDisplay != null) {
                scryedTextDisplay.remove();
                scryedTextDisplay = null;
            }
        }
        /**
         * Spawns the scryed entity once, on the top block at the minimap's center.
         * Also spawns a scaled text display above it if configured.
         */
        private void spawnScryedEntityOnTopBlock() {
            Location centerLoc = visionOrigin.clone();
            World world = centerLoc.getWorld();
            // Place entity 0.1 above the highest block display at the center of the minimap
            double y = visionOrigin.getY() + (highestCenterY * scale) + 0.1;
            centerLoc.setY(y);
            scryedEntity = spawnScryedEntity(centerLoc, scale);
            // Disguise the entity as the target using LibsDisguises
            try {
                Disguise disguise = DisguiseAPI.getDisguise(target);
                if (disguise != null) {
                    DisguiseAPI.disguiseEntity(scryedEntity, disguise);
                }
            } catch (Throwable ignored) {}

            // Spawn a scaled text display above the scryed entity if configured
            if (scryText != null && !scryText.isEmpty()) {
                Location textLoc = centerLoc.clone().add(0, 0.7 * scale + 0.5, 0);
                try {
                    org.bukkit.entity.TextDisplay display = world.spawn(textLoc, org.bukkit.entity.TextDisplay.class, td -> {
                        td.setText(scryText);
                        td.setBillboard(org.bukkit.entity.TextDisplay.Billboard.CENTER);
                        td.setSeeThrough(true);
                        td.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                        td.setShadowed(true);
                        td.setLineWidth(80);
                    });
                    scryedTextDisplay = display;
                } catch (Throwable ignored) {}
            }
        }
    }
}
