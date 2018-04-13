package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.*;

// blockType,blockType2;OffsetRange;matchItem:count,matchItem2:count
public class RightClickBlockTypeInspectItemListener extends PassiveListener {

    Set<Material> materials = new HashSet<>();
    Map<MagicMaterial, List<PassiveSpell>> types = new HashMap<>();
    List<Material> matchMaterials = new ArrayList<>();
    List<Integer> matchMaterialsCount = new ArrayList<>();
    Integer range = 5;

    @Override
    public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
        String[] all = var.split(";");
        if (all.length > 0) {
            String[] split = all[0].split(",");
            for (String s : split) {
                s = s.trim();
                MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
                if (m != null) {
                    List<PassiveSpell> list = types.computeIfAbsent(m, material -> new ArrayList<>());
                    list.add(spell);
                    materials.add(m.getMaterial());
                } else {
                    MagicSpells.error("Invalid type on rightclickblocktypeinspectitem trigger '" + var + "' on passive spell '" + spell.getInternalName() + '\'');
                }
            }
        }
        if (all.length > 1) {
            range = Integer.parseInt(all[1]);
        }
        if (all.length > 2) {
            String[] split = all[2].split(",");
            for (String s : split) {
                String[] temp = s.split(":");
                s = s.trim();
                MagicMaterial m = MagicSpells.getItemNameResolver().resolveBlock(s);
                if (m != null) {
                    List<PassiveSpell> list = types.computeIfAbsent(m, material -> new ArrayList<>());
                    list.add(spell);
                    matchMaterials.add(m.getMaterial());
                    if (temp.length > 1) matchMaterialsCount.add(Integer.parseInt(temp[1])); else matchMaterialsCount.add(1);
                } else {
                    MagicSpells.error("Invalid type on rightclickblocktypeinspectitem trigger '" + var + "' on passive spell '" + spell.getInternalName() + '\'');
                }
            }
        }
    }

    @OverridePriority
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        List<PassiveSpell> list = getSpells(event.getClickedBlock(), event.getClickedBlock().getLocation());
        if (list != null) {
            Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
            for (PassiveSpell spell : list) {
                if (!PassiveListener.isCancelStateOk(spell, event.isCancelled())) continue;
                if (!spellbook.hasSpell(spell, false)) continue;
                boolean casted = spell.activate(event.getPlayer(), event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5));
                if (!PassiveListener.cancelDefaultAction(spell, casted)) continue;
                event.setCancelled(true);
            }
        }
    }

    private List<PassiveSpell> getSpells(Block block, Location location) {
        MaterialData data = block.getState().getData();
        for (Map.Entry<MagicMaterial, List<PassiveSpell>> entry : types.entrySet()) {
            if (entry.getKey().equals(data)) {
                if (!materials.contains(block.getType())) return null;
                Entity[] entities = location.getChunk().getEntities();
                List<Item> items = new ArrayList<>();
                for (Entity entity : entities) {
                    if (entity instanceof Item) {
                        int ix = Math.abs((int) entity.getLocation().getX());
                        int iy = Math.abs((int) entity.getLocation().getY());
                        int iz = Math.abs((int) entity.getLocation().getZ()) + 1;
                        int nx = Math.abs((int) location.getX());
                        int ny = Math.abs((int) location.getY());
                        int nz = Math.abs((int) location.getZ());
                        if ((nx + range > ix && nx - range < ix) ||
                                (ny + range > iy && ny - range < iy) ||
                                (nz + range > iz && nz - range < iz)) {
                            items.add((Item)entity);
                        }
                    }
                }
                if (items.size() == 0) return null;
                int counter = 0;
                List<Item> needRemove = new ArrayList<>();
                List<Integer> needRemoveCount = new ArrayList<>();
                for (int j = 0; j < matchMaterials.size(); j++) {
                    for (Item i : items) {
                        if (i.getItemStack().getType().equals(matchMaterials.get(j))) {
                            if (i.getItemStack().getAmount() == matchMaterialsCount.get(j)) {
                                needRemove.add(i);
                                needRemoveCount.add(0);
                                counter++;
                            } else if (i.getItemStack().getAmount() > matchMaterialsCount.get(j)) {
                                needRemove.add(i);
                                needRemoveCount.add(matchMaterialsCount.get(j));
                                counter++;
                            }
                        }
                    }
                }
                if (counter == matchMaterials.size()) {
                    for (int i = 0; i < needRemove.size(); i++) {
                        if (needRemoveCount.get(i) == 0) {
                            needRemove.get(i).remove();
                        } else {
                            ItemStack item = needRemove.get(i).getItemStack();
                            item.setAmount(item.getAmount() - needRemoveCount.get(i));
                            needRemove.get(i).setItemStack(item);
                        }
                    }
                    return entry.getValue();
                }
            }
        }
        return null;
    }
}
