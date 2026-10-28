package com.arber.skillingtoolkit;

import java.awt.Color;
import java.util.Locale;
import net.runelite.client.util.Text;

enum ImplingType
{
	BABY("Baby", "implingBaby", new Color(180, 235, 255), false),
	YOUNG("Young", "implingYoung", new Color(130, 220, 130), false),
	GOURMET("Gourmet", "implingGourmet", new Color(255, 180, 110), false),
	EARTH("Earth", "implingEarth", new Color(150, 115, 70), false),
	ESSENCE("Essence", "implingEssence", new Color(185, 160, 255), false),
	ECLECTIC("Eclectic", "implingEclectic", new Color(255, 220, 90), false),
	NATURE("Nature", "implingNature", new Color(90, 235, 120), true),
	MAGPIE("Magpie", "implingMagpie", new Color(220, 220, 230), true),
	NINJA("Ninja", "implingNinja", new Color(255, 95, 95), true),
	DRAGON("Dragon", "implingDragon", new Color(255, 110, 35), true),
	LUCKY("Lucky", "implingLucky", new Color(120, 255, 245), true),
	CRYSTAL("Crystal", "implingCrystal", new Color(150, 210, 255), true);

	private final String displayName;
	private final String configKey;
	private final Color color;
	private final boolean defaultEnabled;
	private final String matchName;

	ImplingType(String displayName, String configKey, Color color, boolean defaultEnabled)
	{
		this.displayName = displayName;
		this.configKey = configKey;
		this.color = color;
		this.defaultEnabled = defaultEnabled;
		this.matchName = (displayName + " impling").toLowerCase(Locale.ENGLISH);
	}

	String getDisplayName()
	{
		return displayName;
	}

	String getConfigKey()
	{
		return configKey;
	}

	Color getColor()
	{
		return color;
	}

	boolean isDefaultEnabled()
	{
		return defaultEnabled;
	}

	static ImplingType fromNpcName(String npcName)
	{
		if (npcName == null || npcName.isBlank())
		{
			return null;
		}

		String name = Text.removeTags(npcName).toLowerCase(Locale.ENGLISH).trim();
		for (ImplingType type : values())
		{
			if (name.equals(type.matchName) || name.contains(type.matchName))
			{
				return type;
			}
		}

		return null;
	}
}
