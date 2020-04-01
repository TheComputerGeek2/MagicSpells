package com.nisovin.magicjutsus.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

public class MagicJutsusBlockBreakEvent extends BlockBreakEvent implements IMagicJutsusCompatEvent {

	public MagicJutsusBlockBreakEvent(Block theBlock, Player player) {
		super(theBlock, player);
	}

}
