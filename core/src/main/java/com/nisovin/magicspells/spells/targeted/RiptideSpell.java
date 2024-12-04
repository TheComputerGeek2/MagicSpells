package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class RiptideSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Integer> duration;

	public RiptideSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		duration = getConfigDataInt("duration", 40);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Player> info = getTargetedPlayer(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player player)) return noTarget(data);

		player.startRiptideAttack(duration.get(data), 0, null);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
