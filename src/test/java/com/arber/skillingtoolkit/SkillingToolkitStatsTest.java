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
		stats.addBirdhouseLoot(3L, 5500L, true);
		stats.addBirdhouseXp(4200L);
		stats.addFarmingActivity(FarmingActivityType.HERBS, 2L, 1200L, 18L, 24000L, true);
		stats.addFarmingActivity(FarmingActivityType.HOPS, 8L, 64L, 37L, 29415L, true);

		SkillingToolkitStats restored = SkillingToolkitStats.deserialize(stats.serialize(), "2026-06-20");

		assertEquals(3000L, restored.getXp());
		assertEquals(Skill.FARMING, restored.getTopSkill());
		assertEquals(2L, restored.getActiveSkillCount());
		assertEquals(9000L, restored.getNetValue());
		assertEquals(1L, restored.getActions());
		assertEquals(1L, restored.getBirdhouseRuns());
		assertEquals(4200L, restored.getBirdhouseXp());
		assertEquals(3L, restored.getBirdhouseNests());
		assertEquals(5500L, restored.getBirdhouseNestValue());
		FarmingActivityStats herbStats = restored.getFarmingActivityStats(FarmingActivityType.HERBS);
		assertEquals(1L, herbStats.getRuns());
		assertEquals(2L, herbStats.getSeedsUsed());
		assertEquals(1200L, herbStats.getSeedCost());
		assertEquals(18L, herbStats.getHarvested());
		assertEquals(24000L, herbStats.getHarvestValue());
		assertEquals(22800L, herbStats.getProfit());
		FarmingActivityStats hopsStats = restored.getFarmingActivityStats(FarmingActivityType.HOPS);
		assertEquals(1L, hopsStats.getRuns());
		assertEquals(8L, hopsStats.getSeedsUsed());
		assertEquals(64L, hopsStats.getSeedCost());
		assertEquals(37L, hopsStats.getHarvested());
		assertEquals(29415L, hopsStats.getHarvestValue());
	}

	@Test
	public void resetsExpiredPeriodButKeepsAllTimeValues()
	{
		String saved = "period=2026-06-19;profit=100;cost=25;actions=2;herbs=1,2,300,12,9000;xp=FARMING:50";

		assertEquals(0L, SkillingToolkitStats.deserialize(saved, "2026-06-20").getProfitValue());
		assertEquals(0L, SkillingToolkitStats.deserialize(saved, "2026-06-20").getFarmingActivityStats(FarmingActivityType.HERBS).getHarvested());
		assertEquals(100L, SkillingToolkitStats.deserialize(saved, "all").getProfitValue());
		assertEquals(12L, SkillingToolkitStats.deserialize(saved, "all").getFarmingActivityStats(FarmingActivityType.HERBS).getHarvested());
	}

	@Test
	public void resetGpKeepsNonGpStats()
	{
		SkillingToolkitStats stats = new SkillingToolkitStats("2026-06-30");
		stats.addProfit(12000L);
		stats.addCost(4500L);
		stats.addXp(Skill.FARMING, 2500L);
		stats.addAction();
		stats.addFarmingActivity(FarmingActivityType.HERBS, 1L, 27000L, 8L, 36000L, true);

		stats.resetGp();

		assertEquals(0L, stats.getProfitValue());
		assertEquals(0L, stats.getCostValue());
		assertEquals(2500L, stats.getXp());
		assertEquals(1L, stats.getActions());
		assertEquals(8L, stats.getFarmingActivityStats(FarmingActivityType.HERBS).getHarvested());
	}
}
