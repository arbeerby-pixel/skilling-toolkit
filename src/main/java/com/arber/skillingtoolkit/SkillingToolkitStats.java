package com.arber.skillingtoolkit;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Skill;

class SkillingToolkitStats
{
	private String periodId;
	private long profitValue;
	private long costValue;
	private long actions;
	private long birdhouseRuns;
	private long birdhouseNests;
	private long birdhouseNestValue;
	private long birdhouseXp;
	private final EnumMap<Skill, Long> xpBySkill = new EnumMap<>(Skill.class);
	private final EnumMap<Skill, SkillActivityStats> activityStatsBySkill = new EnumMap<>(Skill.class);
	private final EnumMap<FarmingActivityType, FarmingActivityStats> farmingStatsByType = new EnumMap<>(FarmingActivityType.class);

	SkillingToolkitStats(String periodId)
	{
		this.periodId = periodId;
		for (Skill skill : SkillingToolkitPlugin.SKILLING_TRACKER_SKILLS)
		{
			xpBySkill.put(skill, 0L);
			activityStatsBySkill.put(skill, new SkillActivityStats());
		}
		for (FarmingActivityType type : FarmingActivityType.values())
		{
			farmingStatsByType.put(type, new FarmingActivityStats());
		}
	}

	static SkillingToolkitStats deserialize(String serialized, String currentPeriodId)
	{
		SkillingToolkitStats stats = new SkillingToolkitStats(currentPeriodId);
		if (serialized == null || serialized.isBlank())
		{
			return stats;
		}

		for (String savedValue : serialized.split(";"))
		{
			String[] parts = savedValue.split("=", 2);
			if (parts.length != 2)
			{
				continue;
			}

			switch (parts[0])
			{
				case "period":
					stats.periodId = parts[1];
					break;
				case "profit":
					stats.profitValue = parseLong(parts[1]);
					break;
				case "cost":
					stats.costValue = parseLong(parts[1]);
					break;
				case "actions":
					stats.actions = parseLong(parts[1]);
					break;
				case "birdhouseRuns":
					stats.birdhouseRuns = parseLong(parts[1]);
					break;
				case "birdhouseNests":
					stats.birdhouseNests = parseLong(parts[1]);
					break;
				case "birdhouseNestValue":
					stats.birdhouseNestValue = parseLong(parts[1]);
					break;
				case "birdhouseXp":
					stats.birdhouseXp = parseLong(parts[1]);
					break;
				case "xp":
					stats.parseXp(parts[1]);
					break;
				case "activities":
					stats.parseActivityStats(parts[1]);
					break;
				default:
					stats.parseFarmingStats(parts[0], parts[1]);
					break;
			}
		}

		if (!"all".equals(currentPeriodId) && !currentPeriodId.equals(stats.periodId))
		{
			return new SkillingToolkitStats(currentPeriodId);
		}

		if ("all".equals(currentPeriodId))
		{
			stats.periodId = currentPeriodId;
		}

		return stats;
	}

	SkillingToolkitStats copy()
	{
		SkillingToolkitStats copy = new SkillingToolkitStats(periodId);
		copy.profitValue = profitValue;
		copy.costValue = costValue;
		copy.actions = actions;
		copy.birdhouseRuns = birdhouseRuns;
		copy.birdhouseNests = birdhouseNests;
		copy.birdhouseNestValue = birdhouseNestValue;
		copy.birdhouseXp = birdhouseXp;
		copy.xpBySkill.putAll(xpBySkill);
		for (Map.Entry<Skill, SkillActivityStats> entry : activityStatsBySkill.entrySet())
		{
			copy.activityStatsBySkill.put(entry.getKey(), entry.getValue().copy());
		}
		for (Map.Entry<FarmingActivityType, FarmingActivityStats> entry : farmingStatsByType.entrySet())
		{
			copy.farmingStatsByType.put(entry.getKey(), entry.getValue().copy());
		}
		return copy;
	}

	String serialize()
	{
		StringBuilder xp = new StringBuilder();
		for (Map.Entry<Skill, Long> entry : xpBySkill.entrySet())
		{
			if (xp.length() > 0)
			{
				xp.append(',');
			}

			xp.append(entry.getKey().name()).append(':').append(entry.getValue());
		}

		return "period=" + periodId
			+ ";profit=" + profitValue
			+ ";cost=" + costValue
			+ ";actions=" + actions
			+ ";birdhouseRuns=" + birdhouseRuns
			+ ";birdhouseNests=" + birdhouseNests
			+ ";birdhouseNestValue=" + birdhouseNestValue
			+ ";birdhouseXp=" + birdhouseXp
			+ serializeFarmingStats()
			+ ";xp=" + xp
			+ serializeActivityStats();
	}

	String getPeriodId()
	{
		return periodId;
	}

	void addXp(Skill skill, long xp)
	{
		if (xpBySkill.containsKey(skill))
		{
			xpBySkill.merge(skill, Math.max(0L, xp), Long::sum);
		}
	}

	boolean ensureXpAtLeast(Skill skill, long xp)
	{
		if (!xpBySkill.containsKey(skill))
		{
			return false;
		}

		long value = Math.max(0L, xp);
		if (xpBySkill.getOrDefault(skill, 0L) >= value)
		{
			return false;
		}

		xpBySkill.put(skill, value);
		return true;
	}

	void addProfit(long value)
	{
		profitValue += Math.max(0L, value);
	}

	boolean ensureProfitAtLeast(long value)
	{
		value = Math.max(0L, value);
		if (profitValue >= value)
		{
			return false;
		}

		profitValue = value;
		return true;
	}

