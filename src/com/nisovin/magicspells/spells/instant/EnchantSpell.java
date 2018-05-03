package com.nisovin.magicspells.spells.instant;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Inventory;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.DebugHandler;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.InventoryUtil;

//This spell will only enchant player equipment for now.

public class EnchantSpell extends InstantSpell implements TargetedEntitySpell {

	//Where the enchantments are stored
	protected Map<Enchantment, Integer> enchantments;
	
	//Additional config options
	private String inventoryType;
	private boolean randomEnchantLevel;
	private boolean safeEnchant;
	private boolean forceLevel;
	private boolean toggle;

	private Random random = new Random();
	
	//Sizes of inventory slots
	private static final int hotbarSize = 9;
  	private static final int armorSize = 4;

	public EnchantSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		enchantments = new HashMap<>();
		if (!configKeyExists("enchantments")) throw new NullPointerException("There must be a configuration section called enchantments");
		ConfigurationSection enchantSection = getConfigSection("enchantments");
		for (String key: enchantSection.getKeys(false)) {
			enchantments.put(Enchantment.getByName(key), enchantSection.getInt(key));
		}
		
		/*Enchantment are defined by making an internal configration section within the config
		enchantments:
			MENDING: 1 # Where 1 is the level
		*/
		
		safeEnchant = getConfigBoolean("safe-enchantment", true);
		randomEnchantLevel = getConfigBoolean("random-enchantment-level", false);
		forceLevel = getConfigBoolean("force-level", false);
		inventoryType = getConfigString("inventory-type", "mainHand");
		toggle = getConfigBoolean("toggle", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			//cast the spell normally, if it does't work then we won't do anything
			boolean done = enchant(player);
			if (!done) return PostCastAction.ALREADY_HANDLED;
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

  	private boolean enchant(Player player) {
		
		//Pre-defining the types of inventories we might use.
		PlayerInventory inv = player.getInventory();
		//When we re-add all the modifier items, we need to make sure they are in the same exact spot.
		//Lets store them in indexes.
		ArrayList<Integer> indexes = new ArrayList<>();
		//Everything I'm going to enchant goes here.
		ArrayList<ItemStack> itemsToEnchant = new ArrayList<>();
		//The result of enchanting these items goes in here
		ArrayList<ItemStack> enchantedItems = new ArrayList<>();
		//The array that stores the armor information.
		ItemStack[] wearing = inv.getArmorContents();
		//A placeholder when referring to an item.
		ItemStack item;
		
		//Lets do something different based on the inventory of choice.
		switch (inventoryType) {
			case "mainHand":
				itemsToEnchant.add(inv.getItemInMainHand());
				break;
			case "offHand":
				itemsToEnchant.add(inv.getItemInOffHand());
				break;
			//This one is a bit tricky, I can't ignore AIR items so I'll have to include them in the array.
			case "wearing":
				for (int i = 0; i < armorSize; i++) {
					item = wearing[i];
					itemsToEnchant.add(item);
				}
				break;
			case "hotbar":
				for (int i = 0; i < hotbarSize; i++) {
					//Get the item, check if it is something then add it aswell as its index in the hotbar.
					item = inv.getItem(i);
					if (InventoryUtil.isNothing(item)) continue;
					itemsToEnchant.add(inv.getItem(i));
					indexes.add(i);
				}
				break;
			case "inventory":
				for (int i = 0; i < inv.getSize(); i++) {
					//Get the item, check if it is something then add it aswell as its index in the inventory.
					item = inv.getItem(i);
					if (InventoryUtil.isNothing(item)) continue;
					itemsToEnchant.add(inv.getItem(i));
					indexes.add(i);
				}
				break;
			default:
				//Well, someone didn't read the commit messages.
				MagicSpells.error("Invalid inventory-type defined; Use mainHand, offHand, wearing, hotbar or inventory.");
				throw new IllegalStateException();
		}
		//The spell will not bother with empty items at all.
		if (itemsToEnchant.size() <= 0) return false;
		//Another place holder when dealing with modified items.
		ItemStack newItem;
		/*Lets enchant those items and try to place them back in their respective place.
		We can make this process easier by referring to the INDEX they came from.*/
		switch (inventoryType) {
			case "mainHand":
				newItem = enchant(itemsToEnchant.get(0));
				inv.setItemInMainHand(newItem);
				break;
			case "offHand":
				newItem = enchant(itemsToEnchant.get(0));
				inv.setItemInOffHand(newItem);
				break;
			case "wearing":
				ItemStack[] wearingModified = new ItemStack[4];
				for (int i = 0; i < itemsToEnchant.size(); i++) {
					newItem = enchant(itemsToEnchant.get(i));
					//There is a problem with re-adding items to your inventory if you happen to ignore space.
					//Therefore, I add them in the array.
					wearingModified[i] = newItem;
				}
				/*Now I can replace the armor items in the same order they were collected.
				Thats why I needed to add for AIR materials.*/
				inv.setArmorContents(wearingModified);
				break;
			case "hotbar":
				for (int i = 0; i < itemsToEnchant.size(); i++) {
					newItem = enchant(itemsToEnchant.get(i));
					inv.setItem(indexes.get(i),newItem);
				}
				break;
			case "inventory":
				for (int i = 0; i < itemsToEnchant.size(); i++) {
					newItem = enchant(itemsToEnchant.get(i));
					inv.setItem(indexes.get(i),newItem);
				}
				break;
			default:
				throw new IllegalStateException();
		}
		//Let's force an update on the inventory when everything is done.
		player.updateInventory();
		return true;
	}
	
	//We'll need to return the modified enchanted item since we are adding it directly inside.
	private ItemStack enchant(ItemStack item) {
		ItemStack enchantedItem = item;
		for (Enchantment e: enchantments.keySet()) {
			//For each enchantment and their level. Lets finally enchant that item.
			enchant(enchantedItem, e, enchantments.get(e));
		}
		return enchantedItem;
	}

	private void enchant(ItemStack item, Enchantment enchant, int level) {
		/*The processing order gets tricky. Here is what should happen.
		If the level of the enchantment is 0, just remove that enchantment*/
		if (level <= 0) {
			item.removeEnchantment(enchant);
			return;
		}
		/*If that enchantment is already there. There is two things that can occur
		I can either force the level of the new enchantment or toggle it off.
		Otherwise, It will leave it as the same way.*/
		if (item.containsEnchantment(enchant)) {
			if (forceLevel) item.removeEnchantment(enchant);
			else if (toggle) {
				item.removeEnchantment(enchant);
				return;
			}
			else return;
		}
		/*Now, If I want to safely enchant the item, I'll have to check if that enchantment
		can even be applied from that item. If that is good then I want to check if applying this
		new enchantment is going to affect any of the other ones on the item.*/
		if (safeEnchant) {
			if (!enchant.canEnchantItem(item)) return;

			for (Enchantment e : item.getEnchantments().keySet()) {
				if (enchant.conflictsWith(e)) return;
			}
		}
		//Do they want a random enchantment level? Give them one where the min is always level 1
		if (randomEnchantLevel) level = random.nextInt(level) + 1;
		
		//I will finally safely/unsafely enchant the item.
		if (safeEnchant) item.addEnchantment(enchant, level);
		else item.addUnsafeEnchantment(enchant, level);
		return;
	}
	
	/*It only takes two functions to make an instant spell targeted. Good stuff...
	If they make this spell a subspell of a targeted spell, it should enchant the target PLAYER*/
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player) return enchant((Player)target);
		return false;
	}

	public boolean castAtEntity(LivingEntity target, float power) {
		if (target instanceof Player) return enchant((Player)target);
		return false;
	}

}
