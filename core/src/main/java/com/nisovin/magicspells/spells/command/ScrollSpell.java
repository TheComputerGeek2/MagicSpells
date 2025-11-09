package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.commands.parsers.SpellParser;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;

public class ScrollSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private static final Pattern SCROLL_DATA_USES_PATTERN = Pattern.compile("^\\d+$");
	private static final Pattern CAST_ARGUMENT_USE_COUNT_PATTERN = Pattern.compile("^-?\\d+$");
	private static final NamespacedKey KEY = new NamespacedKey(MagicSpells.getInstance(), "scroll_data");

	private final List<String> predefinedScrolls;

	private Map<Integer, Spell> predefinedScrollSpells;
	private Map<Integer, Integer> predefinedScrollUses;

	private Material itemType;

	private String strUsage;
	private String strOnUse;
	private String strNoSpell;
	private String strUseFail;
	private String strCantTeach;
	private String strScrollName;
	private String strScrollSubtext;
	private String strConsoleUsage;

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

		itemType = Util.getMaterial(getConfigString("item-id", "paper"));

		strUsage = getConfigString("str-usage", "You must hold a single blank paper \nand type /cast scroll <spell> <uses>.");
		strOnUse = getConfigString("str-on-use", "Spell Scroll: %s used. %u uses remaining.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strUseFail = getConfigString("str-use-fail", "Unable to use this scroll right now.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a scroll with that spell.");
		strScrollName = getConfigString("str-scroll-name", "Magic Scroll: %s");
		strScrollSubtext = getConfigString("str-scroll-subtext", "Uses remaining: %u");
		strConsoleUsage = getConfigString("str-console-usage", "Invalid arguments defined!\nValid arguments: <playerName> <spell> <scrollUses>");

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
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (!data.hasArgs()) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		ItemStack inHand = caster.getInventory().getItemInMainHand();
		if (inHand.getAmount() != 1 || itemType != inHand.getType()) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spell spell = MagicSpells.getSpellByName(data.args()[0]);
		Spellbook spellbook = MagicSpells.getSpellbook(caster);
		if (spell == null || !spellbook.hasSpell(spell)) {
			sendMessage(strNoSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}
		if (requireTeachPerm && !spellbook.canTeach(spell)) {
			sendMessage(strCantTeach, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		int uses = defaultUses;
		if (data.args().length > 1 && CAST_ARGUMENT_USE_COUNT_PATTERN.asMatchPredicate().test(data.args()[1])) uses = Integer.parseInt(data.args()[1]);
		if (uses > maxUses || (maxUses > 0 && uses <= 0)) uses = maxUses;

		if (chargeReagentsForSpellPerCharge && uses > 0) {
			SpellReagents reagents = spell.getReagents().multiply(uses);
			if (!hasReagents(caster, reagents)) {
				sendMessage(strMissingReagents, caster, data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}
			removeReagents(caster, reagents);
		}

		inHand = createScroll(spell, uses, inHand);
		caster.getInventory().setItemInMainHand(inHand);
		playSpellEffects(data);

		sendMessage(strCastSelf, caster, data, "%s", spell.getName());
		return new CastResult(PostCastAction.NO_MESSAGES, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args == null || args.length < 1) {
			sender.sendMessage(strConsoleUsage);
			return false;
		}

		List<Player> players = Bukkit.matchPlayer(args[0]);
		if (players.isEmpty()) {
			sender.sendMessage("Invalid player defined!");
			return false;
		}

		Spell spell = MagicSpells.getSpellByName(args[1]);
		if (spell == null) {
			sender.sendMessage(strNoSpell);
			return false;
		}

		int uses = defaultUses;
		if (args.length > 2 && CAST_ARGUMENT_USE_COUNT_PATTERN.asMatchPredicate().test(args[2])) {
			uses = Integer.parseInt(args[2]);
		}

		if (uses > maxUses || (maxUses > 0 && uses <= 0)) uses = maxUses;

		int slot = -1;
		for (int i = 0; i <= 35; i++) {
			if (players.get(0).getInventory().getItem(i) == null) {
				slot = i;
				break;
			}
		}

		if (slot != -1) players.get(0).getInventory().setItem(slot, createScroll(spell, uses, new ItemStack(itemType, 1)));

		return true;
	}
	
	public ItemStack createScroll(Spell spell, int uses, ItemStack item) {
		if (item == null) item = new ItemStack(itemType);

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof Damageable damageable) damageable.setDamage(0);

		String usageCount = uses >= 0 ? String.valueOf(uses) : "many";
		String displayName = strScrollName.replace("%s", spell.getName()).replace("%u", usageCount);
		meta.displayName(Util.getItemMiniMessage(displayName));

		if (strScrollSubtext != null && !strScrollSubtext.isEmpty()) {
			Component lore = Util.getItemMiniMessage(strScrollSubtext.replace("%s", spell.getName()).replace("%u", usageCount));
			meta.lore(Collections.singletonList(lore));
		}

		meta.setEnchantmentGlintOverride(true);
		meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, spell.getInternalName() + (uses > 0 ? "," + uses : ""));
		item.setItemMeta(meta);

		return item;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
		if (executor instanceof Player caster)
			return OwnedSpellParser.suggest(caster, requireTeachPerm ? MagicSpells.getSpellbook(caster)::canTeach : null);

		if (!(executor instanceof ConsoleCommandSender)) return Collections.emptyList();

		List<String> suggestions = TxtUtil.tabCompletePlayerName(executor);
		CommandInput original = input.copy();

		String playerName = input.readString();
		if (playerName.isEmpty() || input.isEmpty() && !input.input().endsWith(" ")) return suggestions;

		Player player = Bukkit.getPlayer(playerName);
		if (player == null) return suggestions;

		String diff = original.difference(input.skipWhitespace(), true);
		SpellParser.suggest().stream().map(spell -> diff + spell).forEach(suggestions::add);

		return suggestions;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!actionAllowedForCast(event.getAction())) return;
		Player player = event.getPlayer();
		ItemStack inHand = player.getInventory().getItemInMainHand();
		ItemMeta meta = inHand.getItemMeta();
		if (itemType != inHand.getType() || inHand.getAmount() > 1 || meta == null) return;
		
		// Check for predefined scroll
		if (ItemUtil.getDurability(inHand) > 0 && predefinedScrollSpells != null) {
			Spell spell = predefinedScrollSpells.get(ItemUtil.getDurability(inHand));
			if (spell != null) {
				int uses = predefinedScrollUses.get(ItemUtil.getDurability(inHand));
				inHand = createScroll(spell, uses, inHand);
				player.getInventory().setItemInMainHand(inHand);
			}
		}
		
		// Get scroll data (spell and uses)
		String scrollDataString = meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
		if (scrollDataString == null || scrollDataString.isEmpty()) return;
		String[] scrollData = scrollDataString.split(",");
		Spell spell = MagicSpells.getSpellByInternalName(scrollData[0]);
		if (spell == null) return;
		int uses = 0;
		if (scrollData.length > 1 && SCROLL_DATA_USES_PATTERN.asMatchPredicate().test(scrollData[1])) uses = Integer.parseInt(scrollData[1]);

		if (requireScrollCastPermOnUse && !MagicSpells.getSpellbook(player).canCast(this)) {
			sendMessage(strUseFail, player, SpellData.NULL);
			return;
		}

		if (ignoreCastPerm && !Perm.CAST.has(player, spell)) player.addAttachment(MagicSpells.plugin, Perm.CAST.getNode(spell), true, 1);
		if (castForFree && !Perm.NO_REAGENTS.has(player)) player.addAttachment(MagicSpells.plugin, Perm.NO_REAGENTS.getNode(), true, 1);

		SpellData data = new SpellData(player);

		SpellCastState state;
		PostCastAction action;
		if (bypassNormalChecks) {
			state = SpellCastState.NORMAL;
			action = spell.cast(data).action();
		} else {
			SpellCastResult result = spell.hardCast(data);
			action = result.action;
			state = result.state;
		}

		if (state != SpellCastState.NORMAL || action == PostCastAction.ALREADY_HANDLED) return;

		if (uses > 0) {
			uses -= 1;
			if (uses > 0) {
				inHand = createScroll(spell, uses, inHand);
				if (textContainsUses) player.getInventory().setItemInMainHand(inHand);
			} else {
				if (removeScrollWhenDepleted) {
					player.getInventory().setItemInMainHand(null);
					event.setCancelled(true);
				} else player.getInventory().setItemInMainHand(new ItemStack(itemType));
			}
		}

		sendMessage(strOnUse, player, MagicSpells.NULL_ARGS, "%s", spell.getName(), "%u", uses >= 0 ? String.valueOf(uses) : "many");
	}
	
	@EventHandler
	public void onItemSwitch(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		ItemStack inHand = player.getInventory().getItem(event.getNewSlot());
		
		if (inHand == null || inHand.getType() != itemType) return;
		
		if (isDurable(inHand) && predefinedScrollSpells != null) {
			Spell spell = predefinedScrollSpells.get(ItemUtil.getDurability(inHand));
			if (spell == null) return;
			
			int uses = predefinedScrollUses.get(ItemUtil.getDurability(inHand));
			inHand = createScroll(spell, uses, inHand);
			player.getInventory().setItem(event.getNewSlot(), inHand);
		}

	}

	private boolean isDurable(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (!(meta instanceof Damageable)) return false;
		return ((Damageable) meta).getDamage() > 0;
	}
	
	private boolean actionAllowedForCast(Action action) {
		return switch (action) {
			case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> rightClickCast;
			case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> leftClickCast;
			default -> false;
		};
	}

	public static Pattern getCastArgumentUseCountPattern() {
		return CAST_ARGUMENT_USE_COUNT_PATTERN;
	}

	public static Pattern getScrollDataUsesPattern() {
		return SCROLL_DATA_USES_PATTERN;
	}

	public List<String> getPredefinedScrolls() {
		return predefinedScrolls;
	}

	public Map<Integer, Spell> getPredefinedScrollSpells() {
		return predefinedScrollSpells;
	}

	public Map<Integer, Integer> getPredefinedScrollUses() {
		return predefinedScrollUses;
	}

	public Material getItemType() {
		return itemType;
	}

	public void setItemType(Material itemType) {
		this.itemType = itemType;
	}

	public String getStrUsage() {
		return strUsage;
	}

	public void setStrUsage(String strUsage) {
		this.strUsage = strUsage;
	}

	public String getStrOnUse() {
		return strOnUse;
	}

	public void setStrOnUse(String strOnUse) {
		this.strOnUse = strOnUse;
	}

	public String getStrNoSpell() {
		return strNoSpell;
	}

	public void setStrNoSpell(String strNoSpell) {
		this.strNoSpell = strNoSpell;
	}

	public String getStrUseFail() {
		return strUseFail;
	}

	public void setStrUseFail(String strUseFail) {
		this.strUseFail = strUseFail;
	}

	public String getStrCantTeach() {
		return strCantTeach;
	}

	public void setStrCantTeach(String strCantTeach) {
		this.strCantTeach = strCantTeach;
	}

	public String getStrScrollName() {
		return strScrollName;
	}

	public void setStrScrollName(String strScrollName) {
		this.strScrollName = strScrollName;
	}

	public String getStrScrollSubtext() {
		return strScrollSubtext;
	}

	public void setStrScrollSubtext(String strScrollSubtext) {
		this.strScrollSubtext = strScrollSubtext;
	}

	public String getStrConsoleUsage() {
		return strConsoleUsage;
	}

	public void setStrConsoleUsage(String strConsoleUsage) {
		this.strConsoleUsage = strConsoleUsage;
	}

	public int getMaxUses() {
		return maxUses;
	}

	public void setMaxUses(int maxUses) {
		this.maxUses = maxUses;
	}

	public int getDefaultUses() {
		return defaultUses;
	}

	public void setDefaultUses(int defaultUses) {
		this.defaultUses = defaultUses;
	}

	public boolean canCastForFree() {
		return castForFree;
	}

	public void setCastForFree(boolean castForFree) {
		this.castForFree = castForFree;
	}

	public boolean shouldLeftClickCast() {
		return leftClickCast;
	}

	public void setLeftClickCast(boolean leftClickCast) {
		this.leftClickCast = leftClickCast;
	}

	public boolean shouldRightClickCast() {
		return rightClickCast;
	}

	public void setRightClickCast(boolean rightClickCast) {
		this.rightClickCast = rightClickCast;
	}

	public boolean shouldIgnoreCastPerm() {
		return ignoreCastPerm;
	}

	public void setIgnoreCastPerm(boolean ignoreCastPerm) {
		this.ignoreCastPerm = ignoreCastPerm;
	}

	public boolean shouldRequireTeachPerm() {
		return requireTeachPerm;
	}

	public void setRequireTeachPerm(boolean requireTeachPerm) {
		this.requireTeachPerm = requireTeachPerm;
	}

	public boolean shouldTextContainUses() {
		return textContainsUses;
	}

	public void setTextContainUses(boolean textContainsUses) {
		this.textContainsUses = textContainsUses;
	}

	public boolean shouldBypassNormalChecks() {
		return bypassNormalChecks;
	}

	public void setBypassNormalChecks(boolean bypassNormalChecks) {
		this.bypassNormalChecks = bypassNormalChecks;
	}

	public boolean shouldRemoveScrollWhenDepleted() {
		return removeScrollWhenDepleted;
	}

	public void setRemoveScrollWhenDepleted(boolean removeScrollWhenDepleted) {
		this.removeScrollWhenDepleted = removeScrollWhenDepleted;
	}

	public boolean shouldScrollCastRequirePermOnUse() {
		return requireScrollCastPermOnUse;
	}

	public void setRequireScrollCastPermOnUse(boolean requireScrollCastPermOnUse) {
		this.requireScrollCastPermOnUse = requireScrollCastPermOnUse;
	}

	public boolean shouldChargeReagentsForSpellPerCharge() {
		return chargeReagentsForSpellPerCharge;
	}

	public void setChargeReagentsForSpellPerCharge(boolean chargeReagentsForSpellPerCharge) {
		this.chargeReagentsForSpellPerCharge = chargeReagentsForSpellPerCharge;
	}

}
