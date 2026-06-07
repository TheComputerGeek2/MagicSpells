package com.nisovin.magicspells.spells.targeted;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class PathfindSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {

    private final ConfigData<Integer> maxPathLength;
    private final ConfigData<Boolean> allowDiagonal;
    private final ConfigData<String> spellToCastName;
    private final ConfigData<String> spellOnEndName;
    private final ConfigData<Integer> maxStepHeight;
    private final ConfigData<Boolean> travelThroughBlocks;
    private List<String> walkableBlocks;
    private List<String> deniedBlocks;

    private Subspell spellToCast;
    private Subspell spellOnEnd;
    private Set<BlockData> walkableBlockData;
    private Set<BlockData> deniedBlockData;

    public PathfindSpell(MagicConfig config, String spellName) {
        super(config, spellName);
        maxPathLength = getConfigDataInt("max-path-length", 128);
        allowDiagonal = getConfigDataBoolean("allow-diagonal", false);
        spellToCastName = getConfigDataString("spell", "");
        spellOnEndName = getConfigDataString("spell-on-end", "");
        maxStepHeight = getConfigDataInt("max-step-height", 1);
        travelThroughBlocks = getConfigDataBoolean("travel-through-blocks", false);
        walkableBlocks = getConfigStringList("walkable-blocks", null);
        deniedBlocks = getConfigStringList("denied-blocks", null);
    }

    @Override
    public void initialize() {
        super.initialize();
        spellToCast = initSubspell(spellToCastName.get(null),
            "PathfindSpell '" + internalName + "' has an invalid spell: '" + spellToCastName.get(null) + "' defined!");
        spellOnEnd = initSubspell(spellOnEndName.get(null),
            "PathfindSpell '" + internalName + "' has an invalid spell-on-end: '" + spellOnEndName.get(null) + "' defined!");
        walkableBlockData = parseBlockDataSet(walkableBlocks);
        deniedBlockData = parseBlockDataSet(deniedBlocks);
    }

    @Override
    public CastResult cast(SpellData data) {
        TargetInfo<LivingEntity> entityInfo = getTargetedEntity(data);
        if (!entityInfo.empty()) return castAtEntity(entityInfo.spellData());

        TargetInfo<Location> locationInfo = getTargetedBlockLocation(data);
        if (locationInfo.noTarget()) return noTarget(locationInfo);

        return castAtLocation(locationInfo.spellData());
    }

    @Override
    public CastResult castAtLocation(SpellData data) {
        if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.location());
        if (!targetEvent.callEvent()) return noTarget(targetEvent);

        data = targetEvent.getSpellData();

        Location from = data.caster().getLocation();
        Location to = data.location();
        return castPath(from, to, data);
    }

    @Override
    public CastResult castAtEntity(SpellData data) {
        if (!data.hasCaster() || !data.hasTarget()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        Location from = data.caster().getLocation();
        Location to = data.target().getLocation();
        return castPath(from, to, data);
    }

    private CastResult castPath(Location from, Location to, SpellData data) {
        boolean throughBlocks = travelThroughBlocks.get(data);

        Location start = getPathNodeLocation(from, throughBlocks);
        Location goal = getPathNodeLocation(to, throughBlocks);

        boolean diagonal = allowDiagonal.get(data);
        Integer maxStepValue = maxStepHeight.get(data);
        int maxStep = maxStepValue != null ? maxStepValue : 1;

        Set<Location> attempted = new HashSet<>();
        List<Location> path = findPath(start, goal, maxPathLength.get(data), diagonal, maxStep, throughBlocks, attempted);
        if (path == null || path.isEmpty()) {
            // Play disabled effect at each attempted node
            for (Location loc : attempted) {
                Location effectLocation = getEffectLocation(loc, throughBlocks);
                playSpellEffects(EffectPosition.DISABLED, effectLocation, data.location(effectLocation));
            }
            return noTarget(data);
        }
        for (Location loc : path) {
            if (!shouldDisplayNode(loc, throughBlocks)) continue;

            SpellData subData = data.location(loc);
            if (spellToCast != null) spellToCast.subcast(subData);
            playSpellEffects(EffectPosition.TARGET, loc, subData);
        }
        // Play DELAYED effect and spell-on-end at the final location
        if (!path.isEmpty()) {
            Location end = path.get(path.size() - 1);
            playSpellEffects(EffectPosition.DELAYED, end, data.location(end));
            if (spellOnEnd != null) spellOnEnd.subcast(data.location(end));
        }
        if (data.hasCaster()) playSpellEffects(EffectPosition.CASTER, data.caster(), data);
        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }

    private List<Location> findPath(Location start, Location goal, int maxLength, boolean allowDiagonal, int maxStep, boolean throughBlocks, Set<Location> attempted) {
        // Simple 3D A* implementation with block-aligned locations for closed set
        class Node implements Comparable<Node> {
            final int x, y, z;
            final String world;
            Node parent;
            double g, h;
            Node(Location loc, Node parent, double g, double h) {
                this.x = loc.getBlockX();
                this.y = loc.getBlockY();
                this.z = loc.getBlockZ();
                this.world = loc.getWorld().getName();
                this.parent = parent;
                this.g = g;
                this.h = h;
            }
            Location toLocation(org.bukkit.World w) { return new Location(w, x, y, z); }
            double f() { return g + h; }
            @Override public int compareTo(Node o) { return Double.compare(f(), o.f()); }
            @Override public boolean equals(Object o) {
                if (!(o instanceof Node n)) return false;
                return x == n.x && y == n.y && z == n.z && world.equals(n.world);
            }
            @Override public int hashCode() { return Objects.hash(x, y, z, world); }
        }
        Set<Node> closed = new HashSet<>();
        PriorityQueue<Node> open = new PriorityQueue<>();
        org.bukkit.World world = start.getWorld();
        Node startNode = new Node(start, null, 0, start.distance(goal));
        Node goalNode = new Node(goal, null, 0, 0);
        open.add(startNode);
        int expanded = 0;
        int maxNodes = 10000; // hard limit to prevent infinite loops
        while (!open.isEmpty() && expanded < maxNodes) {
            Node curr = open.poll();
            if (curr.x == goalNode.x && curr.y == goalNode.y && curr.z == goalNode.z && curr.world.equals(goalNode.world)) {
                List<Location> path = new ArrayList<>();
                for (Node n = curr; n != null; n = n.parent) {
                    path.add(0, getEffectLocation(new Location(world, n.x, n.y, n.z), throughBlocks));
                }
                return path;
            }
            if (closed.contains(curr) || curr.g > maxLength) continue;
            closed.add(curr);
            expanded++;
            if (attempted != null) attempted.add(new Location(world, curr.x, curr.y, curr.z));
            for (Vector dir : getDirections(allowDiagonal)) {
                int nx = curr.x + dir.getBlockX();
                int ny = curr.y + dir.getBlockY();
                int nz = curr.z + dir.getBlockZ();
                Location nextLoc = new Location(world, nx, ny, nz);
                int dy = ny - curr.y;
                if (Math.abs(dy) > maxStep) {
                    // Only allow if climbing and all blocks between are climbable
                    boolean canClimb = true;
                    int step = dy > 0 ? 1 : -1;
                    for (int ystep = curr.y + step; ystep != ny + step; ystep += step) {
                        Location climbLoc = new Location(world, nx, ystep, nz);
                        if (!isClimbable(climbLoc.getBlock().getType())) {
                            canClimb = false;
                            break;
                        }
                    }
                    if (!canClimb) continue;
                }
                if (!isWalkable(nextLoc.getBlock(), throughBlocks)) continue;
                Node nextNode = new Node(nextLoc, curr, curr.g + 1, nextLoc.distance(goal));
                if (closed.contains(nextNode)) continue;
                open.add(nextNode);
            }
        }
        return null;
    }

    private boolean isClimbable(org.bukkit.Material material) {
        return org.bukkit.Tag.CLIMBABLE.isTagged(material);
    }

    private List<Vector> getDirections(boolean diagonal) {
        List<Vector> dirs = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (!diagonal && Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 1) continue;
                    dirs.add(new Vector(dx, dy, dz));
                }
        return dirs;
    }

    private boolean isWalkable(Block block, boolean throughBlocks) {
        if (throughBlocks) {
            return matchesTraversalFilters(block, true);
        }

        // Use the shared BlockUtils definition of a safe standable space to avoid
        // duplicating passability and support checks.
        Location feetLoc = block.getLocation().add(0.5, 0, 0.5);
        if (!BlockUtils.isSafeToStand(feetLoc.clone())) return false;

        Block below = feetLoc.clone().subtract(0, 1, 0).getBlock();
        return matchesTraversalFilters(below, false);
    }

    private Location getPathNodeLocation(Location loc, boolean throughBlocks) {
        if (throughBlocks) {
            Block block = loc.getBlock();
            if (isWalkable(block, true)) {
                return new Location(block.getWorld(), block.getX(), block.getY(), block.getZ());
            }

            org.bukkit.World world = block.getWorld();
            int bx = block.getX();
            int by = block.getY();
            int bz = block.getZ();

            for (int distance = 1; distance <= 2; distance++) {
                for (int dx = -distance; dx <= distance; dx++) {
                    for (int dy = -distance; dy <= distance; dy++) {
                        for (int dz = -distance; dz <= distance; dz++) {
                            int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                            if (manhattan == 0 || manhattan > distance) continue;

                            int x = bx + dx;
                            int y = by + dy;
                            int z = bz + dz;
                            if (y < world.getMinHeight() || y > world.getMaxHeight()) continue;

                            Block nearbyBlock = world.getBlockAt(x, y, z);
                            if (!isWalkable(nearbyBlock, true)) continue;

                            return new Location(world, x, y, z);
                        }
                    }
                }
            }

            return new Location(block.getWorld(), block.getX(), block.getY(), block.getZ());
        }

        org.bukkit.World world = loc.getWorld();
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();

        // Search a small vertical window around the requested location for a valid standable node.
        for (int dy = -1; dy <= 2; dy++) {
            int y = by + dy;
            if (y < world.getMinHeight() || y > world.getMaxHeight()) continue;

            Block feetBlock = world.getBlockAt(bx, y, bz);
            if (isWalkable(feetBlock, throughBlocks)) {
                return new Location(world, feetBlock.getX(), feetBlock.getY(), feetBlock.getZ());
            }
        }

        // Fallback: keep original location if nothing suitable is found.
        return loc;
    }

    private Location getEffectLocation(Location loc, boolean throughBlocks) {
        return new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5);
    }

    private boolean shouldDisplayNode(Location loc, boolean throughBlocks) {
        Block feetBlock = loc.getBlock();
        Block traversedBlock = throughBlocks ? feetBlock : feetBlock.getRelative(0, -1, 0);

        if (!matchesTraversalFilters(traversedBlock, throughBlocks)) return false;

        if (throughBlocks) return true;

        // Without an allow-list, hide paths over air or water; show over normal solid blocks.
        org.bukkit.Material mat = traversedBlock.getType();
        if (mat.isAir() || mat == org.bukkit.Material.WATER) return false;

        return true;
    }

    private boolean matchesTraversalFilters(Block block, boolean throughBlocks) {
        BlockData blockData = block.getBlockData();

        if (deniedBlockData != null) {
            for (BlockData bd : deniedBlockData) {
                if (blockData.matches(bd)) return false;
            }
        }

        if (walkableBlockData != null) {
            for (BlockData bd : walkableBlockData) {
                if (blockData.matches(bd)) return true;
            }
            return false;
        }

        if (throughBlocks) return !block.getType().isAir();

        return true;
    }

    private Set<BlockData> parseBlockDataSet(List<String> blockStrings) {
        if (blockStrings == null) return null;
        Set<BlockData> set = new HashSet<>();
        for (String s : blockStrings) {
            try { set.add(Bukkit.createBlockData(s)); }
            catch (IllegalArgumentException ignored) {}
        }
        return set.isEmpty() ? null : set;
    }
}
