package com.nisovin.magicjutsus.util;

import com.nisovin.magicjutsus.MagicJutsus;

/**
 * This class represents a jutsu animation. It facilitates creating a jutsu effect that happens over a period of time,
 * without having to worry about stopping and starting scheduled tasks.
 * 
 * @author nisovin
 *
 */
public abstract class JutsuAnimation implements Runnable {

	private int taskId;
	private int delay;
	private int interval;
	private int tick;
	
	/**
	 * Create a new jutsu animation with the specified interval and no delay. It will not auto start.
	 * @param interval the animation interval, in server ticks (animation speed)
	 */
	public JutsuAnimation(int interval) {
		this(0, interval, false);
	}
	
	/**
	 * Create a new jutsu animation with the specified interval and no delay.
	 * @param interval the animation interval, in server ticks (animation speed)
	 * @param autoStart whether the animation should start immediately upon being created
	 */
	public JutsuAnimation(int interval, boolean autoStart) {
		this(0, interval, autoStart);
	}
	
	/**
	 * Create a new jutsu animation with the specified interval and delay. It will not auto start.
	 * @param delay the delay before the animation begins, in server ticks
	 * @param interval the animation interval, in server ticks (animation speed)
	 */
	public JutsuAnimation(int delay, int interval) {
		this(delay, interval, false);
	}
	
	/**
	 * Create a new jutsu animation with the specified interval and delay.
	 * @param delay the delay before the animation begins, in server ticks
	 * @param interval the animation interval, in server ticks (animation speed)
	 * @param autoStart whether the animation should start immediately upon being created
	 */
	public JutsuAnimation(int delay, int interval, boolean autoStart) {
		this.delay = delay;
		this.interval = interval;
		this.tick = -1;
		if (autoStart) play();
	}
	
	/**
	 * Start the jutsu animation.
	 */
	public void play() {
		taskId = MagicJutsus.scheduleRepeatingTask(this, delay, interval);
	}
	
	/**
	 * Stop the jutsu animation.
	 */
	protected void stop() {
		MagicJutsus.cancelTask(taskId);
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
	
}
