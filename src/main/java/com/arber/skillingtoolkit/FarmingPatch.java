package com.arber.skillingtoolkit;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

enum FarmingPatch
{
	ARDY_ALLOTMENT_NORTH(8555, new WorldPoint(2667, 3371, 0), 2665, 0),
	ARDY_ALLOTMENT_SOUTH(8554, new WorldPoint(2667, 3378, 0), 2665, 1),
	ARDY_FLOWER(7849, new WorldPoint(2667, 3374, 0)),
	ARDY_HERB(8152, new WorldPoint(2670, 3374, 0)),

	FALLY_ALLOTMENT_EAST(8551, new WorldPoint(3055, 3304, 0)),
	FALLY_ALLOTMENT_WEST(8550, new WorldPoint(3051, 3307, 0)),
	FALLY_FLOWER(7847, new WorldPoint(3054, 3307, 0)),
	FALLY_HERB(8150, new WorldPoint(3058, 3311, 0)),

	MORY_ALLOTMENT_EAST(8557, new WorldPoint(3602, 3522, 0), 2666, 1),
	MORY_ALLOTMENT_WEST(8556, new WorldPoint(3598, 3525, 0), 2666, 0),
	MORY_FLOWER(7850, new WorldPoint(3602, 3525, 0)),
	MORY_HERB(8153, new WorldPoint(3605, 3529, 0)),

	CATHERBY_ALLOTMENT_NORTH(8552, new WorldPoint(2806, 3466, 0)),
	CATHERBY_ALLOTMENT_SOUTH(8553, new WorldPoint(2806, 3461, 0)),
	CATHERBY_FLOWER(7848, new WorldPoint(2810, 3463, 0)),
	CATHERBY_HERB(8151, new WorldPoint(2813, 3463, 0)),

	HOSIDIUS_ALLOTMENT_EAST(27113, new WorldPoint(1738, 3554, 0), 6921, 0),
	HOSIDIUS_ALLOTMENT_WEST(27114, new WorldPoint(1735, 3551, 0), 6921, 1),
	HOSIDIUS_FLOWER(27111, new WorldPoint(1735, 3554, 0)),
	HOSIDIUS_HERB(27115, new WorldPoint(1738, 3551, 0)),

	VARLAMORE_ALLOTMENT_NORTH(50696, new WorldPoint(1582, 3100, 0), 12766, 0),
	VARLAMORE_ALLOTMENT_SOUTH(50695, new WorldPoint(1586, 3095, 0), 12766, 1),
	VARLAMORE_FLOWER(50693, new WorldPoint(1585, 3098, 0)),
	VARLAMORE_HERB(50697, new WorldPoint(1582, 3095, 0)),

	HARMONY_HERB(9372, new WorldPoint(3790, 2838, 0)),
	HARMONY_ALLOTMENT(21950, new WorldPoint(3794, 2837, 0)),

	FARMING_GUILD_ALLOTMENT_NORTH(33694, new WorldPoint(1267, 3732, 0), 8535, 1),
	FARMING_GUILD_ALLOTMENT_SOUTH(33693, new WorldPoint(1267, 3727, 0), 8535, 2),
	FARMING_GUILD_FLOWER(33649, new WorldPoint(1260, 3726, 0)),
	FARMING_GUILD_HERB(33979, new WorldPoint(1239, 3727, 0)),

	PRIFF_ALLOTMENT_NORTH(34922, new WorldPoint(3289, 6101, 0), 9138, 0),
	PRIFF_ALLOTMENT_SOUTH(34921, new WorldPoint(3289, 6098, 0), 9138, 1),
	PRIFF_FLOWER(34919, new WorldPoint(3293, 6100, 0)),

	TROLL_HERB(18816, new WorldPoint(2827, 3694, 0)),
	WEISS_HERB(33176, new WorldPoint(2848, 3935, 0)),

	CHAMPION_GUILD_HOPS(8175, new WorldPoint(3231, 3317, 0), 2672),
	MCGRUBOR_HOPS(8176, new WorldPoint(2669, 3523, 0), 2673),
	YANILLE_HOPS(8173, new WorldPoint(2577, 3106, 0), 2671),
	ENTRANA_HOPS(8174, new WorldPoint(2812, 3338, 0), 2667),
	VARLAMORE_HOPS(55341, new WorldPoint(1366, 2940, 0), 14030),

