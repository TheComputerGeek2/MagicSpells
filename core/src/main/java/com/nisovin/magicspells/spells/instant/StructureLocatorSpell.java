package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.Spell.PostCastAction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.NamespacedKey;

import com.nisovin.magicspells.util.config.ConfigData;

public class StructureLocatorSpell extends InstantSpell {

    private final ConfigData<Integer> searchRadius;

    public StructureLocatorSpell(MagicConfig config, String spellName) {
        super(config, spellName);
        searchRadius = getConfigDataInt("search-radius", 10000);
    }

    @Override
    public CastResult cast(SpellData data) {
        Player player = data.caster() instanceof Player ? (Player) data.caster() : null;
        if (player == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        String structureName = (data.args() != null && data.args().length > 0) ? data.args()[0] : null;
        if (structureName == null || structureName.isEmpty()) {
            player.sendMessage("§cNo structure specified.");
            return new CastResult(PostCastAction.ALREADY_HANDLED, data);
        }

        World world = player.getWorld();
        StructureType structureType = null;
        try {
            NamespacedKey key = NamespacedKey.fromString(structureName);
            if (key != null) {
                structureType = Bukkit.getRegistry(StructureType.class).get(key);
            }
        } catch (Exception ignored) {}

        if (structureType == null) {
            player.sendMessage("§cUnknown structure: " + structureName);
            return new CastResult(PostCastAction.ALREADY_HANDLED, data);
        }

        int radius = searchRadius.get(data);
        var result = world.locateNearestStructure(player.getLocation(), structureType, radius, false);
        Location found = (result != null) ? result.getLocation() : null;
        if (found != null) {
            player.sendMessage("§aNearest " + structureName + ": §e" + found.getBlockX() + ", " + found.getBlockY() + ", " + found.getBlockZ());
        } else {
            player.sendMessage("§cNo structure found within search radius.");
        }
        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }
}
