package com.nisovin.magicspells.spells.instant;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class WaveSpell extends TargetedSpell implements TargetedLocationSpell {

    private final ConfigData<Integer> radius;
    private final ConfigData<Integer> startRadius;
    private final ConfigData<Integer> expandInterval;
    private final ConfigData<Integer> expandingRadiusChange;
    private final ConfigData<Double> visibleRange;
    private final ConfigData<Double> coneAngle;
    private final ConfigData<Boolean> hugSurface;
    private final ConfigData<Vector> relativeOffset;
    private final ConfigData<Boolean> fakeBlocks;
    private final ConfigData<Material> waveMaterial;

    private final String spellOnEndName;
    private final String locationSpellName;
    private Subspell spellOnEnd;
    private Subspell locationSpell;

    public WaveSpell(MagicConfig config, String spellName) {
        super(config, spellName);
        radius = getConfigDataInt("radius", 8);
        startRadius = getConfigDataInt("start-radius", 0);
        expandInterval = getConfigDataInt("expand-interval", 3);
        expandingRadiusChange = getConfigDataInt("expanding-radius-change", 1);
        visibleRange = getConfigDataDouble("visible-range", 20);
        coneAngle = getConfigDataDouble("cone-angle", 90.0); // degrees
        hugSurface = getConfigDataBoolean("hug-surface", true);
        relativeOffset = getConfigDataVector("relative-offset", new Vector());
        fakeBlocks = getConfigDataBoolean("fake-blocks", false);
        waveMaterial = getConfigDataMaterial("wave-material", Material.WATER);
        spellOnEndName = getConfigString("spell-on-end", "");
        locationSpellName = getConfigString("spell", "");
    }

    @Override
    public void initialize() {
        super.initialize();
        String error = "WaveSpell '" + internalName + "' has an invalid '%s' defined!";
        locationSpell = initSubspell(locationSpellName, error.formatted("spell"), true);
        spellOnEnd = initSubspell(spellOnEndName, error.formatted("spell-on-end"), true);
    }

    @Override
    public CastResult cast(SpellData data) {
        TargetInfo<Location> info = getTargetedBlockLocation(data);
        if (info.noTarget()) return noTarget(info);

        return castAtLocation(info.spellData());
    }

    @Override
    public CastResult castAtLocation(SpellData data) {
        new WaveTracker(data);
        playSpellEffects(data);
        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }

    private class WaveTracker implements Runnable {
        private final SpellData data;
        private final Location center;
        private final Set<Location> affected;
        private final int taskId;
        private int currentRadius;
        private int count;
        private final int maxRadius;
        private final int expandingRadiusChange;
        private final double coneAngleRad;
        private final double visibleRange;
        private final boolean hugSurface;
        private final Vector facingDir;

        public WaveTracker(SpellData data) {
            this.data = data.noTarget();
            this.center = data.location().clone();
            Vector rel = WaveSpell.this.relativeOffset.get(data);
            center.add(0, rel.getY(), 0);
            Util.applyRelativeOffset(center, rel.setY(0));
            this.affected = new HashSet<>();
            this.currentRadius = WaveSpell.this.startRadius.get(data);
            this.count = 0;
            this.maxRadius = WaveSpell.this.radius.get(data);
            this.expandingRadiusChange = WaveSpell.this.expandingRadiusChange.get(data);
            this.coneAngleRad = Math.toRadians(WaveSpell.this.coneAngle.get(data));
            this.visibleRange = WaveSpell.this.visibleRange.get(data);
            this.hugSurface = WaveSpell.this.hugSurface.get(data);
            this.facingDir = center.getDirection().normalize();
            this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, WaveSpell.this.expandInterval.get(data));
        }

        @Override
        public void run() {
            currentRadius += expandingRadiusChange;
            if (currentRadius > maxRadius) {
                stop();
                return;
            }
            // For each point in the current ring
            int points = currentRadius * 16;
            double angleStep = coneAngleRad / points;
            double startAngle = -coneAngleRad / 2;
            for (int i = 0; i < points; i++) {
                double angle = startAngle + i * angleStep;
                Vector dir = facingDir.clone().rotateAroundY(angle);
                Location loc = center.clone().add(dir.multiply(currentRadius));
                if (hugSurface) {
                    loc = findSurfaceBelow(loc, 8);
                }
                if (affected.contains(loc.getBlock().getLocation())) continue;
                affected.add(loc.getBlock().getLocation());
                // Play effects and/or set block
                playSpellEffects(EffectPosition.SPECIAL, loc, data);
                if (locationSpell != null) locationSpell.subcast(data.location(loc));
                if (fakeBlocks.get(data)) {
                    for (org.bukkit.entity.Player player : center.getNearbyPlayers(visibleRange)) {
                        player.sendBlockChange(loc, waveMaterial.get(data).createBlockData());
                    }
                }
            }
        }

        private Location findSurfaceBelow(Location loc, int maxDown) {
            Location check = loc.clone();
            for (int i = 0; i < maxDown; i++) {
                Block block = check.getBlock();
                if (!block.isPassable()) {
                    return block.getLocation().add(0, 1, 0);
                }
                check.subtract(0, 1, 0);
            }
            return loc;
        }

        private void stop() {
            if (spellOnEnd != null) spellOnEnd.subcast(data);
            MagicSpells.cancelTask(taskId);
        }
    }
}
