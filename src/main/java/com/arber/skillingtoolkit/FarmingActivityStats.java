package com.arber.skillingtoolkit;

class FarmingActivityStats
{
	private long runs;
	private long seedsUsed;
	private long seedCost;
	private long harvested;
	private long harvestValue;

	FarmingActivityStats copy()
	{
		FarmingActivityStats copy = new FarmingActivityStats();
		copy.runs = runs;
		copy.seedsUsed = seedsUsed;
		copy.seedCost = seedCost;
		copy.harvested = harvested;
		copy.harvestValue = harvestValue;
		return copy;
	}

	void add(long seedsUsed, long seedCost, long harvested, long harvestValue, boolean countRun)
	{
		if (countRun)
		{
			runs++;
		}

		this.seedsUsed += Math.max(0L, seedsUsed);
		this.seedCost += Math.max(0L, seedCost);
		this.harvested += Math.max(0L, harvested);
		this.harvestValue += Math.max(0L, harvestValue);
	}

	String serialize()
	{
		return runs + "," + seedsUsed + "," + seedCost + "," + harvested + "," + harvestValue;
	}

	void deserialize(String value)
	{
		if (value == null || value.isBlank())
		{
			return;
		}

		String[] parts = value.split(",", -1);
		if (parts.length > 0)
		{
			runs = parseLong(parts[0]);
		}
		if (parts.length > 1)
		{
			seedsUsed = parseLong(parts[1]);
		}
		if (parts.length > 2)
		{
			seedCost = parseLong(parts[2]);
		}
		if (parts.length > 3)
		{
			harvested = parseLong(parts[3]);
		}
		if (parts.length > 4)
		{
			harvestValue = parseLong(parts[4]);
		}
	}

	long getRuns()
	{
		return runs;
	}

	long getSeedsUsed()
	{
		return seedsUsed;
	}

	long getSeedCost()
	{
		return seedCost;
	}

	long getHarvested()
	{
		return harvested;
	}

	long getHarvestValue()
	{
		return harvestValue;
	}

	long getProfit()
	{
		return harvestValue - seedCost;
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
