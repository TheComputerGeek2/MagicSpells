package com.nisovin.magicspells.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CancellationException;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.nisovin.magicspells.MagicSpells;

/**
 * A wrapper class, which holds an {@link Entity} which may be immediately available, or delayed.
 */
public class DelayableEntity<E extends Entity> {

	private final CompletableFuture<E> future = new CompletableFuture<>();

	private Location spawnLocation;

	/**
	 * Gets the {@link Entity}, if available.
	 * @return {@link Entity} or {@code null}
	 */
	@Nullable
	@Deprecated
	@ApiStatus.Internal
	public E now() {
		try {
			return future.getNow(null);
		} catch (CancellationException | CompletionException e) {
			return null;
		}
	}

	/**
	 * Operation to run if the {@link Entity} is available.
	 * @param consumer Consumer to run
	 * @see DelayableEntity#accept(Consumer)
	 * @see DelayableEntity#teleport(Location)
	 */
	@Deprecated
	@ApiStatus.Internal
	public void ifPresent(@NotNull Consumer<E> consumer) {
		E entity = now();
		if (entity == null) return;
		consumer.accept(entity);
	}

	/**
	 * Operation to run when the {@link Entity} becomes available.
	 * @param consumer Consumer to run
	 * @see DelayableEntity#ifPresent(Consumer)
	 * @see DelayableEntity#teleport(Location)
	 */
	public void accept(@NotNull Consumer<E> consumer) {
		future.thenAccept(consumer);
	}

	/**
	 * Teleport the {@link Entity}, if available, otherwise store its start location.
	 * @param location {@link Location} to teleport to.
	 * @see DelayableEntity#ifPresent(Consumer)
	 * @see DelayableEntity#accept(Consumer)
	 */
	public void teleport(@NotNull Location location) {
		E entity = now();
		if (entity == null) spawnLocation = location;
		else entity.teleportAsync(location);
	}

	/**
	 * Remove the {@link Entity} or cancel its delayed spawning.
	 */
	public void remove() {
		ifPresent(Entity::remove);
		future.cancel(true);
	}

	/**
	 * Defer an {@link Entity}'s spawn with the provided delay. If {@link DelayableEntity#teleport(Location)}
	 * is called before the entity is present, its teleport location is overridden. If the {@code delay} is {@code <= 0},
	 * the underlying {@link CompletableFuture} is completed instantly.
	 * @param function Function, which spawns the {@link Entity} after the {@code delay}.
	 * @param location Initial {@link Location} to spawn the entity at.
	 * @param delay Delay in server ticks.
	 */
	public DelayableEntity(@NotNull Function<Location, E> function, @NotNull Location location, long delay) {
		spawnLocation = location;

		Runnable spawnEntity = () -> {
			try {
				future.complete(function.apply(spawnLocation));
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		};

		int taskId = -1;
		if (delay <= 0) spawnEntity.run();
		else taskId = MagicSpells.scheduleDelayedTask(spawnEntity, delay);

		int id = taskId;
		future.whenComplete((result, throwable) -> {
			if (future.isCancelled() && id != -1) MagicSpells.cancelTask(id);
			if (throwable == null) return;
			MagicSpells.handleException(new Exception("Delayed entity failed to spawn", throwable));
		});
	}

}
