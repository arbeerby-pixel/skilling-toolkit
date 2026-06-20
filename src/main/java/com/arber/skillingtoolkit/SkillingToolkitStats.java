package com.arber.skillingtoolkit;

import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.Skill;

class SkillingToolkitStats
{
	private String periodId;
	private long profitValue;
	private long costValue;
	private long actions;
	private final EnumMap<Skill, Long> xpBySkill = new EnumMap<>(Skill.class);

	SkillingToolkitStats(String periodId)
	{
		this.periodId = periodId;
		for (Skill skill : SkillingToolkitPlugin.SKILLING_TRACKER_SKILLS)
		{
			xpBySkill.put(skill, 0L);
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
				case "xp":
					stats.parseXp(parts[1]);
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
		copy.xpBySkill.putAll(xpBySkill);
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
			+ ";xp=" + xp;
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
