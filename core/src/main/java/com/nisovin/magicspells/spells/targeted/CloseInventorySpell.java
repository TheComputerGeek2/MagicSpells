package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class CloseInventorySpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Integer> delay;

	public CloseInventorySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		delay = getConfigDataInt("delay", 0);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Player> info = getTargetedPlayer(data);
		if (info.noTarget()) return noTarget(info);
		data = info.spellData();

		close(info.target(), data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player target)) return noTarget(data);
		close(target, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void close(Player target, SpellData data) {
		int delay = this.delay.get(data);

		if (delay > 0) {
			MagicSpells.scheduleDelayedTask(() -> {
				target.closeInventory();
				playSpellEffects(data);
			}, delay, target);
		} else {
			target.closeInventory();
			playSpellEffects(data);
		}
	}

}
