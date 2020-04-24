package com.nisovin.magicspells.spells.command;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;

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

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.ItemUtil;
import com.nisovin.magicspells.util.RegexUtil;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.materials.MagicMaterial;

public class ScrollSpell extends CommandSpell {

	private static final Pattern CAST_ARGUMENT_USE_COUNT_PATTERN = Pattern.compile("^-?[0-9]+$");
	private static final Pattern SCROLL_DATA_USES_PATTERN = Pattern.compile("^[0-9]+$");

	private List<String> predefinedScrolls;
	private Map<Integer, Spell> predefinedScrollSpells;
	private Map<Integer, Integer> predefinedScrollUses;

	private MagicMaterial itemType;

	private String strUsage;
	private String strOnUse;
	private String strNoSpell;
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
	private boolean chargeReagentsForSpellPerCharge;
		
	public ScrollSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		predefinedScrolls = getConfigStringList("predefined-scrolls", null);

		itemType = MagicSpells.getItemNameResolver().resolveItem(getConfigString("item-id", "paper"));

		strUsage = getConfigString("str-usage", "You must hold a single blank paper \nand type /cast scroll <spell> <uses>.");
		strOnUse = getConfigString("str-on-use", "Spell Scroll: %s used. %u uses remaining.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strUseFail = getConfigString("str-use-fail", "Unable to use this scroll right now.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a scroll with that spell.");
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
		chargeReagentsForSpellPerCharge = getConfigBoolean("charge-reagents-for-spell-per-charge", false);

		textContainsUses = strScrollName.contains("%u") || strScrollSubtext.contains("%u");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		if (predefinedScrolls == null || predefinedScrolls.isEmpty()) return;

		predefinedScrollSpells = new HashMap<>();
		predefinedScrollUses = new HashMap<>();
		for (String s : predefinedScrolls) {
			String[] data = s.split(" ");
			try {
				int id = Integer.parseInt(data[0]);
				Spell spell = MagicSpells.getSpellByInternalName(data[1]);
				int uses = defaultUses;
				if (data.length > 2) uses = Integer.parseInt(data[2]);
				if (id > 0 && spell != null) {
					predefinedScrollSpells.put(id, spell);
					predefinedScrollUses.put(id, uses);
				} else MagicSpells.error("ScrollSpell '" + internalName + "' has invalid predefined scroll: " + s);
			} catch (Exception e) {
				MagicSpells.error("ScrollSpell '" + internalName + "' has invalid predefined scroll: " + s);
			}
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
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
			
			Spell spell = MagicSpells.getSpellByInGameName(args[0]);
			Spellbook spellbook = MagicSpells.getSpellbook(player);
			if (spell == null || spellbook == null || !spellbook.hasSpell(spell)) {
				sendMessage(strNoSpell, player, args);
				return PostCastAction.ALREADY_HANDLED;			
			}
			if (requireTeachPerm && !spellbook.canTeach(spell)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			int uses = defaultUses;
			if (args.length > 1 && RegexUtil.matches(CAST_ARGUMENT_USE_COUNT_PATTERN, args[1])) uses = Integer.parseInt(args[1]);
			if (uses > maxUses || (maxUses > 0 && uses <= 0)) uses = maxUses;
			
			if (chargeReagentsForSpellPerCharge && uses > 0) {
				SpellReagents reagents = spell.getReagents().multiply(uses);
				if (!hasReagents(player, reagents)) {
					sendMessage(strMissingReagents, player, args);
					return PostCastAction.ALREADY_HANDLED;
				}
				removeReagents(player, reagents);
			}
			
			inHand = createScroll(spell, uses, inHand);
			player.getEquipment().setItemInMainHand(inHand);
			
			sendMessage(formatMessage(strCastSelf, "%s", spell.getName()), player, args);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	public ItemStack createScroll(Spell spell, int uses, ItemStack item) {
		if (item == null) item = itemType.toItemStack(1);
		item.setDurability((short) 0);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(Util.colorize(strScrollName.replace("%s", spell.getName()).replace("%u", (uses >= 0 ? uses + "" : "many"))));
		if (strScrollSubtext != null && !strScrollSubtext.isEmpty()) {
			List<String> lore = new ArrayList<>();
			lore.add(Util.colorize(strScrollSubtext.replace("%s", spell.getName()).replace("%u", (uses >= 0 ? uses + "" : "many"))));
			meta.setLore(lore);
		}
		item.setItemMeta(meta);
		Util.setLoreData(item, internalName + ':' + spell.getInternalName() + (uses > 0 ? "," + uses : ""));
		item = ItemUtil.addFakeEnchantment(item);
		return item;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		String[] args = Util.splitParams(partial);
		if (args.length == 1) return tabCompleteSpellName(sender, args[0]);
		return null;
	}
	
	private String getSpellDataFromScroll(ItemStack item) {
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
		if (inHand.getDurability() > 0 && predefinedScrollSpells != null) {
			Spell spell = predefinedScrollSpells.get(Integer.valueOf(inHand.getDurability()));
			if (spell != null) {
				int uses = predefinedScrollUses.get(Integer.valueOf(inHand.getDurability()));
				inHand = createScroll(spell, uses, inHand);
				player.getEquipment().setItemInMainHand(inHand);
			}
		}
		
		// Get scroll data (spell and uses)
		String scrollDataString = getSpellDataFromScroll(inHand);
		if (scrollDataString == null || scrollDataString.isEmpty()) return;
		String[] scrollData = scrollDataString.split(",");
		Spell spell = MagicSpells.getSpellByInternalName(scrollData[0]);
		if (spell == null) return;
		int uses = 0;
		if (scrollData.length > 1 && RegexUtil.matches(SCROLL_DATA_USES_PATTERN, scrollData[1])) uses = Integer.parseInt(scrollData[1]);

		if (requireScrollCastPermOnUse && !MagicSpells.getSpellbook(player).canCast(this)) {
			sendMessage(strUseFail, player, MagicSpells.NULL_ARGS);
			return;
		}

		if (ignoreCastPerm && !(Perm.CAST.has(player, spell) || MagicSpells.getInstance().defaultAllPermsFalse)) player.addAttachment(MagicSpells.plugin, Perm.CAST.getNode(spell), true, 1);
		if (castForFree && !(Perm.NOREAGENTS.has(player) || (player.isOp() && MagicSpells.getInstance().opsIgnoreReagents))) player.addAttachment(MagicSpells.plugin, Perm.NOREAGENTS.getNode(), true, 1);

		SpellCastState state;
		PostCastAction action;
		if (bypassNormalChecks) {
			state = SpellCastState.NORMAL;
			action = spell.castSpell(player, SpellCastState.NORMAL, 1.0F, null);
		} else {
			SpellCastResult result = spell.cast(player);
			state = result.state;
			action = result.action;
		}

		if (state != SpellCastState.NORMAL || action == PostCastAction.ALREADY_HANDLED) return;

		if (uses > 0) {
			uses -= 1;
			if (uses > 0) {
				inHand = createScroll(spell, uses, inHand);
				if (textContainsUses) player.getEquipment().setItemInMainHand(inHand);
			} else {
				if (removeScrollWhenDepleted) {
					player.getEquipment().setItemInMainHand(null);
					event.setCancelled(true);
				} else player.getEquipment().setItemInMainHand(itemType.toItemStack(1));
			}
		}

		sendMessage(formatMessage(strOnUse, "%s", spell.getName(), "%u", uses >= 0 ? uses + "" : "many"), player, MagicSpells.NULL_ARGS);
	}
	
	@EventHandler
	public void onItemSwitch(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		ItemStack inHand = player.getInventory().getItem(event.getNewSlot());
		
		if (inHand == null || inHand.getType() != itemType.getMaterial()) return;
		
		if (inHand.getDurability() > 0 && predefinedScrollSpells != null) {
			Spell spell = predefinedScrollSpells.get(Integer.valueOf(inHand.getDurability()));
			if (spell != null) {
				int uses = predefinedScrollUses.get(Integer.valueOf(inHand.getDurability()));
				inHand = createScroll(spell, uses, inHand);
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
