package com.nisovin.magicspells.spells;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.Random;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;

public class MenuSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private Map<UUID, Float> castPower = new HashMap<>();
	private Map<UUID, Location> castLocTarget = new HashMap<>();
	private Map<UUID, LivingEntity> castEntityTarget = new HashMap<>();
	private Map<String, MenuOption> options = new LinkedHashMap<>();

	private Random random = new Random();

	private String title;

	private int size;
	private int delay;

	private boolean uniqueNames;
	private boolean bypassNormalCast;
	private boolean requireEntityTarget;
	private boolean requireLocationTarget;
	private boolean targetOpensMenuInstead;

	public MenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		title = ChatColor.translateAlternateColorCodes('&', getConfigString("title", "Window Title " + spellName));

		delay = getConfigInt("delay", 0);

		uniqueNames = getConfigBoolean("unique-names", false);
		bypassNormalCast = getConfigBoolean("bypass-normal-cast", true);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		requireLocationTarget = getConfigBoolean("require-location-target", false);
		targetOpensMenuInstead = getConfigBoolean("target-opens-menu-instead", false);

		int maxSlot = 8;
		String path = "options.";
		for (String optionName : getConfigKeys("options")) {
			int optionSlot = getConfigInt(path + optionName + ".slot", -1);
			String optionSpellName = getConfigString(path + optionName + ".spell", "");
			float optionPower = getConfigFloat(path + optionName + ".power", 1);

			ItemStack optionItem;
			if (isConfigSection(path + optionName + ".item")) optionItem = Util.getItemStackFromConfig(getConfigSection(path + optionName + ".item"));
			else optionItem = Util.getItemStackFromString(getConfigString(path + optionName + ".item", "stone"));

			int optionQuantity = getConfigInt(path + optionName + ".quantity", 1);
			List<String> modifierList = getConfigStringList(path + optionName + ".modifiers", null);
			boolean optionStayOpen = getConfigBoolean(path + optionName + ".stay-open", false);
			boolean optionDrag = getConfigBoolean(path + optionName + ".drag", false);

			if (optionSlot < 0) continue;
			if (optionItem == null) continue;
			if (optionSpellName.isEmpty()) continue;

			optionItem.setAmount(optionQuantity);
			Util.setLoreData(optionItem, optionName);

			MenuOption option = new MenuOption();
			option.slot = optionSlot;
			option.menuOptionName = optionName;
			option.spellName = optionSpellName;
			option.power = optionPower;
			option.item = optionItem;
			option.modifierList = modifierList;
			option.stayOpen = optionStayOpen;
			option.Drag = optionDrag;
			String optionKey = uniqueNames ? getOptionKey(option.item) : optionName;
			options.put(optionKey, option);
			if (optionSlot > maxSlot) maxSlot = optionSlot;
		}
		size = ((maxSlot / 9) * 9) + 9;
		
		if (options.isEmpty()) MagicSpells.error("MenuSpell '" + spellName + "' has no menu options!");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		for (MenuOption option : options.values()) {
			Subspell spell = new Subspell(option.spellName);
			if (spell.process()) {
				option.spell = spell;
				if (option.modifierList != null) option.menuOptionModifiers = new ModifierSet(option.modifierList);
			} else MagicSpells.error("MenuSpell '" + internalName + "' has an invalid spell listed on '" + option.menuOptionName + '\'');
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity entityTarget = null;
			Location locTarget = null;
			Player opener = player;
			
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
				if (targetInfo != null) entityTarget = targetInfo.getTarget();
				if (entityTarget == null) return noTarget(player);
				if (targetOpensMenuInstead) {
					if (!(entityTarget instanceof Player)) return noTarget(player);
					opener = (Player) entityTarget;
					entityTarget = null;
				}
			} else if (requireLocationTarget) {
				Block block = getTargetedBlock(player, power);
				if (block == null || BlockUtils.isAir(block.getType())) return noTarget(player);
				locTarget = block.getLocation();
			}
			
			open(player, opener, entityTarget, locTarget, power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (requireEntityTarget && !validTargetList.canTarget(caster, target)) return false;
		Player opener = caster;
		if (targetOpensMenuInstead) {
			if (!(target instanceof Player)) return false;
			opener = (Player) target;
			target = null;
		}
		open(caster, opener, target, null, power, MagicSpells.NULL_ARGS);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!targetOpensMenuInstead) return false;
		if (requireEntityTarget && !validTargetList.canTarget(target)) return false;
		if (!(target instanceof Player)) return false;
		open(null, (Player) target, null, null, power, MagicSpells.NULL_ARGS);
		return true;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		open(caster, caster, null, target, power, MagicSpells.NULL_ARGS);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length < 1) return false;
		Player player = PlayerNameUtils.getPlayer(args[0]);
		String[] spellArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : null;
		if (player != null) {
			open(null, player, null, null, 1, spellArgs);
			return true;
		}
		return false;
	}
	
	private String getOptionKey(ItemStack item) {
		int durability = 0;
		if (item.getItemMeta() instanceof Damageable) durability = ((Damageable) item.getItemMeta()).getDamage();
		return item.getType().name() + '_' + durability + '_' + item.getItemMeta().getDisplayName();
	}
	
	private void open(final Player caster, Player opener, LivingEntity entityTarget, Location locTarget, final float power, final String[] args) {
		if (delay < 0) {
			openMenu(caster, opener, entityTarget, locTarget, power, args);
			return;
		}
		final Player p = opener;
		final Location l = locTarget;
		final LivingEntity e = entityTarget;
		MagicSpells.scheduleDelayedTask(() -> openMenu(caster, p, e, l, power, args), delay);
	}
	
	private void openMenu(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power, String[] args) {
		castPower.put(opener.getUniqueId(), power);
		if (requireEntityTarget && entityTarget != null) castEntityTarget.put(opener.getUniqueId(), entityTarget);
		if (requireLocationTarget && locTarget != null) castLocTarget.put(opener.getUniqueId(), locTarget);
		
		Inventory inv = Bukkit.createInventory(opener, size, title);
		applyOptionsToInventory(opener, inv, args);
		opener.openInventory(inv);
		
		if (entityTarget != null && caster != null) {
			playSpellEffects(caster, entityTarget);
			return;
		}

		playSpellEffects(EffectPosition.SPECIAL, opener);
		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster);
		if (locTarget != null) playSpellEffects(EffectPosition.TARGET, locTarget);
	}
	
	private void applyOptionsToInventory(Player opener, Inventory inv, String[] args) {
		inv.clear();
		for (MenuOption option : options.values()) {
			if (option.spell == null) continue;
			if (inv.getItem(option.slot) != null) continue;
			if (option.menuOptionModifiers != null) {
				MagicSpellsGenericPlayerEvent event = new MagicSpellsGenericPlayerEvent(opener);
				option.menuOptionModifiers.apply(event);
				if (event.isCancelled()) continue;
			}
			ItemStack item = option.item.clone();
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(MagicSpells.doArgumentAndVariableSubstitution(meta.getDisplayName(), opener, args));
			List<String> lore = meta.getLore();
			if (lore != null && lore.size() > 1) {
				for (int i = 0; i < lore.size() - 1; i++) {
					lore.set(i, MagicSpells.doArgumentAndVariableSubstitution(lore.get(i), opener, args));
				}
				meta.setLore(lore);
			}
			item.setItemMeta(meta);
			inv.setItem(option.slot, item);
		}
	}
	
	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		if (!event.getView().getTitle().equals(title)) return;
		event.setCancelled(true);
		if (event.getClick() == ClickType.LEFT) {
			final Player player = (Player) event.getWhoClicked();
			UUID id = player.getUniqueId();
			boolean close = true;

			ItemStack item = event.getCurrentItem();
			if (item != null) {
				String key = uniqueNames ? getOptionKey(item) : Util.getLoreData(item);
				if (key != null && !key.isEmpty() && options.containsKey(key)) {
					MenuOption option = options.get(key);
					Subspell spell = option.spell;
					if (spell != null) {
						float power = option.power;
						if (castPower.containsKey(id)) power *= castPower.get(id);

						if (spell.isTargetedEntitySpell() && castEntityTarget.containsKey(id)) spell.castAtEntity(player, castEntityTarget.get(id), power);
						else if (spell.isTargetedLocationSpell() && castLocTarget.containsKey(id)) spell.castAtLocation(player, castLocTarget.get(id), power);
						else if (bypassNormalCast) spell.cast(player, power);
						else spell.getSpell().cast(player, power, null);
					}
					if (option.stayOpen) close = false;
					if (option.drag) event.setCancelled = false;
				}
			}

			castPower.remove(id);
			castLocTarget.remove(id);
			castEntityTarget.remove(id);

			if (close) MagicSpells.scheduleDelayedTask(player::closeInventory, 0);
			else applyOptionsToInventory(player, event.getView().getTopInventory(), MagicSpells.NULL_ARGS);
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		UUID id = event.getPlayer().getUniqueId();
		castPower.remove(id);
		castLocTarget.remove(id);
		castEntityTarget.remove(id);
	}
	
	private static class MenuOption {

		private String menuOptionName;
		private int slot;
		private ItemStack item;
		private String spellName;
		private Subspell spell;
		private float power;
		private List<String> modifierList;
		private ModifierSet menuOptionModifiers;
		private boolean stayOpen;
		
	}

}