	FARMING_GUILD_BUSH(34006, new WorldPoint(1260, 3733, 0), 8535, 3),
	CHAMPION_GUILD_BUSH(7577, new WorldPoint(3182, 3358, 0), 2674),
	RIMMINGTON_BUSH(7578, new WorldPoint(2941, 3222, 0), 2675),
	ARDY_BUSH(7580, new WorldPoint(2617, 3226, 0), 2677),
	ETCETERA_BUSH(7579, new WorldPoint(2592, 3863, 0), 2676),

	FALLY_TREE(8389, new WorldPoint(3003, 3374, 0), 2679),
	TAVERLEY_TREE(8388, new WorldPoint(2935, 3439, 0), 2678),
	FARMING_GUILD_TREE(33732, new WorldPoint(1233, 3735, 0), 8534),
	LUMBRIDGE_TREE(8391, new WorldPoint(3194, 3232, 0), 2681),
	VARROCK_TREE(8390, new WorldPoint(3228, 3458, 0), 11957),
	GNOME_STRONGHOLD_TREE(19147, new WorldPoint(2437, 3416, 0), 2687),
	VARLAMORE_TREE(56953, new WorldPoint(1365, 3320, 0), 14514),

	CATHERBY_FRUIT(7965, new WorldPoint(2860, 3433, 0)),
	FARMING_GUILD_FRUIT(34007, new WorldPoint(1243, 3758, 0), 8533),
	GNOME_STRONGHOLD_FRUIT(7962, new WorldPoint(2475, 3446, 0), 2682),
	GNOME_VILLAGE_FRUIT(7963, new WorldPoint(2490, 3180, 0), 2683),
	BRIMHAVEN_FRUIT(7964, new WorldPoint(2765, 3212, 0), 2669),
	LLETYA_FRUIT(26579, new WorldPoint(2346, 3162, 0), 2689),
	VARLAMORE_FRUIT(56955, new WorldPoint(1350, 3056, 0), 14516),

	FARMING_GUILD_SPIRIT(33733, new WorldPoint(1252, 3751, 0)),
	ETCETERA_SPIRIT(8382, new WorldPoint(2612, 3857, 0)),
	PORT_SARIM_SPIRIT(8338, new WorldPoint(3059, 3257, 0)),
	BRIMHAVEN_SPIRIT(8383, new WorldPoint(2801, 3204, 0)),
	HOSIDIUS_SPIRIT(27116, new WorldPoint(1692, 3541, 0)),

	SEAWEED_NORTH(30500, new WorldPoint(3733, 10273, 1)),
	SEAWEED_SOUTH(30501, new WorldPoint(3733, 10268, 1)),

	HARDWOOD_WEST(30481, new WorldPoint(3703, 3836, 0)),
	HARDWOOD_SOUTH(30480, new WorldPoint(3707, 3834, 0)),
	HARDWOOD_EAST(30482, new WorldPoint(3714, 3836, 0)),
	VARLAMORE_HARDWOOD(50692, new WorldPoint(1687, 2973, 0)),

	ALKHARID_CACTUS(7771, new WorldPoint(3315, 3203, 0)),
	FARMING_GUILD_CACTUS(33761, new WorldPoint(1264, 3747, 0)),
	FARMING_GUILD_CELASTRUS(34629, new WorldPoint(1245, 3751, 0)),
	FARMING_GUILD_REDWOOD(34059, new WorldPoint(1232, 3753, 0)),
	PRIFF_CRYSTAL_TREE(34906, new WorldPoint(3291, 6118, 0)),
	CANIFIS_MUSHROOM(8337, new WorldPoint(3452, 3472, 0)),
	AUBURNVALE_BELLADONNA(56960, new WorldPoint(1450, 3353, 0)),
	DRAYNOR_BELLADONNA(7572, new WorldPoint(3087, 3354, 0)),
	VARLAMORE_CALQUAT(56954, new WorldPoint(1366, 3031, 0), 14515),
	TAI_BWO_WANNAI_CALQUAT(7807, new WorldPoint(2796, 3100, 0), 2688);

	private static final Map<Integer, FarmingPatch> PATCHES_BY_OBJECT_ID = new HashMap<>();
	private static final Map<Integer, List<FarmingPatch>> PATCHES_BY_FARMER_ID = new HashMap<>();
	private static final Map<Long, FarmingPatch> PATCHES_BY_FARMER_AND_NUMBER = new HashMap<>();

