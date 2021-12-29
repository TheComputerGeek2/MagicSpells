package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.handlers.DebugHandler;

public class ResourcePackSpell extends TargetedSpell {

	private static final int HASH_LENGTH = 40;

	private final String url;
	private final String hash;
	private final Component prompt;
	private final boolean required;

	public ResourcePackSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		url = getConfigString("url", null);
		hash = getConfigString("hash", null);
		prompt = Util.getMiniMessage(getConfigString("prompt", ""));
		required = getConfigBoolean("required", false);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (hash.length() != HASH_LENGTH) {
			MagicSpells.error("ResourcePackSpell '" + internalName + "' has an incorrect hash length defined: '" + hash.length() + "' / " + HASH_LENGTH + ".");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL && caster instanceof Player player) {
			TargetInfo<Player> target = getTargetedPlayer(player, power, args);
			Player targetPlayer = target.getTarget();
			if (targetPlayer == null) return noTarget(player);
			try {
				player.setResourcePack(url, hash, required, prompt);
			} catch (IllegalArgumentException e) {
				DebugHandler.debugIllegalArgumentException(e);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
