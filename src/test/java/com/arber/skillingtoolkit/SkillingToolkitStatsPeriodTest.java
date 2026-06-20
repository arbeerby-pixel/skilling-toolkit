package com.arber.skillingtoolkit;

import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SkillingToolkitStatsPeriodTest
{
	@Test
	public void createsStablePeriodIds()
	{
		LocalDate date = LocalDate.of(2026, 1, 1);

		assertEquals("2026-01-01", SkillingToolkitStatsPeriod.DAY.getCurrentPeriodId(date));
		assertEquals("2026-W1", SkillingToolkitStatsPeriod.WEEK.getCurrentPeriodId(date));
		assertEquals("2026-1", SkillingToolkitStatsPeriod.MONTH.getCurrentPeriodId(date));
		assertEquals("2026", SkillingToolkitStatsPeriod.YEAR.getCurrentPeriodId(date));
		assertEquals("all", SkillingToolkitStatsPeriod.ALL_TIME.getCurrentPeriodId(date));
	}
}
