package com.nisovin.magicspells.spells.targeted;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.MagicSpellsPlayerInteractEvent;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.util.HandHandler;
import com.nisovin.magicspells.util.MagicConfig;

public class TelekinesisSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private boolean checkPlugins;
	
	public TelekinesisSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		checkPlugins = getConfigBoolean("check-plugins", true);
		
		losTransparentBlocks = new HashSet<>(losTransparentBlocks);
		losTransparentBlocks.remove(Material.LEVER);
		losTransparentBlocks.remove(Material.STONE_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.ACACIA_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.BIRCH_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.SPRUCE_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.JUNGLE_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.DARK_OAK_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.OAK_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
		losTransparentBlocks.remove(Material.STONE_BUTTON);
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = getTargetedBlock(player, power);
			if (target == null) {
				// Fail
				return noTarget(player);
			}
			
			// Run target event
			SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, target.getLocation(), power);
			EventUtil.call(event);
			if (event.isCancelled()) return noTarget(player);
			
			target = event.getTargetLocation().getBlock();
			
			// Run effect
			boolean activated = activate(player, target);
			if (!activated) return noTarget(player);
			
			playSpellEffects(player, target.getLocation());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean activate(Player caster, Block target) {
		Material targetType = target.getType();
		if (targetType == Material.LEVER || targetType.name().endsWith("BUTTON")) {
			if (checkPlugins(caster, target)) {
				MagicSpells.getVolatileCodeHandler().toggleLeverOrButton(target);
				return true;
			}
		} else if (targetType.name().endsWith("_PLATE")) {
			if (checkPlugins(caster, target)) {
				MagicSpells.getVolatileCodeHandler().pressPressurePlate(target);
				return true;
			}
		}		
		return false;
	}
	
	private boolean checkPlugins(Player caster, Block target) {
		if (!checkPlugins) return true;
		MagicSpellsPlayerInteractEvent event = new MagicSpellsPlayerInteractEvent(caster, Action.RIGHT_CLICK_BLOCK, HandHandler.getItemInMainHand(caster), target, BlockFace.SELF);
		EventUtil.call(event);
		return event.useInteractedBlock() != Result.DENY;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		boolean activated = activate(caster, target.getBlock());
		if (activated) playSpellEffects(caster, target);
		return activated;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}
	
}
