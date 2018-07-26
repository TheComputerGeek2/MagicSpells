package com.nisovin.magicspells.util;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;
import com.nisovin.magicspells.materials.MagicMaterial;

public class TemporaryBlockSet implements Runnable {
	
	private Material original;
	private MagicMaterial replaceWith;
	private boolean callPlaceEvent;
	private Player player;
	
	private ArrayList<Block> blocks;
	
	private BlockSetRemovalCallback callback;
	
	public TemporaryBlockSet(Material original, MagicMaterial replaceWith, boolean callPlaceEvent, Player player) {
		this.original = original;
		this.replaceWith = replaceWith;
		this.callPlaceEvent = callPlaceEvent;
		this.player = player;
		
		this.blocks = new ArrayList<>();
	}
	
	public void add(Block block) {
		if (block.getType() == this.original) {
			if (this.callPlaceEvent) {
				BlockState state = block.getState();
				this.replaceWith.setBlock(block, false);
				MagicSpellsBlockPlaceEvent event = new MagicSpellsBlockPlaceEvent(block, state, block, HandHandler.getItemInMainHand(this.player), this.player, true);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					BlockUtils.setTypeAndData(block, this.original, false);
				} else {
					this.blocks.add(block);
				}
			} else {
				this.replaceWith.setBlock(block);
				this.blocks.add(block);
			}
		}
	}
	
	public boolean contains(Block block) {
		return this.blocks.contains(block);
	}
	
	public void removeAfter(int seconds) {
		removeAfter(seconds, null);
	}
	
	public void removeAfter(int seconds, BlockSetRemovalCallback callback) {
		if (this.blocks.isEmpty()) return;
		this.callback = callback;
		MagicSpells.scheduleDelayedTask(this, seconds * TimeUtil.TICKS_PER_SECOND);
	}
	
	@Override
	public void run() {
		if (this.callback != null) this.callback.run(this);
		remove();
	}
	
	public void remove() {
		for (Block block : this.blocks) {
			if (this.replaceWith.equals(block)) block.setType(this.original);
		}
		this.player = null;
	}
	
	public interface BlockSetRemovalCallback {
	
		void run(TemporaryBlockSet set);
	
	}
	
}
