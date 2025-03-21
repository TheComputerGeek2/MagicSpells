package com.nisovin.magicspells.util.glow;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import com.google.common.collect.Iterables;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.configuration.ConfigurationSection;

import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;

public abstract class PacketBasedGlowManager<TPacket, TEntityDataPacket extends TPacket, TTeamPacket extends TPacket> implements GlowManager, Listener {

	protected final Object2ObjectMap<Pair<UUID, UUID>, GlowDataMap> perPlayerGlows = new Object2ObjectOpenHashMap<>();
	protected final Object2ObjectMap<UUID, GlowDataMap> glows = new Object2ObjectOpenHashMap<>();
	protected final ConcurrentHashMap<String, UUID> scoreboardNames = new ConcurrentHashMap<>();

	protected Collection<TTeamPacket> addTeamPackets;
	protected int sequence = Integer.MIN_VALUE;
	protected boolean libsDisguisesLoaded;

	@Override
	public void load() {
		addTeamPackets = createAddTeamPackets();
		Bukkit.getOnlinePlayers().forEach(player -> sendPackets(player, addTeamPackets));

		libsDisguisesLoaded = Bukkit.getPluginManager().isPluginEnabled("LibsDisguises");
	}

	@Override
	public synchronized void unload() {
		glows.forEach((key, value) -> {
			Entity entity = Bukkit.getEntity(key);
			if (entity == null) return;

			resetGlow(null, entity, value.get(), false);
		});

		perPlayerGlows.forEach((key, value) -> {
			if (glows.containsKey(key.right())) return;

			Player player = Bukkit.getPlayer(key.left());
			if (player == null) return;

			Entity entity = Bukkit.getEntity(key.right());
			if (entity == null) return;

			resetGlow(player, entity, value.get(), false);
		});

		glows.clear();
		perPlayerGlows.clear();
		scoreboardNames.clear();

		Collection<TTeamPacket> removeTeamPackets = createRemoveTeamPackets();
		Bukkit.getOnlinePlayers().forEach(player -> sendPackets(player, removeTeamPackets));
	}

	@Override
	public void applyGlow(@NotNull Entity entity, @NotNull NamespacedKey key, @Nullable NamedTextColor color, int priority, @Range(from = 0, to = Integer.MAX_VALUE) int duration) {
		GlowData data = new GlowData(priority, sequence++, color);
		UUID uuid = entity.getUniqueId();

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(null, uuid);

			GlowDataMap map = glows.computeIfAbsent(uuid, i -> new GlowDataMap());

			GlowData old = map.put(key, data);
			if (old != null && old.taskId != -1) cancelTask(old.taskId);

			if (duration > 0) {
				data.taskId = scheduleDelayedTask(() -> {
					synchronized (this) {
						Entity e = Bukkit.getEntity(uuid);
						if (e == null) {
							map.remove(key);
							if (map.isEmpty()) glows.remove(uuid);
							return;
						}

						removeGlow(e, key);
					}
				}, duration);
			}

			curr = getGlowData(null, uuid);
		}

