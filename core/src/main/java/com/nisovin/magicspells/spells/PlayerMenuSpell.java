package com.nisovin.magicspells.spells;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.castmodifiers.ModifierSet;

public class PlayerMenuSpell extends TargetedSpell implements TargetedEntitySpell {

	private Map<UUID, Float> spellPower;

	private final int delay;
	private final String title;
	private final double radius;
	private final boolean stayOpen;
	private final boolean addOpener;
	private final String skullName;
	private final String skullNameOffline;
	private final String skullNameRadius;
	private final List<String> skullLore;
	private final String spellRangeName;
	private final String spellOfflineName;
	private final boolean castSpellsOnTarget;
	private final String spellOnLeftName;
	private final String spellOnRightName;
	private final String spellOnMiddleName;
	private final String spellOnSneakLeftName;
	private final String spellOnSneakRightName;
	private final List<String> playerModifiersStrings;
	private final String variableTarget;

	private Subspell spellOffline;
	private Subspell spellRange;
	private Subspell spellOnLeft;
	private Subspell spellOnRight;
	private Subspell spellOnMiddle;
	private Subspell spellOnSneakLeft;
	private Subspell spellOnSneakRight;
	private ModifierSet playerModifiers;

	public PlayerMenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		delay = getConfigInt("delay", 0);
		title = getConfigString("title", "PlayerMenuSpell '" + internalName + "'");
		radius = getConfigDouble("radius", 0);
		stayOpen = getConfigBoolean("stay-open", false);
		addOpener = getConfigBoolean("add-opener", false);
		skullName = getConfigString("skull-name", "&6%t");
		skullNameOffline = getConfigString("skull-name-offline", "&4%t");
		skullNameRadius = getConfigString("skull-name-radius", "&4%t &3out of radius.");
		skullLore = getConfigStringList("skull-lore", null);
		spellOfflineName = getConfigString("spell-offline", "");
		spellRangeName = getConfigString("spell-range", "");
		castSpellsOnTarget = getConfigBoolean("cast-spells-on-target", true);
		spellOnLeftName = getConfigString("spell-on-left", "");
		spellOnRightName = getConfigString("spell-on-right", "");
		spellOnMiddleName = getConfigString("spell-on-middle", "");
		spellOnSneakLeftName = getConfigString("spell-on-sneak-left", "");
		spellOnSneakRightName = getConfigString("spell-on-sneak-right", "");
		playerModifiersStrings = getConfigStringList("player-modifiers", null);
		variableTarget = getConfigString("variable-target", null);
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (playerModifiersStrings != null && !playerModifiersStrings.isEmpty()) {
			playerModifiers = new ModifierSet(playerModifiersStrings);
		}
	}

	@Override
	public void initialize() {
		super.initialize();

		spellOffline = initSubspell(spellOfflineName, "PlayerMenuSpell '" + internalName + "' has an invalid spell-offline defined!");
		spellRange = initSubspell(spellRangeName, "PlayerMenuSpell '" + internalName + "' has an invalid spell-range defined!");
		spellOnLeft = initSubspell(spellOnLeftName, "PlayerMenuSpell '" + internalName + "' has an invalid spell-on-left defined!");
		spellOnRight = initSubspell(spellOnRightName, "PlayerMenuSpell '" + internalName + "' has an invalid spell-on-right defined!");
		spellOnMiddle = initSubspell(spellOnMiddleName, "PlayerMenuSpell '" + internalName + "' has an invalid spell-on-middle defined!");
		spellOnSneakLeft = initSubspell(spellOnSneakLeftName, "PlayerMenuSpell '" + internalName + "' has an invalid spell-on-sneak-left defined!");
		spellOnSneakRight = initSubspell(spellOnSneakRightName, "PlayerMenuSpell '" + internalName + "' has an invalid spell-on-sneak-right defined!");

		spellPower = new HashMap<>();
	}

	@Override
	public PostCastAction castSpell(LivingEntity livingEntity, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && livingEntity instanceof Player) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(livingEntity, power);
			if (targetInfo == null) return noTarget(livingEntity);
			Player target = targetInfo.getTarget();
			if (target == null) return noTarget(livingEntity);
			openDelay(target, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		openDelay((Player) target, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		openDelay((Player) target, power);
		return true;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length < 1) return false;
		Player player = Bukkit.getPlayer(args[0]);
		if (player == null) return false;
		openDelay(player, 1);
		return true;
	}

	private void openDelay(Player opener, float power) {
		spellPower.put(opener.getUniqueId(), power);
		if (delay > 0) MagicSpells.scheduleDelayedTask(() -> open(opener), delay);
		else open(opener);
	}

	private String translate(Player player, Player target, String string) {
		if (target != null) string = string.replaceAll("%t", target.getName());
		string = string.replaceAll("%a", player.getName());
		return Util.doVarReplacementAndColorize(player, string);
	}

	private void processClickSpell(Subspell subspell, Player caster, Player target, float power) {
		if (subspell == null) return;
		if (castSpellsOnTarget && subspell.isTargetedEntitySpell()) {
			subspell.castAtEntity(caster, target, power);
			return;
		}
		subspell.cast(caster, power);
	}

	private void open(Player opener) {
		List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
		if (!addOpener) players.remove(opener);
		if (playerModifiers != null) players.removeIf(player -> !playerModifiers.check(player));
		if (radius > 0) players.removeIf(player -> opener.getLocation().distance(player.getLocation()) > radius);

		int size = (int) Math.ceil((players.size()+1) / 9.0) * 9;
		Inventory inv = Bukkit.createInventory(opener, size, internalName);

		for (int i = 0; i < players.size(); i++) {
			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			ItemMeta itemMeta = head.getItemMeta();
			SkullMeta skullMeta = (SkullMeta) itemMeta;
			if (skullMeta == null) continue;
			skullMeta.setOwningPlayer(players.get(i));
			itemMeta.setDisplayName(translate(opener, players.get(i), skullName));
			if (skullLore != null) {
				List<String> lore = new ArrayList<>();
				for (String loreLine : skullLore) {
					lore.add(translate(opener, players.get(i), loreLine));
				}
				itemMeta.setLore(lore);
			}
			head.setItemMeta(skullMeta);
			inv.setItem(i, head);
		}
		opener.openInventory(inv);
		Util.setInventoryTitle(opener, title);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		spellPower.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler
	public void onItemClick(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		if (!event.getView().getTitle().equals(internalName)) return;
		event.setCancelled(true);
		ItemStack item = event.getCurrentItem();
		if (item == null) return;
		ItemMeta itemMeta = item.getItemMeta();
		SkullMeta skullMeta = (SkullMeta) itemMeta;
		if (skullMeta == null) return;
		OfflinePlayer target = skullMeta.getOwningPlayer();
		float power = spellPower.containsKey(player.getUniqueId()) ?  spellPower.get(player.getUniqueId()) : 1;
		if (target == null || !target.isOnline()) {
			itemMeta.setDisplayName(translate(player, null, skullNameOffline));
			if (spellOffline != null) spellOffline.cast(player, power);
			if (stayOpen) item.setItemMeta(itemMeta);
			else {
				player.closeInventory();
				spellPower.remove(player.getUniqueId());
			}
			return;
		} else {
			itemMeta.setDisplayName(translate(player, (Player) target, skullName));
			item.setItemMeta(itemMeta);
		}
		Player targetPlayer = (Player) target;
		if (radius > 0  && targetPlayer.getLocation().distance(player.getLocation()) > radius) {
			itemMeta.setDisplayName(translate(player, targetPlayer, skullNameRadius));
			if (spellRange != null) spellRange.cast(player, power);
			if (stayOpen) item.setItemMeta(itemMeta);
			else {
				player.closeInventory();
				spellPower.remove(player.getUniqueId());
			}
			return;
		}
		switch (event.getClick()) {
			case LEFT: processClickSpell(spellOnLeft, player, targetPlayer, power); break;
			case RIGHT: processClickSpell(spellOnRight, player, targetPlayer, power); break;
			case MIDDLE: processClickSpell(spellOnMiddle, player, targetPlayer, power); break;
			case SHIFT_LEFT: processClickSpell(spellOnSneakLeft, player, targetPlayer, power); break;
			case SHIFT_RIGHT: processClickSpell(spellOnSneakRight, player, targetPlayer, power); break;
		}
		if (variableTarget != null && !variableTarget.isEmpty() && MagicSpells.getVariableManager().getVariable(variableTarget) != null) {
			MagicSpells.getVariableManager().set(variableTarget, player, target.getName());
		}
		if (stayOpen) open(player);
		else {
			player.closeInventory();
			spellPower.remove(player.getUniqueId());
		}
	}
}
