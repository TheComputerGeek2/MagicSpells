package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.LocationUtil;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class TotemSpell extends TargetedSpell implements TargetedLocationSpell {

	private final Set<Totem> totems;
	private final PulserTicker ticker;

	private final int yOffset;
	private final int interval;
	private final int totalPulses;
	private final int capPerPlayer;

	private double maxDistanceSquared;
	private final int maxDuration;

	private final boolean marker;
	private final boolean gravity;
	private final boolean visibility;
	private final boolean targetable;
	private final boolean totemNameVisible;
	private final boolean onlyCountOnSuccess;
	private final boolean centerStand;
	private final boolean allowCasterTarget;
	private final boolean silenced;

	private final String strAtCap;
	private final String totemName;

	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	private ItemStack mainHand;
	private ItemStack offHand;

	private final List<String> spellNames;
	private List<Subspell> spells;

	private final String spellNameOnBreak;
	private Subspell spellOnBreak;

	public TotemSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		// Equipment
		MagicItem magicMainHandItem = MagicItems.getMagicItemFromString(getConfigString("main-hand", ""));
		if (magicMainHandItem != null) {
			mainHand = magicMainHandItem.getItemStack();
			if (mainHand != null && BlockUtils.isAir(mainHand.getType())) mainHand = null;
		}

		MagicItem magicOffHandItem = MagicItems.getMagicItemFromString(getConfigString("off-hand", ""));
		if (magicOffHandItem != null) {
			offHand = magicOffHandItem.getItemStack();
			if (offHand != null && BlockUtils.isAir(offHand.getType())) offHand = null;
		}

		MagicItem magicHelmetItem = MagicItems.getMagicItemFromString(getConfigString("helmet", ""));
		if (magicHelmetItem != null) {
			helmet = magicHelmetItem.getItemStack();
			if (helmet != null && BlockUtils.isAir(helmet.getType())) helmet = null;
		}

		MagicItem magicChestplateItem = MagicItems.getMagicItemFromString(getConfigString("chestplate", ""));
		if (magicChestplateItem != null) {
			chestplate = magicChestplateItem.getItemStack();
			if (chestplate != null && BlockUtils.isAir(chestplate.getType())) chestplate = null;
		}

		MagicItem magicLeggingsItem = MagicItems.getMagicItemFromString(getConfigString("leggings", ""));
		if (magicLeggingsItem != null) {
			leggings = magicLeggingsItem.getItemStack();
			if (leggings != null && BlockUtils.isAir(leggings.getType())) leggings = null;
		}

		MagicItem magicBootsItem = MagicItems.getMagicItemFromString(getConfigString("boots", ""));
		if (magicBootsItem != null) {
			boots = magicBootsItem.getItemStack();
			if (boots != null && BlockUtils.isAir(boots.getType())) boots = null;
		}

		if (mainHand != null) mainHand.setAmount(1);
		if (offHand != null) offHand.setAmount(1);
		if (helmet != null) helmet.setAmount(1);
		if (chestplate != null) chestplate.setAmount(1);
		if (leggings != null) leggings.setAmount(1);
		if (boots != null) boots.setAmount(1);

		yOffset = getConfigInt("y-offset", 0);
		interval = getConfigInt("interval", 30);
		totalPulses = getConfigInt("total-pulses", 5);
		capPerPlayer = getConfigInt("cap-per-player", 10);

		maxDistanceSquared = getConfigDouble("max-distance", 30);
		maxDistanceSquared *= maxDistanceSquared;

		maxDuration = getConfigInt("max-duration", 0);

		marker = getConfigBoolean("marker", false);
		gravity = getConfigBoolean("gravity", false);
		visibility = getConfigBoolean("visible", true);
		targetable = getConfigBoolean("targetable", true);
		totemNameVisible = getConfigBoolean("totem-name-visible", true);
		onlyCountOnSuccess = getConfigBoolean("only-count-on-success", false);
		centerStand = getConfigBoolean("center-stand", true);
		allowCasterTarget = getConfigBoolean("allow-caster-target", false);
		silenced = getConfigBoolean("silenced", false);
		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");
		totemName = getConfigString("totem-name", "");

		spellNames = getConfigStringList("spells", null);
		spellNameOnBreak = getConfigString("spell-on-break", "");

		totems = new HashSet<>();
		ticker = new PulserTicker();
	}

	@Override
	public void initialize() {
		super.initialize();

		spells = new ArrayList<>();
		if (spellNames != null && !spellNames.isEmpty()) {
			for (String spellName : spellNames) {
				Subspell spell = new Subspell(spellName);
				if (!spell.process() || !spell.isTargetedLocationSpell()) continue;
				spells.add(spell);
			}
		}

		if (!spellNameOnBreak.isEmpty()) {
			spellOnBreak = new Subspell(spellNameOnBreak);
			if (!spellOnBreak.process() || !spellOnBreak.isTargetedLocationSpell()) {
				MagicSpells.error("TotemSpell '" + internalName + "' has an invalid spell-on-break defined");
				spellOnBreak = null;
			}
		}

		if (spells.isEmpty()) MagicSpells.error("TotemSpell '" + internalName + "' has no spells defined!");
	}

	@Override
	public void turnOff() {
		for (Totem t : totems) {
			t.stop();
		}

		totems.clear();
		ticker.stop();
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (capPerPlayer > 0) {
				int count = 0;
				for (Totem pulser : totems) {
					if (!pulser.caster.equals(caster)) continue;

					count++;
					if (count >= capPerPlayer) {
						sendMessage(strAtCap, caster, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			List<Block> lastTwo = getLastTwoTargetedBlocks(caster, power);
			Block target = null;

			if (lastTwo != null && lastTwo.size() == 2) target = lastTwo.get(0);
			if (target == null) return noTarget(caster);
			if (yOffset > 0) target = target.getRelative(BlockFace.UP, yOffset);
			else if (yOffset < 0) target = target.getRelative(BlockFace.DOWN, yOffset);
			if (!BlockUtils.isAir(target.getType()) && target.getType() != Material.SNOW && target.getType() != Material.TALL_GRASS) return noTarget(caster);

			if (target != null) {
				SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, caster, target.getLocation(), power);
				EventUtil.call(event);
				if (event.isCancelled()) return noTarget(caster);
				target = event.getTargetLocation().getBlock();
				power = event.getPower();
			}
			createTotem(caster, target.getLocation(), power, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power, String[] args) {
		Block block = target.getBlock();
		if (yOffset > 0) block = block.getRelative(BlockFace.UP, yOffset);
		else if (yOffset < 0) block = block.getRelative(BlockFace.DOWN, yOffset);

		if (BlockUtils.isAir(block.getType()) || block.getType() == Material.SNOW || block.getType() == Material.TALL_GRASS) {
			if (!centerStand) {
				Location loc = target.clone();
				createTotem(caster, loc, power, args);
			} else createTotem(caster, block.getLocation(), power, args);
			return true;
		}
		block = block.getRelative(BlockFace.UP);
		if (BlockUtils.isAir(block.getType()) || block.getType() == Material.SNOW || block.getType() == Material.TALL_GRASS) {
			if (!centerStand) {
				Location loc = target.clone();
				createTotem(caster, loc, power, args);
			} else createTotem(caster, block.getLocation(), power, args);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtLocation(Location target, float power, String[] args) {
		return castAtLocation(null, target, power, args);
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return castAtLocation(null, target, power, null);
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		return castAtLocation(caster, target, power, null);
	}

	private void createTotem(LivingEntity caster, Location loc, float power, String[] args) {
		Location loc2 = loc.clone();
		if (centerStand) loc2 = loc.clone().add(0.5, 0, 0.5);

		Totem totem = new Totem(caster, loc2, power);
		totems.add(totem);
		if (maxDuration > 0) {
			MagicSpells.scheduleDelayedTask(() -> {
				totem.stop();
				totems.remove(totem);
			}, maxDuration);
		}

		ticker.start();
		if (caster != null) playSpellEffects(caster, loc2);
		else playSpellEffects(EffectPosition.TARGET, loc2);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (totems.isEmpty()) return;
		Player player = event.getEntity();
		Iterator<Totem> iter = totems.iterator();
		while (iter.hasNext()) {
			Totem pulser = iter.next();
			if (pulser.caster == null) continue;
			if (!pulser.caster.equals(player)) continue;
			pulser.stop();
			iter.remove();
		}
	}

	@EventHandler
	public void onSpellTarget(SpellTargetEvent e) {
		LivingEntity target = e.getTarget();
		if (totems.isEmpty()) return;
		for (Totem t : totems) {
			if (target.equals(t.armorStand) && !targetable) e.setCancelled(true);
			else if (e.getCaster().equals(t.caster) && target.equals(t.armorStand) && !allowCasterTarget) e.setCancelled(true);
		}
	}

	@EventHandler
	public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
		if (totems.isEmpty()) return;
		for (Totem t : totems) {
			if (t.armorStand.equals(e.getRightClicked())) e.setCancelled(true);
		}
	}

	private class Totem {

		private final LivingEntity caster;
		private final LivingEntity armorStand;
		private Location totemLocation;

		private final float power;
		private int pulseCount;

		private Totem(LivingEntity caster, Location loc, float power) {
			this.caster = caster;
			this.power = power;

			pulseCount = 0;
			loc.setYaw(caster.getLocation().getYaw());
			armorStand = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
			if (!totemName.isEmpty()) {
				if (caster instanceof Player) armorStand.setCustomName(MagicSpells.doArgumentAndVariableSubstitution(Util.colorize(totemName), (Player) caster, null));
				else armorStand.setCustomName(Util.colorize(totemName));
				armorStand.setCustomNameVisible(totemNameVisible);
			}
			EntityEquipment totemEquipment = armorStand.getEquipment();
			armorStand.setGravity(gravity);
			armorStand.addScoreboardTag("MS_Totem");
			if (totemEquipment != null) {
				if (mainHand != null) totemEquipment.setItemInMainHand(mainHand);
				if (offHand != null) totemEquipment.setItemInOffHand(offHand);
				if (helmet != null) totemEquipment.setHelmet(helmet);
				if (chestplate != null) totemEquipment.setChestplate(chestplate);
				if (leggings != null) totemEquipment.setLeggings(leggings);
				if (boots != null) totemEquipment.setBoots(boots);
			}
			((ArmorStand) armorStand).setVisible(visibility);
			((ArmorStand) armorStand).setMarker(marker);
			armorStand.setSilent(silenced);
			armorStand.setInvulnerable(true);
			totemLocation = armorStand.getLocation();
		}

		private boolean pulse() {
			totemLocation = armorStand.getLocation();
			if (caster == null) {
				if (!armorStand.isDead()) return activate();
				stop();
				return true;
			} else if (caster.isValid() && !armorStand.isDead() && totemLocation.getChunk().isLoaded()) {
				if (maxDistanceSquared > 0 && (!LocationUtil.isSameWorld(totemLocation, caster) || totemLocation.distanceSquared(caster.getLocation()) > maxDistanceSquared)) {
					stop();
					return true;
				}
				return activate();
			}
			stop();
			return true;
		}

		private boolean activate() {
			boolean activated = false;
			for (Subspell spell : spells) {
				activated = spell.castAtLocation(caster, totemLocation, power) || activated;
			}

			playSpellEffects(EffectPosition.SPECIAL, totemLocation);
			if (totalPulses > 0 && (activated || !onlyCountOnSuccess)) {
				pulseCount += 1;
				if (pulseCount >= totalPulses) {
					stop();
					return true;
				}
			}
			return false;
		}

		private void stop() {
			if (!totemLocation.getChunk().isLoaded()) totemLocation.getChunk().load();
			armorStand.remove();
			playSpellEffects(EffectPosition.DISABLED, totemLocation);
			if (spellOnBreak != null) spellOnBreak.castAtLocation(caster, totemLocation, power);
		}

	}

	private class PulserTicker implements Runnable {

		private int taskId = -1;

		private void start() {
			if (taskId < 0) taskId = MagicSpells.scheduleRepeatingTask(this, 0, interval);
		}

		@Override
		public void run() {
			for (Totem p : new HashSet<>(totems)) {
				boolean remove = p.pulse();
				if (remove) totems.remove(p);
			}
			if (totems.isEmpty()) stop();
		}

		private void stop() {
			if (taskId > 0) {
				MagicSpells.cancelTask(taskId);
				taskId = -1;
			}
		}

	}

}
