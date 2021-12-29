package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class SkinSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private String texture;
	private String signature;
	
	public SkinSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		texture = getConfigString("texture", null);
		signature = getConfigString("signature", null);
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<Player> targetInfo = getTargetedPlayer(caster, power, args);
			if (targetInfo == null || targetInfo.getTarget() == null) return noTarget(caster);

			Util.setSkin(targetInfo.getTarget(), texture, signature);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		if (!(target instanceof Player player)) return false;
		Util.setSkin(player, texture, signature);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!(target instanceof Player player)) return false;
		Util.setSkin(player, texture, signature);
		return true;
	}

}
