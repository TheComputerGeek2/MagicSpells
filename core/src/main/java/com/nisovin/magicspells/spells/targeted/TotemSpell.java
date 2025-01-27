package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class TotemSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Multimap<UUID, Totem> totems;

	private final int capPerPlayer;
	private final ConfigData<Integer> interval;
	private final ConfigData<Integer> yOffset;
	private final ConfigData<Integer> maxDuration;
	private final ConfigData<Integer> totalPulses;

	private final ConfigData<Double> maxDistance;

	private final ConfigData<Boolean> marker;
	private final ConfigData<Boolean> gravity;
	private final ConfigData<Boolean> silenced;
	private final ConfigData<Boolean> visibility;
	private final ConfigData<Boolean> targetable;
	private final ConfigData<Boolean> centerStand;
	private final ConfigData<Boolean> totemNameVisible;
	private final ConfigData<Boolean> allowCasterTarget;
	private final ConfigData<Boolean> onlyCountOnSuccess;

	private final ConfigData<Component> totemName;

	private final String strAtCap;

	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	private ItemStack mainHand;
	private ItemStack offHand;

	private final List<String> spellNames;
	private List<Subspell> spells;

	private final String spellOnBreakName;
	private Subspell spellOnBreak;

	private final String spellOnSpawnName;
	private Subspell spellOnSpawn;

	public TotemSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		// Equipment
		MagicItem magicMainHandItem = MagicItems.getMagicItemFromString(getConfigString("main-hand", ""));
		if (magicMainHandItem != null) {
			mainHand = magicMainHandItem.getItemStack();
			if (mainHand != null && mainHand.getType().isAir()) mainHand = null;
		}

		MagicItem magicOffHandItem = MagicItems.getMagicItemFromString(getConfigString("off-hand", ""));
		if (magicOffHandItem != null) {
			offHand = magicOffHandItem.getItemStack();
			if (offHand != null && offHand.getType().isAir()) offHand = null;
		}

		MagicItem magicHelmetItem = MagicItems.getMagicItemFromString(getConfigString("helmet", ""));
		if (magicHelmetItem != null) {
			helmet = magicHelmetItem.getItemStack();
			if (helmet != null && helmet.getType().isAir()) helmet = null;
		}

		MagicItem magicChestplateItem = MagicItems.getMagicItemFromString(getConfigString("chestplate", ""));
		if (magicChestplateItem != null) {
			chestplate = magicChestplateItem.getItemStack();
			if (chestplate != null && chestplate.getType().isAir()) chestplate = null;
		}

		MagicItem magicLeggingsItem = MagicItems.getMagicItemFromString(getConfigString("leggings", ""));
		if (magicLeggingsItem != null) {
			leggings = magicLeggingsItem.getItemStack();
			if (leggings != null && leggings.getType().isAir()) leggings = null;
		}

		MagicItem magicBootsItem = MagicItems.getMagicItemFromString(getConfigString("boots", ""));
		if (magicBootsItem != null) {
			boots = magicBootsItem.getItemStack();
			if (boots != null && boots.getType().isAir()) boots = null;
		}

		if (mainHand != null) mainHand.setAmount(1);
		if (offHand != null) offHand.setAmount(1);
		if (helmet != null) helmet.setAmount(1);
		if (chestplate != null) chestplate.setAmount(1);
		if (leggings != null) leggings.setAmount(1);
		if (boots != null) boots.setAmount(1);

		yOffset = getConfigDataInt("y-offset", 0);
		interval = getConfigDataInt("interval", 30);
		maxDuration = getConfigDataInt("max-duration", 0);
		totalPulses = getConfigDataInt("total-pulses", 5);
		capPerPlayer = getConfigInt("cap-per-player", 10);

		maxDistance = getConfigDataDouble("max-distance", 30);

		marker = getConfigDataBoolean("marker", false);
		gravity = getConfigDataBoolean("gravity", false);
		visibility = getConfigDataBoolean("visible", true);
		targetable = getConfigDataBoolean("targetable", true);
		totemNameVisible = getConfigDataBoolean("totem-name-visible", true);
		onlyCountOnSuccess = getConfigDataBoolean("only-count-on-success", false);
		centerStand = getConfigDataBoolean("center-stand", true);
		allowCasterTarget = getConfigDataBoolean("allow-caster-target", false);
		silenced = getConfigDataBoolean("silenced", false);
		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");
		totemName = getConfigDataComponent("totem-name", null);

		spellNames = getConfigStringList("spells", null);
		spellOnBreakName = getConfigString("spell-on-break", "");
		spellOnSpawnName = getConfigString("spell-on-spawn", "");

		totems = HashMultimap.create();
	}

	@Override
	public void initialize() {
		super.initialize();

		String prefix = "TotemSpell '" + internalName + "' has an invalid ";

		spells = new ArrayList<>();
		if (spellNames != null && !spellNames.isEmpty()) {
			Subspell spell;

			for (String spellName : spellNames) {
				spell = initSubspell(spellName, prefix + "spell: '" + spellName + "' defined!");
				if (spell == null) continue;

				spells.add(spell);
			}
		}

		spellOnBreak = initSubspell(spellOnBreakName,
			prefix + "spell-on-break: '" + spellOnBreakName + "' defined!",
			true);
		spellOnSpawn = initSubspell(spellOnSpawnName,
			prefix + "spell-on-spawn: '" + spellOnSpawnName + "' defined!",
			true);

		if (spells.isEmpty()) MagicSpells.error("TotemSpell '" + internalName + "' has no spells defined!");
	}

	@Override
	public void turnOff() {
		totems.values().forEach(totem -> totem.stop(false));
		totems.clear();
	}

	@Override
	public CastResult cast(SpellData data) {
		if (capPerPlayer > 0 && totems.get(data.caster().getUniqueId()).size() >= capPerPlayer)
			return noTarget(strAtCap, data);

		RayTraceResult result = rayTraceBlocks(data);
		if (result == null) return noTarget(data);

		Block target = result.getHitBlock().getRelative(result.getHitBlockFace());

		int yOffset = this.yOffset.get(data);
		if (yOffset != 0) target = target.getRelative(0, yOffset, 0);

		if (!target.isPassable()) return noTarget(data);

		SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, target.getLocation());
		if (!targetEvent.callEvent()) return noTarget(targetEvent);
		data = targetEvent.getSpellData();

		createTotem(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		if (capPerPlayer > 0 && data.hasCaster() && totems.get(data.caster().getUniqueId()).size() >= capPerPlayer)
			return noTarget(strAtCap, data);

		Location location = data.location();

		int yOffset = this.yOffset.get(data);
		if (yOffset != 0) {
			location.add(0, yOffset, 0);
			data = data.location(location);
		}

		if (!location.getBlock().isPassable()) return noTarget(data);

		createTotem(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void createTotem(SpellData data) {
		Location loc = data.location();
		if (centerStand.get(data)) {
			loc.set(loc.getBlockX() + 0.5, loc.getBlockY(), loc.getBlockZ() + 0.5);
			data = data.location(loc);
		}

		Totem totem = new Totem(data);
		totems.put(data.hasCaster() ? data.caster().getUniqueId() : null, totem);

		int maxDuration = this.maxDuration.get(data);
		if (maxDuration > 0) MagicSpells.scheduleDelayedTask(totem::stop, maxDuration, loc);

		playSpellEffects(data);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (totems.isEmpty()) return;
		removeTotems(event.getPlayer());
	}

	@EventHandler
	public void onSpellTarget(SpellTargetEvent e) {
		if (totems.isEmpty()) return;

		LivingEntity target = e.getTarget();
		for (Totem totem : totems.values()) {
			if (!target.equals(totem.armorStand)) continue;

			if (!totem.targetable) e.setCancelled(true);
			else if (!totem.allowCasterTarget && e.getCaster().equals(totem.data.caster())) e.setCancelled(true);

			return;
		}
	}

	@EventHandler
	public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (totems.isEmpty()) return;

		for (Totem totem : totems.values()) {
			if (totem.armorStand.equals(e.getRightClicked())) {
				e.setCancelled(true);
				return;
			}
		}
	}

	public Multimap<UUID, Totem> getTotems() {
		return totems;
	}

	public boolean hasTotem(LivingEntity caster) {
		return !totems.get(caster.getUniqueId()).isEmpty();
	}

	public void removeTotems(LivingEntity caster) {
		Collection<Totem> removed = totems.removeAll(caster.getUniqueId());
		removed.forEach(totem -> totem.stop(false));
	}

	public class Totem implements Runnable {

		private final ArmorStand armorStand;
		private final Location totemLocation;
		private SpellData data;

		private final boolean targetable;
		private final boolean allowCasterTarget;
		private final boolean onlyCountOnSuccess;

		private ScheduledTask task;
		private int pulseCount;
		private final int totalPulses;

		private final double maxDistanceSq;

		private Totem(SpellData data) {
			double maxDistance = TotemSpell.this.maxDistance.get(data);
			maxDistanceSq = maxDistance * maxDistance;

			totalPulses = TotemSpell.this.totalPulses.get(data);

			targetable = TotemSpell.this.targetable.get(data);
			allowCasterTarget = TotemSpell.this.allowCasterTarget.get(data);
			onlyCountOnSuccess = TotemSpell.this.onlyCountOnSuccess.get(data);

			pulseCount = 0;

			Location loc = data.location();
			loc.setYaw(data.caster().getLocation().getYaw());

			armorStand = loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
				stand.customName(totemName.get(data));
				stand.setCustomNameVisible(totemNameVisible.get(data));

				stand.setMarker(marker.get(data));
				stand.setSilent(silenced.get(data));
				stand.setGravity(gravity.get(data));
				stand.setInvulnerable(true);
				stand.setVisible(visibility.get(data));
				stand.addScoreboardTag("MS_Totem");

				EntityEquipment totemEquipment = stand.getEquipment();
				totemEquipment.setItemInMainHand(mainHand);
				totemEquipment.setItemInOffHand(offHand);
				totemEquipment.setHelmet(helmet);
				totemEquipment.setChestplate(chestplate);
				totemEquipment.setLeggings(leggings);
				totemEquipment.setBoots(boots);

				stand.setPersistent(false);
			});

			totemLocation = armorStand.getLocation();
			this.data = data.location(totemLocation);

			if (spellOnSpawn != null) spellOnSpawn.subcast(this.data.retarget(armorStand, null));

			task = MagicSpells.scheduleRepeatingTask(this, 0, interval.get(data));
		}

		@Override
		public void run() {
			armorStand.getLocation(totemLocation);
			data = data.location(totemLocation);

			if (!armorStand.isValid() || !totemLocation.isChunkLoaded()) {
				stop();
				return;
			}

			if (data.hasCaster()) {
				if (!data.caster().isValid()) {
					stop();
					return;
				}

				if (maxDistanceSq > 0 && (!data.caster().getWorld().equals(totemLocation.getWorld()) || totemLocation.distanceSquared(data.caster().getLocation()) > maxDistanceSq)) {
					stop();
					return;
				}
			}

			boolean activated = false;
			for (Subspell spell : spells) activated = spell.subcast(data).success() || activated;

			playSpellEffects(EffectPosition.SPECIAL, totemLocation, data);

			if (totalPulses > 0 && (activated || !onlyCountOnSuccess) && ++pulseCount >= totalPulses)
				stop();
		}

		private void stop() {
			stop(true);
		}

		private void stop(boolean remove) {
			if (task == null) return;

			MagicSpells.cancelTask(task);
			task = null;

			if (remove) totems.remove(data.hasCaster() ? data.caster().getUniqueId() : null, this);

			totemLocation.getWorld().getChunkAtAsync(totemLocation).thenAccept(chunk -> {
				armorStand.remove();

				playSpellEffects(EffectPosition.DISABLED, totemLocation, data);
				if (spellOnBreak != null) spellOnBreak.subcast(data);
			});
		}

	}

}
