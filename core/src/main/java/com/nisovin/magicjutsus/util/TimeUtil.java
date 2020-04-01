package com.nisovin.magicjutsus.util;

public class TimeUtil {
	
	public static final int TICKS_PER_SECOND = 20;
	public static final long MILLISECONDS_PER_SECOND = 1000;
	public static final long SECONDS_PER_MINUTE = 60;
	public static final long MINUTES_PER_HOUR = 60;
	public static final long MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * SECONDS_PER_MINUTE;
	public static final long MILLISECONDS_PER_HOUR = MILLISECONDS_PER_MINUTE * MINUTES_PER_HOUR;
	public static final long HOURS_PER_DAY = 24;
	public static final long DAYS_PER_WEEK = 7;
	public static final long HOURS_PER_WEEK = HOURS_PER_DAY * DAYS_PER_WEEK;
	public static final int TICKS_PER_MINUTE = (int)(TICKS_PER_SECOND * SECONDS_PER_MINUTE);
	
}
