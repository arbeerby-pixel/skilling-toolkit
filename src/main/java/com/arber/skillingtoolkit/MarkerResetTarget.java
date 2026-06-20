package com.arber.skillingtoolkit;

public enum MarkerResetTarget
{
	COMPOST("Compost"),
	NEEDS_COMPOST("Needs compost"),
	PROTECTION("Crop protection"),
	PATCH_TIMERS("Patch timers"),
	BIRDHOUSES("Bird houses"),
	ALL("All markers");

	private final String displayName;

	MarkerResetTarget(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
