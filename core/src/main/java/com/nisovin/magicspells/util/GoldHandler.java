package com.nisovin.magicspells.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GoldHandler {

    ItemStack copperCoin;
    ItemStack silverCoin;
    ItemStack goldCoin;

    public GoldHandler() {
        int maxStack = 64;

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();
    }

    public void takeGold(int amount, Player player) {
        Inventory inv = player.getInventory();

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        stackGold(player);

        //TAKE GOLD
        if (inv.contains(Material.GOLD_INGOT) && amount/4096 >= 1 && amount != 0) {
            for (ItemStack is : inv.all(Material.GOLD_INGOT).values()) {
                if (is.isSimilar(goldCoin)) {
                    while (amount - 4096 >= 0 && amount != 0 && is.getAmount() != 0) {
                        is.setAmount(is.getAmount() - 1);
                        amount -= 4096;
                    }
                }
            }
        }

        //TAKE SILVER
        if (inv.contains(Material.IRON_INGOT) && amount/64 >= 1 && amount != 0) {
            for (ItemStack is : inv.all(Material.IRON_INGOT).values()) {
                if (is.isSimilar(silverCoin)) {
                    while (amount - 64 >= 0 && amount != 0 && is.getAmount() != 0) {
                        is.setAmount(is.getAmount() - 1);
                        amount -= 64;
                    }
                }
            }
        }

        //TAKE COPPER
        if (inv.contains(Material.IRON_NUGGET) && amount != 0) {
            for (ItemStack is : inv.all(Material.IRON_NUGGET).values()) {
                if (is.isSimilar(copperCoin)) {
                    while (amount - 1 >= 0 && amount != 0 && is.getAmount() != 0) {
                        is.setAmount(is.getAmount() - 1);
                        amount -= 1;
                    }
                }
            }
        }

        //SILVER -> COPPER
        if (inv.contains(Material.IRON_INGOT) && amount != 0) {
            copperify(player);

            if (inv.contains(Material.IRON_NUGGET)) {
                for (ItemStack is : inv.all(Material.IRON_NUGGET).values()) {
                    if (is.isSimilar(copperCoin)) {
                        while (amount - 1 >= 0 && amount != 0 && is.getAmount() != 0) {
                            is.setAmount(is.getAmount() - 1);
                            amount -= 1;
                        }
                    }
                }
            }
        }

        //GOLD -> COPPER
        if (inv.contains(Material.GOLD_INGOT) && amount != 0) {
            silverify(player);

            if (inv.contains(Material.IRON_INGOT)) {
                for (ItemStack is : inv.all(Material.IRON_INGOT).values()) {
                    if (is.isSimilar(silverCoin)) {
                        while (amount - 64 >= 0 && amount != 0 && is.getAmount() != 0) {
                            is.setAmount(is.getAmount() - 1);
                            amount -= 64;
                        }
                    }
                }

                if (amount != 0) copperify(player);
                if (inv.contains(Material.IRON_NUGGET)) {
                    for (ItemStack is : inv.all(Material.IRON_NUGGET).values()) {
                        if (is.isSimilar(copperCoin)) {
                            while (amount - 1 >= 0 && amount != 0 && is.getAmount() != 0) {
                                is.setAmount(is.getAmount() - 1);
                                amount -= 1;
                            }
                        }
                    }
                }
            }
        }
    }

    public void giveGold(int amount, Player player) {
        Inventory inv = player.getInventory();

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        stackGold(player);

        //ADD GOLD
        if (amount / 4096 >= 1 && amount != 0) {
            while (amount - 4096 >= 0 && amount != 0) {
                player.getInventory().addItem(goldCoin);
                amount -= 4096;
            }
        }

        //ADD SILVER
        if (amount / 64 >= 1 && amount != 0) {
            while (amount - 64 >= 0 && amount != 0) {
                player.getInventory().addItem(silverCoin);
                amount -= 64;
            }
        }

        //ADD COPPER
        if (amount != 0) {
            while (amount - 1 >= 0 && amount != 0) {
                player.getInventory().addItem(copperCoin);
                amount -= 1;
            }
        }
    }

    public int getTotalGold(Player player) {
        Inventory inv = player.getInventory();
        int totalCount = 0;

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        if (inv.contains(Material.IRON_NUGGET)) {
            for (ItemStack is : inv.all(Material.IRON_NUGGET).values()) {
                if (is.isSimilar(copperCoin)) {
                    totalCount += is.getAmount();
                }
            }
        }

        if (inv.contains(Material.IRON_INGOT)) {
            for (ItemStack is : inv.all(Material.IRON_INGOT).values()) {
                if (is.isSimilar(silverCoin)) {
                    totalCount += is.getAmount() * 64;
                }
            }
        }

        if (inv.contains(Material.GOLD_INGOT)) {
            for (ItemStack is : inv.all(Material.GOLD_INGOT).values()) {
                if (is.isSimilar(goldCoin)) {
                    totalCount += is.getAmount() * 4096;
                }
            }
        }
        return totalCount;
    }

    public int getCopper(Player player) {
        Inventory inv = player.getInventory();
        int copperCount = 0;

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        if (inv.contains(Material.IRON_NUGGET)) {
            for (ItemStack is : inv.all(Material.IRON_NUGGET).values()) {
                if (is.isSimilar(copperCoin)) {
                    copperCount += is.getAmount();
                }
            }
        }

        return copperCount;
    }

    public int getSilver(Player player) {
        Inventory inv = player.getInventory();
        int silverCount = 0;

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        if (inv.contains(Material.IRON_INGOT)) {
            for (ItemStack is : inv.all(Material.IRON_INGOT).values()) {
                if (is.isSimilar(silverCoin)) {
                    silverCount += is.getAmount();
                }
            }
        }

        return silverCount;
    }

    public int getGold(Player player) {
        Inventory inv = player.getInventory();
        int goldCount = 0;

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        if (inv.contains(Material.GOLD_INGOT)) {
            for (ItemStack is : inv.all(Material.GOLD_INGOT).values()) {
                if (is.isSimilar(goldCoin)) {
                    goldCount += is.getAmount();
                }
            }
        }

        return goldCount;
    }

    public double getRemainingSilver(int amount) {
        double remainingSilver = amount / 64;
        return remainingSilver;
    }

    public void removeCopper(Player player, int amount) {
        Inventory inv = player.getInventory();

        copperCoin = getCopperStack();

        if (getCopper(player) >= amount) {
            if (inv.contains(Material.IRON_NUGGET)) {
                for (ItemStack is : inv.all(Material.IRON_NUGGET).values()) {
                    if (is.isSimilar(copperCoin) && amount > 0) {
                        if (amount - is.getAmount() < 0) {
                            int preAmount = amount;

                            is.setAmount(is.getAmount() - preAmount);
                            amount -= amount;
                        }
                        else {
                            int preAmount = amount;

                            amount -= is.getAmount();
                            is.setAmount(is.getAmount() - preAmount);
                        }
                    }
                    else return;
                }
            }
        }
    }

    public void removeSilver(Player player, int amount) {
        Inventory inv = player.getInventory();

        silverCoin = silverStack();

        if (getSilver(player) >= amount) {
            if (inv.contains(Material.IRON_INGOT)) {
                for (ItemStack is : inv.all(Material.IRON_INGOT).values()) {
                    if (is.isSimilar(silverCoin) && amount > 0) {
                        if (amount - is.getAmount() < 0) {
                            int preAmount = amount;

                            is.setAmount(is.getAmount() - preAmount);
                            amount -= amount;
                        }
                        else {
                            int preAmount = amount;

                            amount -= is.getAmount();
                            is.setAmount(is.getAmount() - preAmount);
                        }
                    }
                    else return;
                }
            }
        }
    }

    public void removeGold(Player player, int amount) {
        Inventory inv = player.getInventory();

        goldCoin = goldStack();

        if (getGold(player) >= amount) {
            if (inv.contains(Material.GOLD_INGOT)) {
                for (ItemStack is : inv.all(Material.GOLD_INGOT).values()) {
                    if (is.isSimilar(goldCoin) && amount > 0) {
                        if (amount - is.getAmount() < 0) {
                            int preAmount = amount;

                            is.setAmount(is.getAmount() - preAmount);
                            amount -= amount;
                        }
                        else {
                            int preAmount = amount;

                            amount -= is.getAmount();
                            is.setAmount(is.getAmount() - preAmount);
                        }
                    }
                    else return;
                }
            }
        }
    }

    public void copperify(Player player) {
        Inventory inv = player.getInventory();

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        copperCoin.setAmount(64);

        if (inv.contains(Material.IRON_INGOT)) {
            for (ItemStack is : inv.all(Material.IRON_INGOT).values()) {
                if (is.isSimilar(silverCoin)) {
                    is.setAmount(is.getAmount() - 1);
                    inv.addItem(copperCoin);

                    return;
                }
            }
        }
    }

    public void silverify(Player player) {
        Inventory inv = player.getInventory();

        copperCoin = getCopperStack();
        silverCoin = silverStack();
        goldCoin = goldStack();

        silverCoin.setAmount(64);

        if (inv.contains(Material.GOLD_INGOT)) {
            for (ItemStack is : inv.all(Material.GOLD_INGOT).values()) {
                if (is.isSimilar(goldCoin)) {
                    is.setAmount(is.getAmount() - 1);
                    inv.addItem(silverCoin);

                    return;
                }
            }
        }
    }

    public void stackGold(Player player) {
        int copperCount = getCopper(player);
        int silverCount = getSilver(player);
        int goldCount = getGold(player);

        Inventory inv = player.getInventory();

        if (copperCount > 0) {
            int stacks = copperCount/64;

            inv.remove(Material.IRON_NUGGET);

            while (stacks >= 1 && copperCount >= 64) {
                copperCoin.setAmount(64);

                inv.addItem(copperCoin);
                stacks -= 1;
                copperCount -= 64;
            }
            while (copperCount >= 1) {
                copperCoin.setAmount(1);

                inv.addItem(copperCoin);
                copperCount -= 1;
            }
        }
        if (silverCount > 0) {
            int stacks = silverCount/64;

            inv.remove(Material.IRON_INGOT);

            while (stacks >= 1 && silverCount >= 64) {
                silverCoin.setAmount(64);

                inv.addItem(silverCoin);
                stacks -= 1;
                silverCount -= 64;
            }
            while (silverCount >= 1) {
                silverCoin.setAmount(1);

                inv.addItem(silverCoin);
                silverCount -= 1;
            }
        }
        if (goldCount > 0) {
            int stacks = goldCount/64;

            inv.remove(Material.GOLD_INGOT);

            while (stacks >= 1 && goldCount >= 64) {
                goldCoin.setAmount(64);

                inv.addItem(goldCoin);
                stacks -= 1;
                goldCount -= 64;
            }
            while (goldCount >= 1) {
                goldCoin.setAmount(1);

                inv.addItem(goldCoin);
                goldCount -= 1;
            }
        }
    }

    public ItemStack getCopperStack() {
        copperCoin = new ItemStack(Material.IRON_NUGGET);

        ItemMeta copperMeta = copperCoin.getItemMeta();
        copperMeta.setDisplayName(ChatColor.GOLD + "Copper Coin");

        List<String> copperLore = new ArrayList<>();

        copperLore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "A coin made of pure copper, used as currency for the");
        copperLore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "smallest of purchases, such as food.");

        copperMeta.setLore(copperLore);

        copperCoin.setItemMeta(copperMeta);
        return copperCoin;
    }

    public ItemStack silverStack() {
        silverCoin = new ItemStack(Material.IRON_INGOT);

        ItemMeta silverMeta = silverCoin.getItemMeta();
        silverMeta.setDisplayName(ChatColor.GOLD + "Silver Coin");

        List<String> silverLore = new ArrayList<>();

        silverLore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "A coin made of pure silver, used as currency for most");
        silverLore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "purchases.");

        silverMeta.setLore(silverLore);

        silverCoin.setItemMeta(silverMeta);
        return silverCoin;
    }

    public ItemStack goldStack() {
        goldCoin = new ItemStack(Material.GOLD_INGOT);

        ItemMeta goldMeta = goldCoin.getItemMeta();
        goldMeta.setDisplayName(ChatColor.GOLD + "Gold Coin");

        List<String> goldLore = new ArrayList<>();

        goldLore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "A coin made of pure gold, used as currency for");
        goldLore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "high-end purchases.");

        goldMeta.setLore(goldLore);

        goldCoin.setItemMeta(goldMeta);
        return goldCoin;
    }

}