		updateGlow(null, entity, prev, curr);
	}

	@Override
	public void applyGlow(@NotNull Player player, @NotNull Entity entity, @NotNull NamespacedKey key, @NotNull NamedTextColor color, int priority, @Range(from = 0, to = Integer.MAX_VALUE) int duration) {
		Pair<UUID, UUID> pair = Pair.of(player.getUniqueId(), entity.getUniqueId());
		GlowData data = new GlowData(priority, sequence++, color);

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(pair);

			GlowDataMap map = perPlayerGlows.computeIfAbsent(pair, i -> new GlowDataMap());

			GlowData old = map.put(key, data);
			if (old != null && old.taskId != -1) cancelTask(old.taskId);

			if (duration > 0) {
				data.taskId = scheduleDelayedTask(() -> {
					synchronized (this) {
						Player p = Bukkit.getPlayer(pair.left());
						if (p == null) {
							map.remove(key);
							if (map.isEmpty()) perPlayerGlows.remove(pair);
							return;
						}

						Entity e = Bukkit.getEntity(pair.right());
						if (e == null) {
							map.remove(key);
							if (map.isEmpty()) perPlayerGlows.remove(pair);
							return;
						}

						removeGlow(p, e, key);
					}
				}, duration);
			}

			curr = getGlowData(pair);
		}

		updateGlow(player, entity, prev, curr);
	}

	@Override
	public void removeGlow(@NotNull Entity entity, @NotNull NamespacedKey key) {
		UUID uuid = entity.getUniqueId();

		GlowDataMap map = glows.get(uuid);
		if (map == null) return;

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(null, uuid);

			map.remove(key);
			if (map.isEmpty()) glows.remove(uuid);

			curr = getGlowData(null, uuid);
		}

		if (curr == null) {
			resetGlow(null, entity, prev, true);
			return;
		}

		updateGlow(null, entity, prev, curr);
	}

	@Override
	public void removeGlow(@NotNull Player player, @NotNull Entity entity, @NotNull NamespacedKey key) {
		Pair<UUID, UUID> pair = Pair.of(player.getUniqueId(), entity.getUniqueId());

		GlowDataMap map = perPlayerGlows.get(pair);
		if (map == null) return;

		GlowData prev, curr;
		synchronized (this) {
			prev = getGlowData(pair);

			map.remove(key);
			if (map.isEmpty()) perPlayerGlows.remove(pair);

			curr = getGlowData(pair);
		}

		if (curr == null) {
			resetGlow(player, entity, prev, false);
			return;
		}

		updateGlow(player, entity, prev, curr);
	}

	protected void updateGlow(@Nullable Player player, @NotNull Entity entity, @Nullable GlowData prev, @NotNull GlowData curr) {
		if (prev != null && prev.color() == curr.color()) {
			curr.lastEntryName(prev.lastEntryName);
			return;
		}

		Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		curr.lastEntryName(getScoreboardEntryName(player, entity));

		TEntityDataPacket entityDataPacket = createEntityDataPacket(entity, true);
		TTeamPacket teamPacket = null;
		boolean reset = false;
		if (curr.color != null) {
			teamPacket = createJoinTeamPacket(curr);
			scoreboardNames.put(curr.lastEntryName(), entity.getUniqueId());
		} else if (prev != null) {
			Scoreboard scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = createResetTeamPacket(scoreboard, prev);

			scoreboardNames.put(curr.lastEntryName(), entity.getUniqueId());
			reset = true;
		}

		if (player != null) {
			if (teamPacket != null) sendPacket(player, teamPacket);
			sendPacket(player, entityDataPacket);

			return;
		}

		Iterable<Player> trackedPlayers = entity.getTrackedBy();
		if (entity instanceof Player p)
			trackedPlayers = Iterables.concat(trackedPlayers, Collections.singleton(p));

		Pair<UUID, UUID> pair = ObjectObjectMutablePair.of(null, entity.getUniqueId());
		for (Player viewer : trackedPlayers) {
			GlowDataMap targetedMap = perPlayerGlows.get(pair.left(viewer.getUniqueId()));
			if (targetedMap != null && targetedMap.get().compareTo(curr) < 0) continue;

			if (reset) {
				Scoreboard scoreboard = viewer.getScoreboard();

				if (scoreboard != mainScoreboard) {
					sendPacket(viewer, createResetTeamPacket(scoreboard, prev));
					sendPacket(viewer, entityDataPacket);
					continue;
				}
			}

			if (teamPacket != null) sendPacket(viewer, teamPacket);
			sendPacket(viewer, entityDataPacket);
		}
	}

	protected void resetGlow(@Nullable Player player, @NotNull Entity entity, @NotNull GlowData data, boolean checkPerPlayer) {
		Scoreboard mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

		TEntityDataPacket entityDataPacket = createEntityDataPacket(entity, false);
		TTeamPacket teamPacket = null;
		if (data.color() != null) {
			Scoreboard scoreboard = player == null ? mainScoreboard : player.getScoreboard();
			teamPacket = createResetTeamPacket(scoreboard, data);
		}

		if (player != null) {
			if (teamPacket != null) sendPacket(player, teamPacket);
			sendPacket(player, entityDataPacket);

			return;
		}

		Iterable<Player> trackedPlayers = entity.getTrackedBy();
		if (entity instanceof Player p)
			trackedPlayers = Iterables.concat(trackedPlayers, Collections.singleton(p));

		Pair<UUID, UUID> pair = checkPerPlayer ? ObjectObjectMutablePair.of(null, entity.getUniqueId()) : null;
		for (Player viewer : trackedPlayers) {
			if (checkPerPlayer) {
				GlowDataMap targetedMap = perPlayerGlows.get(pair.left(viewer.getUniqueId()));

				if (targetedMap != null) {
					GlowData curr = targetedMap.get();

					if (curr != null) {
						updateGlow(viewer, entity, data, curr);
						continue;
					}
				}
			}

			Scoreboard scoreboard = viewer.getScoreboard();
			if (scoreboard != mainScoreboard) {
				sendPacket(viewer, createResetTeamPacket(scoreboard, data));
				sendPacket(viewer, entityDataPacket);

				continue;
			}

			if (teamPacket != null) sendPacket(viewer, teamPacket);
			sendPacket(viewer, entityDataPacket);
		}
	}

	protected GlowData getGlowData(@Nullable UUID player, @NotNull UUID entity) {
		synchronized (this) {
			GlowDataMap map = glows.get(entity);
			GlowData data = map == null ? null : map.get();

			GlowDataMap targetedMap = perPlayerGlows.get(Pair.of(player, entity));
			if (targetedMap == null) return data;

			GlowData targetedData = targetedMap.get();
			if (data == null) return targetedData;

			return data.compareTo(targetedData) < 0 ? data : targetedData;
		}
	}

	protected GlowData getGlowData(@NotNull Pair<@NotNull UUID, @NotNull UUID> pair) {
		synchronized (this) {
			GlowDataMap map = glows.get(pair.right());
			GlowData data = map == null ? null : map.get();

			GlowDataMap targetedMap = perPlayerGlows.get(pair);
			if (targetedMap == null) return data;

			GlowData targetedData = targetedMap.get();
			if (data == null) return targetedData;

			return data.compareTo(targetedData) < 0 ? data : targetedData;
		}
	}

	protected void sendPackets(@NotNull Player player, @NotNull Collection<? extends TPacket> packets) {
		for (TPacket packet : packets)
			sendPacket(player, packet);
	}

	protected String getScoreboardEntryName(@Nullable Player player, @NotNull Entity entity) {
		if (!libsDisguisesLoaded) return entity.getScoreboardEntryName();
		return LibsDisguiseHelper.getDisguisedScoreboardEntryName(player, entity);
	}

	protected <T> T getStringOption(@NotNull String name, T def, @NotNull Function<String, T> converter, @NotNull ConfigurationSection config, @NotNull Consumer<String> onError) {
		String string = config.getString("general.glow-spell-scoreboard-teams." + name);
		if (string == null) return def;

		T value = converter.apply(string);
		if (value != null) return value;

		onError.accept("Invalid value '" + string + "' for '" + name + "' in 'glow-spell-scoreboard-teams' in 'general.yml'.");
		return def;
	}

	protected Collection<String> filterTeamEntries(@NotNull Player player, @NotNull Collection<String> entries) {
		UUID playerUUID = player.getUniqueId();

		List<String> filtered = new ArrayList<>(entries.size());
		boolean modified = false;

		for (String entry : entries) {
			UUID entityUUID = scoreboardNames.get(entry);
			if (entityUUID == null) {
				filtered.add(entry);
				continue;
			}

			GlowData data = getGlowData(playerUUID, entityUUID);
			if (data == null || data.color() == null) {
				filtered.add(entry);
				continue;
			}

			modified = true;
		}

		return modified ? filtered : null;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		sendPackets(event.getPlayer(), addTeamPackets);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTrackEntity(PlayerTrackEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getEntity();

		scheduleDelayedTask(() -> {
			GlowData data = getGlowData(player.getUniqueId(), entity.getUniqueId());
			if (data == null) return;

			updateGlow(player, entity, null, data);
		}, 0);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerUntrackEntity(PlayerUntrackEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getEntity();

		GlowData data = getGlowData(player.getUniqueId(), entity.getUniqueId());
		if (data == null) return;

		resetGlow(player, entity, data, false);
	}

	protected abstract Collection<TTeamPacket> createAddTeamPackets();

	protected abstract Collection<TTeamPacket> createRemoveTeamPackets();

	protected abstract TEntityDataPacket createEntityDataPacket(@NotNull Entity entity, boolean forceGlow);

	protected abstract TTeamPacket createJoinTeamPacket(@NotNull GlowData data);

	protected abstract TTeamPacket createResetTeamPacket(@NotNull Scoreboard scoreboard, @NotNull GlowData data);

	protected abstract void sendPacket(@NotNull Player player, @NotNull TPacket packet);

	protected abstract void cancelTask(int taskId);

	protected abstract int scheduleDelayedTask(Runnable runnable, long delay);

	protected static final class GlowData implements Comparable<GlowData> {

		private final NamedTextColor color;
		private final int priority;
		private final int sequence;

		private String lastEntryName;
		private int taskId;

		public GlowData(int priority, int sequence, NamedTextColor color) {
			this.priority = priority;
			this.sequence = sequence;
			this.color = color;

			taskId = -1;
		}

		public NamedTextColor color() {
			return color;
		}

		public int priority() {
			return priority;
		}

		public int sequence() {
			return sequence;
		}

		public int taskId() {
			return taskId;
		}

		public void taskId(int taskId) {
			this.taskId = taskId;
		}

		public String lastEntryName() {
			return lastEntryName;
		}

		public void lastEntryName(String lastEntryName) {
			this.lastEntryName = lastEntryName;
		}

		@Override
		public int compareTo(@NotNull GlowData o) {
			int compare = o.priority - priority;
			return compare == 0 ? o.sequence - sequence : compare;
		}

	}

	protected static final class GlowDataMap {

		private final Map<NamespacedKey, GlowData> keyedGlowData = new Object2ObjectOpenHashMap<>();
		private final PriorityQueue<GlowData> glowData = new PriorityQueue<>();

		public GlowDataMap() {

		}

		public GlowData put(NamespacedKey key, GlowData data) {
			GlowData old = keyedGlowData.put(key, data);
			if (old != null) glowData.remove(old);

			glowData.add(data);

			return old;
		}

		public void remove(NamespacedKey key) {
			GlowData old = keyedGlowData.remove(key);
			if (old == null) return;

			glowData.remove(old);
		}

		public GlowData get() {
			return glowData.peek();
		}

		public boolean isEmpty() {
			return keyedGlowData.isEmpty();
		}

	}

}
