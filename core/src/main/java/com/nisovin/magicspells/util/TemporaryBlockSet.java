package com.nisovin.magicspells.util;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.MagicSpellsBlockPlaceEvent;

public class TemporaryBlockSet implements Runnable {

	private Random random;

	private LivingEntity livingEntity;
	private Material original;
	private boolean callPlaceEvent;

	private List<Block> blocks;
	private List<Material> replaceMaterials;

	private BlockSetRemovalCallback callback;
	
	public TemporaryBlockSet(Material original, Material replaceWith, boolean callPlaceEvent, LivingEntity livingEntity) {
		this.original = original;
		this.callPlaceEvent = callPlaceEvent;
		this.livingEntity = livingEntity;

		random = ThreadLocalRandom.current();
		blocks = new ArrayList<>();
		replaceMaterials = new ArrayList<>();

		replaceMaterials.add(replaceWith);
	}

	public TemporaryBlockSet(Material original, List<Material> replaceMaterials, boolean callPlaceEvent, LivingEntity livingEntity) {
		this.original = original;
		this.replaceMaterials = replaceMaterials;
		this.callPlaceEvent = callPlaceEvent;
		this.livingEntity = livingEntity;

		random = new Random();
		blocks = new ArrayList<>();
	}
	
	public void add(Block block) {
		if (replaceMaterials.isEmpty()) return;
		if (block.getType() != original) return;
		int r = random.nextInt(replaceMaterials.size());
		if (!callPlaceEvent) {
			block.setType(replaceMaterials.get(r));
			blocks.add(block);
			return;
		}

		BlockState state = block.getState();
		block.setType(replaceMaterials.get(r), false);
		MagicSpellsBlockPlaceEvent event = null;
		if (livingEntity instanceof Player) event = new MagicSpellsBlockPlaceEvent(block, state, block, livingEntity.getEquipment().getItemInMainHand(), (Player) livingEntity, true);
		if (event != null) EventUtil.call(event);
		if (event != null && event.isCancelled()) BlockUtils.setTypeAndData(block, original, original.createBlockData(), false);
		else blocks.add(block);
	}

	public void untrack(Block block) {
		blocks.remove(block);
	}

	public boolean contains(Block block) {
		return blocks.contains(block);
	}
	
	public void removeAfter(int ticks) {
		removeAfter(ticks, null);
	}
	
	public void removeAfter(int ticks, BlockSetRemovalCallback callback) {
		if (blocks.isEmpty()) return;
		this.callback = callback;
		MagicSpells.scheduleDelayedTask(this, ticks);
	}
	
	@Override
	public void run() {
		if (callback != null) callback.run(this);
		remove();
	}
	
	public void remove() {
		for (Block block : blocks) {
			if (replaceMaterials.contains(block.getType())) block.setType(original);
		}
		livingEntity = null;
	}
	
	public interface BlockSetRemovalCallback {
	
		void run(TemporaryBlockSet set);
	
	}
	
}
