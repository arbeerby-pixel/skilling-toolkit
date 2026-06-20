package com.arber.skillingtoolkit;

public enum TrackerResetTarget
{
	XP("XP"),
	PROFIT("Profit"),
	COST("Cost"),
	ACTIONS("Actions"),
	ALL("All trackers");

	private final String displayName;

	TrackerResetTarget(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}

