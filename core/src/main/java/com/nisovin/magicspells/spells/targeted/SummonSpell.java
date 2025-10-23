package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.util.RayTraceResult;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class SummonSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final Map<UUID, SummonData> pending;

	private final ConfigData<Integer> maxAcceptDelay;

	private final ConfigData<Boolean> requireExactName;
	private final ConfigData<Boolean> requireAcceptance;

	private final String strUsage;
	private final String acceptCommand;
	private final String strSummonPending;
	private final String strSummonExpired;
	private final String strSummonAccepted;

	public SummonSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		maxAcceptDelay = getConfigDataInt("max-accept-delay", 90);

		requireExactName = getConfigDataBoolean("require-exact-name", false);
		requireAcceptance = getConfigDataBoolean("require-acceptance", true);

		strUsage = getConfigString("str-usage", "Usage: /cast summon <playername>, or /cast summon \nwhile looking at a sign with a player name on the first line.");
		acceptCommand = getConfigString("accept-command", "accept");
		strSummonPending = getConfigString("str-summon-pending", "You are being summoned! Type /accept to teleport.");
		strSummonExpired = getConfigString("str-summon-expired", "The summon has expired.");
		strSummonAccepted = getConfigString("str-summon-accepted", "You have been summoned.");

		pending = new HashMap<>();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		// Get target name and landing location
		String targetName = "";
		Location landLoc = null;

		if (data.hasArgs()) {
			targetName = data.args()[0];
			landLoc = data.caster().getLocation().add(0, .25, 0);
		} else {
			RayTraceResult result = rayTraceBlocks(data.power(10));
			if (result != null && result.getHitBlock().getState() instanceof Sign sign) {
				targetName = Util.getStringFromComponent(sign.getSide(Side.FRONT).line(0));
				if (targetName.isEmpty()) targetName = Util.getStringFromComponent(sign.getSide(Side.BACK).line(0));
				landLoc = sign.getLocation().add(.5, .25, .5);
			}
		}

		// Check usage
		if (targetName.isEmpty()) {
			// Fail -- show usage
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		// Check location
		if (!BlockUtils.isSafeToStand(landLoc.clone())) {
			sendMessage(strUsage, caster, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		// Get player
		LivingEntity target = requireExactName.get(data) ? Bukkit.getPlayerExact(targetName) : Bukkit.getPlayer(targetName);
		if (target == null || !validTargetList.canTarget(caster, target)) return noTarget(data);

		SpellTargetEvent targetEvent = new SpellTargetEvent(this, data, target);
		if (!targetEvent.callEvent()) return noTarget(targetEvent);

		data = targetEvent.getSpellData();
		target = data.target();

		// Teleport player
		if (requireAcceptance.get(data)) {
			pending.put(target.getUniqueId(), new SummonData(landLoc, System.currentTimeMillis(), maxAcceptDelay.get(data), data));
			sendMessage(strSummonPending, target, data);
		} else {
			target.teleportAsync(landLoc);
			sendMessage(strSummonAccepted, target, data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.hasCaster()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (requireAcceptance.get(data) && data.target() instanceof Player target) {
			pending.put(target.getUniqueId(), new SummonData(data.caster().getLocation(), System.currentTimeMillis(), maxAcceptDelay.get(data), data));
			sendMessage(strSummonPending, target, data);
		} else {
			data.target().teleportAsync(data.caster().getLocation());
			sendMessage(strSummonAccepted, data.target(), data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		if (requireAcceptance.get(data) && data.target() instanceof Player target) {
			pending.put(target.getUniqueId(), new SummonData(data.location(), System.currentTimeMillis(), maxAcceptDelay.get(data), data));
			sendMessage(strSummonPending, target, data);
		} else {
			data.target().teleportAsync(data.location());
			sendMessage(strSummonAccepted, data.target(), data);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!event.getMessage().equalsIgnoreCase('/' + acceptCommand)) return;

		Player player = event.getPlayer();

		SummonData data = pending.remove(player.getUniqueId());
		if (data == null) return;

		event.setCancelled(true);

		if (data.maxAcceptDelay > 0 && data.time + data.maxAcceptDelay * TimeUtil.MILLISECONDS_PER_SECOND < System.currentTimeMillis()) {
			sendMessage(strSummonExpired, player, data.spellData);
			return;
		}

		player.teleportAsync(data.location);
		sendMessage(strSummonAccepted, player, data.spellData);
		playSpellEffects(EffectPosition.DELAYED, player, data.spellData);
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return args.length == 1 ? TxtUtil.tabCompletePlayerName(sender) : null;
	}

	private record SummonData(Location location, long time, int maxAcceptDelay, SpellData spellData) {
	}

}
