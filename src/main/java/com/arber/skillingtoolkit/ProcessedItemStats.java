package com.arber.skillingtoolkit;

final class ProcessedItemStats
{
	private final int itemId;
	private long processed;
	private long xp;
	private long inputCost;
	private long outputValue;
	private long firstSeenMillis;
	private long lastSeenMillis;

	ProcessedItemStats(int itemId)
	{
		this.itemId = itemId;
	}

	ProcessedItemStats copy()
	{
		ProcessedItemStats copy = new ProcessedItemStats(itemId);
		copy.processed = processed;
		copy.xp = xp;
		copy.inputCost = inputCost;
		copy.outputValue = outputValue;
		copy.firstSeenMillis = firstSeenMillis;
		copy.lastSeenMillis = lastSeenMillis;
		return copy;
	}

	void add(long processed, long xp, long inputCost, long outputValue, long nowMillis)
	{
		this.processed += Math.max(0L, processed);
		this.xp += Math.max(0L, xp);
		this.inputCost += Math.max(0L, inputCost);
		this.outputValue += Math.max(0L, outputValue);
		if (firstSeenMillis <= 0L)
		{
			firstSeenMillis = nowMillis;
		}
		lastSeenMillis = Math.max(lastSeenMillis, nowMillis);
	}

	String serialize()
	{
		return itemId + "," + processed + "," + xp + "," + inputCost + "," + outputValue + "," + firstSeenMillis + "," + lastSeenMillis;
	}

	static ProcessedItemStats deserialize(String value)
	{
		if (value == null || value.isBlank())
		{
			return null;
		}

		String[] parts = value.split(",", -1);
		if (parts.length < 5)
		{
			return null;
		}

		int itemId = parseInt(parts[0]);
		if (itemId <= 0)
		{
			return null;
		}

		ProcessedItemStats stats = new ProcessedItemStats(itemId);
		stats.processed = parseLong(parts[1]);
		stats.xp = parseLong(parts[2]);
		stats.inputCost = parseLong(parts[3]);
		stats.outputValue = parseLong(parts[4]);
		stats.firstSeenMillis = parts.length > 5 ? parseLong(parts[5]) : 0L;
		stats.lastSeenMillis = parts.length > 6 ? parseLong(parts[6]) : stats.firstSeenMillis;
		return stats;
	}

	int getItemId()
	{
		return itemId;
	}

	long getProcessed()
	{
		return processed;
	}

	long getXp()
	{
		return xp;
	}

	long getInputCost()
	{
		return inputCost;
	}

	long getOutputValue()
	{
		return outputValue;
	}

	long getProfit()
	{
		return outputValue - inputCost;
	}

	long getDurationMillis()
	{
		if (firstSeenMillis <= 0L || lastSeenMillis <= firstSeenMillis)
		{
			return 0L;
		}

		return lastSeenMillis - firstSeenMillis;
	}

	long getLastSeenMillis()
	{
		return lastSeenMillis;
	}

	private static int parseInt(String value)
	{
		try
		{
			return Math.max(0, Integer.parseInt(value.trim()));
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private static long parseLong(String value)
	{
		try
		{
			return Math.max(0L, Long.parseLong(value.trim()));
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}
}
