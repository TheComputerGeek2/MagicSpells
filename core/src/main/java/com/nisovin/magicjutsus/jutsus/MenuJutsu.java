package com.nisovin.magicjutsus.jutsus;

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

import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.TargetInfo;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.castmodifiers.ModifierSet;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.MagicJutsusGenericPlayerEvent;

public class MenuJutsu extends TargetedJutsu implements TargetedEntityJutsu, TargetedLocationJutsu {

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

	public MenuJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		title = ChatColor.translateAlternateColorCodes('&', getConfigString("title", "Window Title " + jutsuName));

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
			String optionJutsuName = getConfigString(path + optionName + ".jutsu", "");
			float optionPower = getConfigFloat(path + optionName + ".power", 1);

			ItemStack optionItem;
			if (isConfigSection(path + optionName + ".item")) optionItem = Util.getItemStackFromConfig(getConfigSection(path + optionName + ".item"));
			else optionItem = Util.getItemStackFromString(getConfigString(path + optionName + ".item", "stone"));

			int optionQuantity = getConfigInt(path + optionName + ".quantity", 1);
			List<String> modifierList = getConfigStringList(path + optionName + ".modifiers", null);
			boolean optionStayOpen = getConfigBoolean(path + optionName + ".stay-open", false);

			if (optionSlot < 0) continue;
			if (optionItem == null) continue;
			if (optionJutsuName.isEmpty()) continue;

			optionItem.setAmount(optionQuantity);
			Util.setLoreData(optionItem, optionName);

			MenuOption option = new MenuOption();
			option.slot = optionSlot;
			option.menuOptionName = optionName;
			option.jutsuName = optionJutsuName;
			option.power = optionPower;
			option.item = optionItem;
			option.modifierList = modifierList;
			option.stayOpen = optionStayOpen;
			String optionKey = uniqueNames ? getOptionKey(option.item) : optionName;
			options.put(optionKey, option);
			if (optionSlot > maxSlot) maxSlot = optionSlot;
		}
		size = ((maxSlot / 9) * 9) + 9;
		
		if (options.isEmpty()) MagicJutsus.error("MenuJutsu '" + jutsuName + "' has no menu options!");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		for (MenuOption option : options.values()) {
			Ninjutsu jutsu = new Ninjutsu(option.jutsuName);
			if (jutsu.process()) {
				option.jutsu = jutsu;
				if (option.modifierList != null) option.menuOptionModifiers = new ModifierSet(option.modifierList);
			} else MagicJutsus.error("MenuJutsu '" + internalName + "' has an invalid jutsu listed on '" + option.menuOptionName + '\'');
		}
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
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
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (requireEntityTarget && !validTargetList.canTarget(caster, target)) return false;
		if (!(caster instanceof Player)) return false;
		Player opener = (Player) caster;
		if (targetOpensMenuInstead) {
			if (!(target instanceof Player)) return false;
			opener = (Player) target;
			target = null;
		}
		open((Player) caster, opener, target, null, power, MagicJutsus.NULL_ARGS);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!targetOpensMenuInstead) return false;
		if (requireEntityTarget && !validTargetList.canTarget(target)) return false;
		if (!(target instanceof Player)) return false;
		open(null, (Player) target, null, null, power, MagicJutsus.NULL_ARGS);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		if (!(caster instanceof Player)) return false;
		open((Player) caster, (Player) caster, null, target, power, MagicJutsus.NULL_ARGS);
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
		String[] jutsuArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : null;
		if (player != null) {
			open(null, player, null, null, 1, jutsuArgs);
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
		MagicJutsus.scheduleDelayedTask(() -> openMenu(caster, p, e, l, power, args), delay);
	}
	
	private void openMenu(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power, String[] args) {
		castPower.put(opener.getUniqueId(), power);
		if (requireEntityTarget && entityTarget != null) castEntityTarget.put(opener.getUniqueId(), entityTarget);
		if (requireLocationTarget && locTarget != null) castLocTarget.put(opener.getUniqueId(), locTarget);
		
		Inventory inv = Bukkit.createInventory(opener, size, title);
		applyOptionsToInventory(opener, inv, args);
		opener.openInventory(inv);
		
		if (entityTarget != null && caster != null) {
			playJutsuEffects(caster, entityTarget);
			return;
		}

		playJutsuEffects(EffectPosition.SPECIAL, opener);
		if (caster != null) playJutsuEffects(EffectPosition.CASTER, caster);
		if (locTarget != null) playJutsuEffects(EffectPosition.TARGET, locTarget);
	}
	
	private void applyOptionsToInventory(Player opener, Inventory inv, String[] args) {
		inv.clear();
		for (MenuOption option : options.values()) {
			if (option.jutsu == null) continue;
			if (inv.getItem(option.slot) != null) continue;
			if (option.menuOptionModifiers != null) {
				MagicJutsusGenericPlayerEvent event = new MagicJutsusGenericPlayerEvent(opener);
				option.menuOptionModifiers.apply(event);
				if (event.isCancelled()) continue;
			}
			ItemStack item = option.item.clone();
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(MagicJutsus.doArgumentAndVariableSubstitution(meta.getDisplayName(), opener, args));
			List<String> lore = meta.getLore();
			if (lore != null && lore.size() > 1) {
				for (int i = 0; i < lore.size() - 1; i++) {
					lore.set(i, MagicJutsus.doArgumentAndVariableSubstitution(lore.get(i), opener, args));
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
					Ninjutsu jutsu = option.jutsu;
					if (jutsu != null) {
						float power = option.power;
						if (castPower.containsKey(id)) power *= castPower.get(id);

						if (jutsu.isTargetedEntityJutsu() && castEntityTarget.containsKey(id)) jutsu.castAtEntity(player, castEntityTarget.get(id), power);
						else if (jutsu.isTargetedLocationJutsu() && castLocTarget.containsKey(id)) jutsu.castAtLocation(player, castLocTarget.get(id), power);
						else if (bypassNormalCast) jutsu.cast(player, power);
						else jutsu.getJutsu().cast(player, power, null);
					}
					if (option.stayOpen) close = false;
				}
			}

			castPower.remove(id);
			castLocTarget.remove(id);
			castEntityTarget.remove(id);

			if (close) MagicJutsus.scheduleDelayedTask(player::closeInventory, 0);
			else applyOptionsToInventory(player, event.getView().getTopInventory(), MagicJutsus.NULL_ARGS);
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
		private String jutsuName;
		private Ninjutsu jutsu;
		private float power;
		private List<String> modifierList;
		private ModifierSet menuOptionModifiers;
		private boolean stayOpen;
		
	}

}
