package com.nisovin.magicspells.spells.instant;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

import io.papermc.lib.PaperLib;

public class RecallSpell extends InstantSpell implements TargetedEntitySpell {

	private ConfigData<Double> maxRange;

	private boolean useBedLocation;
	private boolean allowCrossWorld;

	private String strNoMark;
	private String strTooFar;
	private String strOtherWorld;
	private String strRecallFailed;

	private MarkSpell markSpell;
	private final String markSpellName;

	public RecallSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxRange = getConfigDataDouble("max-range", 0);

		useBedLocation = getConfigBoolean("use-bed-location", false);
		allowCrossWorld = getConfigBoolean("allow-cross-world", true);

		strNoMark = getConfigString("str-no-mark", "You have no mark to recall to.");
		strTooFar = getConfigString("str-too-far", "You mark is too far away.");
		strOtherWorld = getConfigString("str-other-world", "Your mark is in another world.");
		strRecallFailed = getConfigString("str-recall-failed", "Could not recall.");
		markSpellName = getConfigString("mark-spell", "mark");
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(markSpellName);
		if (spell instanceof MarkSpell) markSpell = (MarkSpell) spell;
		else MagicSpells.error("RecallSpell '" + internalName + "' has an invalid mark-spell defined!");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location markLocation = null;
			if (args != null && args.length == 1 && caster.hasPermission("magicspells.advanced." + internalName)) {
				Player target = PlayerNameUtils.getPlayer(args[0]);
				if (useBedLocation && target != null) markLocation = target.getBedSpawnLocation();
				else if (markSpell != null) {
					Location loc = markSpell.getEffectiveMark(target != null ? target.getName().toLowerCase() : args[0].toLowerCase());
					if (loc != null) markLocation = loc;
				}
			} else markLocation = getRecallLocation(caster);

			if (markLocation == null) {
				sendMessage(strNoMark, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			Location from = caster.getLocation();

			if (!allowCrossWorld && !LocationUtil.isSameWorld(markLocation, from)) {
				sendMessage(strOtherWorld, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			double maxRange = this.maxRange.get(caster, null, power, args);
			if (maxRange > 0 && markLocation.distanceSquared(from) > maxRange * maxRange) {
				sendMessage(strTooFar, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			boolean canTeleport = (!(caster instanceof Vehicle)) && !caster.isDead();
			if (!canTeleport) {
				MagicSpells.error("Recall teleport blocked for " + caster.getName());
				sendMessage(strRecallFailed, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}
			PaperLib.teleportAsync(caster, markLocation);

			playSpellEffects(EffectPosition.CASTER, from);
			playSpellEffects(EffectPosition.TARGET, markLocation);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		Location mark = getRecallLocation(caster);
		if (mark == null) return false;
		PaperLib.teleportAsync(target, mark);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	private Location getRecallLocation(LivingEntity caster) {
		if (useBedLocation && caster instanceof Player) return ((Player) caster).getBedSpawnLocation();
		if (markSpell == null) return null;
		return markSpell.getEffectiveMark(caster);
	}

	public boolean shouldUseBedLocation() {
		return useBedLocation;
	}

	public void setUseBedLocation(boolean useBedLocation) {
		this.useBedLocation = useBedLocation;
	}

	public boolean shouldAllowCrossWorld() {
		return allowCrossWorld;
	}

	public void setAllowCrossWorld(boolean allowCrossWorld) {
		this.allowCrossWorld = allowCrossWorld;
	}

	public String getStrNoMark() {
		return strNoMark;
	}

	public void setStrNoMark(String strNoMark) {
		this.strNoMark = strNoMark;
	}

	public String getStrTooFar() {
		return strTooFar;
	}

	public void setStrTooFar(String strTooFar) {
		this.strTooFar = strTooFar;
	}

	public String getStrOtherWorld() {
		return strOtherWorld;
	}

	public void setStrOtherWorld(String strOtherWorld) {
		this.strOtherWorld = strOtherWorld;
	}

	public String getStrRecallFailed() {
		return strRecallFailed;
	}

	public void setStrRecallFailed(String strRecallFailed) {
		this.strRecallFailed = strRecallFailed;
	}

	public MarkSpell getMarkSpell() {
		return markSpell;
	}

	public void setMarkSpell(MarkSpell markSpell) {
		this.markSpell = markSpell;
	}

}
