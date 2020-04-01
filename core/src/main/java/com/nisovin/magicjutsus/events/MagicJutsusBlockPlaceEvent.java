package com.nisovin.magicjutsus.events;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.BlockPlaceEvent;

public class MagicJutsusBlockPlaceEvent extends BlockPlaceEvent implements IMagicJutsusCompatEvent {

	public MagicJutsusBlockPlaceEvent(Block placedBlock, BlockState replacedBlockState, Block placedAgainst, ItemStack itemInHand, Player thePlayer, boolean canBuild) {
		super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, EquipmentSlot.HAND);
	}

	public MagicJutsusBlockPlaceEvent(Block placedBlock, BlockState replacedBlockState, Block placedAgainst, ItemStack itemInHand, Player thePlayer, boolean canBuild, EquipmentSlot equipmentSlot) {
		super(placedBlock, replacedBlockState, placedAgainst, itemInHand, thePlayer, canBuild, equipmentSlot);
	}

}
