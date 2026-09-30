package com.arber.skillingtoolkit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RunCountdownInfoBoxTest
{
	@Test
	public void formatsMissingTimerWithoutReady()
	{
		assertEquals("-", RunCountdownInfoBox.formatCountdown(0L, 1_000L));
	}

	@Test
	public void formatsMinuteSecondCountdown()
	{
		assertEquals("8:13", RunCountdownInfoBox.formatCountdown(493_000L, 0L));
	}

	@Test
	public void formatsHourMinuteCountdown()
	{
		assertEquals("1:20", RunCountdownInfoBox.formatCountdown(4_800_000L, 0L));
	}
}
