package com.arber.skillingtoolkit;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatchTimerTest
{
	private static final long MINUTE = 60_000L;

	@Test
	public void formatsRemainingTimeWithoutSeconds()
	{
		PatchTimer timer = new PatchTimer(new WorldPoint(1, 2, 0), 125L * MINUTE, "attas");

		assertEquals("2h 5m", timer.formatRemaining(0L));
		assertEquals("1m", timer.formatRemaining(124L * MINUTE + 1L));
	}

	@Test
	public void expiresAtDoneTime()
	{
		PatchTimer timer = new PatchTimer(new WorldPoint(1, 2, 0), MINUTE, "iasor");

		assertFalse(timer.isExpired(MINUTE - 1L));
		assertTrue(timer.isExpired(MINUTE));
		assertEquals("", timer.formatRemaining(MINUTE));
	}
}
