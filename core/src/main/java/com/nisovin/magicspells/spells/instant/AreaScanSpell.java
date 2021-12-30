package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class AreaScanSpell extends InstantSpell {

    private Material material;

    private String strNotFound;
    private String spellToCast;

    private Subspell spell;

    private int radius;

    private boolean getDistance;

    public AreaScanSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        String blockName = getConfigString("block-type", "");

        if (!blockName.isEmpty()) material = Util.getMaterial(blockName);

        strNotFound = getConfigString("str-not-found", "No blocks target found.");

        radius = getConfigInt("radius", 4);

        spellToCast = getConfigString("spell", "");
        getDistance = strCastSelf != null && strCastSelf.contains("%b");

        if (material == null) MagicSpells.error("AreaScanSpell '" + internalName + "' has no target block defined!");

        if (radius > MagicSpells.getGlobalRadius()) radius = MagicSpells.getGlobalRadius();
    }

    @Override
    public void initialize() {
        super.initialize();

        spell = new Subspell(spellToCast);
        if (!spell.process()) {
            if (!spellToCast.isEmpty())
                MagicSpells.error("AreaScanSpell '" + internalName + "' has an invalid spell defined!");
                spell = null;
        }
    }

    @Override
    public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
        if (state == SpellCastState.NORMAL && caster instanceof Player) {
            int distance = -1;
            if (material != null) {
                Block foundBlock = null;

                Location loc = caster.getLocation();
                World world = caster.getWorld();
                int cx = loc.getBlockX();
                int cy = loc.getBlockY();
                int cz = loc.getBlockZ();

                for (int r = 1; r <= Math.round(radius * power); r++) {
                    for (int x = -r; x <= r; x++) {
                        for (int y = -r; y <= r; y++) {
                            for (int z = -r; z <= r; z++) {
                                if (x == r || y == r || z == r || -x == r || -y == r || -z == r) {
                                    Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                                    if (material.equals(block.getType())) {
                                        foundBlock = block;
                                        if (spell.isTargetedLocationSpell()) spell.castAtLocation(caster, block.getLocation().add(0.5, 0.5, 0.5), power);
                                        playSpellEffects(EffectPosition.TARGET, caster);
                                        playSpellEffectsTrail(caster.getLocation(), block.getLocation());
                                    }
                                }
                            }
                        }
                    }
                }

                if (foundBlock == null) {
                    sendMessage(strNotFound, caster, args);
                    return PostCastAction.ALREADY_HANDLED;
                }
                if (getDistance) distance = (int) Math.round(caster.getLocation().distance(foundBlock.getLocation()));
            }

            playSpellEffects(EffectPosition.CASTER, caster);
            if (getDistance) {
                sendMessage(strCastSelf, caster, args, "%d", distance + "");
                sendMessageNear(caster, strCastOthers);
                return PostCastAction.NO_MESSAGES;
            }
        }

        return PostCastAction.HANDLE_NORMALLY;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getStrNotFound() {
        return strNotFound;
    }

    public void setStrNotFound(String strNotFound) {
        this.strNotFound = strNotFound;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public boolean shouldGetDistance() {
        return getDistance;
    }

    public void setGetDistance(boolean getDistance) {
        this.getDistance = getDistance;
    }
}
