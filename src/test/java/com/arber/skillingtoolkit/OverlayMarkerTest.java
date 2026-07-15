package com.arber.skillingtoolkit;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OverlayMarkerTest
{
	@Test
	public void nearbyCompostMarkersAreMerged()
	{
		Set<WorldPoint> rendered = new HashSet<>();
		rendered.add(new WorldPoint(3200, 3200, 0));

		assertTrue(SkillingToolkitOverlay.hasNearbyMarker(rendered, new WorldPoint(3204, 3203, 0)));
		assertFalse(SkillingToolkitOverlay.hasNearbyMarker(rendered, new WorldPoint(3206, 3200, 0)));
		assertFalse(SkillingToolkitOverlay.hasNearbyMarker(rendered, new WorldPoint(3200, 3200, 1)));
	}
}
