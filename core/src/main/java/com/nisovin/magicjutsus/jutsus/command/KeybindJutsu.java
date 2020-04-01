package com.nisovin.magicjutsus.jutsus.command;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.BlockUtils;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.CommandJutsu;

public class KeybindJutsu extends CommandJutsu {

	private Map<String, Keybinds> playerKeybinds;

	private ItemStack wandItem;
	private ItemStack defaultJutsuIcon;

	public KeybindJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		playerKeybinds = new HashMap<>();

		wandItem = Util.getItemStackFromString(getConfigString("wand-item", "blaze_rod"));
		defaultJutsuIcon = Util.getItemStackFromString(getConfigString("default-jutsu-icon", "redstone"));
	}
	
	@Override
	protected void initialize() {
		super.initialize();

		Util.forEachPlayerOnline(this::loadKeybinds);
	}

	private void loadKeybinds(Player player) {
		File file = new File(MagicJutsus.plugin.getDataFolder(), "jutsubooks" + File.separator + "keybinds-" + player.getName().toLowerCase() + ".txt");
		if (!file.exists()) return;
		try {
			Keybinds keybinds = new Keybinds(player);
			YamlConfiguration conf = new YamlConfiguration();
			conf.load(file);
			for (String key : conf.getKeys(false)) {
				int slot = Integer.parseInt(key);
				String jutsuName = conf.getString(key);
				Jutsu jutsu = MagicJutsus.getJutsuByInternalName(jutsuName);
				if (jutsu != null) keybinds.setKeybind(slot, jutsu);
			}
			playerKeybinds.put(player.getName(), keybinds);
		} catch (Exception e) {
			MagicJutsus.plugin.getLogger().severe("Failed to load player keybinds for " + player.getName());
			e.printStackTrace();
		}

	}
	
	private void saveKeybinds(Keybinds keybinds) {		
		File file = new File(MagicJutsus.plugin.getDataFolder(), "jutsubooks" + File.separator + "keybinds-" + keybinds.player.getName().toLowerCase() + ".txt");
		YamlConfiguration conf = new YamlConfiguration();
		Jutsu[] binds = keybinds.keybinds;
		for (int i = 0; i < binds.length; i++) {
			if (binds[i] == null) continue;
			conf.set(i + "", binds[i].getInternalName());
		}
		try {
			conf.save(file);
		} catch (IOException e) {
			MagicJutsus.plugin.getLogger().severe("Failed to save keybinds for " + keybinds.player.getName());
			e.printStackTrace();
		}
	}
	
	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args.length != 1) {
				player.sendMessage("invalid args");
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Keybinds keybinds = playerKeybinds.computeIfAbsent(player.getName(), name -> new Keybinds(player));
			
			int slot = player.getInventory().getHeldItemSlot();
			ItemStack item = player.getEquipment().getItemInMainHand();
			
			if (args[0].equalsIgnoreCase("clear")) {
				keybinds.clearKeybind(slot);
				saveKeybinds(keybinds);
				return PostCastAction.HANDLE_NORMALLY;
			}
			if (args[0].equalsIgnoreCase("clearall")) {
				keybinds.clearKeybinds();
				saveKeybinds(keybinds);
				return PostCastAction.HANDLE_NORMALLY;
			}
			if (item != null && !BlockUtils.isAir(item.getType())) {
				player.sendMessage("not empty");
				return PostCastAction.ALREADY_HANDLED;
			}

			Jutsu jutsu = MagicJutsus.getJutsubook(player).getJutsuByName(args[0]);
			if (jutsu == null) {
				player.sendMessage("no jutsu");
				return PostCastAction.ALREADY_HANDLED;
			}

			keybinds.setKeybind(slot, jutsu);
			keybinds.select(slot);
			saveKeybinds(keybinds);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		keybinds.deselect(event.getPreviousSlot());
		keybinds.select(event.getNewSlot());
	}
	
	@EventHandler
	public void onAnimate(PlayerAnimationEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		boolean casted = keybinds.castKeybind(event.getPlayer().getInventory().getHeldItemSlot());
		if (casted) event.setCancelled(true);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds == null) return;
		
		if (keybinds.hasKeybind(event.getPlayer().getInventory().getHeldItemSlot())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		loadKeybinds(event.getPlayer());
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}
	
	private class Keybinds {
		
		private Player player;
		private Jutsu[] keybinds;

		private Keybinds(Player player) {
			this.player = player;
			this.keybinds = new Jutsu[10];
		}

		private void deselect(int slot) {
			Jutsu jutsu = keybinds[slot];
			if (jutsu == null) return;
			ItemStack jutsuIcon = jutsu.getJutsuIcon();
			if (jutsuIcon == null) jutsuIcon = defaultJutsuIcon;
			MagicJutsus.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, jutsuIcon);
		}

		private void select(int slot) {
			Jutsu jutsu = keybinds[slot];
			if (jutsu == null) return;
			MagicJutsus.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, wandItem);
		}

		private boolean hasKeybind(int slot) {
			return keybinds[slot] != null;
		}

		private boolean castKeybind(int slot) {
			Jutsu jutsu = keybinds[slot];
			if (jutsu == null) return false;
			jutsu.cast(player);
			return true;
		}

		private void setKeybind(int slot, Jutsu jutsu) {
			keybinds[slot] = jutsu;
		}

		private void clearKeybind(int slot) {
			keybinds[slot] = null;
			MagicJutsus.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, null);
		}

		private void clearKeybinds() {
			for (int i = 0; i < keybinds.length; i++) {
				if (keybinds[i] == null) continue;
				keybinds[i] = null;
				MagicJutsus.getVolatileCodeHandler().sendFakeSlotUpdate(player, i, null);
			}
		}
		
	}

}