	static
	{
		for (FarmingPatch patch : values())
		{
			PATCHES_BY_OBJECT_ID.put(patch.objectId, patch);
			if (patch.farmerId > 0)
			{
				PATCHES_BY_FARMER_ID.computeIfAbsent(patch.farmerId, ignored -> new ArrayList<>()).add(patch);
				if (patch.patchNumber >= 0)
				{
					PATCHES_BY_FARMER_AND_NUMBER.put(farmerPatchKey(patch.farmerId, patch.patchNumber), patch);
				}
			}
		}
	}

	private final int objectId;
	private final WorldPoint tile;
	private final int farmerId;
	private final int patchNumber;

	FarmingPatch(int objectId, WorldPoint tile)
	{
		this(objectId, tile, -1, -1);
	}

	FarmingPatch(int objectId, WorldPoint tile, int farmerId)
	{
		this(objectId, tile, farmerId, -1);
	}

	FarmingPatch(int objectId, WorldPoint tile, int farmerId, int patchNumber)
	{
		this.objectId = objectId;
		this.tile = tile;
		this.farmerId = farmerId;
		this.patchNumber = patchNumber;
	}

	static FarmingPatch fromObjectId(int objectId)
	{
		return PATCHES_BY_OBJECT_ID.get(objectId);
	}

	static FarmingPatch fromFarmer(int farmerId, int patchNumber, WorldPoint anchor)
	{
		if (patchNumber >= 0)
		{
			FarmingPatch numberedPatch = PATCHES_BY_FARMER_AND_NUMBER.get(farmerPatchKey(farmerId, patchNumber));
			if (numberedPatch != null && numberedPatch.supportsProtection())
			{
				return numberedPatch;
			}
		}

		List<FarmingPatch> patches = PATCHES_BY_FARMER_ID.get(farmerId);
		if (patches == null || patches.isEmpty())
		{
			return null;
		}

		if (patches.size() == 1 || anchor == null)
		{
			FarmingPatch onlyPatch = patches.get(0);
			return onlyPatch.supportsProtection() ? onlyPatch : null;
		}

		FarmingPatch nearestPatch = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (FarmingPatch patch : patches)
		{
			if (!patch.supportsProtection())
			{
				continue;
			}

			if (patch.tile.getPlane() != anchor.getPlane())
			{
				continue;
			}

			int distance = patch.tile.distanceTo(anchor);
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearestPatch = patch;
			}
		}

		return nearestPatch;
	}

	static FarmingPatch nearestTo(WorldPoint point, int maxDistance)
	{
		if (point == null)
		{
			return null;
		}

		FarmingPatch nearestPatch = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (FarmingPatch patch : values())
		{
			WorldPoint tile = patch.tile;
			if (tile.getPlane() != point.getPlane())
			{
				continue;
			}

			int distance = tile.distanceTo(point);
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearestPatch = patch;
			}
		}

		return nearestDistance <= maxDistance ? nearestPatch : null;
	}

	static FarmingPatch nearestProtectableTo(WorldPoint point, int maxDistance)
	{
		if (point == null)
		{
			return null;
		}

		FarmingPatch nearestPatch = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (FarmingPatch patch : values())
		{
			if (!patch.supportsProtection())
			{
				continue;
			}

			WorldPoint tile = patch.tile;
			if (tile.getPlane() != point.getPlane())
			{
				continue;
			}

			int distance = tile.distanceTo(point);
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearestPatch = patch;
			}
		}

		return nearestDistance <= maxDistance ? nearestPatch : null;
	}

	WorldPoint getTile()
	{
		return tile;
	}

	boolean supportsProtection()
	{
		String patchName = name();
		return !patchName.contains("_HERB")
			&& !patchName.contains("_FLOWER")
			&& !patchName.contains("SEAWEED")
			&& !patchName.contains("BELLADONNA")
			&& !patchName.contains("MUSHROOM");
	}

	boolean isAllotment()
	{
		return name().contains("ALLOTMENT");
	}

	private static long farmerPatchKey(int farmerId, int patchNumber)
	{
		return (((long) farmerId) << 32) | (patchNumber & 0xFFFFFFFFL);
	}
}
