package com.nisovin.magicjutsus.jutsus.command;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import com.nisovin.magicjutsus.Perm;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.materials.MagicMaterial;
import com.nisovin.magicjutsus.util.ItemUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.JutsuReagents;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.Util;

public class ScrollJutsu extends CommandJutsu {

	private static final Pattern CAST_ARGUMENT_USE_COUNT_PATTERN = Pattern.compile("^-?[0-9]+$");
	private static final Pattern SCROLL_DATA_USES_PATTERN = Pattern.compile("^[0-9]+$");

	private List<String> predefinedScrolls;
	private Map<Integer, Jutsu> predefinedScrollJutsus;
	private Map<Integer, Integer> predefinedScrollUses;

	private MagicMaterial itemType;

	private String strUsage;
	private String strOnUse;
	private String strNoJutsu;
	private String strUseFail;
	private String strCantTeach;
	private String strScrollName;
	private String strScrollSubtext;

	private int maxUses;
	private int defaultUses;

	private boolean castForFree;
	private boolean leftClickCast;
	private boolean rightClickCast;
	private boolean ignoreCastPerm;
	private boolean requireTeachPerm;
	private boolean textContainsUses;
	private boolean bypassNormalChecks;
	private boolean removeScrollWhenDepleted;
	private boolean requireScrollCastPermOnUse;
	private boolean chargeReagentsForJutsuPerCharge;
		
	public ScrollJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		predefinedScrolls = getConfigStringList("predefined-scrolls", null);

		itemType = MagicJutsus.getItemNameResolver().resolveItem(getConfigString("item-id", "paper"));

		strUsage = getConfigString("str-usage", "You must hold a single blank paper \nand type /cast scroll <jutsu> <uses>.");
		strOnUse = getConfigString("str-on-use", "Jutsu Scroll: %s used. %u uses remaining.");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu by that name.");
		strUseFail = getConfigString("str-use-fail", "Unable to use this scroll right now.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a scroll with that jutsu.");
		strScrollName = getConfigString("str-scroll-name", "Magic Scroll: %s");
		strScrollSubtext = getConfigString("str-scroll-subtext", "Uses remaining: %u");

		maxUses = getConfigInt("max-uses", 10);
		defaultUses = getConfigInt("default-uses", 5);

		castForFree = getConfigBoolean("cast-for-free", true);
		leftClickCast = getConfigBoolean("left-click-cast", false);
		rightClickCast = getConfigBoolean("right-click-cast", true);
		ignoreCastPerm = getConfigBoolean("ignore-cast-perm", false);
		requireTeachPerm = getConfigBoolean("require-teach-perm", true);
		bypassNormalChecks = getConfigBoolean("bypass-normal-checks", false);
		removeScrollWhenDepleted = getConfigBoolean("remove-scroll-when-depleted", true);
		requireScrollCastPermOnUse = getConfigBoolean("require-scroll-cast-perm-on-use", true);
		chargeReagentsForJutsuPerCharge = getConfigBoolean("charge-reagents-for-jutsu-per-charge", false);

		textContainsUses = strScrollName.contains("%u") || strScrollSubtext.contains("%u");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (predefinedScrolls == null || predefinedScrolls.isEmpty()) return;

