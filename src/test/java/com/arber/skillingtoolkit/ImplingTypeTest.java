package com.arber.skillingtoolkit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImplingTypeTest
{
	@Test
	public void matchesImplingNpcNames()
	{
		assertEquals(ImplingType.BABY, ImplingType.fromNpcName("Baby impling"));
		assertEquals(ImplingType.DRAGON, ImplingType.fromNpcName("<col=ff9040>Dragon impling</col>"));
		assertEquals(ImplingType.LUCKY, ImplingType.fromNpcName("Lucky impling"));
		assertEquals(ImplingType.CRYSTAL, ImplingType.fromNpcName("Crystal impling"));
	}

	@Test
	public void ignoresNonImplingImps()
	{
		assertNull(ImplingType.fromNpcName("Imp"));
		assertNull(ImplingType.fromNpcName("Ancient imp"));
		assertNull(ImplingType.fromNpcName("Wandering impling"));
		assertNull(ImplingType.fromNpcName(null));
	}
}
