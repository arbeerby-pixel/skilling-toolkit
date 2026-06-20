package com.arber.skillingtoolkit;

import net.runelite.api.coords.WorldPoint;

class PatchTimer
{
	private static final long MILLIS_PER_MINUTE = 60_000L;

	private final WorldPoint worldPoint;
	private final long doneAtMillis;
	private final String cropName;

	PatchTimer(WorldPoint worldPoint, long doneAtMillis, String cropName)
	{
		this.worldPoint = worldPoint;
		this.doneAtMillis = doneAtMillis;
		this.cropName = cropName;
	}

	WorldPoint getWorldPoint()
	{
		return worldPoint;
	}

	long getDoneAtMillis()
	{
		return doneAtMillis;
	}

	String getCropName()
	{
		return cropName;
	}

	boolean isExpired(long nowMillis)
	{
		return doneAtMillis <= nowMillis;
	}

	String formatRemaining(long nowMillis)
	{
		long remainingMillis = Math.max(0L, doneAtMillis - nowMillis);
		long remainingMinutes = (remainingMillis + MILLIS_PER_MINUTE - 1L) / MILLIS_PER_MINUTE;
		if (remainingMinutes <= 0L)
		{
			return "";
		}

		long hours = remainingMinutes / 60L;
		long minutes = remainingMinutes % 60L;
		if (hours > 0L)
		{
			return hours + "h " + minutes + "m";
		}

		return minutes + "m";
	}
}
