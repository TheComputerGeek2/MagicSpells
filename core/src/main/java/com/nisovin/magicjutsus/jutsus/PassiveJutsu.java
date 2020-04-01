package com.nisovin.magicjutsus.jutsus;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicjutsus.Jutsu;
import com.nisovin.magicjutsus.Ninjutsu;
import com.nisovin.magicjutsus.util.Util;
import com.nisovin.magicjutsus.MagicJutsus;
import com.nisovin.magicjutsus.util.CastItem;
import com.nisovin.magicjutsus.util.MagicConfig;
import com.nisovin.magicjutsus.util.compat.EventUtil;
import com.nisovin.magicjutsus.events.JutsuCastEvent;
import com.nisovin.magicjutsus.events.JutsuCastedEvent;
import com.nisovin.magicjutsus.events.JutsuTargetEvent;
import com.nisovin.magicjutsus.jutsueffects.EffectPosition;
import com.nisovin.magicjutsus.jutsus.passive.PassiveManager;
import com.nisovin.magicjutsus.jutsus.passive.PassiveTrigger;
import com.nisovin.magicjutsus.events.JutsuTargetLocationEvent;

public class PassiveJutsu extends Jutsu {

	private static PassiveManager manager;
	
	private Random random = new Random();

	private float chance;

	private int delay;

	private boolean disabled = false;
	private boolean ignoreCancelled;
	private boolean castWithoutTarget;
	private boolean sendFailureMessages;
	private boolean cancelDefaultAction;
	private boolean requireCancelledEvent;
	private boolean cancelDefaultActionWhenCastFails;

	private List<String> triggers;
	private List<String> jutsuNames;
	private List<Ninjutsu> jutsus;

