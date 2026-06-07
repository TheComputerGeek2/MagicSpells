package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.Spell.PostCastAction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.nisovin.magicspells.util.config.ConfigData;

public class CartographySpell extends InstantSpell {

    private final ConfigData<Integer> areaSize;
    private final ConfigData<Double> scale;
    private final ConfigData<Integer> period;
    private final ConfigData<Integer> iterations;
    private final ConfigData<Integer> durationTicks;
    private final ConfigData<Vector> relativeOffset;
    private final ConfigData<Boolean> snapToBlockCenter;
    private final ConfigData<Boolean> hideRoof;

    private final List<String> allowedBlockStrings;
    private final List<String> deniedBlockStrings;
    private final List<String> roofMaterialStrings;
    private Set<BlockData> allowedBlocks;
    private Set<BlockData> deniedBlocks;
    private Set<BlockData> roofMaterials;

    public CartographySpell(MagicConfig config, String spellName) {
        super(config, spellName);
        areaSize = getConfigDataInt("area-size", 10);
        scale = getConfigDataDouble("scale", 0.2);
        period = getConfigDataInt("period", 20);
        iterations = getConfigDataInt("iterations", 5);
        durationTicks = getConfigDataInt("duration-ticks", 100);
        relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0, 0));
        snapToBlockCenter = getConfigDataBoolean("snap-to-block-center", true);
        hideRoof = getConfigDataBoolean("hide-roof", true);

        allowedBlockStrings = getConfigStringList("allowed-blocks", null);
        deniedBlockStrings = getConfigStringList("denied-blocks", null);
        roofMaterialStrings = getConfigStringList("roof-materials", null);
    }

    @Override
    public void initialize() {
        super.initialize();
        allowedBlocks = parseBlockDataSet(allowedBlockStrings);
        deniedBlocks = parseBlockDataSet(deniedBlockStrings);
        roofMaterials = parseBlockDataSet(roofMaterialStrings);
    }

    @Override
    public CastResult cast(SpellData data) {
        if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        LivingEntity caster = data.caster();

        Location origin = caster.getLocation().add(caster.getLocation().getDirection().multiply(3));
        origin.setPitch(0f);
        origin.setYaw(0f);

        int area = areaSize.get(data);
        double scl = scale.get(data);
        int periodTicks = period.get(data);
        int maxIterations = iterations.get(data);
        int visionDuration = durationTicks.get(data);
        Vector relOffset = relativeOffset.get(data);
        boolean snap = snapToBlockCenter.get(data);
        boolean hideRoofVal = hideRoof.get(data);

        if (snap) {
            origin.setX(origin.getBlockX() + 0.5);
            origin.setZ(origin.getBlockZ() + 0.5);
        }

        origin.setY(origin.getY() + (area * scl) / 2.0);

        Location targetLoc = caster.getTargetBlockExact(area) != null ? caster.getTargetBlockExact(area).getLocation() : caster.getLocation();

        MiniMapVision vision = new MiniMapVision(targetLoc, origin, area, scl, visionDuration, periodTicks, maxIterations, relOffset, hideRoofVal, allowedBlocks, deniedBlocks, roofMaterials);
        vision.start();

        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }

    private static class MiniMapVision implements Runnable {

        private final Location center;
        private final Location displayOrigin;
        private final int areaSize;
        private final double scale;
        private final int visionDuration;
        private final int periodTicks;
        private final int maxIterations;
        private final Vector relOffset;
        private final boolean hideRoof;
        private final Set<BlockData> allowedBlocks;
        private final Set<BlockData> deniedBlocks;
        private final Set<BlockData> roofMaterials;

        private final java.util.Set<Long> visibleColumns = new java.util.HashSet<>();

        private int iteration = 0;
        private int taskId = -1;
        private final java.util.List<Entity> spawned = new java.util.ArrayList<>();

        private MiniMapVision(Location center, Location displayOrigin, int areaSize, double scale, int visionDuration, int periodTicks, int maxIterations, Vector relOffset, boolean hideRoof, Set<BlockData> allowedBlocks, Set<BlockData> deniedBlocks, Set<BlockData> roofMaterials) {
            this.center = center.clone();
            this.displayOrigin = displayOrigin.clone();
            this.areaSize = areaSize;
            this.scale = scale;
            this.visionDuration = visionDuration;
            this.periodTicks = periodTicks;
            this.maxIterations = maxIterations;
            this.relOffset = relOffset;
            this.hideRoof = hideRoof;
            this.allowedBlocks = allowedBlocks;
            this.deniedBlocks = deniedBlocks;
            this.roofMaterials = roofMaterials;
        }

        public void start() {
            updateVision();
            if (maxIterations > 1) {
                taskId = com.nisovin.magicspells.MagicSpells.scheduleRepeatingTask(this, periodTicks, periodTicks);
            }
        }

        @Override
        public void run() {
            iteration++;
            clearVision();
            updateVision();
            if (iteration >= maxIterations - 1) {
                com.nisovin.magicspells.MagicSpells.scheduleDelayedTask(this::clearVision, visionDuration);
                com.nisovin.magicspells.MagicSpells.cancelTask(taskId);
            }
        }

        private void updateVision() {
            World world = center.getWorld();
            int half = areaSize / 2;

            Location base = center.clone().add(relOffset);
            int baseY = base.getBlockY();

            visibleColumns.clear();

            for (int x = -half; x < half; x++) {
                for (int z = -half; z < half; z++) {
                    int worldX = base.getBlockX() + x;
                    int worldZ = base.getBlockZ() + z;

                    Block visibleBlock = findVisibleBlock(world, worldX, worldZ, baseY);
                    if (visibleBlock == null) continue;

                    // Render the top visible block for this column.
                    int relY = visibleBlock.getY() - baseY;
                    Vector offset = new Vector(x * scale, relY * scale, z * scale);
                    Location displayLoc = displayOrigin.clone().add(offset);

                    BlockDisplay display = world.spawn(displayLoc, BlockDisplay.class, e -> {
                        e.setBlock(visibleBlock.getBlockData());
                        e.setTransformation(new Transformation(
                            new Vector3f(0, 0, 0),
                            new Quaternionf(0, 0, 0, 1),
                            new Vector3f((float) scale, (float) scale, (float) scale),
                            new Quaternionf(0, 0, 0, 1)
                        ));
                    });
                    spawned.add(display);

                    // Also render the vertical stack below the visible block (e.g., full cactus
                    // or foliage column) down to the first denied block, air, or solid ground.
                    Block below = visibleBlock.getRelative(0, -1, 0);
                    while (true) {
                        if (below.getY() < world.getMinHeight()) break;
                        if (below.getType().isAir()) break;

                        Block support = below.getRelative(0, -1, 0);
                        // Stop if the block under this one is denied by config.
                        if (isDenied(support)) break;

                        if (!isAllowed(below)) {
                            below = support;
                            continue;
                        }

                        int relYBelow = below.getY() - baseY;
                        Vector offsetBelow = new Vector(x * scale, relYBelow * scale, z * scale);
                        Location displayLocBelow = displayOrigin.clone().add(offsetBelow);
                        Block currentBelow = below;

                        BlockDisplay displayBelow = world.spawn(displayLocBelow, BlockDisplay.class, e -> {
                            e.setBlock(currentBelow.getBlockData());
                            e.setTransformation(new Transformation(
                                new Vector3f(0, 0, 0),
                                new Quaternionf(0, 0, 0, 1),
                                new Vector3f((float) scale, (float) scale, (float) scale),
                                new Quaternionf(0, 0, 0, 1)
                            ));
                        });
                        spawned.add(displayBelow);

                        // If this block is solid ground, stop stacking further down.
                        if (below.getType().isOccluding()) break;

                        below = support;
                    }

                    visibleColumns.add(encodeColumnKey(x, z));
                }
            }

            spawnMiniMobs(world, base, half);
        }

        private void clearVision() {
            for (Entity e : spawned) e.remove();
            spawned.clear();
        }

        private boolean isDenied(Block block) {
            if (deniedBlocks == null) return false;

            BlockData data = block.getBlockData();
            for (BlockData bd : deniedBlocks) {
                if (data.matches(bd)) return true;
            }
            return false;
        }

        private boolean isAllowed(Block block) {
            if (allowedBlocks == null) return !block.getType().isAir();

            BlockData data = block.getBlockData();
            for (BlockData bd : allowedBlocks) {
                if (data.matches(bd)) return true;
            }
            return false;
        }

        private boolean isRoof(Block block) {
            if (roofMaterials == null) return false;

            BlockData data = block.getBlockData();
            for (BlockData bd : roofMaterials) {
                if (data.matches(bd)) return true;
            }
            return false;
        }

        private Block findVisibleBlock(World world, int x, int z, int baseY) {
            Block block = world.getHighestBlockAt(x, z);

            // If the top-most block is denied, skip this column entirely.
            if (isDenied(block)) return null;

            // Walk downward while the block is considered "see-through" or invalid.
            while (true) {
                if (block.getY() < world.getMinHeight()) return null;

                if (!block.getType().isAir() && !isRoof(block) && isAllowed(block) && hasOpenFace(block)) return block;

                Block below = block.getRelative(0, -1, 0);
                // If we hit a denied block while searching, treat the column as denied.
                if (isDenied(below)) return null;
                block = below;
            }
        }

        private boolean hasOpenFace(Block block) {
            // Check 6 neighboring faces; show only if at least one touches air.
            int bx = block.getX();
            int by = block.getY();
            int bz = block.getZ();
            World world = block.getWorld();

            return world.getBlockAt(bx + 1, by, bz).getType().isAir()
                || world.getBlockAt(bx - 1, by, bz).getType().isAir()
                || world.getBlockAt(bx, by + 1, bz).getType().isAir()
                || world.getBlockAt(bx, by - 1, bz).getType().isAir()
                || world.getBlockAt(bx, by, bz + 1).getType().isAir()
                || world.getBlockAt(bx, by, bz - 1).getType().isAir();
        }

        private void spawnMiniMobs(World world, Location base, int half) {
            for (Entity entity : world.getNearbyEntities(base, half, half, half)) {
                if (!(entity instanceof Mob mob)) continue;
                if (entity.getScoreboardTags().contains("MS_CARTO_MINI")) continue;

                Location mobLoc = mob.getLocation();
                double dx = mobLoc.getX() - base.getX();
                double dy = mobLoc.getY() - base.getY();
                double dz = mobLoc.getZ() - base.getZ();

                // Check whether this mob's column is not denied according to the map.
                int colX = mobLoc.getBlockX() - base.getBlockX();
                int colZ = mobLoc.getBlockZ() - base.getBlockZ();
                if (colX < -half || colX >= half || colZ < -half || colZ >= half) continue;
                if (!visibleColumns.contains(encodeColumnKey(colX, colZ))) continue;

                Vector offset = new Vector(dx * scale, dy * scale, dz * scale);
                Location displayLoc = displayOrigin.clone().add(offset);

                Entity mini = spawnMiniEntity(displayLoc, mob);
                if (mini != null) spawned.add(mini);
            }
        }

        private Entity spawnMiniEntity(Location loc, Mob original) {
            try {
                Entity spawnedEntity = loc.getWorld().spawnEntity(loc, original.getType());
                if (spawnedEntity instanceof Mob mini) {
                    mini.setAI(false);
                    mini.setSilent(true);
                    mini.setInvulnerable(true);
                    mini.setRemoveWhenFarAway(true);
                    mini.addScoreboardTag("MS_CARTO_MINI");

                    // Match rotation to the original entity.
                    mini.setRotation(original.getLocation().getYaw(), original.getLocation().getPitch());

                    if (spawnedEntity instanceof LivingEntity living) {
                        try {
                            var attr = living.getAttribute(org.bukkit.attribute.Attribute.valueOf("SCALE"));
                            if (attr != null) attr.setBaseValue(scale);
                        } catch (Throwable ignored) {}
                    }
                }
                return spawnedEntity;
            } catch (Throwable ignored) {
                return null;
            }
        }

        private long encodeColumnKey(int x, int z) {
            return ((long) x << 32) ^ (z & 0xffffffffL);
        }
    }

    private Set<BlockData> parseBlockDataSet(List<String> blockStrings) {
        if (blockStrings == null) return null;
        Set<BlockData> set = new HashSet<>();
        for (String s : blockStrings) {
            try {
                set.add(Bukkit.createBlockData(s));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return set.isEmpty() ? null : set;
    }
}
