package com.arber.skillingtoolkit;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HerbSackContentsTest
{
	@Test
	public void parsesHerbSackCheckLine()
	{
		HerbSackContents.ParsedHerbLine line = HerbSackContents.parseChatLine("16 x Grimy ranarr weed.");

		assertEquals(HerbSackHerb.GRIMY_RANARR_WEED, line.getHerb());
		assertEquals(16, line.getQuantity());
	}

	@Test
	public void parsesCommaSeparatedQuantity()
	{
		HerbSackContents.ParsedHerbLine line = HerbSackContents.parseChatLine("1,234 x Grimy huasca");

		assertEquals(HerbSackHerb.GRIMY_HUASCA, line.getHerb());
		assertEquals(1234, line.getQuantity());
	}

	@Test
	public void ignoresUnknownLines()
	{
		assertNull(HerbSackContents.parseChatLine("The herb sack is empty."));
		assertNull(HerbSackContents.parseChatLine("You can't possibly put that in there."));
	}

	@Test
	public void recognisesAllHerbSackItems()
	{
		assertTrue(SkillingToolkitPlugin.isHerbSackItem(ItemID.HERB_SACK));
		assertTrue(SkillingToolkitPlugin.isHerbSackItem(ItemID.OPEN_HERB_SACK));
		assertTrue(SkillingToolkitPlugin.isHerbSackItem(ItemID.SILKLINED_HERB_SACK));
		assertTrue(SkillingToolkitPlugin.isHerbSackItem(ItemID.OPEN_SILKLINED_HERB_SACK));
	}
}
