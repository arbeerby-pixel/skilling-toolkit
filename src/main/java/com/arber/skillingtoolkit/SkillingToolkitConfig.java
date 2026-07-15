package com.arber.skillingtoolkit;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(SkillingToolkitConfig.GROUP)
public interface SkillingToolkitConfig extends Config
{
	String GROUP = "skillingToolkit";

	@ConfigSection(
		name = "General",
		description = "Configure shared Skilling Toolkit behavior.",
		position = 0
	)
	String generalSection = "generalSection";

	@ConfigSection(
		name = "Farming",
		description = "Configure farming helpers.",
		position = 1
	)
	String farmingSection = "farmingSection";

	@ConfigSection(
		name = "Hunter",
		description = "Configure Hunter helpers.",
		position = 2
	)
	String hunterSection = "hunterSection";

	@ConfigSection(
		name = "Interface",
		description = "Configure interface helpers.",
		position = 3
	)
	String interfaceSection = "interfaceSection";

	@ConfigSection(
		name = "Trackers",
		description = "Configure chat tab trackers.",
		position = 4
	)
	String trackerSection = "trackerSection";

	@ConfigItem(
		keyName = "markerResetTarget",
		name = "Reset markers",
		description = "Choose which saved overlay markers should be reset.",
		section = generalSection,
		position = 0
	)
	default MarkerResetTarget markerResetTarget()
	{
		return MarkerResetTarget.ALL;
	}

	@ConfigItem(
		keyName = "resetSelectedMarkers",
		name = "Reset selected markers",
		description = "Clear the saved markers selected above.",
		section = generalSection,
		position = 1
	)
	default boolean resetSelectedMarkers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showCompostIndicator",
		name = "Compost indicator",
		description = "Show an overlay on farming patches after you apply compost, supercompost, or ultracompost.",
		section = farmingSection,
		position = 0
	)
	default boolean showCompostIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNeedsCompostIcon",
		name = "Needs compost icon",
		description = "Show a grey bottomless compost bucket on patches that still need compost.",
		section = farmingSection,
		position = 1
	)
	default boolean showNeedsCompostIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCompostIcon",
		name = "Compost icon",
		description = "Show the colored bottomless compost bucket icon on composted farming patches.",
		section = farmingSection,
		position = 2
	)
	default boolean showCompostIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "clearCompostOnHarvest",
		name = "Clear on harvest",
		description = "Clear the compost marker when harvesting or clearing the same patch.",
		section = farmingSection,
		position = 3
	)
	default boolean clearCompostOnHarvest()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCropProtectionIndicator",
		name = "Crop protection indicator",
		description = "Show a green check on farming patches after you pay a gardener to protect crops.",
		section = farmingSection,
		position = 4
	)
	default boolean showCropProtectionIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPatchTimers",
		name = "Anima patch timer",
		description = "Show Attas, Iasor, and Kronos decay timers as hours and minutes on the Anima patch.",
		section = farmingSection,
		position = 5
	)
	default boolean showPatchTimers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBirdhouseSeedIndicator",
		name = "Birdhouse seed indicator",
		description = "Show a green check on bird houses after seeds have been added.",
		section = hunterSection,
		position = 0
	)
	default boolean showBirdhouseSeedIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "tradeButtonClock",
		name = "Trade button clock",
		description = "Replace the Trade chat button text with the local time in 24-hour format.",
		section = interfaceSection,
		position = 0
	)
	default boolean tradeButtonClock()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInventorySpaces",
		name = "Inventory spaces",
		description = "Show an infobox with the number of free inventory spaces. Hover it to see inventory value.",
		section = interfaceSection,
		position = 1
	)
	default boolean showInventorySpaces()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHerbSackViewer",
		name = "Herb sack viewer",
		description = "Show the last checked herb sack contents and estimated value when hovering the herb sack.",
		section = interfaceSection,
		position = 2
	)
	default boolean showHerbSackViewer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "priceSource",
		name = "Value price source",
		description = "Choose the prices used for inventory value and value trackers.",
		section = interfaceSection,
		position = 3
	)
	default ToolkitPriceSource priceSource()
	{
		return ToolkitPriceSource.GE_GUIDE;
	}

	@ConfigItem(
		keyName = "trackerMode",
		name = "Tracker mode",
		description = "Session resets all trackers when RuneLite starts. Forever continues all trackers from the saved values.",
		section = trackerSection,
		position = 0
	)
	default TrackerPersistenceMode trackerMode()
	{
		return TrackerPersistenceMode.FOREVER;
	}

	@ConfigItem(
		keyName = "xpTracker",
		name = "XP tracker",
		description = "Show skilling XP gained in the dynamic chat tab trackers.",
		section = trackerSection,
		position = 1
	)
	default boolean xpTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "topSkillTracker",
		name = "Top skill tracker",
		description = "Show the skilling skill with the most tracked XP in the dynamic chat tab trackers.",
		section = trackerSection,
		position = 2
	)
	default boolean topSkillTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "profitTracker",
		name = "Profit tracker",
		description = "Show estimated value gained from skilling inventory changes.",
		section = trackerSection,
		position = 3
	)
	default boolean profitTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "costTracker",
		name = "Cost tracker",
		description = "Show estimated value spent from skilling inventory changes.",
		section = trackerSection,
		position = 4
	)
	default boolean costTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "actionsTracker",
		name = "Actions tracker",
		description = "Show the number of skilling XP events tracked this session.",
		section = trackerSection,
		position = 5
	)
	default boolean actionsTracker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "resetTrackerTarget",
		name = "Reset tracker",
		description = "Choose which tracker should be reset.",
		section = trackerSection,
		position = 6
	)
	default TrackerResetTarget resetTrackerTarget()
	{
		return TrackerResetTarget.XP;
	}

	@ConfigItem(
		keyName = "resetSelectedTracker",
		name = "Reset selected tracker",
		description = "Reset the tracker selected above.",
		section = trackerSection,
		position = 7
	)
	default boolean resetSelectedTracker()
	{
		return false;
	}

	@ConfigItem(
		keyName = "savedXpTrackerValuesV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedXpTrackerValues()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedProfitTrackerValue",
		name = "",
		description = "",
		hidden = true
	)
	default String savedProfitTrackerValue()
	{
		return "0";
	}

	@ConfigItem(
		keyName = "savedCostTrackerValue",
		name = "",
		description = "",
		hidden = true
	)
	default String savedCostTrackerValue()
	{
		return "0";
	}

	@ConfigItem(
		keyName = "savedActionsTrackerValue",
		name = "",
		description = "",
		hidden = true
	)
	default String savedActionsTrackerValue()
	{
		return "0";
	}

	@ConfigItem(
		keyName = "savedStatsDayV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsDay()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsWeekV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsWeek()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsMonthV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsMonth()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsYearV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsYear()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedStatsAllTimeV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedStatsAllTime()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedCompostPatchesV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedCompostPatches()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedNeedsCompostPatchesV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedNeedsCompostPatches()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedProtectedPatchesV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedProtectedPatches()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedPatchTimersV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedPatchTimers()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedSeededBirdhousesV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedSeededBirdhouses()
	{
		return "";
	}

	@ConfigItem(
		keyName = "savedChatTabTrackerOrderV1",
		name = "",
		description = "",
		hidden = true
	)
	default String savedChatTabTrackerOrder()
	{
		return "";
	}
}