	void addCost(long value)
	{
		costValue += Math.max(0L, value);
	}

	void resetGp()
	{
		profitValue = 0L;
		costValue = 0L;
	}

	boolean ensureCostAtLeast(long value)
	{
		value = Math.max(0L, value);
		if (costValue >= value)
		{
			return false;
		}

		costValue = value;
		return true;
	}

	void addAction()
	{
		actions++;
	}

	void addBirdhouseLoot(long nests, long nestValue, boolean countRun)
	{
		if (countRun)
		{
			birdhouseRuns++;
		}

		birdhouseNests += Math.max(0L, nests);
		birdhouseNestValue += Math.max(0L, nestValue);
	}

	void addBirdhouseXp(long xp)
	{
		birdhouseXp += Math.max(0L, xp);
	}

	void addFarmingActivity(FarmingActivityType type, long seedsUsed, long seedCost, long harvested, long harvestValue, boolean countRun)
	{
		farmingStatsByType.get(type).add(seedsUsed, seedCost, harvested, harvestValue, countRun);
	}

	void addActivity(Skill skill, int itemId, long processed, long xp, long inputCost, long outputValue, long nowMillis)
	{
		SkillActivityStats stats = activityStatsBySkill.get(skill);
		if (stats != null)
		{
			stats.add(itemId, processed, xp, inputCost, outputValue, nowMillis);
		}
	}

	boolean ensureActionsAtLeast(long value)
	{
		value = Math.max(0L, value);
		if (actions >= value)
		{
			return false;
		}

		actions = value;
		return true;
	}

	long getXp()
	{
		long total = 0L;
		for (long xp : xpBySkill.values())
		{
			total += xp;
		}
		return total;
	}

	long getXp(Skill skill)
	{
		return xpBySkill.getOrDefault(skill, 0L);
	}

	Skill getTopSkill()
	{
		Skill topSkill = null;
		long topXp = 0L;
		for (Map.Entry<Skill, Long> entry : xpBySkill.entrySet())
		{
			if (entry.getValue() > topXp)
			{
				topXp = entry.getValue();
				topSkill = entry.getKey();
			}
		}

		return topSkill;
	}

	long getTopSkillXp()
	{
		Skill topSkill = getTopSkill();
		return topSkill == null ? 0L : getXp(topSkill);
	}

	long getProfitValue()
	{
		return profitValue;
	}

	long getCostValue()
	{
		return costValue;
	}

	long getNetValue()
	{
		return profitValue - costValue;
	}

	long getActions()
	{
		return actions;
	}

	long getBirdhouseRuns()
	{
		return birdhouseRuns;
	}

	long getBirdhouseNests()
	{
		return birdhouseNests;
	}

	long getBirdhouseNestValue()
	{
		return birdhouseNestValue;
	}

	long getBirdhouseXp()
	{
		return birdhouseXp;
	}

	long getActiveSkillCount()
	{
		long count = 0L;
		for (long xp : xpBySkill.values())
		{
			if (xp > 0)
			{
				count++;
			}
		}

		return count;
	}

	FarmingActivityStats getFarmingActivityStats(FarmingActivityType type)
	{
		return farmingStatsByType.get(type).copy();
	}

	List<ProcessedItemStats> getActivityStats(Skill skill)
	{
		SkillActivityStats stats = activityStatsBySkill.get(skill);
		return stats == null ? Collections.emptyList() : stats.getItemsByRecent();
	}

	private void parseXp(String value)
	{
		if (value == null || value.isBlank())
		{
			return;
		}

		for (String skillValue : value.split(","))
		{
			String[] parts = skillValue.split(":", 2);
			if (parts.length != 2)
			{
				continue;
			}

			try
			{
				Skill skill = Skill.valueOf(parts[0]);
				if (xpBySkill.containsKey(skill))
				{
					xpBySkill.put(skill, parseLong(parts[1]));
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore stale or manually edited values.
			}
		}
	}

	private String serializeFarmingStats()
	{
		StringBuilder serialized = new StringBuilder();
		for (FarmingActivityType type : FarmingActivityType.values())
		{
			serialized.append(';')
				.append(type.getKey())
				.append('=')
				.append(farmingStatsByType.get(type).serialize());
		}
		return serialized.toString();
	}

	private String serializeActivityStats()
	{
		StringBuilder serialized = new StringBuilder();
		for (Map.Entry<Skill, SkillActivityStats> entry : activityStatsBySkill.entrySet())
		{
			String skillStats = entry.getValue().serialize();
			if (skillStats.isBlank())
			{
				continue;
			}

			if (serialized.length() > 0)
			{
				serialized.append('#');
			}

			serialized.append(entry.getKey().name())
				.append(':')
				.append(skillStats);
		}

		return ";activities=" + serialized;
	}

	private void parseActivityStats(String value)
	{
		if (value == null || value.isBlank())
		{
			return;
		}

		for (String skillValue : value.split("#", -1))
		{
			String[] parts = skillValue.split(":", 2);
			if (parts.length != 2)
			{
				continue;
			}

			try
			{
				Skill skill = Skill.valueOf(parts[0]);
				SkillActivityStats stats = activityStatsBySkill.get(skill);
				if (stats != null)
				{
					stats.deserialize(parts[1]);
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore stale or manually edited values.
			}
		}
	}

	private void parseFarmingStats(String key, String value)
	{
		for (FarmingActivityType type : FarmingActivityType.values())
		{
			if (type.getKey().equals(key))
			{
				farmingStatsByType.get(type).deserialize(value);
				return;
			}
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
