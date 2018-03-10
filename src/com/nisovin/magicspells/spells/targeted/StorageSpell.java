package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell.PostCastAction;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class StorageSpell extends InstantSpell {
    private int rows;
    private String chestName;
    private boolean savesContents;
    private List<Integer> staticSlots;
    private String fillStaticSlots;
    private ItemStack fillStaticSlotsItem;
    private int saveInterval;
    private Player invPlayer;
    private InventoryView inventory;

    File storagesMain;
    File storagesThis;

    public StorageSpell(MagicConfig config, String spellName) {
        super(config, spellName);
        storagesMain = new File(MagicSpells.plugin.getDataFolder(), "storages");
        storagesThis = new File(storagesMain, internalName);
        rows = getConfigInt("rows", 3) * 9;
        chestName = ChatColor.translateAlternateColorCodes('&', getConfigString("chest-name", internalName));
        savesContents = getConfigBoolean("saves-contents", true);
        saveInterval = getConfigInt("save-interval", 5);
        staticSlots = getConfigIntList("static-slots", null);
        fillStaticSlots = getConfigString("fill-static-slots", "0");
        fillStaticSlotsItem = Util.getItemStackFromString(fillStaticSlots);
    }

    public PostCastAction castSpell(final Player player, SpellCastState state, float power, String[] args) {
        if (state == SpellCastState.NORMAL) {
            Inventory storage = Bukkit.createInventory(player, rows, chestName);
            invPlayer = player;
            player.openInventory(storage);
            MagicSpells.scheduleRepeatingTask(new Runnable() {
                public void run() {
                    inventory = player.getOpenInventory();
                    if (inventory.getTopInventory().getTitle().equals(chestName)) {
                        saveStorage(player, inventory);
                    }

                }
            }, saveInterval, saveInterval);
        }

        playSpellEffects(EffectPosition.CASTER, player);
        return PostCastAction.HANDLE_NORMALLY;
    }

    @EventHandler
    public void saveStorage(Player invPlayer, InventoryView inventory) {
        if (inventory.getTopInventory().getTitle().equals(chestName) && savesContents) {
            YamlConfiguration storageData = new YamlConfiguration();
            storageData.set("Contents", inventory.getTopInventory().getContents());
            if (!storagesMain.exists()) {
                storagesMain.mkdirs();
            }

            if (!storagesThis.exists()) {
                storagesThis.mkdirs();
            }

            try {
                storageData.save(new File(storagesThis, invPlayer.getUniqueId().toString() + ".yml"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @EventHandler
    public void loadStorage(InventoryOpenEvent event) {
        Player player = (Player)event.getPlayer();
        if (event.getInventory().getTitle().equals(chestName)) {
            YamlConfiguration storageData = YamlConfiguration.loadConfiguration(new File(storagesThis, player.getUniqueId().toString() + ".yml"));
            ItemStack[] storageContents = (ItemStack[])((List)storageData.get("Contents")).toArray(new ItemStack[0]);
            event.getInventory().setContents(storageContents);
            if (!fillStaticSlots.equals(null)) {
                int staticSlotsSize = staticSlots.size();

                for(int i = 0; i < staticSlotsSize; ++i) {
                    event.getInventory().setItem(staticSlots.get(i), fillStaticSlotsItem);
                }
            }
            if (fillStaticSlots.equals(null)) {
                int staticSlotsSize = staticSlots.size();

                for(int i = 0; i < staticSlotsSize; ++i) {
                    event.getInventory().setItem(staticSlots.get(i), new ItemStack(Material.AIR));
                }
            }
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (event.getAction().equals(InventoryAction.COLLECT_TO_CURSOR)) {
            event.setCancelled(true);
        }
        if (event.getRawSlot() == -999) {
            event.setCancelled(true);
        }
        if (!staticSlots.isEmpty()) {
            if (event.getInventory().getTitle().equals(chestName) && staticSlots.contains(event.getRawSlot())) {
                event.setCancelled(true);
            }
        }
        return;

    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent event) {
        Set<Integer> staticSlotsSet = new HashSet(staticSlots);
        if (!staticSlots.isEmpty()) {
            if (event.getInventory().getTitle().equals(chestName) && event.getInventorySlots().stream().anyMatch((slotId) -> staticSlotsSet.contains(slotId))) {
                event.setCancelled(true);
            }
        }
        return;

    }
}
