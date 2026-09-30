package com.arber.skillingtoolkit;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.IntToLongFunction;
import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InventoryTransformationTest
{
	@Test
	public void birdHouseCraftingDoesNotCountAsProfitOrCost()
	{
		int[][] birdHouseLogs = {
			{ItemID.BIRD_HOUSE, ItemID.LOGS},
			{ItemID.OAK_BIRD_HOUSE, ItemID.OAK_LOGS},
			{ItemID.WILLOW_BIRD_HOUSE, ItemID.WILLOW_LOGS},
			{ItemID.TEAK_BIRD_HOUSE, ItemID.TEAK_LOGS},
			{ItemID.MAPLE_BIRD_HOUSE, ItemID.MAPLE_LOGS},
			{ItemID.MAHOGANY_BIRD_HOUSE, ItemID.MAHOGANY_LOGS},
			{ItemID.YEW_BIRD_HOUSE, ItemID.YEW_LOGS},
			{ItemID.MAGIC_BIRD_HOUSE, ItemID.MAGIC_LOGS},
			{ItemID.REDWOOD_BIRD_HOUSE, ItemID.REDWOOD_LOGS}
		};

		for (int[] pair : birdHouseLogs)
		{
			Map<Integer, Long> gained = quantities(pair[0], 1L);
			Map<Integer, Long> spent = quantities(pair[1], 1L, ItemID.CLOCKWORK, 1L);

			SkillingToolkitPlugin.neutralizeBirdHouseCrafting(gained, spent);

			assertEquals(new HashMap<>(), gained);
			assertEquals(new HashMap<>(), spent);
		}
	}

	@Test
	public void placedBirdHouseRemainsARealCost()
	{
		Map<Integer, Long> gained = new HashMap<>();
		Map<Integer, Long> spent = quantities(ItemID.MAHOGANY_BIRD_HOUSE, 1L);

		SkillingToolkitPlugin.neutralizeBirdHouseCrafting(gained, spent);

		assertEquals(quantities(ItemID.MAHOGANY_BIRD_HOUSE, 1L), spent);
	}

	@Test
	public void incompleteCraftingChangeIsNotNeutralized()
	{
		Map<Integer, Long> gained = quantities(ItemID.MAHOGANY_BIRD_HOUSE, 1L);
		Map<Integer, Long> spent = quantities(ItemID.MAHOGANY_LOGS, 1L);

		SkillingToolkitPlugin.neutralizeBirdHouseCrafting(gained, spent);

		assertEquals(quantities(ItemID.MAHOGANY_BIRD_HOUSE, 1L), gained);
		assertEquals(quantities(ItemID.MAHOGANY_LOGS, 1L), spent);
	}

	@Test
	public void chargedItemOnlyCountsTheConsumedChargeValue()
	{
		Map<Integer, Long> gained = quantities(2, 1L);
		Map<Integer, Long> spent = quantities(1, 1L);
		IntToLongFunction prices = itemId -> itemId == 1 ? 18_000L : 15_000L;
		IntFunction<String> names = itemId -> itemId == 1 ? "Digsite pendant(5)" : "Digsite pendant(4)";

		SkillingToolkitPlugin.InventoryValueAdjustment adjustment =
			SkillingToolkitPlugin.neutralizeChargedItemChanges(gained, spent, prices, names);

		assertEquals(new HashMap<>(), gained);
		assertEquals(new HashMap<>(), spent);
		assertEquals(0L, adjustment.gainedValue);
		assertEquals(3_000L, adjustment.spentValue);
	}

	@Test
	public void chargedItemFallsBackToAProportionalChargePrice()
	{
		Map<Integer, Long> gained = quantities(2, 1L);
		Map<Integer, Long> spent = quantities(1, 1L);
		IntToLongFunction prices = itemId -> itemId == 1 ? 20_000L : 0L;
		IntFunction<String> names = itemId -> itemId == 1 ? "Skills necklace(4)" : "Skills necklace(3)";

		SkillingToolkitPlugin.InventoryValueAdjustment adjustment =
			SkillingToolkitPlugin.neutralizeChargedItemChanges(gained, spent, prices, names);

		assertEquals(5_000L, adjustment.spentValue);
	}

	@Test
	public void unrelatedNumberedItemsAreNotMerged()
	{
		Map<Integer, Long> gained = quantities(2, 1L);
		Map<Integer, Long> spent = quantities(1, 1L);
		IntToLongFunction prices = itemId -> 10_000L;
		IntFunction<String> names = itemId -> itemId == 1 ? "Potion(4)" : "Different potion(3)";

		SkillingToolkitPlugin.InventoryValueAdjustment adjustment =
			SkillingToolkitPlugin.neutralizeChargedItemChanges(gained, spent, prices, names);

		assertEquals(quantities(2, 1L), gained);
		assertEquals(quantities(1, 1L), spent);
		assertEquals(0L, adjustment.gainedValue);
		assertEquals(0L, adjustment.spentValue);
	}

	@Test
	public void openedBirdNestDoesNotCountAsNewProfitOrCost()
	{
		Map<Integer, Long> gained = quantities(2, 1L, 3, 4L);
		Map<Integer, Long> spent = quantities(1, 1L);
		IntFunction<String> names = itemId ->
		{
			switch (itemId)
			{
				case 1:
					return "Bird nest";
				case 2:
					return "Bird nest";
				case 3:
					return "Ranarr seed";
				default:
					return "";
			}
		};

		SkillingToolkitPlugin.neutralizeOpenedBirdNestChanges(gained, spent, names);

		assertEquals(new HashMap<>(), gained);
		assertEquals(new HashMap<>(), spent);
	}

	@Test
	public void suspiciousLargeInventoryReplacementIsIgnored()
	{
		assertTrue(SkillingToolkitPlugin.isSuspiciousInventoryValueReplacement(40_400_000L, 39_700_000L, 1, 1));
		assertTrue(SkillingToolkitPlugin.isSuspiciousInventoryValueReplacement(51_800_000L, 43_600_000L, 1, 1));
		assertTrue(SkillingToolkitPlugin.isSuspiciousInventoryValueReplacement(1_100_000L, 1_000_000L, 2, 2));
		assertFalse(SkillingToolkitPlugin.isSuspiciousInventoryValueReplacement(80_000L, 26_000L, 1, 1));
		assertFalse(SkillingToolkitPlugin.isSuspiciousInventoryValueReplacement(1_100_000L, 120_000L, 2, 2));
	}

	@Test
	public void inventoryValueChangesNeedSkillingContext()
	{
		assertFalse(SkillingToolkitPlugin.shouldAcceptInventoryValueChange(false, false, false, false));
		assertTrue(SkillingToolkitPlugin.shouldAcceptInventoryValueChange(false, true, false, false));
		assertTrue(SkillingToolkitPlugin.shouldAcceptInventoryValueChange(false, false, true, false));
		assertTrue(SkillingToolkitPlugin.shouldAcceptInventoryValueChange(true, false, false, false));
		assertFalse(SkillingToolkitPlugin.shouldAcceptInventoryValueChange(false, false, true, true));
		assertTrue(SkillingToolkitPlugin.shouldAcceptInventoryValueChange(false, true, false, true));
	}

	@Test
	public void herbSackTransfersAreIgnoredForInventoryValueTracking()
	{
		assertTrue(SkillingToolkitPlugin.isHerbSackInventoryTransferOption(ItemID.HERB_SACK, "Empty"));
		assertTrue(SkillingToolkitPlugin.isHerbSackInventoryTransferOption(ItemID.OPEN_HERB_SACK, "Fill"));
		assertTrue(SkillingToolkitPlugin.isHerbSackInventoryTransferOption(ItemID.SILKLINED_HERB_SACK, "Open"));
		assertTrue(SkillingToolkitPlugin.isHerbSackInventoryTransferOption(ItemID.OPEN_SILKLINED_HERB_SACK, "Close"));
		assertFalse(SkillingToolkitPlugin.isHerbSackInventoryTransferOption(ItemID.OPEN_HERB_SACK, "Check"));
		assertFalse(SkillingToolkitPlugin.isHerbSackInventoryTransferOption(ItemID.RANARR_WEED, "Empty"));
	}

	@Test
	public void permanentCompostMarkersDisableAutoClear()
	{
		assertTrue(SkillingToolkitPlugin.shouldAutoClearCompostMarkers(true, false));
		assertFalse(SkillingToolkitPlugin.shouldAutoClearCompostMarkers(true, true));
		assertFalse(SkillingToolkitPlugin.shouldAutoClearCompostMarkers(false, false));
	}

	private static Map<Integer, Long> quantities(int itemId, long quantity, Object... additional)
	{
		Map<Integer, Long> quantities = new HashMap<>();
		quantities.put(itemId, quantity);
		for (int i = 0; i < additional.length; i += 2)
		{
			quantities.put((Integer) additional[i], (Long) additional[i + 1]);
		}
		return quantities;
	}
}
