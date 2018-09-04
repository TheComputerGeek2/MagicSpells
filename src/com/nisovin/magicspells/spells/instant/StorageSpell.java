package com.nisovin.magicspells.spells.instant;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;

@SuppressWarnings("Convert2Lambda")
public class StorageSpell extends InstantSpell {

    private int rows;
    private String storageTitle;
    boolean savesContents;
    //private List<Integer> staticSlots;
    //private String fillStaticSlots;
    //private ItemStack fillStaticSlotsItem;
    InventoryView inventory;

    File storagesMain;
    File storagesThis;

    public StorageSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        rows = getConfigInt("rows", 3) * 9;
        storageTitle = ChatColor.translateAlternateColorCodes('&', getConfigString("storage-title", internalName));
        savesContents = getConfigBoolean("saves-contents", true);
        //staticSlots = getConfigIntList("static-slots", null);
        //fillStaticSlots = getConfigString("fill-static-slots", "0");
        //fillStaticSlotsItem = Util.getItemStackFromString(fillStaticSlots);

        storagesMain = new File(MagicSpells.plugin.getDataFolder(), "storages");
        storagesThis = new File(storagesMain, internalName);

    }

    public PostCastAction castSpell(Player player,SpellCastState state, float power, String[] args) {
        if (state == SpellCastState.NORMAL) {
            openStorage(player);
            MagicSpells.scheduleRepeatingTask(new Runnable() {
                @Override
                public void run() {
                    inventory = player.getOpenInventory();
                    saveStorage(player, inventory);
                    if (player.getOpenInventory().equals(null)) {
                        return;
                    }
                }
            }, 0, 5);
        }
        playSpellEffects(EffectPosition.CASTER, player);
        return PostCastAction.HANDLE_NORMALLY;
    }

    public void saveStorage(Player player, InventoryView inventory) {
        if (inventory.getTopInventory().getTitle().equals(storageTitle) && savesContents) {
            YamlConfiguration storageData = new YamlConfiguration();
            storageData.set("Contents", inventory.getTopInventory().getContents());
            if (!storagesMain.exists()) {
                storagesMain.mkdirs();
            }
            if (!storagesThis.exists()) {
                storagesMain.mkdirs();
            }
            try {
                storageData.save(new File(storagesThis, player.getUniqueId().toString() + ".yml"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void openStorage(Player player) {
        Inventory storage = Bukkit.createInventory(player, rows, storageTitle);
        player.openInventory(storage);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player)event.getPlayer();

        if (event.getInventory().getTitle().equals(storageTitle)) {
            saveStorage(player, event.getView());
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player)event.getPlayer();

        if (event.getInventory().getTitle().equals(storageTitle)) {
            return;
        }

    }

}
