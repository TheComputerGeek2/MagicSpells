package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class FlySpell extends TargetedSpell implements TargetedEntitySpell {

	private final Set<UUID> wasAllowedFlight;

	private final ConfigData<Boolean> setFlying;
	private final ConfigData<TargetBooleanState> targetBooleanState;

	public FlySpell(MagicConfig config, String spellName) {
		super(config, spellName);

		wasAllowedFlight = new HashSet<>();

		setFlying = getConfigDataBoolean("set-flying", true);
		targetBooleanState = getConfigDataTargetBooleanState("target-state", TargetBooleanState.TOGGLE);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Player> info = getTargetedPlayer(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		setFlyingState(info.target(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player player)) return noTarget(data);
		setFlyingState(player, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	protected void turnOff() {
		Player player;
		for (UUID uuid : wasAllowedFlight) {
			player = Bukkit.getPlayer(uuid);
			if (player == null) continue;
			if (player.getGameMode() == GameMode.CREATIVE) continue;
			if (player.getGameMode() == GameMode.SPECTATOR) continue;
			player.setAllowFlight(false);
		}
		wasAllowedFlight.clear();
	}

	private void setFlyingState(Player target, SpellData data) {
		boolean newState = targetBooleanState.get(data).getBooleanState(target.isFlying() || target.getAllowFlight());
		boolean setFlying = this.setFlying.get(data);

		UUID uuid = target.getUniqueId();
		if (newState) {
			if (!target.getAllowFlight()) {
				target.setAllowFlight(true);
				wasAllowedFlight.add(uuid);
			}
			if (setFlying) target.teleport(target.getLocation().add(0, 0.25, 0));
		} else {
			boolean wasAllowed = wasAllowedFlight.remove(uuid);
			if (wasAllowed) target.setAllowFlight(false);
		}
		if (setFlying) target.setFlying(newState);

		playSpellEffects(data);
	}

}
