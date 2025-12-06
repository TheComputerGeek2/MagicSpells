package com.nisovin.magicspells.volatilecode.v1_21_10;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.scores.Team.Visibility;
import net.minecraft.world.scores.Team.CollisionRule;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.glow.LibsDisguiseHelper;
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper;
import com.nisovin.magicspells.util.glow.PacketBasedGlowManager;

public class VolatileGlowManager_v1_21_10 extends PacketBasedGlowManager<Packet<?>, ClientboundSetEntityDataPacket, ClientboundSetPlayerTeamPacket> {

	private static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);

	private final Set<Packet<?>> handled = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	private final MethodHandle teamPacketHandle;
	private final VolatileCodeHelper helper;

	public VolatileGlowManager_v1_21_10(VolatileCodeHelper helper) {
		this.helper = helper;

		try {
			teamPacketHandle = MethodHandles
				.privateLookupIn(ClientboundSetPlayerTeamPacket.class, MethodHandles.lookup())
				.findConstructor(
					ClientboundSetPlayerTeamPacket.class,
					MethodType.methodType(void.class, String.class, int.class, Optional.class, Collection.class)
				);
		} catch (Exception e) {
			throw new RuntimeException("Encountered an error while initializing VolatileGlowManagerLatest", e);
		}

		helper.registerEvents(this);
	}

	@Override
	public void load() {
		super.load();

		Bukkit.getOnlinePlayers().forEach(this::addGlowChannelHandler);
	}

	@Override
	public synchronized void unload() {
		Bukkit.getOnlinePlayers().forEach(this::removeGlowChannelHandler);

		super.unload();
		handled.clear();
	}

	@Override
	protected Collection<ClientboundSetPlayerTeamPacket> createAddTeamPackets() {
		List<ClientboundSetPlayerTeamPacket> teamPackets = new ArrayList<>();

		ConfigurationSection config = helper.getMainConfig();
		boolean seeFriendlyInvisibles = config.getBoolean("general.glow-spell-scoreboard-teams.see-friendly-invisibles", false);
		CollisionRule collision = getStringOption("collision-rule", CollisionRule.ALWAYS, StringRepresentable.createNameLookup(CollisionRule.values()), config, helper::error);
		Visibility visibility = getStringOption("name-tag-visibility", Visibility.ALWAYS, StringRepresentable.createNameLookup(Visibility.values()), config, helper::error);

		Scoreboard scoreboard = new Scoreboard();
		for (ChatFormatting formatting : ChatFormatting.values()) {
			if (!formatting.isColor()) continue;

			PlayerTeam team = new PlayerTeam(scoreboard, "magicspells:" + formatting.getName());
			team.setSeeFriendlyInvisibles(seeFriendlyInvisibles);
			team.setNameTagVisibility(visibility);
			team.setCollisionRule(collision);
			team.setColor(formatting);

			teamPackets.add(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
		}

		return teamPackets;
	}

	@Override
	protected Collection<ClientboundSetPlayerTeamPacket> createRemoveTeamPackets() {
		List<ClientboundSetPlayerTeamPacket> packets = new ArrayList<>();

		Scoreboard scoreboard = new Scoreboard();
		for (ChatFormatting formatting : ChatFormatting.values()) {
			if (!formatting.isColor()) continue;

			PlayerTeam team = new PlayerTeam(scoreboard, "magicspells:" + formatting.getName());
			packets.add(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
		}

		return packets;
	}

	@Override
	protected ClientboundSetEntityDataPacket createEntityDataPacket(@NotNull Entity entity, boolean forceGlow) {
		byte metadata = ((CraftEntity) entity).getHandle().getEntityData().get(DATA_SHARED_FLAGS_ID);
		if (forceGlow) metadata |= 0x40;

		return new ClientboundSetEntityDataPacket(
			entity.getEntityId(),
			list(new SynchedEntityData.DataValue<>(
				0,
				EntityDataSerializers.BYTE,
				metadata
			))
		);
	}

	@Override
	protected Collection<ClientboundSetPlayerTeamPacket> createJoinTeamPacket(@NotNull GlowData data) {
		return data.lastScoreboardEntry().map(entry -> {
			try {
				return (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
					"magicspells:" + data.color(),
					3,
					Optional.empty(),
					list(entry)
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	protected Collection<ClientboundSetPlayerTeamPacket> createResetTeamPacket(org.bukkit.scoreboard.@NotNull Scoreboard scoreboard, @NotNull GlowData data) {
		return data.lastScoreboardEntry().map(entry -> {
			try {
				Team team = scoreboard.getEntryTeam(entry);
				if (team != null) {
					return (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
						team.getName(),
						3,
						Optional.empty(),
						list(entry)
					);
				}

				return (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
					"magicspells:" + data.color(),
					4,
					Optional.empty(),
					list(entry)
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	protected void sendPacket(@NotNull Player player, @NotNull Packet<?> packet) {
		if (!(packet instanceof ClientboundSetPlayerTeamPacket teamPacket) || teamPacket.getTeamAction() == null)
			handled.add(packet);

		ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
		connection.send(packet);
	}

	@Override
	protected void registerEvents(Listener listener) {
		helper.registerEvents(listener);
	}

	@Override
	protected void cancelTask(int taskId) {
		helper.cancelTask(taskId);
	}

	@Override
	public int scheduleDelayedTask(Runnable runnable, long delay) {
		return helper.scheduleDelayedTask(runnable, delay);
	}

	private void addGlowChannelHandler(Player player) {
		ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
		pipeline.addBefore("unbundler", "magicspells:glow_channel_handler", new GlowChannelHandler(player));
	}

	private void removeGlowChannelHandler(Player player) {
		ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();

		if (pipeline.get("magicspells:glow_channel_handler") != null)
			pipeline.remove("magicspells:glow_channel_handler");
	}

	private <T> List<T> list(T element) {
		List<T> list = new ArrayList<>(1);
		list.add(element);

		return list;
	}

	@Override
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		super.onPlayerJoin(event);
		addGlowChannelHandler(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		removeGlowChannelHandler(event.getPlayer());
	}

	private class GlowChannelHandler extends ChannelOutboundHandlerAdapter {

		private final Player player;

		private GlowChannelHandler(Player player) {
			this.player = player;
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (msg instanceof Packet<?> p && handled.contains(p)) {
				super.write(ctx, msg, promise);
				return;
			}

			synchronized (VolatileGlowManager_v1_21_10.this) {
				if (glows.isEmpty() && perPlayerGlows.isEmpty()) {
					super.write(ctx, msg, promise);
					return;
				}
			}

			switch (msg) {
				case ClientboundSetEntityDataPacket packet -> handleEntityData(packet);
				case ClientboundSetPlayerTeamPacket packet -> {
					msg = handleTeamPacket(packet);
					if (msg == null) return;
				}
				default -> {}
			}

			super.write(ctx, msg, promise);
		}

		private void handleEntityData(ClientboundSetEntityDataPacket packet) {
			List<SynchedEntityData.DataValue<?>> packedItems = packet.packedItems();
			if (packedItems.isEmpty()) return;

			SynchedEntityData.DataValue<?> item = packedItems.getFirst();
			if (item.id() != 0) return;

			byte flags = (byte) item.value();
			if ((flags & 0x40) > 0) return;

			UUID uuid;
			if (!libsDisguisesLoaded || packet.id() != LibsDisguiseHelper.getSelfDisguiseId()) {
				var entity = ((CraftPlayer) player).getHandle().level().moonrise$getEntityLookup().get(packet.id());
				if (entity == null) return;

				uuid = entity.getUUID();
			} else uuid = player.getUniqueId();

			GlowData data = getGlowData(player.getUniqueId(), uuid);
			if (data == null) return;

			flags |= 0x40;
			packedItems.set(0, new SynchedEntityData.DataValue<>(0, EntityDataSerializers.BYTE, flags));
		}

		private ClientboundSetPlayerTeamPacket handleTeamPacket(ClientboundSetPlayerTeamPacket packet) {
			ClientboundSetPlayerTeamPacket.Action playerAction = packet.getPlayerAction();
			if (playerAction == null || packet.getTeamAction() != null) return packet;

			Collection<String> entries = packet.getPlayers();
			if (entries.isEmpty()) return packet;

			Collection<String> filtered = filterTeamEntries(player, entries);
			if (filtered == null) return packet;
			if (filtered.isEmpty()) return null;

			try {
				return (ClientboundSetPlayerTeamPacket) teamPacketHandle.invoke(
					packet.getName(),
					playerAction == ClientboundSetPlayerTeamPacket.Action.ADD ? 3 : 4,
					packet.getParameters(),
					filtered
				);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

	}

}