	public PassiveJutsu(MagicConfig config, String jutsuName) {
		super(config, jutsuName);
		
		if (manager == null) manager = new PassiveManager();

		chance = getConfigFloat("chance", 100F) / 100F;

		delay = getConfigInt("delay", -1);

		ignoreCancelled = getConfigBoolean("ignore-cancelled", true);
		castWithoutTarget = getConfigBoolean("cast-without-target", false);
		sendFailureMessages = getConfigBoolean("send-failure-messages", false);
		cancelDefaultAction = getConfigBoolean("cancel-default-action", false);
		requireCancelledEvent = getConfigBoolean("require-cancelled-event", false);
		cancelDefaultActionWhenCastFails = getConfigBoolean("cancel-default-action-when-cast-fails", false);

		triggers = getConfigStringList("triggers", null);
		jutsuNames = getConfigStringList("jutsus", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		// Create jutsu list
		jutsus = new ArrayList<>();
		if (jutsuNames != null) {
			for (String jutsuName : jutsuNames) {
				Ninjutsu jutsu = new Ninjutsu(jutsuName);
				if (!jutsu.process()) continue;
				jutsus.add(jutsu);
			}
		}
		if (jutsus.isEmpty()) {
			MagicJutsus.error("PassiveJutsu '" + internalName + "' has no jutsus defined!");
			return;
		}

		// Get trigger
		int trigCount = 0;
		if (triggers != null) {
			for (String strigger : triggers) {
				String type = strigger;
				String var = null;
				if (strigger.contains(" ")) {
					String[] data = Util.splitParams(strigger, 2);
					type = data[0];
					var = data[1];
				}
				type = type.toLowerCase();

				PassiveTrigger trigger = PassiveTrigger.getByName(type);
				if (trigger != null) {
					manager.registerJutsu(this, trigger, var);
					trigCount++;
				} else {
					MagicJutsus.error("PassiveJutsu '" + internalName + "' has an invalid trigger defined: " + strigger);
				}
			}
		}
		if (trigCount == 0) MagicJutsus.error("PassiveJutsu '" + internalName + "' has no triggers defined!");
	}
	
	public static PassiveManager getManager() {
		return manager;
	}
	
	public List<Ninjutsu> getActivatedJutsus() {
		return jutsus;
	}

	public boolean cancelDefaultAction() {
		return cancelDefaultAction;
	}

	public boolean cancelDefaultActionWhenCastFails() {
		return cancelDefaultActionWhenCastFails;
	}

	public boolean ignoreCancelled() {
		return ignoreCancelled;
	}

	public boolean requireCancelledEvent() {
		return requireCancelledEvent;
	}

	@Override
	public boolean canBind(CastItem item) {
		return false;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}

	public static void resetManager() {
		if (manager == null) return;
		manager.turnOff();
		manager = null;
	}

	private boolean isActuallyNonTargeted(Jutsu jutsu) {
		if (jutsu instanceof ExternalCommandJutsu) return !((ExternalCommandJutsu) jutsu).requiresPlayerTarget();
		if (jutsu instanceof BuffJutsu) return !((BuffJutsu) jutsu).isTargeted();
		return false;
	}

	@Override
	public PostCastAction castJutsu(LivingEntity livingEntity, JutsuCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}
	
	public boolean activate(Player caster) {
		return activate(caster, null, null);
	}
	
	public boolean activate(Player caster, float power) {
		return activate(caster, null, null, power);
	}
	
	public boolean activate(Player caster, LivingEntity target) {
		return activate(caster, target, null, 1F);
	}
	
	public boolean activate(Player caster, Location location) {
		return activate(caster, null, location, 1F);
	}
	
	public boolean activate(final Player caster, final LivingEntity target, final Location location) {
		return activate(caster, target, location, 1F);
	}
	
	public boolean activate(final Player caster, final LivingEntity target, final Location location, final float power) {
		if (delay < 0) return activateJutsus(caster, target, location, power);
		MagicJutsus.scheduleDelayedTask(() -> activateJutsus(caster, target, location, power), delay);
		return false;
	}
	
	// DEBUG INFO: level 3, activating passive jutsu jutsuname for player playername state state
	// DEBUG INFO: level 3, casting jutsu effect jutsuname
	// DEBUG INFO: level 3, casting without target
	// DEBUG INFO: level 3, casting at entity
	// DEBUG INFO: level 3, target cancelled (TE)
	// DEBUG INFO: level 3, casting at location
	// DEBUG INFO: level 3, target cancelled (TL)
	// DEBUG INFO: level 3, casting normally
	// DEBUG INFO: level 3, target cancelled (UE)
	// DEBUG INFO: level 3, target cancelled (UL)
	// DEBUG INFO: level 3, passive jutsu cancelled
	private boolean activateJutsus(Player caster, LivingEntity target, Location location, float basePower) {
		JutsuCastState state = getCastState(caster);
		MagicJutsus.debug(3, "Activating passive jutsu '" + name + "' for player " + caster.getName() + " (state: " + state + ')');
		if (state != JutsuCastState.NORMAL && sendFailureMessages) {
			if (state == JutsuCastState.ON_COOLDOWN) {
				MagicJutsus.sendMessage(formatMessage(strOnCooldown, "%c", Math.round(getCooldown(caster)) + ""), caster, null);
			} else if (state == JutsuCastState.MISSING_REAGENTS) {
				MagicJutsus.sendMessage(strMissingReagents, caster, MagicJutsus.NULL_ARGS);
				if (MagicJutsus.showStrCostOnMissingReagents() && strCost != null && !strCost.isEmpty()) {
					MagicJutsus.sendMessage("    (" + strCost + ')', caster, MagicJutsus.NULL_ARGS);
				}
			}
			return false;
		}

		if (disabled || (chance < 0.999 && random.nextFloat() > chance) || state != JutsuCastState.NORMAL) return false;

		disabled = true;
		JutsuCastEvent event = new JutsuCastEvent(this, caster, JutsuCastState.NORMAL, basePower, null, cooldown, reagents.clone(), 0);
		EventUtil.call(event);
		if (event.isCancelled() || event.getJutsuCastState() != JutsuCastState.NORMAL) {
			MagicJutsus.debug(3, "   Passive jutsu cancelled");
			disabled = false;
			return false;
		}
		if (event.haveReagentsChanged() && !hasReagents(caster, event.getReagents())) {
			disabled = false;
			return false;
		}
		setCooldown(caster, event.getCooldown());
		basePower = event.getPower();
		boolean jutsuEffectsDone = false;
		for (Ninjutsu jutsu : jutsus) {
			MagicJutsus.debug(3, "    Casting jutsu effect '" + jutsu.getJutsu().getName() + '\'');
			if (castWithoutTarget) {
				MagicJutsus.debug(3, "    Casting without target");
				jutsu.cast(caster, basePower);
				if (!jutsuEffectsDone) {
					playJutsuEffects(EffectPosition.CASTER, caster);
					jutsuEffectsDone = true;
				}
			} else if (jutsu.isTargetedEntityJutsu() && target != null && !isActuallyNonTargeted(jutsu.getJutsu())) {
				MagicJutsus.debug(3, "    Casting at entity");
				JutsuTargetEvent targetEvent = new JutsuTargetEvent(this, caster, target, basePower);
				EventUtil.call(targetEvent);
				if (!targetEvent.isCancelled()) {
					target = targetEvent.getTarget();
					jutsu.castAtEntity(caster, target, targetEvent.getPower());
					if (!jutsuEffectsDone) {
						playJutsuEffects(caster, target);
						jutsuEffectsDone = true;
					}
				} else MagicJutsus.debug(3, "      Target cancelled (TE)");
			} else if (jutsu.isTargetedLocationJutsu() && (location != null || target != null)) {
				MagicJutsus.debug(3, "    Casting at location");
				Location loc = null;
				if (location != null) loc = location;
				else if (target != null) loc = target.getLocation();
				if (loc != null) {
					JutsuTargetLocationEvent targetEvent = new JutsuTargetLocationEvent(this, caster, loc, basePower);
					EventUtil.call(targetEvent);
					if (!targetEvent.isCancelled()) {
						loc = targetEvent.getTargetLocation();
						jutsu.castAtLocation(caster, loc, targetEvent.getPower());
						if (!jutsuEffectsDone) {
							playJutsuEffects(caster, loc);
							jutsuEffectsDone = true;
						}
					} else MagicJutsus.debug(3, "      Target cancelled (TL)");
				}
			} else {
				MagicJutsus.debug(3, "    Casting normally");
				float power = basePower;
				if (target != null) {
					JutsuTargetEvent targetEvent = new JutsuTargetEvent(this, caster, target, power);
					EventUtil.call(targetEvent);
					if (!targetEvent.isCancelled()) {
						power = targetEvent.getPower();
					} else {
						MagicJutsus.debug(3, "      Target cancelled (UE)");
						continue;
					}
				} else if (location != null) {
					JutsuTargetLocationEvent targetEvent = new JutsuTargetLocationEvent(this, caster, location, basePower);
					EventUtil.call(targetEvent);
					if (!targetEvent.isCancelled()) {
						power = targetEvent.getPower();
					} else {
						MagicJutsus.debug(3, "      Target cancelled (UL)");
						continue;
					}
				}
				jutsu.cast(caster, power);
				if (!jutsuEffectsDone) {
					playJutsuEffects(EffectPosition.CASTER, caster);
					jutsuEffectsDone = true;
				}
			}
		}

		removeReagents(caster, event.getReagents());
		sendMessage(strCastSelf, caster, MagicJutsus.NULL_ARGS);
		JutsuCastedEvent event2 = new JutsuCastedEvent(this, caster, JutsuCastState.NORMAL, basePower, null, event.getCooldown(), event.getReagents(), PostCastAction.HANDLE_NORMALLY);
		EventUtil.call(event2);
		disabled = false;
		return true;
	}

}
