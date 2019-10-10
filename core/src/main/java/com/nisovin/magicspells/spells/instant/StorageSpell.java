package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageSpell extends InstantSpell {

    private String title;
    private int rows;

    private File storageFile;
    private YamlConfiguration storageConfig;

    private List<String> staticSlots;
    private List<String>  prefilledItems;

    private int saveInterval;
    private boolean saveContents;

    private ItemStack[] storedItems;

    public StorageSpell(MagicConfig config,  String spellName) {
        super(config, spellName);

        title = ChatColor.translateAlternateColorCodes('&', getConfigString("title", "Window Title " + spellName));
        rows = getConfigInt("rows", 1);
        if (rows > 6) rows = 6;

        staticSlots = getConfigStringList("static-slots", null);
        prefilledItems = getConfigStringList("prefilled-items", null);

        saveInterval = getConfigInt("save-delay", 0);
        saveContents = getConfigBoolean("save-contents", true);
    }

    @Override
    public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
        if (state == SpellCastState.NORMAL) {
            openStorageInventory(player);
            playSpellEffects(EffectPosition.CASTER, player);
        }
        return PostCastAction.HANDLE_NORMALLY;
    }

    private void openStorageInventory(Player caster) {
        Inventory storageInv = Bukkit.createInventory(caster, rows*9, title);
        storageFile = new File(MagicSpells.plugin.getDataFolder().getAbsolutePath() + "\\storages\\", caster.getUniqueId() + ".yml");

        if (storageFile.exists()) storageInv = loadInventory(storageInv, caster);

        if (prefilledItems != null) storageInv = processItemsAndSlots(prefilledItems, storageInv);
        if (staticSlots != null) storageInv = processItemsAndSlots(staticSlots, storageInv);

        caster.openInventory(storageInv);
    }

    private Inventory processItemsAndSlots(List<String> list, Inventory inventory) {
        for (String itemString : list) {
            if (!itemString.contains(":")) continue;

            String[] itemSplit = itemString.split(":");
            if (itemSplit[1] == null) continue;
            if (Util.getItemStackFromString(itemSplit[1]) == null) continue;

            int slot = -1;
            try{
                slot = Integer.parseInt(itemSplit[0]);
            } catch (NumberFormatException e) {
                DebugHandler.debugNumberFormat(e);
            }

            if (Util.getItemStackFromString(itemSplit[1]) == null) continue;

            ItemStack item = Util.getItemStackFromString(itemSplit[1]);
            inventory.setItem(slot, item);
        }
        return inventory;
    }

    private List<Integer> getSlots(List<String> list) {
        List<Integer> slots = new ArrayList<>();

        for (String string : list) {
            if (!string.contains(":")) continue;
            String[] stringArray = string.split(":");
            try{
                slots.add(Integer.parseInt(stringArray[0]));
            } catch (NumberFormatException e) {
                DebugHandler.debugNumberFormat(e);
            }
        }
        return slots;
    }

    private void saveInventory(Inventory inventory, Player player) {
        storageFile = new File(MagicSpells.plugin.getDataFolder().getAbsolutePath() + "\\storages\\", player.getUniqueId() + ".yml");

        if (!storageFile.exists()) {
            storageFile.getParentFile().mkdirs();
            try {
                storageFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        storageConfig = YamlConfiguration.loadConfiguration(storageFile);

        if (staticSlots != null) {
            List<Integer> staticSlotsSlots = getSlots(staticSlots);

            for (int slot: staticSlotsSlots) {
                inventory.setItem(slot, new ItemStack(Material.AIR));
            }
        }
        storedItems = inventory.getContents();

        storageConfig.set(internalName, storedItems);
        saveFile(storageFile, storageConfig);
    }

    private void saveFile(File file, YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Inventory loadInventory(Inventory inventory, Player player) {
        storageFile = new File(MagicSpells.plugin.getDataFolder().getAbsolutePath() + "\\storages\\", player.getUniqueId() + ".yml");
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);

        List<ItemStack> loadedItemsList = (List<ItemStack>) storageConfig.get(internalName);
        if (loadedItemsList == null) return null;

        ItemStack[] loadedItems = loadedItemsList.toArray(new ItemStack[0]);

        inventory.setContents(loadedItems);

        return inventory;
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Player player = (Player) event.getWhoClicked();

        if (!view.getTitle().equals(title)) return;
        if (staticSlots == null) return;

        for (int slot : getSlots(staticSlots)) {
            if (event.getRawSlot() != event.getSlot()) continue;
            if (event.getSlot() == slot) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        InventoryView view = event.getView();
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        if (!view.getTitle().equals(title)) return;
        saveInventory(inventory, player);
    }

}
