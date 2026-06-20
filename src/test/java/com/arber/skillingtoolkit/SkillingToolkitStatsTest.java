package com.arber.skillingtoolkit;

import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SkillingToolkitStatsTest
{
	@Test
	public void roundTripsTrackedValues()
	{
		SkillingToolkitStats stats = new SkillingToolkitStats("2026-06-20");
		stats.addXp(Skill.FARMING, 2500L);
		stats.addXp(Skill.HUNTER, 500L);
		stats.addProfit(12000L);
		stats.addCost(3000L);
		stats.addAction();

		SkillingToolkitStats restored = SkillingToolkitStats.deserialize(stats.serialize(), "2026-06-20");

		assertEquals(3000L, restored.getXp());
		assertEquals(Skill.FARMING, restored.getTopSkill());
		assertEquals(9000L, restored.getNetValue());
		assertEquals(1L, restored.getActions());
	}

	@Test
	public void resetsExpiredPeriodButKeepsAllTimeValues()
	{
		String saved = "period=2026-06-19;profit=100;cost=25;actions=2;xp=FARMING:50";

		assertEquals(0L, SkillingToolkitStats.deserialize(saved, "2026-06-20").getProfitValue());
		assertEquals(100L, SkillingToolkitStats.deserialize(saved, "all").getProfitValue());
	}
}
