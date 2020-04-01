package com.nisovin.magicjutsus.jutsus.command;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.Jutsubook;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.RegexUtil;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuLearnEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.events.JutsuLearnEvent.LearnSource;

// TODO this should not be hardcoded to use a book
public class TomeJutsu extends CommandJutsu {

	private static final Pattern INT_PATTERN = Pattern.compile("^[0-9]+$");
	
	private boolean consumeBook;
	private boolean allowOverwrite;
	private boolean requireTeachPerm;
	private boolean cancelReadOnLearn;

	private int maxUses;
	private int defaultUses;

	private String strUsage;
	private String strNoBook;
	private String strNoJutsu;
	private String strLearned;
	private String strCantLearn;
	private String strCantTeach;
	private String strAlreadyKnown;
	private String strAlreadyHasJutsu;

	public TomeJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		consumeBook = getConfigBoolean("consume-book", false);
		allowOverwrite = getConfigBoolean("allow-overwrite", false);
		requireTeachPerm = getConfigBoolean("require-teach-perm", true);
		cancelReadOnLearn = getConfigBoolean("cancel-read-on-learn", true);

		maxUses = getConfigInt("max-uses", 5);
		defaultUses = getConfigInt("default-uses", -1);

		strUsage = getConfigString("str-usage", "Usage: While holding a written book, /cast " + name + " <jutsu> [uses]");
		strNoBook = getConfigString("str-no-book", "You must be holding a written book.");
		strNoJutsu = getConfigString("str-no-jutsu", "You do not know a jutsu with that name.");
		strLearned = getConfigString("str-learned", "You have learned the %s jutsu.");
		strCantLearn = getConfigString("str-cant-learn", "You cannot learn the jutsu in this tome.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a tome with that jutsu.");
		strAlreadyKnown = getConfigString("str-already-known", "You already know the %s jutsu.");
		strAlreadyHasJutsu = getConfigString("str-already-has-jutsu", "That book already contains a jutsu.");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			Jutsu jutsu;
			if (args == null || args.length == 0) {
				sendMessage(strUsage, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Jutsubook jutsubook = MagicJutsus.getJutsubook(player);
			jutsu = MagicJutsus.getJutsuByInGameName(args[0]);
			if (jutsu == null || jutsubook == null || !jutsubook.hasJutsu(jutsu)) {
				sendMessage(strNoJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (requireTeachPerm && !MagicJutsus.getJutsubook(player).canTeach(jutsu)) {
				sendMessage(strCantTeach, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			ItemStack item = player.getEquipment().getItemInMainHand();
			if (item.getType() != Material.WRITTEN_BOOK) {
				sendMessage(strNoBook, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!allowOverwrite && getJutsuDataFromTome(item) != null) {
				sendMessage(strAlreadyHasJutsu, player, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			int uses = defaultUses;
			if (args.length > 1 && RegexUtil.matches(INT_PATTERN, args[1])) uses = Integer.parseInt(args[1]);
			item = createTome(jutsu, uses, item);
			player.getEquipment().setItemInMainHand(item);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
	private String getJutsuDataFromTome(ItemStack item) {
		String loreData = Util.getLoreData(item);
		if (loreData != null && loreData.startsWith(internalName + ':')) return loreData.replace(internalName + ':', "");
		return null;
	}

	public ItemStack createTome(Jutsu jutsu, int uses, ItemStack item) {
		if (maxUses > 0 && uses > maxUses) uses = maxUses;
		else if (uses < 0) uses = defaultUses;
		if (item == null) {
			item = new ItemStack(Material.WRITTEN_BOOK, 1);
			BookMeta bookMeta = (BookMeta)item.getItemMeta();
			bookMeta.setTitle(getName() + ": " + jutsu.getName());
			item.setItemMeta(bookMeta);
		}
		Util.setLoreData(item, internalName + ':' + jutsu.getInternalName() + (uses > 0 ? "," + uses : ""));
		return item;
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;
		ItemStack item = event.getItem();
		if (item.getType() != Material.WRITTEN_BOOK) return;
		
		String jutsuData = getJutsuDataFromTome(item);
		if (jutsuData == null || jutsuData.isEmpty()) return;
		
		String[] data = jutsuData.split(",");
		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(data[0]);
		int uses = -1;
		if (data.length > 1) uses = Integer.parseInt(data[1]);
		Jutsubook jutsubook = MagicJutsus.getJutsubook(event.getPlayer());
		if (jutsu == null) return;
		if (jutsubook == null) return;
		
		if (jutsubook.hasJutsu(jutsu)) {
			sendMessage(formatMessage(strAlreadyKnown, "%s", jutsu.getName()), event.getPlayer(), MagicJutsus.NULL_ARGS);
			return;
		}
		if (!jutsubook.canLearn(jutsu)) {
			sendMessage(formatMessage(strCantLearn, "%s", jutsu.getName()), event.getPlayer(), MagicJutsus.NULL_ARGS);
			return;
		}
		JutsuLearnEvent learnEvent = new JutsuLearnEvent(jutsu, event.getPlayer(), LearnSource.TOME, event.getPlayer().getEquipment().getItemInMainHand());
		EventUtil.call(learnEvent);
		if (learnEvent.isCancelled()) {
			sendMessage(formatMessage(strCantLearn, "%s", jutsu.getName()), event.getPlayer(), MagicJutsus.NULL_ARGS);
			return;
		}
		jutsubook.addJutsu(jutsu);
		jutsubook.save();
		sendMessage(formatMessage(strLearned, "%s", jutsu.getName()), event.getPlayer(), MagicJutsus.NULL_ARGS);
		if (cancelReadOnLearn) event.setCancelled(true);

		if (uses > 0) {
			uses--;
			if (uses > 0) Util.setLoreData(item, internalName + ':' + data[0] + ',' + uses);
			else Util.removeLoreData(item);

		}
		if (uses <= 0 && consumeBook) event.getPlayer().getEquipment().setItemInMainHand(null);
		playJutsuEffects(EffectPosition.DELAYED, event.getPlayer());
	}

}
