package com.nisovin.magicjutsus.shop;

import java.io.File;
import java.util.regex.Pattern;

import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.events.MagicJutsusLoadedEvent;
import com.nisovin.magicjutsus.jutsus.command.ScrollJutsu;

public class MagicJutsusShop extends JavaPlugin implements Listener {
	
	private static final String USE_COUNT_REGEXP = "^[0-9]+( .+)?$";
	private static final Pattern USE_COUNT_PATTERN = Pattern.compile(USE_COUNT_REGEXP);
	
	private static final String CURRENCY_REGEXP = "^[0-9]+(\\.[0-9]+)?$";
	private static final Pattern CURRENCY_PATTERN = Pattern.compile(CURRENCY_REGEXP);
	
	private boolean ignoreOtherPlugins;
	private boolean requireKnownJutsu;
	private boolean requireTeachPerm;
	
	private String firstLine;
	private String strAlreadyKnown;
	private String strCantAfford;
	private String strCantLearn;
	private String strPurchased;
	
	private String firstLineScroll;
	private String scrollJutsuName;
	private String strCantAffordScroll;
	private String strPurchasedScroll;
	
	private CurrencyHandler currency;
	
	@Override
	public void onEnable() {
		load();
		
		// Register events
		EventUtil.register(this, this);
	}
	
