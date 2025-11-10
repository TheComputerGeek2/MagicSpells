package com.nisovin.magicspells.spells.command;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerInteractEvent;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.nisovin.magicspells.Perm;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.commands.parsers.OwnedSpellParser;

// Advanced perm is for specifying the number of uses if it isn't normally allowed

public class ImbueSpell extends CommandSpell implements BlockingSuggestionProvider.Strings<CommandSourceStack> {

	private static final Pattern CAST_ARG_USES_PATTERN = Pattern.compile("\\d+");
	private static final NamespacedKey KEY = new NamespacedKey(MagicSpells.getInstance(), "imbue_data");

	private final Set<Material> allowedItemTypes;
	private final List<Material> allowedItemMaterials;

	private final ConfigData<Integer> maxUses;
	private final ConfigData<Integer> defaultUses;

	private String strUsage;
	private String strItemName;
	private String strItemLore;
	private String strCantImbueItem;
	private String strCantImbueSpell;

	private boolean consumeItem;
	private boolean leftClickCast;
	private boolean rightClickCast;
	private boolean nameAndLoreHaveUses;
	private final ConfigData<Boolean> allowSpecifyUses;
	private final ConfigData<Boolean> requireTeachPerm;
	private final ConfigData<Boolean> chargeReagentsForSpellPerUse;

	public ImbueSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		allowedItemTypes = new HashSet<>();
		allowedItemMaterials = new ArrayList<>();

		List<String> allowed = getConfigStringList("allowed-items", null);
		if (allowed != null) {
			for (String s : allowed) {
				Material m = Util.getMaterial(s);
				if (m == null) continue;
				allowedItemTypes.add(m);
				allowedItemMaterials.add(m);
			}
		}

		maxUses = getConfigDataInt("max-uses", 10);
		defaultUses = getConfigDataInt("default-uses", 5);
		strUsage = getConfigString("str-usage", "Usage: /cast imbue <spell> [uses]");
		strItemName = getConfigString("str-item-name", "");
		strItemLore = getConfigString("str-item-lore", "Imbued: %s");
		strCantImbueItem = getConfigString("str-cant-imbue-item", "You can't imbue that item.");
		strCantImbueSpell = getConfigString("str-cant-imbue-spell", "You can't imbue that spell.");

		consumeItem = getConfigBoolean("consume-item", false);
		leftClickCast = getConfigBoolean("left-click-cast", true);
		rightClickCast = getConfigBoolean("right-click-cast", false);
		allowSpecifyUses = getConfigDataBoolean("allow-specify-uses", true);
		requireTeachPerm = getConfigDataBoolean("require-teach-perm", true);
		chargeReagentsForSpellPerUse = getConfigDataBoolean("charge-reagents-for-spell-per-use", true);

