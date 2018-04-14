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
// when range == -1, offset position y will add one, and range is 0.1d.
public class RightClickBlockTypeInspectItemListener extends PassiveListener {

    Set<Material> materials = new HashSet<>();
    Map<MagicMaterial, List<PassiveSpell>> types = new HashMap<>();
    List<List<Material>> matchMaterials = new ArrayList<>();
    List<List<Integer>> matchMaterialsCount = new ArrayList<>();
    Double range = 5.0d;

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
            range = Double.parseDouble(all[1]);
        }
        if (all.length > 2) {
            String[] split = all[2].split(",");
            List<Material> tempMaterialList = new ArrayList<>();
            List<Integer> tempCountList = new ArrayList<>();
            for (String s : split) {
                String[] temp = s.split(":");
                s = s.trim();
                MagicMaterial m = MagicSpells.getItemNameResolver().resolveItem(s);
                if (m == null) { m = MagicSpells.getItemNameResolver().resolveBlock(s); }
                if (m != null) {
                    tempMaterialList.add(m.getMaterial());
                    if (temp.length > 1) tempCountList.add(Integer.parseInt(temp[1])); else tempCountList.add(1);
                } else {
                    MagicSpells.error("Invalid type on rightclickblocktypeinspectitem trigger '" + var + "' on passive spell '" + spell.getInternalName() + '\'');
                }
            }
            matchMaterials.add(tempMaterialList);
            matchMaterialsCount.add(tempCountList);
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
                        if (Math.floor(range) >= 0) {
                            if (range == 0) range = 0.1d;
//                        MagicSpells.error("range:" + range + ",ix:" + Math.floor(entity.getLocation().getX()) + ",iy:" + Math.floor(entity.getLocation().getY()) + ",iz:" + Math.floor(entity.getLocation().getZ()) + ",nx:" + location.getX() + ",ny:" + location.getY() + ",nz:" + location.getZ());
//                        MagicSpells.error("check1:" + (location.getX() + range > Math.floor(entity.getLocation().getX())));
//                        MagicSpells.error("check2:" + (location.getX() - range < Math.floor(entity.getLocation().getX())));
//                        MagicSpells.error("check3:" + (location.getY() + range > Math.floor(entity.getLocation().getY())));
//                        MagicSpells.error("check4:" + (location.getY() - range < Math.floor(entity.getLocation().getY())));
//                        MagicSpells.error("check5:" + (location.getZ() + range > Math.floor(entity.getLocation().getZ())));
//                        MagicSpells.error("check6:" + (location.getZ() - range < Math.floor(entity.getLocation().getZ())));
                            if (location.getX() + range >= Math.floor(entity.getLocation().getX()) && location.getX() - range <= Math.floor(entity.getLocation().getX()) &&
                                    location.getY() + range >= Math.floor(entity.getLocation().getY()) && location.getY() - range <= Math.floor(entity.getLocation().getY()) &&
                                    location.getZ() + range >= Math.floor(entity.getLocation().getZ()) && location.getZ() - range <= Math.floor(entity.getLocation().getZ())) {
                                items.add((Item)entity);
                            }
                        } else if (Math.floor(range) == -1) {
                            if (location.getX() + 0.1d >= Math.floor(entity.getLocation().getX()) && location.getX() - 0.1d <= Math.floor(entity.getLocation().getX()) &&
                                    location.getY() + 1.1d >= Math.floor(entity.getLocation().getY()) && location.getY() + 0.9d <= Math.floor(entity.getLocation().getY()) &&
                                    location.getZ() + 0.1d >= Math.floor(entity.getLocation().getZ()) && location.getZ() - 0.1d <= Math.floor(entity.getLocation().getZ())) {
                                items.add((Item)entity);
                            }
                        }
                    }
                }
                if (items.size() == 0) return null;
                int counter = 0;
                int index = -1;
                List<Item> needRemove = new ArrayList<>();
                List<Integer> needRemoveCount = new ArrayList<>();
                for (int h = 0; h < matchMaterials.size(); h++) {
                    counter = 0;
                    for (int j = 0; j < matchMaterials.get(h).size(); j++) {
                        for (Item i : items) {
                            if (i.getItemStack().getType().equals(matchMaterials.get(h).get(j))) {
                                if (i.getItemStack().getAmount() == matchMaterialsCount.get(h).get(j)) {
                                    needRemove.add(i);
                                    needRemoveCount.add(0);
                                    counter++;
                                } else if (i.getItemStack().getAmount() > matchMaterialsCount.get(h).get(j)) {
                                    needRemove.add(i);
                                    needRemoveCount.add(matchMaterialsCount.get(h).get(j));
                                    counter++;
                                }
                            }
                        }
                    }
                    if (counter == matchMaterials.get(h).size()) {
                        index = h;
                        break;
                    }
                }
//                MagicSpells.error("counter:" + counter + ",index:" + index);
                if (index != -1) {
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
