package com.nisovin.magicspells.spells.targeted;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.materials.MagicMaterial;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;

public class BombSpell extends TargetedSpell implements TargetedLocationSpell {

	MagicMaterial bomb;
	int fuse;
	int interval;
	Subspell spell;
	
	public BombSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		bomb = MagicSpells.getItemNameResolver().resolveBlock(getConfigString("block", "stone"));
		fuse = getConfigInt("fuse", 100);
		interval = getConfigInt("interval", 20);
		spell = new Subspell(getConfigString("spell", ""));
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (!spell.process() || !spell.isTargetedLocationSpell()) {
			spell = null;
			MagicSpells.error("Invalid spell on BombSpell '" + internalName + '\'');
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			List<Block> blocks = getLastTwoTargetedBlocks(player, power);
			if (blocks.size() != 2) return noTarget(player);
			if (!blocks.get(1).getType().isSolid()) return noTarget(player);
			Block target = blocks.get(0);
			boolean ok = bomb(player, target.getLocation(), power);
			if (!ok) return noTarget(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean bomb(final Player player, final Location loc, final float power) {
		final Block block = loc.getBlock();
		if (block.getType() == Material.AIR || block.getType() == Material.TALL_GRASS || block.getType() == Material.SNOW) {
			bomb.setBlock(block);
			if (player != null) {
				playSpellEffects(player, loc);
			} else {
				playSpellEffects(EffectPosition.TARGET, loc);
			}
			new SpellAnimation(interval, interval, true) {
				
				int time = 0;
				Location l = block.getLocation().add(0.5, 0.5, 0.5);
				@Override
				protected void onTick(int tick) {
					time += interval;
					if (time >= fuse) {
						stop();
						if (bomb.equals(block)) {
							block.setType(Material.AIR);
							playSpellEffects(EffectPosition.DELAYED, l);
							spell.castAtLocation(player, l, power);
						}						
					} else if (!bomb.equals(block)) {
						stop();
					} else {
						playSpellEffects(EffectPosition.SPECIAL, l);
					}
				}
				
			};
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		return bomb(caster, target, power);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return bomb(null, target, power);
	}

}
