package com.nisovin.magicspells.shop;

import java.io.File;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.spells.command.ScrollSpell;

public class MagicSpellsShop extends JavaPlugin implements Listener {
	
	private static final String USE_COUNT_REGEXP = "^[0-9]+( .+)?$";
	private static final Pattern USE_COUNT_PATTERN = Pattern.compile(USE_COUNT_REGEXP);
	
	private static final String CURRENCY_REGEXP = "^[0-9]+(\\.[0-9]+)?$";
	private static final Pattern CURRENCY_PATTERN = Pattern.compile(CURRENCY_REGEXP);
	
	private boolean ignoreOtherPlugins;
	private boolean requireKnownSpell;
	private boolean requireTeachPerm;
	
	private String firstLine;
	private String strAlreadyKnown;
	private String strCantAfford;
	private String strCantLearn;
	private String strPurchased;
	
	private String firstLineScroll;
	private String scrollSpellName;
	private String strCantAffordScroll;
	private String strPurchasedScroll;
	
	private CurrencyHandler currency;
	
	@Override
	public void onEnable() {
		load();
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	public void load() {
		if (!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
		
		Configuration config = getConfig();
		
		this.ignoreOtherPlugins = config.getBoolean("ignore-other-plugins", false);
		this.requireKnownSpell = config.getBoolean("require-known-spell", true);
		this.requireTeachPerm = config.getBoolean("require-teach-perm", true);
		
		this.firstLine = config.getString("first-line", "[SPELL SHOP]");
		this.strAlreadyKnown = config.getString("str-already-known", "You already know that spell.");
		this.strCantAfford = config.getString("str-cant-afford", "You cannot afford that spell.");
		this.strCantLearn = config.getString("str-cant-learn", "You are not able to buy that spell.");
		this.strPurchased = config.getString("str-purchased", "You have purchased the %s spell.");
		
		this.firstLineScroll = config.getString("first-line-scroll", "[SCROLL SHOP]");
		this.scrollSpellName = config.getString("scroll-spell-name", "scroll");
		this.strCantAffordScroll = config.getString("str-cant-afford-scroll", "You cannot afford that scroll.");
		this.strPurchasedScroll = config.getString("str-purchased-scroll", "You have purchased a scroll for the %s spell.");
		
		this.currency = new CurrencyHandler(config);
		
		getLogger().info("MagicSpellsShop config loaded.");
	}
	
	@EventHandler
	public void onMagicSpellsLoad(MagicSpellsLoadedEvent event) {
		load();
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onInteract(PlayerInteractEvent event) {
		if (!this.ignoreOtherPlugins && event.isCancelled()) return;
		
		// Check for right-click on sign
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		if (block == null || !(block.getBlockData() instanceof WallSign)) return;

		// Get shop sign
		Sign sign = (Sign)block.getState();
		String[] lines = sign.getLines();
		if (lines[0].equals(this.firstLine)) {
			processSpellShopSign(event.getPlayer(), lines);
		} else if (lines[0].equals(this.firstLineScroll)) {
			processScrollShopSign(event.getPlayer(), lines);
		}		
	}
	
	private void processSpellShopSign(Player player, String[] lines) {
		// Get spell
		String spellName = lines[1];
		Spell spell = MagicSpells.getSpellByInGameName(spellName);
		if (spell == null) return;
		
		// Check if already known
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		if (spellbook.hasSpell(spell)) {
			MagicSpells.sendMessage(MagicSpells.formatMessage(this.strAlreadyKnown, "%s", spellName), player, null);
			return;
		}
		
		// Get cost
		Cost cost = getCost(lines[2]);
		
		// Check for currency
		if (!this.currency.has(player, cost.amount, cost.currency)) {
			MagicSpells.sendMessage(MagicSpells.formatMessage(this.strCantAfford, "%s", spellName, "%c", cost + ""), player, null);
			return;
		}
		
		// Attempt to teach
		boolean taught = MagicSpells.teachSpell(player, spellName);
		if (!taught) {
			MagicSpells.sendMessage(MagicSpells.formatMessage(this.strCantLearn, "%s", spellName), player, null);
			return;
		}
		
		// Remove currency
		currency.remove(player, cost.amount, cost.currency);
		
		// Success!
		MagicSpells.sendMessage(MagicSpells.formatMessage(strPurchased, "%s", spellName, "%c", cost + ""), player, null);
	}
	
	private void processScrollShopSign(Player player, String[] lines) {
		// Get spell
		String spellName = lines[1];
		Spell spell = MagicSpells.getSpellByInGameName(spellName);
		if (spell == null) return;
		
		// Get uses
		if (!USE_COUNT_PATTERN.asMatchPredicate().test(lines[2])) return;
		int uses = Integer.parseInt(lines[2].split(" ")[0]);
		
		// Get cost
		Cost cost = getCost(lines[3]);
		
		// Check if can afford
		if (!this.currency.has(player, cost.amount, cost.currency)) {
			MagicSpells.sendMessage(MagicSpells.formatMessage(this.strCantAffordScroll, "%s", spellName, "%c", cost + "", "%u", uses + ""), player, null);
			return;
		}
		
		// Create scroll
		ScrollSpell scrollSpell = (ScrollSpell)MagicSpells.getSpellByInternalName(this.scrollSpellName);
		ItemStack scroll = scrollSpell.createScroll(spell, uses, null);
		
		// Remove currency
		this.currency.remove(player, cost.amount, cost.currency);
		
		// Give to player
		PlayerInventory inventory = player.getInventory();
		int slot = inventory.firstEmpty();
		if (inventory.getItemInMainHand().isEmpty()) {
			inventory.setItemInMainHand(scroll);
			player.updateInventory();
		} else if (slot >= 0) {
			player.getInventory().setItem(slot, scroll);
			player.updateInventory();
		} else {
			player.getWorld().dropItem(player.getLocation(), scroll);
		}
		
		// Done!
		MagicSpells.sendMessage(MagicSpells.formatMessage(this.strPurchasedScroll, "%s", spellName, "%c", cost + "", "%u", uses + ""), player, null);
	}
	
	private Cost getCost(String line) {
		Cost cost = new Cost();
		
		// Can we exit early?
		if (line.isEmpty()) return cost;
		
		if (!line.contains(" ") && CURRENCY_PATTERN.asMatchPredicate().test(line)) {
			cost.amount = Double.parseDouble(line);
		} else if (line.contains(" ")) {
			String[] s = line.split(" ");
			if (CURRENCY_PATTERN.asMatchPredicate().test(s[0])) {
				cost.amount = Double.parseDouble(s[0]);
				cost.currency = s[1];
			}
		}
		
		return cost;
	}
	
	@EventHandler
	public void onSignCreate(SignChangeEvent event) {
		if (event.isCancelled()) return;
		
		boolean isSpellShop;
		
		String[] lines = event.getLines();
		if (lines[0].equals(this.firstLine)) {
			isSpellShop = true;
		} else if (lines[0].equals(this.firstLineScroll)) {
			isSpellShop = false;
		} else {
			return;
		}
		
		// Check permission
		Player player = event.getPlayer();
		if (!Perm.CREATESIGNSHOP.has(player)) {
			player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
			event.setCancelled(true);
			return;
		}
		
		// Check for valid spell
		String spellName = lines[1];
		Spell spell = MagicSpells.getSpellByInGameName(spellName);
		if (spell == null) {
			player.sendMessage(ChatColor.RED + "A spell by that name does not exist.");
			event.setCancelled(true);
			return;
		}
		
		// Check permissions
		Spellbook spellbook = MagicSpells.getSpellbook(player);
		if (this.requireKnownSpell && !spellbook.hasSpell(spell)) {
			player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
			event.setCancelled(true);
			return;
		}
		if (this.requireTeachPerm && !spellbook.canTeach(spell)) {
			player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
			event.setCancelled(true);
			return;
		}
		
		// Get cost
		Cost cost = getCost(lines[isSpellShop ? 2 : 3]);
		
		// FIXME this should probably just use string.format
		String shopType = isSpellShop ? "Spell" : "Scroll";
		String effectiveCurrencyLabel = this.currency.isValidCurrency(cost.currency) ? cost.currency : "currency";
		player.sendMessage(shopType + " shop created: " + spellName + (isSpellShop ? "" : '(' + lines[2] + ')') +
				" for " + cost.amount + ' ' + effectiveCurrencyLabel + '.');
		
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		if (!(event.getBlock().getBlockData() instanceof WallSign)) return;

		Sign sign = (Sign) event.getBlock().getState();
		String line = sign.getLine(0);
		if (!isShopSignFirstLine(line)) return;
		if (!Perm.CREATESIGNSHOP.has(event.getPlayer())) event.setCancelled(true);
	}

	private static class Cost {
		
		double amount = 0;
		String currency = null;
		
	}
	
	private boolean isShopSignFirstLine(String firstLine) {
		return firstLine.equals(this.firstLine) || firstLine.equals(this.firstLineScroll);
	}

}
