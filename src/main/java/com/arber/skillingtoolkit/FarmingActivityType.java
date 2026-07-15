package com.arber.skillingtoolkit;

enum FarmingActivityType
{
	HERBS("herbs"),
	ALLOTMENTS("allotments"),
	HOPS("hops");

	private final String key;

	FarmingActivityType(String key)
	{
		this.key = key;
	}

	String getKey()
	{
		return key;
	}
}
