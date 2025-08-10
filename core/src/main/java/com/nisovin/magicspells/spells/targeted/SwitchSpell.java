package com.nisovin.magicspells.spells.targeted;

import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import org.bukkit.event.player.PlayerTeleportEvent;

public class SwitchSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Integer> switchBack;

	public SwitchSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		switchBack = getConfigDataInt("switch-back", 0);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return noTarget(data);

		Location targetLoc = data.target().getLocation();
		Location casterLoc = data.caster().getLocation();
		data.caster().teleportAsync(targetLoc, PlayerTeleportEvent.TeleportCause.PLUGIN , TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
		data.target().teleportAsync(casterLoc, PlayerTeleportEvent.TeleportCause.PLUGIN , TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
		playSpellEffects(data);

		int switchBack = this.switchBack.get(data);
		if (switchBack <= 0) return new CastResult(PostCastAction.HANDLE_NORMALLY, data);

		MagicSpells.scheduleDelayedTask(() -> {
			if (!data.caster().isValid() || !data.target().isValid()) return;
			Location targetLoc1 = data.target().getLocation();
			Location casterLoc1 = data.caster().getLocation();
			data.target().teleportAsync(targetLoc1, PlayerTeleportEvent.TeleportCause.PLUGIN , TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
			data.caster().teleportAsync(casterLoc1, PlayerTeleportEvent.TeleportCause.PLUGIN , TeleportFlag.EntityState.RETAIN_PASSENGERS, TeleportFlag.EntityState.RETAIN_VEHICLE);
		}, switchBack);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
