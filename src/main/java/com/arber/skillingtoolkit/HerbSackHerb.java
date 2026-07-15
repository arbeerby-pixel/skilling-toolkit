package com.arber.skillingtoolkit;

import java.util.Locale;
import net.runelite.api.ItemID;

enum HerbSackHerb
{
	GRIMY_GUAM_LEAF(ItemID.GRIMY_GUAM_LEAF, "Grimy guam leaf"),
	GRIMY_MARRENTILL(ItemID.GRIMY_MARRENTILL, "Grimy marrentill"),
	GRIMY_TARROMIN(ItemID.GRIMY_TARROMIN, "Grimy tarromin"),
	GRIMY_HARRALANDER(ItemID.GRIMY_HARRALANDER, "Grimy harralander"),
	GRIMY_RANARR_WEED(ItemID.GRIMY_RANARR_WEED, "Grimy ranarr weed"),
	GRIMY_TOADFLAX(ItemID.GRIMY_TOADFLAX, "Grimy toadflax"),
	GRIMY_IRIT_LEAF(ItemID.GRIMY_IRIT_LEAF, "Grimy irit leaf"),
	GRIMY_AVANTOE(ItemID.GRIMY_AVANTOE, "Grimy avantoe"),
	GRIMY_KWUARM(ItemID.GRIMY_KWUARM, "Grimy kwuarm"),
	GRIMY_SNAPDRAGON(ItemID.GRIMY_SNAPDRAGON, "Grimy snapdragon"),
	GRIMY_CADANTINE(ItemID.GRIMY_CADANTINE, "Grimy cadantine"),
	GRIMY_LANTADYME(ItemID.GRIMY_LANTADYME, "Grimy lantadyme"),
	GRIMY_DWARF_WEED(ItemID.GRIMY_DWARF_WEED, "Grimy dwarf weed"),
	GRIMY_TORSTOL(ItemID.GRIMY_TORSTOL, "Grimy torstol"),
	GRIMY_NOXIFER(ItemID.GRIMY_NOXIFER, "Grimy noxifer"),
	GRIMY_GOLPAR(ItemID.GRIMY_GOLPAR, "Grimy golpar"),
	GRIMY_BUCHU_LEAF(ItemID.GRIMY_BUCHU_LEAF, "Grimy buchu leaf"),
	GRIMY_HUASCA(ItemID.GRIMY_HUASCA, "Grimy huasca");

	private final int itemId;
	private final String displayName;
	private final String normalizedName;

	HerbSackHerb(int itemId, String displayName)
	{
		this.itemId = itemId;
		this.displayName = displayName;
		this.normalizedName = normalize(displayName);
	}

	int getItemId()
	{
		return itemId;
	}

	String getDisplayName()
	{
		return displayName;
	}

	static HerbSackHerb fromName(String name)
	{
		String normalized = normalize(name);
		for (HerbSackHerb herb : values())
		{
			if (herb.normalizedName.equals(normalized))
			{
				return herb;
			}
		}

		return null;
	}

	private static String normalize(String text)
	{
		return text == null
			? ""
			: text.toLowerCase(Locale.ENGLISH)
				.replace(".", "")
				.replace(",", "")
				.trim()
				.replaceAll("\\s+", " ");
	}
}
