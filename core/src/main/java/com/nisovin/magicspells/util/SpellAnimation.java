package com.nisovin.magicspells.util;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

import com.nisovin.magicspells.MagicSpells;

/**
 * This class represents a spell animation. It facilitates creating a spell effect that happens over a period of time,
 * without having to worry about stopping and starting scheduled tasks.
 *
 * @author nisovin
 *
 */
public abstract class SpellAnimation implements Runnable {

	private static final Set<SpellAnimation> animations = new HashSet<>();

	private final boolean async;

	private ScheduledTask task;
	private int delay;
	private int interval;
	private int tick;

	/**
	 * Create a new spell animation with the specified interval and no delay. It will not auto start.
	 * @param interval the animation interval, in server ticks (animation speed)
	 */
	public SpellAnimation(int interval) {
		this(0, interval, false, false);
	}

	/**
	 * Create a new spell animation with the specified interval and no delay.
	 * @param interval the animation interval, in server ticks (animation speed)
	 * @param autoStart whether the animation should start immediately upon being created
	 */
	public SpellAnimation(int interval, boolean autoStart) {
		this(0, interval, autoStart, false);
	}

	/**
	 * Create a new spell animation with the specified interval and delay. It will not auto start.
	 * @param delay the delay before the animation begins, in server ticks
	 * @param interval the animation interval, in server ticks (animation speed)
	 */
	public SpellAnimation(int delay, int interval) {
		this(delay, interval, false, false);
	}

	/**
	 * Create a new spell animation with the specified interval and delay.
	 * @param delay the delay before the animation begins, in server ticks
	 * @param interval the animation interval, in server ticks (animation speed)
	 * @param autoStart whether the animation should start immediately upon being created
	 * @param async whether the animation should be off the main thread
	 */
	public SpellAnimation(int delay, int interval, boolean autoStart, boolean async) {
		this.async = async;
		this.delay = delay;
		this.interval = interval;
		this.tick = -1;
		animations.add(this);
		if (autoStart) play();
	}

	/**
	 * Start the spell animation.
	 */
	public void play() {
		if (async) task = Bukkit.getAsyncScheduler().runAtFixedRate(MagicSpells.getInstance(), t -> run(), delay, interval * 50L, TimeUnit.MILLISECONDS);
		else task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(MagicSpells.getInstance(), t -> run(), delay, interval);
	}

	/**
	 * Stop the spell animation.
	 */
	public void stop() {
		stop(true);
	}

	public void stop(boolean removeEntry) {
		task.cancel();
		if (removeEntry) animations.remove(this);
	}

	/**
	 * This method is called every time the animation ticks (with the interval defined in the constructor).
	 * @param tick the current tick number, starting with 0
	 */
	protected abstract void onTick(int tick);

	@Override
	public final void run() {
		onTick(++tick);
	}

	/**
	 * Returns all existing animations.
	 * @return Current animations.
	 */
	public static Set<SpellAnimation> getAnimations() {
		return animations;
	}

}