		predefinedScrollJutsus = new HashMap<>();
		predefinedScrollUses = new HashMap<>();
		for (String s : predefinedScrolls) {
			String[] data = s.split(" ");
			try {
				int id = Integer.parseInt(data[0]);
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(data[1]);
				int uses = defaultUses;
				if (data.length > 2) uses = Integer.parseInt(data[2]);
				if (id > 0 && jutsu != null) {
					predefinedScrollJutsus.put(id, jutsu);
					predefinedScrollUses.put(id, uses);
				} else MagicJutsus.error("ScrollJutsu '" + internalName + "' has invalid predefined scroll: " + s);
			} catch (Exception e) {
				MagicJutsus.error("ScrollJutsu '" + internalName + "' has invalid predefined scroll: " + s);
			}
		}
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			ItemStack inHand = player.getEquipment().getItemInMainHand();
			if (inHand.getAmount() != 1 || !itemType.equals(inHand)) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Jutsu jutsu = MagicJutsus.getJutsuByInGameName(args[0]);
			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			if (jutsu == null || jutsubook == null || !jutsubook.hasJutsu(jutsu)) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;			
			}
			if (requireTeachPerm && !jutsubook.canTeach(jutsu)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			int uses = defaultUses;
			if (args.length > 1 && RegexUtil.matches(CAST_ARGUMENT_USE_COUNT_PATTERN, args[1])) uses = Integer.parseInt(args[1]);
			if (uses > maxUses || (maxUses > 0 && uses <= 0)) uses = maxUses;
			
			if (chargeReagentsForJutsuPerCharge && uses > 0) {
				JutsuReagents reagents = jutsu.getReagents().multiply(uses);
				if (!hasReagents(player, reagents)) {
					sendMessage(strMissingReagents, player, args);
					return PostCastAction.ALREADY_HANDLED;
				}
				removeReagents(player, reagents);
			}
			
			inHand = createScroll(jutsu, uses, inHand);
			player.getEquipment().setItemInMainHand(inHand);
			
			sendMessage(formatMessage(strCastSelf, "%s", jutsu.getName()), player, args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	public ItemStack createScroll(Jutsu jutsu, int uses, ItemStack item) {
		if (item == null) item = itemType.toItemStack(1);
		item.setDurability((short) 0);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', strScrollName.replace("%s", jutsu.getName()).replace("%u", (uses >= 0 ? uses + "" : "many"))));
		if (strScrollSubtext != null && !strScrollSubtext.isEmpty()) {
			List<String> lore = new ArrayList<>();
			lore.add(ChatColor.translateAlternateColorCodes('&', strScrollSubtext.replace("%s", jutsu.getName()).replace("%u", (uses >= 0 ? uses + "" : "many"))));
			meta.setLore(lore);
		}
		item.setItemMeta(meta);
		Util.setLoreData(item, internalName + ':' + jutsu.getInternalName() + (uses > 0 ? "," + uses : ""));
		item = ItemUtil.addFakeEnchantment(item);
		return item;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) return tabCompleteJutsuName(sender, args[0]);
		return null;
	}
	
	private String getJutsuDataFromScroll(ItemStack item) {
		String loreData = Util.getLoreData(item);
		if (loreData != null && loreData.startsWith(internalName + ':')) return loreData.replace(internalName + ':', "");
		return null;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!actionAllowedForCast(event.getAction())) return;
		Player player = event.getPlayer();
		ItemStack inHand = player.getEquipment().getItemInMainHand();
		if (itemType.getMaterial() != inHand.getType() || inHand.getAmount() > 1) return;
		
		// Check for predefined scroll
		if (inHand.getDurability() > 0 && predefinedScrollJutsus != null) {
			Jutsu jutsu = predefinedScrollJutsus.get(Integer.valueOf(inHand.getDurability()));
			if (jutsu != null) {
				int uses = predefinedScrollUses.get(Integer.valueOf(inHand.getDurability()));
				inHand = createScroll(jutsu, uses, inHand);
				player.getEquipment().setItemInMainHand(inHand);
			}
		}
		
		// Get scroll data (jutsu and uses)
		String scrollDataString = getJutsuDataFromScroll(inHand);
		if (scrollDataString == null || scrollDataString.isEmpty()) return;
		String[] scrollData = scrollDataString.split(",");
		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(scrollData[0]);
		if (jutsu == null) return;
		int uses = 0;
		if (scrollData.length > 1 && RegexUtil.matches(SCROLL_DATA_USES_PATTERN, scrollData[1])) uses = Integer.parseInt(scrollData[1]);

		if (requireScrollCastPermOnUse && !MagicJutsus.getJutsubook(player).canCast(this)) {
			sendMessage(strUseFail, player, MagicJutsus.NULL_ARGS);
			return;
		}

		if (ignoreCastPerm && !Perm.CAST.has(player, jutsu)) player.addAttachment(MagicJutsus.plugin, Perm.CAST.getNode(jutsu), true, 1);
		if (castForFree && !Perm.NOREAGENTS.has(player)) player.addAttachment(MagicJutsus.plugin, Perm.NOREAGENTS.getNode(), true, 1);

		JutsuCastState state;
		PostCastAction action;
		if (bypassNormalChecks) {
			state = JutsuCastState.NORMAL;
			action = jutsu.castJutsu(player, JutsuCastState.NORMAL, 1.0F, null);
		} else {
			JutsuCastResult result = jutsu.cast(player);
			state = result.state;
			action = result.action;
		}

		if (state != JutsuCastState.NORMAL || action == PostCastAction.ALREADY_HANDLED) return;

		if (uses > 0) {
			uses -= 1;
			if (uses > 0) {
				inHand = createScroll(jutsu, uses, inHand);
				if (textContainsUses) player.getEquipment().setItemInMainHand(inHand);
			} else {
				if (removeScrollWhenDepleted) {
					player.getEquipment().setItemInMainHand(null);
					event.setCancelled(true);
				} else player.getEquipment().setItemInMainHand(itemType.toItemStack(1));
			}
		}

		sendMessage(formatMessage(strOnUse, "%s", jutsu.getName(), "%u", uses >= 0 ? uses + "" : "many"), player, MagicJutsus.NULL_ARGS);
	}
	
	@EventHandler
	public void onItemSwitch(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		ItemStack inHand = player.getInventory().getItem(event.getNewSlot());
		
		if (inHand == null || inHand.getType() != itemType.getMaterial()) return;
		
		if (inHand.getDurability() > 0 && predefinedScrollJutsus != null) {
			Jutsu jutsu = predefinedScrollJutsus.get(Integer.valueOf(inHand.getDurability()));
			if (jutsu != null) {
				int uses = predefinedScrollUses.get(Integer.valueOf(inHand.getDurability()));
				inHand = createScroll(jutsu, uses, inHand);
				player.getInventory().setItem(event.getNewSlot(), inHand);
			}
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

}
