package com.nisovin.magicspells.mana;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.ManaChangeEvent;

public class ManaBar {

	private final String playerName;

	private ManaRank rank;

	private int mana;
	private int maxMana;
	private int regenAmount;
	private String barFormat;

	public ManaBar(Player player, ManaRank rank) {
		playerName = player.getName().toLowerCase();
		setRank(rank);
	}

	public void setRank(ManaRank rank) {
		this.rank = rank;
		mana = rank.getStartingMana();
		maxMana = rank.getMaxMana();
		regenAmount = rank.getRegenAmount();
		barFormat = rank.getBarFormat();
	}

	public Player getPlayer() {
		return Bukkit.getPlayerExact(playerName);
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

	public String getBarFormat() {
		return barFormat;
	}

	public void setBarFormat(String barFormat) {
		this.barFormat = barFormat;
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
