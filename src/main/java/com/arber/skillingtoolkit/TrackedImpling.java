package com.arber.skillingtoolkit;

import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

final class TrackedImpling
{
	private final NPC npc;
	private final ImplingType type;
	private final WorldPoint worldPoint;
	private final int distance;
	private final boolean nearest;
	private final boolean live;
	private final long lastSeenMillis;

	private TrackedImpling(NPC npc, ImplingType type, WorldPoint worldPoint, int distance, boolean nearest, boolean live, long lastSeenMillis)
	{
		this.npc = npc;
		this.type = type;
		this.worldPoint = worldPoint;
		this.distance = distance;
		this.nearest = nearest;
		this.live = live;
		this.lastSeenMillis = lastSeenMillis;
	}

	static TrackedImpling live(NPC npc, ImplingType type, WorldPoint worldPoint, int distance, long lastSeenMillis)
	{
		return new TrackedImpling(npc, type, worldPoint, distance, false, true, lastSeenMillis);
	}

	static TrackedImpling lastSeen(ImplingType type, WorldPoint worldPoint, int distance, long lastSeenMillis)
	{
		return new TrackedImpling(null, type, worldPoint, distance, false, false, lastSeenMillis);
	}

	NPC getNpc()
	{
		return npc;
	}

	ImplingType getType()
	{
		return type;
	}

	WorldPoint getWorldPoint()
	{
		return worldPoint;
	}

	int getDistance()
	{
		return distance;
	}

	boolean isNearest()
	{
		return nearest;
	}

	boolean isLive()
	{
		return live;
	}

	long getLastSeenMillis()
	{
		return lastSeenMillis;
	}

	TrackedImpling withNearest(boolean nearest)
	{
		return new TrackedImpling(npc, type, worldPoint, distance, nearest, live, lastSeenMillis);
	}
}
