package com.arber.skillingtoolkit;

import net.runelite.api.coords.WorldPoint;

class CompostPatch
{
	private final WorldPoint worldPoint;
	private final CompostType compostType;

	CompostPatch(WorldPoint worldPoint, CompostType compostType)
	{
		this.worldPoint = worldPoint;
		this.compostType = compostType;
	}

	WorldPoint getWorldPoint()
	{
		return worldPoint;
	}

	CompostType getCompostType()
	{
		return compostType;
	}
}
