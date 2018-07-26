package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.MaterialData;

import com.nisovin.magicspells.materials.MagicBlockMaterial;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.Util;

public class LightwalkSpell extends BuffSpell {
	
	private HashMap<String, Block> lightwalkers;
	private MagicMaterial mat = new MagicBlockMaterial(Material.GLOWSTONE);

	public LightwalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		this.lightwalkers = new HashMap<>();
	}

	@Override
	public boolean castBuff(Player player, float power, String[] args) {
		this.lightwalkers.put(player.getName(), null);
		return true;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		String playerName = p.getName();
		if (this.lightwalkers.containsKey(playerName)) {
			Block oldBlock = this.lightwalkers.get(playerName);
			Block newBlock = p.getLocation().getBlock().getRelative(BlockFace.DOWN);
			if (!Objects.equals(oldBlock, newBlock) && allowedType(newBlock.getType()) && newBlock.getType() != Material.AIR) {
				if (isExpired(p)) {
					turnOff(p);
				} else {
					if (oldBlock != null) Util.restoreFakeBlockChange(p, oldBlock);
					Util.sendFakeBlockChange(p, newBlock, mat);
					this.lightwalkers.put(playerName, newBlock);
					addUse(p);
					chargeUseCost(p);
				}
			}
		}
	}
	
	private boolean allowedType(Material mat) {
		return mat == Material.DIRT || 
			mat == Material.GRASS ||
			mat == Material.GRAVEL ||
			mat == Material.STONE ||
			mat == Material.COBBLESTONE ||
			mat.name().endsWith("WOOD") ||
			mat.name().endsWith("LOG") ||
			mat == Material.NETHERRACK ||
			mat == Material.SOUL_SAND ||
			mat == Material.SAND ||
			mat == Material.SANDSTONE ||
			mat == Material.GLASS ||
			mat.name().endsWith("WOOL") ||
			mat.name().endsWith("SLAB") ||
			mat == Material.BRICK ||
			mat == Material.OBSIDIAN;
	}
	
	@Override
	public void turnOffBuff(Player player) {
		Block b = this.lightwalkers.remove(player.getName());
		if (b == null) return;
		Util.restoreFakeBlockChange(player, b);
	}

	@Override
	protected void turnOff() {
		for (String s : this.lightwalkers.keySet()) {
			Player p = PlayerNameUtils.getPlayer(s);
			if (p == null) continue;
			
			Block b = this.lightwalkers.get(s);
			if (b == null) continue;
			
			Util.restoreFakeBlockChange(p, b);
		}
		this.lightwalkers.clear();
	}

	@Override
	public boolean isActive(Player player) {
		return this.lightwalkers.containsKey(player.getName());
	}

}
