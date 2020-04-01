package com.nisovin.magicjutsus.mana;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.ManaChangeEvent;

public class ManaBar {

	private String prefix;
	private String playerName;

	private ManaRank rank;

	private ChatColor colorFull;
	private ChatColor colorEmpty;

	private int mana;
	private int maxMana;
	private int regenAmount;
	
	public ManaBar(Player player, ManaRank rank) {
		playerName = player.getName().toLowerCase();
		setRank(rank);		
	}
	
	public void setRank(ManaRank rank) {
		this.rank = rank;
		mana = rank.getStartingMana();
		maxMana = rank.getMaxMana();
		regenAmount = rank.getRegenAmount();
		setDisplayData(rank.getPrefix(), rank.getColorFull(), rank.getColorEmpty());
	}
	
	public Player getPlayer() {
		return PlayerNameUtils.getPlayerExact(playerName);
	}
	
	public ManaRank getManaRank() {
		return rank;
	}
	
	public int getMana() {
		return mana;
	}
	
	public int getMaxMana() {
		return maxMana;
	}
	
	public int getRegenAmount() {
		return regenAmount;
	}
	
	public void setMaxMana(int max) {
		maxMana = max;
		if (mana > maxMana) mana = maxMana;
	}
	
	public void setRegenAmount(int amount) {
		regenAmount = amount;
	}
	
	private void setDisplayData(String prefix, ChatColor colorFull, ChatColor colorEmpty) {
		this.prefix = ChatColor.translateAlternateColorCodes('&', prefix);
		this.colorFull = colorFull;
		this.colorEmpty = colorEmpty;
	}
	
	public String getPrefix() {
		return prefix;
	}

	public ChatColor getColorFull() {
		return colorFull;
	}

	public ChatColor getColorEmpty() {
		return colorEmpty;
	}
	
	public boolean has(int amount) {
		return mana >= amount;
	}
	
	public boolean changeMana(int amount, ManaChangeReason reason) {
		int newAmt = mana;
		
		if (amount > 0) {
			if (mana == maxMana) return false;
			newAmt += amount;
			if (newAmt > maxMana) newAmt = maxMana;
		} else if (amount < 0) {
			if (mana == 0) return false;
			newAmt += amount;
			if (newAmt < 0) newAmt = 0;
		}
		if (newAmt == mana) return false;
		
		newAmt = callManaChangeEvent(newAmt, reason);
		if (newAmt > maxMana) newAmt = maxMana;
		if (newAmt < 0) newAmt = 0;
		if (newAmt == mana) return false;
		mana = newAmt;
		return true;
	}
	
	public boolean setMana(int amount, ManaChangeReason reason) {
		int newAmt = amount;
		if (newAmt > maxMana) newAmt = maxMana;
		else if (newAmt < 0) newAmt = 0;
		
		newAmt = callManaChangeEvent(newAmt, reason);
		if (newAmt == mana) return false;
		mana = newAmt;
		return true;
	}
	
	public boolean regenerate() {
		if ((regenAmount > 0 && mana == maxMana) || (regenAmount < 0 && mana == 0)) return false;
		return changeMana(regenAmount, ManaChangeReason.REGEN);
	}
	
	private int callManaChangeEvent(int newAmt, ManaChangeReason reason) {
		Player player = getPlayer();
		if (player == null || !player.isOnline()) return newAmt;
		ManaChangeEvent event = new ManaChangeEvent(player, mana, newAmt, maxMana, reason);
		EventUtil.call(event);
		return event.getNewAmount();
	}
	
}