	public void load() {
		if (!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();
		
		Configuration config = getConfig();
		
		this.ignoreOtherPlugins = config.getBoolean("ignore-other-plugins", false);
		this.requireKnownJutsu = config.getBoolean("require-known-jutsu", true);
		this.requireTeachPerm = config.getBoolean("require-teach-perm", true);
		
		this.firstLine = config.getString("first-line", "[JUTSU SHOP]");
		this.strAlreadyKnown = config.getString("str-already-known", "You already know that jutsu.");
		this.strCantAfford = config.getString("str-cant-afford", "You cannot afford that jutsu.");
		this.strCantLearn = config.getString("str-cant-learn", "You are not able to buy that jutsu.");
		this.strPurchased = config.getString("str-purchased", "You have purchased the %s jutsu.");
		
		this.firstLineScroll = config.getString("first-line-scroll", "[SCROLL SHOP]");
		this.scrollJutsuName = config.getString("scroll-jutsu-name", "scroll");
		this.strCantAffordScroll = config.getString("str-cant-afford-scroll", "You cannot afford that scroll.");
		this.strPurchasedScroll = config.getString("str-purchased-scroll", "You have purchased a scroll for the %s jutsu.");
		
		this.currency = new CurrencyHandler(config);
		
		getLogger().info("MagicJutsusShop config loaded.");
	}
	
	@EventHandler
	public void onMagicJutsusLoad(MagicJutsusLoadedEvent event) {
		load();
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onInteract(PlayerInteractEvent event) {
		if (!this.ignoreOtherPlugins && event.isCancelled()) return;
		
		// Check for right-click on sign
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) return;
		
		// Get shop sign
		Sign sign = (Sign)block.getState();
		String[] lines = sign.getLines();		
		if (lines[0].equals(this.firstLine)) {
			processJutsuShopSign(event.getPlayer(), lines);
		} else if (lines[0].equals(this.firstLineScroll)) {
			processScrollShopSign(event.getPlayer(), lines);
		}		
	}
	
	private void processJutsuShopSign(Player player, String[] lines) {
		// Get jutsu
		String jutsuName = lines[1];
		Jutsu jutsu = MagicJutsus.getJutsuByInGameName(jutsuName);
		if (jutsu == null) return;
		
		// Check if already known
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		if (jutsubook.hasJutsu(jutsu)) {
			MagicJutsus.sendMessage(MagicJutsus.formatMessage(this.strAlreadyKnown, "%s", jutsuName), player, null);
			return;
		}
		
		// Get cost
		Cost cost = getCost(lines[2]);
		
		// Check for currency
		if (!this.currency.has(player, cost.amount, cost.currency)) {
			MagicJutsus.sendMessage(MagicJutsus.formatMessage(this.strCantAfford, "%s", jutsuName, "%c", cost + ""), player, null);
			return;
		}
		
		// Attempt to teach
		boolean taught = MagicJutsus.teachJutsu(player, jutsuName);
		if (!taught) {
			MagicJutsus.sendMessage(MagicJutsus.formatMessage(this.strCantLearn, "%s", jutsuName), player, null);
			return;
		}
		
		// Remove currency
		currency.remove(player, cost.amount, cost.currency);
		
		// Success!
		MagicJutsus.sendMessage(MagicJutsus.formatMessage(strPurchased, "%s", jutsuName, "%c", cost + ""), player, null);
	}
	
	private void processScrollShopSign(Player player, String[] lines) {
		// Get jutsu
		String jutsuName = lines[1];
		Jutsu jutsu = MagicJutsus.getJutsuByInGameName(jutsuName);
		if (jutsu == null) return;
		
		// Get uses
		if (!RegexUtil.matches(USE_COUNT_PATTERN, lines[2])) return;
		int uses = Integer.parseInt(lines[2].split(" ")[0]);
		
		// Get cost
		Cost cost = getCost(lines[3]);
		
		// Check if can afford
		if (!this.currency.has(player, cost.amount, cost.currency)) {
			MagicJutsus.sendMessage(MagicJutsus.formatMessage(this.strCantAffordScroll, "%s", jutsuName, "%c", cost + "", "%u", uses + ""), player, null);
			return;
		}
		
		// Create scroll
		ScrollJutsu scrollJutsu = (ScrollJutsu)MagicJutsus.getJutsuByInternalName(this.scrollJutsuName);
		ItemStack scroll = scrollJutsu.createScroll(jutsu, uses, null);
		
		// Remove currency
		this.currency.remove(player, cost.amount, cost.currency);
		
		// Give to player
		int slot = player.getInventory().firstEmpty();
		if (player.getItemInHand() == null) {
			player.setItemInHand(scroll);
			player.updateInventory();
		} else if (slot >= 0) {
			player.getInventory().setItem(slot, scroll);
			player.updateInventory();
		} else {
			player.getWorld().dropItem(player.getLocation(), scroll);
		}
		
		// Done!
		MagicJutsus.sendMessage(MagicJutsus.formatMessage(this.strPurchasedScroll, "%s", jutsuName, "%c", cost + "", "%u", uses + ""), player, null);
	}
	
	private Cost getCost(String line) {
		Cost cost = new Cost();
		
		// Can we exit early?
		if (line.isEmpty()) return cost;
		
		if (!line.contains(" ") && RegexUtil.matches(CURRENCY_PATTERN, line)) {
			cost.amount = Double.parseDouble(line);
		} else if (line.contains(" ")) {
			String[] s = line.split(" ");
			if (RegexUtil.matches(CURRENCY_PATTERN, s[0])) {
				cost.amount = Double.parseDouble(s[0]);
				cost.currency = s[1];
			}
		}
		
		return cost;
	}
	
	@EventHandler
	public void onSignCreate(SignChangeEvent event) {
		if (event.isCancelled()) return;
		
		boolean isJutsuShop;
		
		String lines[] = event.getLines();
		if (lines[0].equals(this.firstLine)) {
			isJutsuShop = true;
		} else if (lines[0].equals(this.firstLineScroll)) {
			isJutsuShop = false;
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
		
		// Check for valid jutsu
		String jutsuName = lines[1];
		Jutsu jutsu = MagicJutsus.getJutsuByInGameName(jutsuName);
		if (jutsu == null) {
			player.sendMessage(ChatColor.RED + "A jutsu by that name does not exist.");
			event.setCancelled(true);
			return;
		}
		
		// Check permissions
		Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
		if (this.requireKnownJutsu && !jutsubook.hasJutsu(jutsu)) {
			player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
			event.setCancelled(true);
			return;
		}
		if (this.requireTeachPerm && !jutsubook.canTeach(jutsu)) {
			player.sendMessage(ChatColor.RED + "You do not have permission to do that.");
			event.setCancelled(true);
			return;
		}
		
		// Get cost
		Cost cost = getCost(lines[isJutsuShop ? 2 : 3]);
		
		// FIXME this should probably just use string.format
		String shopType = isJutsuShop ? "Jutsu" : "Scroll";
		String effectiveCurrencyLabel = this.currency.isValidCurrency(cost.currency) ? cost.currency : "currency";
		player.sendMessage(shopType + " shop created: " + jutsuName + (isJutsuShop ? "" : '(' + lines[2] + ')') +
				" for " + cost.amount + ' ' + effectiveCurrencyLabel + '.');
		
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		
		Material mat = event.getBlock().getType();
		if (mat != Material.WALL_SIGN && mat != Material.SIGN_POST) return;
		
		Sign sign = (Sign)event.getBlock().getState();
		String line = sign.getLine(0);
		if (!isShopSignFirstLine(line)) return;
		if (!Perm.CREATESIGNSHOP.has(event.getPlayer())) event.setCancelled(true);
	}

	@Override
	public void onDisable() {
		
	}
	
	private static class Cost {
		
		double amount = 0;
		String currency = null;
		
	}
	
	private boolean isShopSignFirstLine(String firstLine) {
		return firstLine.equals(this.firstLine) || firstLine.equals(this.firstLineScroll);
	}

}
