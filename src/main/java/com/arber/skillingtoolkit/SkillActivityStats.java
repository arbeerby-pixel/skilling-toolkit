package com.arber.skillingtoolkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SkillActivityStats
{
	private static final int MAX_TRACKED_ITEMS = 12;

	private final Map<Integer, ProcessedItemStats> items = new HashMap<>();

	SkillActivityStats copy()
	{
		SkillActivityStats copy = new SkillActivityStats();
		for (Map.Entry<Integer, ProcessedItemStats> entry : items.entrySet())
		{
			copy.items.put(entry.getKey(), entry.getValue().copy());
		}
		return copy;
	}

	void add(int itemId, long processed, long xp, long inputCost, long outputValue, long nowMillis)
	{
		if (itemId <= 0 || (processed <= 0 && xp <= 0 && inputCost <= 0 && outputValue <= 0))
		{
			return;
		}

		items.computeIfAbsent(itemId, ProcessedItemStats::new)
			.add(processed, xp, inputCost, outputValue, nowMillis);
		trimOldItems();
	}

	List<ProcessedItemStats> getItemsByRecent()
	{
		List<ProcessedItemStats> sorted = new ArrayList<>();
		for (ProcessedItemStats stats : items.values())
		{
			sorted.add(stats.copy());
		}

		sorted.sort((left, right) ->
		{
			int comparison = Long.compare(right.getLastSeenMillis(), left.getLastSeenMillis());
			if (comparison != 0)
			{
				return comparison;
			}

			comparison = Long.compare(right.getXp(), left.getXp());
			if (comparison != 0)
			{
				return comparison;
			}

			return Long.compare(right.getProcessed(), left.getProcessed());
		});
		return sorted;
	}

	String serialize()
	{
		StringBuilder serialized = new StringBuilder();
		for (ProcessedItemStats stats : getItemsByRecent())
		{
			if (serialized.length() > 0)
			{
				serialized.append('|');
			}

			serialized.append(stats.serialize());
		}
		return serialized.toString();
	}

	void deserialize(String value)
	{
		items.clear();
		if (value == null || value.isBlank())
		{
			return;
		}

		for (String itemValue : value.split("\\|", -1))
		{
			ProcessedItemStats stats = ProcessedItemStats.deserialize(itemValue);
			if (stats != null)
			{
				items.put(stats.getItemId(), stats);
			}
		}
		trimOldItems();
	}

	private void trimOldItems()
	{
		if (items.size() <= MAX_TRACKED_ITEMS)
		{
			return;
		}

		List<ProcessedItemStats> sorted = getItemsByRecent();
		for (int index = MAX_TRACKED_ITEMS; index < sorted.size(); index++)
		{
			items.remove(sorted.get(index).getItemId());
		}
	}
}
