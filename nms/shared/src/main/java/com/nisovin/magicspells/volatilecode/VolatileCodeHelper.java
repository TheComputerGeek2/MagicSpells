package com.nisovin.magicspells.volatilecode;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public interface VolatileCodeHelper {

	void error(String message);

	ScheduledTask scheduleDelayedTask(Runnable task, long delay);

}
