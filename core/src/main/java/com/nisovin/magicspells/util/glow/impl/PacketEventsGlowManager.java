package com.nisovin.magicspells.util.glow.impl;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.configuration.ConfigurationSection;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.glow.LibsDisguiseHelper;
import com.nisovin.magicspells.util.glow.PacketBasedGlowManager;

public class PacketEventsGlowManager extends PacketBasedGlowManager<PacketWrapper<?>, WrapperPlayServerEntityMetadata, WrapperPlayServerTeams> {

	private final GlowPacketListener listener;

	public PacketEventsGlowManager() {
		listener = new GlowPacketListener();

		MagicSpells.registerEvents(this);
	}

	@Override
	public void load() {
		super.load();

		PacketEvents.getAPI().getEventManager().registerListener(listener);
	}

	@Override
	public synchronized void unload() {
		PacketEvents.getAPI().getEventManager().unregisterListener(listener);

		super.unload();
	}

	@Override
	protected Collection<WrapperPlayServerTeams> createAddTeamPackets() {
		ConfigurationSection config = MagicSpells.getMagicConfig().getMainConfig();

		boolean seeFriendlyInvisibles = config.getBoolean("general.glow-spell-scoreboard-teams.see-friendly-invisibles", false);
		OptionData optionData = seeFriendlyInvisibles ? OptionData.ALL : OptionData.FRIENDLY_FIRE;

		NameTagVisibility visibility = getStringOption("name-tag-visibility", NameTagVisibility.ALWAYS, NameTagVisibility::fromID, config, MagicSpells::error);
		CollisionRule collisionRule = getStringOption("collision-rule", CollisionRule.ALWAYS, CollisionRule::fromID, config, MagicSpells::error);

		return NamedTextColor.NAMES.values()
			.stream()
			.map(color -> {
				String name = "magicspells:" + color;

				return new WrapperPlayServerTeams(
					name,
					TeamMode.CREATE,
					new WrapperPlayServerTeams.ScoreBoardTeamInfo(
						Component.text(name),
						null,
						null,
						visibility,
						collisionRule,
						color,
						optionData
					)
				);
			})
			.toList();
	}

	@Override
	protected Collection<WrapperPlayServerTeams> createRemoveTeamPackets() {
		return NamedTextColor.NAMES.values()
			.stream()
			.map(color -> {
				String name = "magicspells:" + color;
				return new WrapperPlayServerTeams(name, TeamMode.REMOVE, (WrapperPlayServerTeams.ScoreBoardTeamInfo) null);
			})
			.toList();
	}

	@Override
	protected WrapperPlayServerEntityMetadata createEntityDataPacket(@NotNull Entity entity, boolean forceGlow) {
		byte metadata = MagicSpells.getVolatileCodeHandler().getEntityMetadata(entity);
		if (forceGlow) metadata |= 0x40;

		return new WrapperPlayServerEntityMetadata(
			entity.getEntityId(),
			List.of(new EntityData(0, EntityDataTypes.BYTE, metadata))
		);
	}

	@Override
	protected Collection<WrapperPlayServerTeams> createJoinTeamPacket(@NotNull GlowData data) {
		return data.lastScoreboardEntry().map(entry -> new WrapperPlayServerTeams(
			"magicspells:" + data.color(),
			TeamMode.ADD_ENTITIES,
			(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
			entry
		));
	}

	@Override
	protected Collection<WrapperPlayServerTeams> createResetTeamPacket(@NotNull Scoreboard scoreboard, @NotNull GlowData data) {
		return data.lastScoreboardEntry().map(entry -> {
			Team team = scoreboard.getEntryTeam(entry);
			if (team != null) {
				return new WrapperPlayServerTeams(
					team.getName(),
					TeamMode.ADD_ENTITIES,
					(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
					entry
				);
			}

			return new WrapperPlayServerTeams(
				"magicspells:" + data.color(),
				TeamMode.REMOVE_ENTITIES,
				(WrapperPlayServerTeams.ScoreBoardTeamInfo) null,
				entry
			);
		});
	}

	@Override
	protected void sendPacket(@NotNull Player player, @NotNull PacketWrapper<?> packetWrapper) {
		PlayerManager manager = PacketEvents.getAPI().getPlayerManager();
		if (packetWrapper instanceof WrapperPlayServerEntityMetadata) manager.sendPacket(player, packetWrapper);
		else manager.sendPacketSilently(player, packetWrapper);
	}

	@Override
	protected void registerEvents(Listener listener) {
		MagicSpells.registerEvents(listener);
	}

	@Override
	protected void cancelTask(int taskId) {
		MagicSpells.cancelTask(taskId);
	}

	@Override
	public int scheduleDelayedTask(Runnable runnable, long delay) {
		return MagicSpells.scheduleDelayedTask(runnable, delay);
	}

	private final class GlowPacketListener extends PacketListenerAbstract {

		public GlowPacketListener() {
			super(PacketListenerPriority.LOWEST);
		}

		@Override
		public void onPacketSend(PacketSendEvent event) {
			synchronized (PacketEventsGlowManager.this) {
				if (glows.isEmpty() && perPlayerGlows.isEmpty())
					return;
			}

			try {
				if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) handleEntityData(event);
				else if (event.getPacketType() == PacketType.Play.Server.TEAMS) handleTeams(event);
			} catch (Throwable throwable) {
				MagicSpells.error("Encountered an error while intercepting a packet - cancelling packet send.");
				throwable.printStackTrace();

				event.setCancelled(true);
			}
		}

		private void handleEntityData(PacketSendEvent event) {
			WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);

			List<EntityData> metadata = packet.getEntityMetadata();
			if (metadata.isEmpty()) return;

			EntityData entityData = metadata.getFirst();
			if (entityData.getIndex() != 0) return;

			byte flags = (byte) entityData.getValue();
			if ((flags & 0x40) > 0) return;

			Player player = event.getPlayer();

			UUID uuid;
			if (!libsDisguisesLoaded || packet.getEntityId() != LibsDisguiseHelper.getSelfDisguiseId()) {
				Entity entity = MagicSpells.getVolatileCodeHandler().getEntityFromId(player.getWorld(), packet.getEntityId());
				if (entity == null) return;

				uuid = entity.getUniqueId();
			} else uuid = player.getUniqueId();

			GlowData data = getGlowData(player.getUniqueId(), uuid);
			if (data == null) return;

			flags |= 0x40;
			entityData.setValue(flags);
		}

		private void handleTeams(PacketSendEvent event) {
			WrapperPlayServerTeams packet = new WrapperPlayServerTeams(event);

			TeamMode mode = packet.getTeamMode();
			if (mode != TeamMode.REMOVE_ENTITIES && mode != TeamMode.ADD_ENTITIES) return;

			Collection<String> entries = packet.getPlayers();
			if (entries.isEmpty()) return;

			Collection<String> filtered = filterTeamEntries(event.getPlayer(), entries);
			if (filtered == null) return;

			if (filtered.isEmpty()) {
				event.setCancelled(true);
				return;
			}

			packet.setPlayers(filtered);
		}

	}

}
