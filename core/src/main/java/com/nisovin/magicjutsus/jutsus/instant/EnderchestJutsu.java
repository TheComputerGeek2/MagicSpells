package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;

public class EnderchestJutsu extends InstantJutsu implements TargetedEntityJutsu {

	public EnderchestJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL && livingEntity instanceof Player) {
			Player player = (Player) livingEntity;
			if (args != null && args.length == 1 && player.hasPermission("magicjutsus.advanced." + internalName)) {
				Player target = PlayerNameUtils.getPlayer(args[0]);
				if (target == null) {
					player.sendMessage("Invalid player target");
					return PostCastAction.ALREADY_HANDLED;
				}
				player.openInventory(target.getEnderChest());
			} else player.openInventory(player.getEnderChest());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Player)) return false;
		if (!(caster instanceof Player)) return false;
		((Player) caster).openInventory(((Player) target).getEnderChest());
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
