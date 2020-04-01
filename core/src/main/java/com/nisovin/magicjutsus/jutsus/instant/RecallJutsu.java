package com.nisovin.magicjutsus.jutsus.instant;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.LocationUtil;
import com.nisovin.magicjutsus.jutsus.InstantJutsu;
import com.nisovin.magicjutsus.util.PlayerNameUtils;
import com.nisovin.magicjutsus.jutsus.TargetedEntityJutsu;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;

public class RecallJutsu extends InstantJutsu implements TargetedEntityJutsu {

	private double maxRange;

	private boolean useBedLocation;
	private boolean allowCrossWorld;

	private String strNoMark;
	private String strTooFar;
	private String strOtherWorld;
	private String strRecallFailed;
	
	private MarkJutsu markJutsu;
	private String markJutsuName;

	public RecallJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);

		maxRange = getConfigDouble("max-range", 0);

		useBedLocation = getConfigBoolean("use-bed-location", false);
		allowCrossWorld = getConfigBoolean("allow-cross-world", true);

		strNoMark = getConfigString("str-no-mark", "You have no mark to recall to.");
		strTooFar = getConfigString("str-too-far", "You mark is too far away.");
		strOtherWorld = getConfigString("str-other-world", "Your mark is in another world.");
		strRecallFailed = getConfigString("str-recall-failed", "Could not recall.");
		markJutsuName = getConfigString("mark-jutsu", "mark");
	}
	
	@Override
	public void initialize() {
		super.initialize();

		Jutsu jutsu = MagicJutsus.getJutsuByInternalName(markJutsuName);
		if (jutsu instanceof MarkJutsu) markJutsu = (MarkJutsu) jutsu;
		else MagicJutsus.error("RecallJutsu '" + internalName + "' has an invalid mark-jutsu defined!");
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		if (state == JutsuCastState.NORMAL) {
			Location markLocation = null;
			if (args != null && args.length == 1 && livingEntity.hasPermission("magicjutsus.advanced." + internalName)) {
				Player target = PlayerNameUtils.getPlayer(args[0]);				
				if (useBedLocation && target != null) markLocation = target.getBedSpawnLocation();
				else if (markJutsu != null) {
					Location loc = markJutsu.getEffectiveMark(target != null ? target.getName().toLowerCase() : args[0].toLowerCase());
					if (loc != null) markLocation = loc;
				}
			} else markLocation = getRecallLocation(livingEntity);

			if (markLocation == null) {
				sendMessage(strNoMark, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			if (!allowCrossWorld && !LocationUtil.isSameWorld(markLocation, livingEntity.getLocation())) {
				sendMessage(strOtherWorld, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			if (maxRange > 0 && markLocation.toVector().distanceSquared(livingEntity.getLocation().toVector()) > maxRange * maxRange) {
				sendMessage(strTooFar, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Location from = livingEntity.getLocation();
			boolean teleported = livingEntity.teleport(markLocation);
			if (!teleported) {
				MagicJutsus.error("Recall teleport blocked for " + livingEntity.getName());
				sendMessage(strRecallFailed, livingEntity, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			playJutsuEffects(EffectPosition.CASTER, from);
			playJutsuEffects(EffectPosition.TARGET, markLocation);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		Location mark = getRecallLocation(caster);
		if (mark == null) return false;
		target.teleport(mark);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	private Location getRecallLocation(LivingEntity caster) {
		if (useBedLocation && caster instanceof Player) return ((Player) caster).getBedSpawnLocation();
		if (markJutsu == null) return null;
		return markJutsu.getEffectiveMark(caster);
	}

}
