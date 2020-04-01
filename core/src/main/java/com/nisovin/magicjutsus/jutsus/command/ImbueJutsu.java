package com.nisovin.magicjutsus.jutsus.command;

import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicjutsus.Perm;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.materials.ItemNameResolver;

// Advanced perm is for specifying the number of uses if it isn't normally allowed

public class ImbueJutsu extends CommandJutsu {

	private static final Pattern CAST_ARG_USES_PATTERN  = Pattern.compile("[0-9]+");

	private Set<Material> allowedItemTypes;
	private List<MagicMaterial> allowedItemMaterials;

	private int maxUses;
	private int defaultUses;

	private String key;
	private String strUsage;
	private String strItemName;
	private String strItemLore;
	private String strCantImbueItem;
	private String strCantImbueJutsu;

	private boolean consumeItem;
	private boolean leftClickCast;
	private boolean rightClickCast;
	private boolean allowSpecifyUses;
	private boolean requireTeachPerm;
	private boolean nameAndLoreHasUses;
	private boolean chargeReagentsForJutsuPerUse;

	public ImbueJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		allowedItemTypes = new HashSet<>();
		allowedItemMaterials = new ArrayList<>();

		List<String> allowed = getConfigStringList("allowed-items", null);
		if (allowed != null) {
			ItemNameResolver resolver = MagicJutsus.getItemNameResolver();
			for (String s : allowed) {
				MagicMaterial m = resolver.resolveItem(s);
				if (m == null) continue;
				allowedItemTypes.add(m.getMaterial());
				allowedItemMaterials.add(m);
			}
		}

		maxUses = getConfigInt("max-uses", 10);
		defaultUses = getConfigInt("default-uses", 5);

		key = "Imb" + internalName;
		strUsage = getConfigString("str-usage", "Usage: /cast imbue <jutsu> [uses]");
		strItemName = getConfigString("str-item-name", "");
		strItemLore = getConfigString("str-item-lore", "Imbued: %s");
		strCantImbueItem = getConfigString("str-cant-imbue-item", "You can't imbue that item.");
		strCantImbueJutsu = getConfigString("str-cant-imbue-jutsu", "You can't imbue that jutsu.");

		consumeItem = getConfigBoolean("consume-item", false);
		leftClickCast = getConfigBoolean("left-click-cast", true);
		rightClickCast = getConfigBoolean("right-click-cast", false);
		allowSpecifyUses = getConfigBoolean("allow-specify-uses", true);
		requireTeachPerm = getConfigBoolean("require-teach-perm", true);
		chargeReagentsForJutsuPerUse = getConfigBoolean("charge-reagents-for-jutsu-per-use", true);

		nameAndLoreHasUses = strItemName.contains("%u") || strItemLore.contains("%u");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Get item
			ItemStack inHand = player.getEquipment().getItemInMainHand();
			if (!allowedItemTypes.contains(inHand.getType())) {
				sendMessage(strCantImbueItem, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			boolean allowed = false;
			for (MagicMaterial m : allowedItemMaterials) {
				if (m.equals(inHand)) {
					allowed = true;
					break;
				}
			}
			if (!allowed) {
				sendMessage(strCantImbueItem, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// Check for already imbued
			if (getImbueData(inHand) != null) {
				sendMessage(strCantImbueItem, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[0]);
			if (jutsu == null) {
				sendMessage(strCantImbueJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!MagicJutsus.getJutsubook(player).hasJutsu(jutsu)) {
				sendMessage(strCantImbueJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			if (requireTeachPerm && !MagicJutsus.getJutsubook(player).canTeach(jutsu)) {
				sendMessage(strCantImbueJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			int uses = defaultUses;
			if (args.length > 1 && RegexUtil.matches(CAST_ARG_USES_PATTERN, args[1]) && (allowSpecifyUses || Perm.ADVANCED_IMBUE.has(player))) {
				uses = Integer.parseInt(args[1]);
				if (uses > maxUses) uses = maxUses;
				else if (uses <= 0) uses = 1;
			}
			
			if (chargeReagentsForJutsuPerUse && !Perm.NOREAGENTS.has(player)) {
				JutsuReagents reagents = jutsu.getReagents().multiply(uses);
				if (!hasReagents(player, reagents)) {
					sendMessage(strMissingReagents, player, args);
					return PostCastAction.ALREADY_HANDLED;
				}
				removeReagents(player, reagents);
			}
			
			setItemNameAndLore(inHand, jutsu, uses);
			setImbueData(inHand, jutsu.getInternalName() + ',' + uses);
			player.getEquipment().setItemInMainHand(inHand);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event) {
		if (event.useItemInHand() == Result.DENY) return;
		if (!event.hasItem()) return;
		Action action = event.getAction();
		if (!actionAllowedForCast(action)) return;
		ItemStack item = event.getItem();
		if (!allowedItemTypes.contains(item.getType())) return;
		
		boolean allowed = false;
		for (MagicMaterial m : allowedItemMaterials) {
			if (m.equals(item)) {
				allowed = true;
				break;
			}
		}
		if (!allowed) return;
		
		String imbueData = getImbueData(item);
		if (imbueData == null || imbueData.isEmpty()) return;
		String[] data = imbueData.split(",");
		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(data[0]);
		int uses = Integer.parseInt(data[1]);

		if (jutsu == null || uses <= 0) {
			Util.removeLoreData(item);
			return;
		}

		jutsu.castJutsu(event.getPlayer(), JutsuCastState.NORMAL, 1.0F, MagicJutsus.NULL_ARGS);
		uses--;
		if (uses <= 0) {
			if (consumeItem) event.getPlayer().getEquipment().setItemInMainHand(null);
			else {
				Util.removeLoreData(item);
				if (nameAndLoreHasUses) setItemNameAndLore(item, jutsu, 0);
			}
		} else {
			if (nameAndLoreHasUses) setItemNameAndLore(item, jutsu, uses);
			setImbueData(item, jutsu.getInternalName() + ',' + uses);
		}
	}
	
	private boolean actionAllowedForCast(Action action) {
		switch (action) {
			case RIGHT_CLICK_AIR:
			case RIGHT_CLICK_BLOCK:
				return rightClickCast;
			case LEFT_CLICK_AIR:
			case LEFT_CLICK_BLOCK:
				return leftClickCast;
			default:
				return false;
		}
	}
	
	private void setItemNameAndLore(ItemStack item, Jutsu jutsu, int uses) {
		ItemMeta meta = item.getItemMeta();
		if (!strItemName.isEmpty()) meta.setDisplayName(strItemName.replace("%s", jutsu.getName()).replace("%u", uses+""));
		if (!strItemLore.isEmpty()) meta.setLore(Arrays.asList(strItemLore.replace("%s", jutsu.getName()).replace("%u", uses+"")));
		item.setItemMeta(meta);
	}
	
	private void setImbueData(ItemStack item, String data) {
		Util.setLoreData(item, key + ':' + data);
	}
	
	private String getImbueData(ItemStack item) {
		String s = Util.getLoreData(item);
		if (s != null && s.startsWith(key + ':')) return s.replace(key + ':', "");
		return null;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}

}
