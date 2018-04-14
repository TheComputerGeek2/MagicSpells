package com.nisovin.magicspells.spells.passive;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.util.Util;
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

// debug;blockType,blockType2;offsetRange;matchItem:count,matchItem2:count
// when range == -1, offset position y will add one, and range is 0.1d.
public class RightClickBlockTypeInspectItemListener extends PassiveListener {

    Set<Material> materials = new HashSet<>();
    Map<MagicMaterial, List<PassiveSpell>> types = new HashMap<>();
    List<List<Boolean>> isPredefineItem = new ArrayList<>();
    List<List<ItemStack>> matchItemStacks = new ArrayList<>();
    List<List<Material>> matchMaterials = new ArrayList<>();
    List<List<Integer>> matchMaterialsCount = new ArrayList<>();
    List<Double> range = new ArrayList<>();
    Boolean tempDebug = false;

    @Override
    public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
        String[] all = var.split(";");
        if (all.length > 0) {
            tempDebug = Boolean.parseBoolean(all[0]);
        }
        if (all.length > 1) {
            String[] split = all[1].split(",");
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
        if (all.length > 2) {
            range.add(Double.parseDouble(all[2]));
        }
        if (all.length > 3) {
            String[] split = all[3].split(",");
            List<ItemStack> tempItemStackList = new ArrayList<>();
            List<Material> tempMaterialList = new ArrayList<>();
            List<Integer> tempCountList = new ArrayList<>();
            List<Boolean> tempIsPredefineList = new ArrayList<>();
            for (String s : split) {
                boolean tempIsPredefine = false;
                String[] temp = s.split(":");
                if (temp[0].contains("PRE|")) {
                    String preName = temp[0].replace("PRE|", "");
                    ItemStack isa = Util.getItemStackFromString(preName);
                    if (temp.length > 1) {
                        tempCountList.add(Integer.parseInt(temp[1]));
                        isa.setAmount(Integer.parseInt(temp[1]));
                    } else {
                        tempCountList.add(1);
                        isa.setAmount(1);
                    }
                    tempItemStackList.add(isa);
                    tempMaterialList.add(null);
                    tempIsPredefine = true;
                } else {
                    s = s.trim();
                    MagicMaterial m = MagicSpells.getItemNameResolver().resolveItem(s);
                    if (m == null) { m = MagicSpells.getItemNameResolver().resolveBlock(s); }
                    if (m != null) {
                        tempItemStackList.add(null);
                        tempMaterialList.add(m.getMaterial());
                        if (temp.length > 1) tempCountList.add(Integer.parseInt(temp[1])); else tempCountList.add(1);
                    } else {
                        MagicSpells.error("Invalid type on rightclickblocktypeinspectitem trigger '" + var + "' on passive spell '" + spell.getInternalName() + '\'');
                    }
                }
                tempIsPredefineList.add(tempIsPredefine);
            }
            matchItemStacks.add(tempItemStackList);
            matchMaterials.add(tempMaterialList);
            matchMaterialsCount.add(tempCountList);
            isPredefineItem.add(tempIsPredefineList);
        }
        if (tempDebug) {
            // DEBUG OUTPUT START
            MagicSpells.error("============================= Icy Debug Output Start =============================");
            MagicSpells.error("matchItemStacks size: " + matchItemStacks.size());
            MagicSpells.error("matchMaterials size: " + matchMaterials.size());
            MagicSpells.error("matchMaterialsCount size: " + matchMaterialsCount.size());
            MagicSpells.error("==================================================================================");
            for (int i = 0; i < matchMaterials.size(); i++) {
                List<String> temp = new ArrayList<>();
                for (int j = 0; j < matchMaterials.get(i).size(); j++) {
                    if (isPredefineItem.get(i).get(j)) {
                        temp.add(matchMaterials.get(i).get(j) + "x" + matchMaterialsCount.get(i).get(j));
                    } else {
                        temp.add(matchMaterials.get(i).get(j) + "x" + matchMaterialsCount.get(i).get(j));
                    }
                }
                MagicSpells.error("Range: " + range.get(i) + ", Items:" + String.join(" ", temp));
            }
            MagicSpells.error("============================= Icy Debug Output End   =============================");
            // DEBUG OUTPUT END
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
                for (int h = 0; h < range.size(); h++) {
                    double sRange = range.get(h);
                    List<ItemStack> sPredefine = matchItemStacks.get(h);
                    List<Material> sMaterials = matchMaterials.get(h);
                    List<Integer> sCounts = matchMaterialsCount.get(h);
                    List<Boolean> sIsPredefine = isPredefineItem.get(h);
                    if (tempDebug) {
                        MagicSpells.error("[" + h + "]sPredefine size: " + sPredefine.size());
                        MagicSpells.error("[" + h + "]sMaterials size: " + sMaterials.size());
                        MagicSpells.error("[" + h + "]sCounts size: " + sCounts.size());
                        List<String> debugMaterials1 = new ArrayList<>();
                        for (int i = 0; i < sMaterials.size(); i++) {
                            if (sIsPredefine.get(i)) {
                                debugMaterials1.add(sPredefine.get(i).getType().name() + "x" + sCounts.get(i));
                            } else {
                                debugMaterials1.add(sMaterials.get(i).name() + "x" + sCounts.get(i));
                            }
                        }
                        MagicSpells.error("[" + h + "]ready: r:" + sRange + " I:" + String.join(",", debugMaterials1));
                    }
                    Entity[] entities = location.getChunk().getEntities();
                    List<Item> itemsMatchRange = new ArrayList<>();
                    for (Entity entity : entities) {
                        if (entity instanceof Item) {
                            if (Math.floor(sRange) >= 0) {
                                if (sRange == 0) range.set(h, 0.1d);
                                if (tempDebug) {
                                    String name = ((Item)entity).getItemStack().getType().name();
                                    MagicSpells.error("[" + h + "," + name + "]range:" + sRange + ",ix:" + Math.floor(entity.getLocation().getX()) + ",iy:" + Math.floor(entity.getLocation().getY()) + ",iz:" + Math.floor(entity.getLocation().getZ()) + ",nx:" + location.getX() + ",ny:" + location.getY() + ",nz:" + location.getZ());
                                    MagicSpells.error("[" + h + "," + name + "]check1:" + (location.getX() + sRange > Math.floor(entity.getLocation().getX())));
                                    MagicSpells.error("[" + h + "," + name + "]check2:" + (location.getX() - sRange < Math.floor(entity.getLocation().getX())));
                                    MagicSpells.error("[" + h + "," + name + "]check3:" + (location.getY() + sRange > Math.floor(entity.getLocation().getY())));
                                    MagicSpells.error("[" + h + "," + name + "]check4:" + (location.getY() - sRange < Math.floor(entity.getLocation().getY())));
                                    MagicSpells.error("[" + h + "," + name + "]check5:" + (location.getZ() + sRange > Math.floor(entity.getLocation().getZ())));
                                    MagicSpells.error("[" + h + "," + name + "]check6:" + (location.getZ() - sRange < Math.floor(entity.getLocation().getZ())));
                                }
                                if (location.getX() + sRange >= Math.floor(entity.getLocation().getX()) && location.getX() - sRange <= Math.floor(entity.getLocation().getX()) &&
                                        location.getY() + sRange >= Math.floor(entity.getLocation().getY()) && location.getY() - sRange <= Math.floor(entity.getLocation().getY()) &&
                                        location.getZ() + sRange >= Math.floor(entity.getLocation().getZ()) && location.getZ() - sRange <= Math.floor(entity.getLocation().getZ())) {
                                    itemsMatchRange.add((Item)entity);
                                }
                            } else if (Math.floor(sRange) == -1) {
                                if (location.getX() + 0.1d >= Math.floor(entity.getLocation().getX()) && location.getX() - 0.1d <= Math.floor(entity.getLocation().getX()) &&
                                        location.getY() + 1.1d >= Math.floor(entity.getLocation().getY()) && location.getY() + 0.9d <= Math.floor(entity.getLocation().getY()) &&
                                        location.getZ() + 0.1d >= Math.floor(entity.getLocation().getZ()) && location.getZ() - 0.1d <= Math.floor(entity.getLocation().getZ())) {
                                    itemsMatchRange.add((Item)entity);
                                }
                            }
                        }
                    }
                    if (itemsMatchRange.size() > 0) {
                        if (tempDebug) {
                            List<String> debugMaterials2 = new ArrayList<>();
                            for (int i = 0; i < itemsMatchRange.size(); i++) {
                                debugMaterials2.add(itemsMatchRange.get(i).getItemStack().getType().name() + "x" + itemsMatchRange.get(i).getItemStack().getAmount());
                            }
                            MagicSpells.error("[" + h + "]filter: " + String.join(",", debugMaterials2));
                        }
                        int counter = 0;
                        List<Item> needRemove = new ArrayList<>();
                        List<Integer> needRemoveCount = new ArrayList<>();
                        List<Boolean> bIsPredefine = new ArrayList<>(sIsPredefine);
                        List<ItemStack> bPredefine = new ArrayList<>(sPredefine);
                        List<Material> bMaterials = new ArrayList<>(sMaterials);
                        List<Integer> bCounts = new ArrayList<>(sCounts);
                        for (Item item : itemsMatchRange) {
                            for (int j = 0; j < bMaterials.size(); j++) {
                                MagicSpells.error("[" + h + "]if predefine: " + bIsPredefine.get(j));
                                boolean matchItemWithoutCount = false;
                                if (bIsPredefine.get(j)) {
                                    MagicSpells.error("[" + h + "]predefine match: " + item.getItemStack().isSimilar(bPredefine.get(j)));
                                    if (item.getItemStack().isSimilar(bPredefine.get(j))) {
                                        matchItemWithoutCount = true;
                                    }
                                } else {
                                    if (item.getItemStack().getType().equals(bMaterials.get(j))) {
                                        matchItemWithoutCount = true;
                                    }
                                }
                                if (matchItemWithoutCount) {
                                    if (item.getItemStack().getAmount() == bCounts.get(j)) {
                                        needRemove.add(item);
                                        needRemoveCount.add(0);
                                        counter++;
                                        bIsPredefine.remove(j);
                                        bPredefine.remove(j);
                                        bMaterials.remove(j);
                                        bCounts.remove(j);
                                    } else if (item.getItemStack().getAmount() > bCounts.get(j)) {
                                        needRemove.add(item);
                                        needRemoveCount.add(bCounts.get(j));
                                        counter++;
                                        bIsPredefine.remove(j);
                                        bPredefine.remove(j);
                                        bMaterials.remove(j);
                                        bCounts.remove(j);
                                    }
                                }
                            }
                        }
                        if (counter == sMaterials.size()) {
                            if (tempDebug) {
                                List<String> debugMaterials3 = new ArrayList<>();
                                for (int i = 0; i < sMaterials.size(); i++) {
                                    if (sIsPredefine.get(i)) {
                                        debugMaterials3.add("PRE|" + sPredefine.get(i).getType().name() + "x" + sCounts.get(i));
                                    } else {
                                        debugMaterials3.add(sMaterials.get(i).name() + "x" + sCounts.get(i));
                                    }
                                }
                                MagicSpells.error("[" + h + "]remove: " + String.join(",", debugMaterials3));
                            }
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
            }
        }
        return null;
    }
}
