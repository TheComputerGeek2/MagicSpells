package com.nisovin.magicspells.spells.targeted;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ToggleSpell extends TargetedSpell {

    private List<String> typesToToggle;
    private int radius;

    public ToggleSpell (MagicConfig config,  String spellName) {
        super(config, spellName);

        typesToToggle = getConfigStringList("types-to-toggle", null);
        radius = getConfigInt("radius", 10);

    }

    @Override
    public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
        if (state == SpellCastState.NORMAL) {
            toggleBlocks(player.getLocation());
            playSpellEffects(EffectPosition.CASTER, player);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

    private void toggleBlocks(Location location) {
        List<Block> blocks = new ArrayList<>();
        for(int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
            for(int y = location.getBlockY() - radius; y <= location.getBlockY() + radius; y++) {
                for(int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                    Block block = location.getWorld().getBlockAt(x, y, z);

                    if (typesToToggle.contains(block.getType().toString().toLowerCase())) {
                        if (block.getBlockData() instanceof Bisected && block.getBlockData() instanceof Door) {
                            Bisected bisected = (Bisected) block.getBlockData();
                            if (bisected.getHalf() == Bisected.Half.BOTTOM) continue;
                        }
                        if (block.getBlockData() instanceof Openable) {
                            Openable openable = (Openable) block.getBlockData();

                            if (openable.isOpen()) openable.setOpen(false);
                            else openable.setOpen(true);

                            block.setBlockData(openable, true);
                            playSpellEffects(EffectPosition.SPECIAL, block.getLocation());
                        }
                        else if (block.getBlockData() instanceof Powerable){
                            Powerable powerable = (Powerable) block.getBlockData();

                            if (powerable.isPowered()) powerable.setPowered(false);
                            else powerable.setPowered(true);

                            block.setBlockData(powerable, true);
                            playSpellEffects(EffectPosition.SPECIAL, block.getLocation());
                        }
                    }
                }
            }
        }
    }

}
