package com.arber.skillingtoolkit;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FarmingPatchTest
{
	@Test
	public void resolvesFossilIslandSquirrelToNearestHardwoodPatch()
	{
		assertEquals(
			FarmingPatch.HARDWOOD_WEST,
			FarmingPatch.fromFarmer(NpcID.FOSSIL_SQUIRREL_GARDENER1, -1, new WorldPoint(3703, 3836, 0)));
		assertEquals(
			FarmingPatch.HARDWOOD_SOUTH,
			FarmingPatch.fromFarmer(NpcID.FOSSIL_SQUIRREL_GARDENER2, -1, new WorldPoint(3707, 3834, 0)));
		assertEquals(
			FarmingPatch.HARDWOOD_EAST,
			FarmingPatch.fromFarmer(NpcID.FOSSIL_SQUIRREL_GARDENER3, -1, new WorldPoint(3714, 3836, 0)));
	}
}