		nameAndLoreHaveUses = strItemName.contains("%u") || strItemLore.contains("%u");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (!data.hasArgs()) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		// Get item
		ItemStack inHand = caster.getInventory().getItemInMainHand();
		if (!allowedItemTypes.contains(inHand.getType())) {
			sendMessage(strCantImbueItem, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (!inHand.hasItemMeta() || inHand.getItemMeta().getPersistentDataContainer().has(KEY)) {
			sendMessage(strCantImbueItem, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spell spell = MagicSpells.getSpellByName(data.args()[0]);
		if (spell == null || !MagicSpells.getSpellbook(caster).hasSpell(spell)) {
			sendMessage(strCantImbueSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (requireTeachPerm.get(data) && !MagicSpells.getSpellbook(caster).canTeach(spell)) {
			sendMessage(strCantImbueSpell, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		int uses = defaultUses.get(data), maxUses = this.maxUses.get(data);
		if (data.args().length > 1 && CAST_ARG_USES_PATTERN.asMatchPredicate().test(data.args()[1]) && (allowSpecifyUses.get(data) || Perm.ADVANCED_IMBUE.has(caster))) {
			uses = Integer.parseInt(data.args()[1]);
		}
		uses = Math.max(Math.min(uses, maxUses), 1);

		if (chargeReagentsForSpellPerUse.get(data) && !Perm.NO_REAGENTS.has(caster)) {
			SpellReagents reagents = spell.getReagents().multiply(uses);
			if (!hasReagents(caster, reagents)) {
				sendMessage(strMissingReagents, caster, data);
				return new CastResult(PostCastAction.ALREADY_HANDLED, data);
			}

			removeReagents(caster, reagents);
		}

		int finalUses = uses;
		setItemNameAndLore(inHand, spell, uses);
		inHand.editMeta(meta -> meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, spell.getInternalName() + ',' + finalUses));
		caster.getInventory().setItemInMainHand(inHand);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@Override
	public @NotNull Iterable<@NotNull String> stringSuggestions(@NotNull CommandContext<CommandSourceStack> context, @NotNull CommandInput input) {
		CommandSourceStack stack = context.sender();

		CommandSender executor = Objects.requireNonNullElse(stack.getExecutor(), stack.getSender());
		if (!(executor instanceof Player caster)) return Collections.emptyList();

		return OwnedSpellParser.suggest(caster,
			requireTeachPerm.get(new SpellData(caster)) ? MagicSpells.getSpellbook(caster)::canTeach : null
		);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent event) {
		if (event.useItemInHand() == Result.DENY) return;
		if (!event.hasItem()) return;
		Action action = event.getAction();
		if (!actionAllowedForCast(action)) return;
		ItemStack item = event.getItem();
		if (item == null) return;
		ItemMeta meta = item.getItemMeta();
		if (meta == null || !allowedItemTypes.contains(item.getType())) return;

		boolean allowed = false;
		for (Material m : allowedItemMaterials) {
			if (m == item.getType()) {
				allowed = true;
				break;
			}
		}
		if (!allowed) return;

		String imbueData = meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
		if (imbueData == null || imbueData.isEmpty()) return;
		String[] data = imbueData.split(",");
		Spell spell = MagicSpells.getSpellByInternalName(data[0]);
		int uses = Integer.parseInt(data[1]);

		if (spell == null || uses <= 0) {
			item.editMeta(m -> m.getPersistentDataContainer().remove(KEY));
			return;
		}

		spell.cast(new SpellData(event.getPlayer(), 1.0F, MagicSpells.NULL_ARGS));
		uses--;
		if (uses <= 0) {
			if (consumeItem) event.getPlayer().getInventory().setItemInMainHand(null);
			else {
				item.editMeta(m -> m.getPersistentDataContainer().remove(KEY));
				if (nameAndLoreHaveUses) setItemNameAndLore(item, spell, 0);
			}
		} else {
			if (nameAndLoreHaveUses) setItemNameAndLore(item, spell, uses);
			int finalUses = uses;
			item.editMeta(m -> m.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, spell.getInternalName() + ',' + finalUses));
		}
	}

	private boolean actionAllowedForCast(Action action) {
		return switch (action) {
			case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> rightClickCast;
			case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> leftClickCast;
			default -> false;
		};
	}

	private void setItemNameAndLore(ItemStack item, Spell spell, int uses) {
		ItemMeta meta = item.getItemMeta();
		if (!strItemName.isEmpty()) {
			String displayName = strItemName.replace("%s", spell.getName()).replace("%u", uses + "");
			meta.displayName(Util.getItemMiniMessage(displayName));
		}
		if (!strItemLore.isEmpty()) {
			String lore = strItemLore.replace("%s", spell.getName()).replace("%u", uses + "");
			meta.lore(Collections.singletonList(Util.getItemMiniMessage(lore)));
		}
		item.setItemMeta(meta);
	}

	public Set<Material> getAllowedItemTypes() {
		return allowedItemTypes;
	}

	public List<Material> getAllowedItemMaterials() {
		return allowedItemMaterials;
	}

	public String getStrUsage() {
		return strUsage;
	}

	public void setStrUsage(String strUsage) {
		this.strUsage = strUsage;
	}

	public String getStrItemName() {
		return strItemName;
	}

	public void setStrItemName(String strItemName) {
		this.strItemName = strItemName;
	}

	public String getStrItemLore() {
		return strItemLore;
	}

	public void setStrItemLore(String strItemLore) {
		this.strItemLore = strItemLore;
	}

	public String getStrCantImbueItem() {
		return strCantImbueItem;
	}

	public void setStrCantImbueItem(String strCantImbueItem) {
		this.strCantImbueItem = strCantImbueItem;
	}

	public String getStrCantImbueSpell() {
		return strCantImbueSpell;
	}

	public void setStrCantImbueSpell(String strCantImbueSpell) {
		this.strCantImbueSpell = strCantImbueSpell;
	}

	public boolean shouldConsumeItem() {
		return consumeItem;
	}

	public void setConsumeItem(boolean consumeItem) {
		this.consumeItem = consumeItem;
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

	public boolean shouldNameAndLoreHaveUses() {
		return nameAndLoreHaveUses;
	}

	public void setNameAndLoreHaveUses(boolean nameAndLoreHasUses) {
		this.nameAndLoreHaveUses = nameAndLoreHasUses;
	}

}
