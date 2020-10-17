package com.nisovin.magicspells.spells.passive;

import java.util.EnumSet;

import org.bukkit.World;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellForgetEvent;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// No trigger variable currently used
public class BuffListener extends PassiveListener {

	private final EnumSet<GameMode> gameModes = EnumSet.of(GameMode.CREATIVE, GameMode.SPECTATOR);

	@Override
	public void initialize(String var) {
		for (Subspell s : passiveSpell.getActivatedSpells()) {
			if (!(s.getSpell() instanceof BuffSpell)) continue;
			BuffSpell buff = (BuffSpell) s.getSpell();
			buff.setAsEverlasting();
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			on(player, false);
		}

		for (World world : Bukkit.getWorlds()) {
			for (LivingEntity livingEntity : world.getLivingEntities()) {
				if (!canTrigger(livingEntity)) continue;
				on(livingEntity, true);
			}
		}
	}

	@OverridePriority
	@EventHandler
	public void onEntitySpawn(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player) return;
		if (!(entity instanceof LivingEntity)) return;
		on((LivingEntity) entity, true);
	}

	@OverridePriority
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (!(entity instanceof LivingEntity)) continue;
			on((LivingEntity) entity, true);
		}
	}

	@OverridePriority
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		for (Entity entity : event.getChunk().getEntities()) {
			if (!(entity instanceof LivingEntity)) continue;
			off((LivingEntity) entity);
		}
	}

	@OverridePriority
	@EventHandler
	public void onGameModeChange(PlayerGameModeChangeEvent event) {
		if (gameModes.contains(event.getNewGameMode())) off(event.getPlayer());
		else on(event.getPlayer(), true);
	}

	@OverridePriority
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		on(event.getPlayer(), false);
	}

	@OverridePriority
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		off(event.getPlayer());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		off(event.getEntity());
	}

	@OverridePriority
	@EventHandler
	public void onPlayerRespawn(final PlayerRespawnEvent event) {
		MagicSpells.scheduleDelayedTask(() -> on(event.getPlayer(), false), 1);
	}

	@OverridePriority
	@EventHandler
	public void onSpellLearn(final SpellLearnEvent event) {
		Spell spell = event.getSpell();
		if (!(spell instanceof PassiveSpell)) return;
		if (!spell.getInternalName().equalsIgnoreCase(passiveSpell.getInternalName())) return;
		MagicSpells.scheduleDelayedTask(() -> on(event.getLearner(), false), 1);
	}

	@OverridePriority
	@EventHandler
	public void onSpellForget(SpellForgetEvent event) {
		Spell spell = event.getSpell();
		if (!(spell instanceof PassiveSpell)) return;
		if (!spell.getInternalName().equalsIgnoreCase(passiveSpell.getInternalName())) return;
		off(event.getForgetter());
	}

	private void on(LivingEntity entity, boolean ignoreGameMode) {
		if (!canTrigger(entity, ignoreGameMode)) return;
		if (!hasSpell(entity)) return;

		for (Subspell s : passiveSpell.getActivatedSpells()) {
			if (!(s.getSpell() instanceof BuffSpell)) continue;
			BuffSpell buff = (BuffSpell) s.getSpell();
			if (buff.isActive(entity)) continue;
			buff.castAtEntity(entity, entity, 1F);
		}
	}

	private void off(LivingEntity entity) {
		if (!canTrigger(entity, true)) return;
		if (!hasSpell(entity)) return;

		for (Subspell s : passiveSpell.getActivatedSpells()) {
			if (!(s.getSpell() instanceof BuffSpell)) continue;
			BuffSpell buff = (BuffSpell) s.getSpell();
			if (!buff.isActive(entity)) continue;
			buff.turnOff(entity);
		}
	}

}
