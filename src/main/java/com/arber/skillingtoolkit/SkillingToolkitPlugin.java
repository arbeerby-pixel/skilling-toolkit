package com.arber.skillingtoolkit;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostClientTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Skilling Toolkit",
	configName = SkillingToolkitConfig.GROUP,
	description = "Adds Farming and Hunter helpers, skilling trackers, inventory helpers, and a Trade-tab clock.",
	tags = {"skilling", "farming", "compost", "protection", "birdhouse", "hunter", "herb", "sack", "tracker", "xp", "profit", "cost", "clock", "inventory"}
)
public class SkillingToolkitPlugin extends Plugin
{
	private static final int PATCH_CLEAR_MESSAGE_TICK_WINDOW = 8;
	private static final int PROTECTION_INTERACTION_TICK_WINDOW = 150;
	private static final int PATCH_READY_CLEAR_DISTANCE = 5;
	private static final int TRACKED_PATCH_NORMALIZE_DISTANCE = 5;
	private static final int KNOWN_PATCH_NORMALIZE_DISTANCE = 3;
	private static final int PROTECTION_PAYMENT_FALLBACK_DISTANCE = 10;
	private static final int HARVEST_SCENE_SCAN_INTERVAL = 5;
	private static final int INVENTORY_SIZE = 28;
	private static final int INVENTORY_TRACKER_IGNORE_TICKS = 10;
	private static final int STORAGE_INVENTORY_TRACKER_IGNORE_TICKS = 35;
	private static final int IGNORED_INVENTORY_RETURN_TICKS = 12;
	private static final int PENDING_INVENTORY_VALUE_TICKS = 8;
	private static final int PENDING_HERB_CLEANING_TICKS = 4;
	private static final int ACTIVE_SKILL_INVENTORY_TICKS = 8;
	private static final int SUSPICIOUS_EMPTY_INVENTORY_ITEM_TYPES = 8;
	private static final long SUSPICIOUS_VALUE_REPLACEMENT_MIN_GP = 1_000_000L;
	private static final long SUSPICIOUS_VALUE_REPLACEMENT_VERY_LARGE_GP = 5_000_000L;
	private static final int SUSPICIOUS_VALUE_REPLACEMENT_MIN_TYPES = 2;
	private static final int SUSPICIOUS_VALUE_REPLACEMENT_CLOSE_PERCENT = 85;
	private static final Pattern CHARGED_ITEM_NAME = Pattern.compile("^(.+?)\\s*\\((\\d+)\\)$");
	private static final int TOOLKIT_NAVIGATION_PRIORITY = 1;
	private static final int TOOLKIT_NAVIGATION_ICON_SIZE = 24;
	private static final int PROFIT_TRACKER_COLOR = 0x00FF00;
	private static final int COST_TRACKER_COLOR = 0xFF4040;
	private static final int XP_TRACKER_COLOR = 0xFFFF00;
	private static final int ACTIONS_TRACKER_COLOR = 0x40E0FF;
	private static final int DEFAULT_CHAT_TAB_COLOR = 0xFFFFFF;
	private static final int CHAT_TAB_VALUE_Y_OFFSET = 0;
	private static final int CHAT_TAB_LABEL_Y_OFFSET = 10;
	private static final int CHAT_TAB_TRACKER_TEXT_HEIGHT = 14;
	private static final int CHAT_TAB_LABEL_COLOR = 0xC8BFA7;
	private static final String SAVED_XP_TRACKER_KEY = "savedXpTrackerValuesV1";
	private static final String SAVED_PROFIT_TRACKER_KEY = "savedProfitTrackerValue";
	private static final String SAVED_COST_TRACKER_KEY = "savedCostTrackerValue";
	private static final String SAVED_ACTIONS_TRACKER_KEY = "savedActionsTrackerValue";
	private static final String SAVED_COMPOST_PATCHES_KEY = "savedCompostPatchesV1";
	private static final String SAVED_NEEDS_COMPOST_PATCHES_KEY = "savedNeedsCompostPatchesV1";
	private static final String SAVED_PROTECTED_PATCHES_KEY = "savedProtectedPatchesV1";
	private static final String SAVED_PATCH_TIMERS_KEY = "savedPatchTimersV1";
	private static final String SAVED_SEEDED_BIRDHOUSES_KEY = "savedSeededBirdhousesV1";
	private static final String SAVED_CHAT_TAB_TRACKER_ORDER_KEY = "savedChatTabTrackerOrderV1";
	private static final String SAVED_STATS_DAY_KEY = "savedStatsDayV1";
	private static final String SAVED_STATS_WEEK_KEY = "savedStatsWeekV1";
	private static final String SAVED_STATS_MONTH_KEY = "savedStatsMonthV1";
	private static final String SAVED_STATS_YEAR_KEY = "savedStatsYearV1";
	private static final String SAVED_STATS_ALL_TIME_KEY = "savedStatsAllTimeV1";
	private static final String TOOLKIT_UI_COORDINATION_GROUP = "arberToolkitUi";
	private static final String TOOLKIT_UI_OWNER_KEY = "activeOwner";
	private static final String TOOLKIT_UI_OWNER_TICK_KEY = "activeOwnerTick";
	private static final String TOOLKIT_UI_LAST_TOGGLE_TICK_KEY = "lastToggleTick";
	private static final String TOOLKIT_UI_OWNER_SKILLING = "SKILLING";
	private static final String TOOLKIT_UI_OWNER_PVM = "PVM";
	private static final String SKILLING_PLUGIN_CLASS_NAME = "com.arber.skillingtoolkit.SkillingToolkitPlugin";
	private static final String PVM_PLUGIN_CLASS_NAME = "com.arber.prayerpottimer.CombatPotionTimersPlugin";
	private static final int TOOLKIT_UI_OWNER_GRACE_TICKS = 2;
	private static final String DID_I_COMPOST_PLUGIN_CLASS_NAME = "com.compost.DidICompostPlugin";
	private static final DateTimeFormatter TRADE_CLOCK_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final int PATCH_TIMER_INTERACTION_TICK_WINDOW = 12;
	private static final int FARMING_STATS_INTERACTION_TICK_WINDOW = 150;
	private static final int ANIMA_PATCH_VARBIT = 7911;
	private static final int ANIMA_STAGE_COUNT = 9;
	private static final int ANIMA_STAGE_MINUTES = 640;
	private static final String ANIMA_ATTAS = "attas";
	private static final String ANIMA_IASOR = "iasor";
	private static final String ANIMA_KRONOS = "kronos";
	private static final Pattern COMPOST_USED_ON_PATCH = Pattern.compile(
		"you treat the .+ with (?<compostType>ultra|super|)compost\\.");
	private static final Pattern FERTILE_SOIL_CAST = Pattern.compile(
		"^the .+ has been treated with (?<compostType>ultra|super|)compost.*");
	private static final Pattern ALREADY_TREATED = Pattern.compile(
		"this .+ has already been (treated|fertilised) with (?<compostType>ultra|super|)compost(?: - the spell can't make it any more fertile)?\\.");
	private static final Pattern INSPECT_PATCH_COMPOSTED = Pattern.compile(
		"this is an? .+\\. the soil has been treated with (?<compostType>ultra|super|)compost\\..*");
	private static final Pattern INSPECT_PATCH_NEEDS_COMPOST = Pattern.compile(
		"this is an? .+\\. the soil has not been treated.*");
	private static final Pattern CLEAR_HERB_PATCH = Pattern.compile("the herb patch is now empty.*");
	private static final Pattern CLEAR_PATCH = Pattern.compile("you have successfully cleared this patch for new crops.*");
	private static final Pattern CLEAR_TREE = Pattern.compile("you examine the tree for signs of disease and find that it is in perfect health.*");
	private static final Pattern CLEAR_ALLOTMENT = Pattern.compile("the allotment is now empty.*");
	private static final Pattern CLEAR_SEAWEED = Pattern.compile("you pick some giant seaweed.*");
	private static final Pattern CLEAR_BELLADONNA = Pattern.compile("you pick some deadly nightshade.*");
	private static final Pattern CLEAR_MUSHROOM = Pattern.compile("you pick a bittercap mushroom.*");
	private static final int BIRDHOUSE_INTERACTION_TICK_WINDOW = 150;
	private static final int BIRDHOUSE_XP_INTERACTION_TICK_WINDOW = 12;
	private static final int TRACKED_BIRDHOUSE_NORMALIZE_DISTANCE = 4;
	private static final long BIRDHOUSE_DURATION_MILLIS = 50L * 60_000L;
	private static final long BIRDHOUSE_STATS_RUN_GROUP_MILLIS = 10L * 60_000L;
	private static final long FARMING_STATS_RUN_GROUP_MILLIS = 15L * 60_000L;
	private static final int HERB_SACK_CHECK_TIMEOUT_TICKS = 4;

	private static final int[] CHAT_TAB_TRACKER_SLOT_COMPONENTS = {
		ComponentID.CHATBOX_TAB_CLAN,
		ComponentID.CHATBOX_TAB_CHANNEL,
		ComponentID.CHATBOX_TAB_PRIVATE,
		ComponentID.CHATBOX_TAB_PUBLIC,
		ComponentID.CHATBOX_TAB_GAME
	};

	private static final String[] CHAT_TAB_TRACKER_SLOT_NAMES = {
		"clan",
		"channel",
		"private",
		"public",
		"game"
	};

	static final Skill[] SKILLING_TRACKER_SKILLS = {
		Skill.AGILITY,
		Skill.CONSTRUCTION,
		Skill.COOKING,
		Skill.CRAFTING,
		Skill.FARMING,
		Skill.FIREMAKING,
		Skill.FISHING,
		Skill.FLETCHING,
		Skill.HERBLORE,
		Skill.HUNTER,
		Skill.MINING,
		Skill.RUNECRAFT,
		Skill.SMITHING,
		Skill.THIEVING,
		Skill.WOODCUTTING
	};

	private static final String[] PATCH_NAME_PARTS = {
		"allotment",
		"anima patch",
		"belladonna patch",
		"bush patch",
		"cactus patch",
		"calquat patch",
		"celastrus patch",
		"crystal tree patch",
		"flower patch",
		"fruit tree patch",
		"grapevine",
		"hardwood tree patch",
		"herb patch",
		"hespori patch",
		"hops patch",
		"mushroom patch",
		"redwood patch",
		"seaweed patch",
		"spirit tree patch",
		"tree patch",
		"vine patch"
	};

	private static final String[] FARMING_CROP_NAME_PARTS = {
		"allotment",
		"apple tree",
		"asgarnian hops",
		"avantoe",
		"banana tree",
		"barley",
		"belladonna",
		"cadantine",
		"cadavaberry",
		"calquat",
		"calquat fruit",
		"cactus",
		"cactus spine",
		"celastrus",
		"coconut",
		"cooking apple",
		"crystal tree",
		"curry leaf",
		"curry leaves",
		"curry tree",
		"diseased",
		"dragonfruit",
		"dragonfruit tree",
		"dwarf weed",
		"dwellberry",
		"dwellberries",
		"flower",
		"grapevine",
		"guam",
		"hammerstone hops",
		"harralander",
		"herb",
		"herbs",
		"hespori",
		"hops",
		"irit",
		"jangerberry",
		"jangerberries",
		"jute",
		"krandorian hops",
		"kwuarm",
		"lantadyme",
		"limpwurt",
		"magic tree",
		"mahogany",
		"maple tree",
		"marigold",
		"marrentill",
		"mushroom",
		"nasturtium",
		"oak tree",
		"onion",
		"orange tree",
		"palm tree",
		"papaya tree",
		"papaya fruit",
		"pineapple plant",
		"poison ivy berries",
		"poison ivy",
		"potato",
		"potato cactus",
		"ranarr",
		"redberry",
		"redberries",
		"redwood",
		"rosemary",
		"sapling",
		"seaweed",
		"seedling",
		"snape grass",
		"snapdragon",
		"spirit tree",
		"strawberry",
		"sweetcorn",
		"tarromin",
		"teak",
		"toadflax",
		"tomato",
		"torstol",
		"watermelon",
		"whiteberry",
		"white berries",
		"white lily",
		"white tree fruit",
		"wildblood hops",
		"willow tree",
		"woad",
		"yanillian hops",
		"yew tree"
	};

	private static final String[] STRONG_FARMING_ACTION_PARTS = {
		"check-health",
		"compost",
		"cure",
		"harvest",
		"prune",
		"rake",
		"water"
	};

	private static final String[] HARVESTABLE_FARMING_ACTION_PARTS = {
		"check-health",
		"harvest",
		"pick"
	};

	private static final Map<String, PatchGrowthInfo> PATCH_GROWTH_TIMES = createPatchGrowthTimes();
	private static final Set<Integer> HERB_SEED_ITEMS = immutableItemSet(
		ItemID.GUAM_SEED,
		ItemID.MARRENTILL_SEED,
		ItemID.TARROMIN_SEED,
		ItemID.HARRALANDER_SEED,
		ItemID.RANARR_SEED,
		ItemID.TOADFLAX_SEED,
		ItemID.IRIT_SEED,
		ItemID.AVANTOE_SEED,
		ItemID.KWUARM_SEED,
		ItemID.SNAPDRAGON_SEED,
		ItemID.CADANTINE_SEED,
		ItemID.LANTADYME_SEED,
		ItemID.DWARF_WEED_SEED,
		ItemID.TORSTOL_SEED,
		ItemID.NOXIFER_SEED,
		ItemID.GOLPAR_SEED,
		ItemID.BUCHU_SEED,
		ItemID.HUASCA_SEED
	);
	private static final Set<Integer> HERB_HARVEST_ITEMS = immutableItemSet(
		ItemID.GRIMY_GUAM_LEAF,
		ItemID.GUAM_LEAF,
		ItemID.GRIMY_MARRENTILL,
		ItemID.MARRENTILL,
		ItemID.GRIMY_TARROMIN,
		ItemID.TARROMIN,
		ItemID.GRIMY_HARRALANDER,
		ItemID.HARRALANDER,
		ItemID.GRIMY_RANARR_WEED,
		ItemID.RANARR_WEED,
		ItemID.GRIMY_TOADFLAX,
		ItemID.TOADFLAX,
		ItemID.GRIMY_IRIT_LEAF,
		ItemID.IRIT_LEAF,
		ItemID.GRIMY_AVANTOE,
		ItemID.AVANTOE,
		ItemID.GRIMY_KWUARM,
		ItemID.KWUARM,
		ItemID.GRIMY_SNAPDRAGON,
		ItemID.SNAPDRAGON,
		ItemID.GRIMY_CADANTINE,
		ItemID.CADANTINE,
		ItemID.GRIMY_LANTADYME,
		ItemID.LANTADYME,
		ItemID.GRIMY_DWARF_WEED,
		ItemID.DWARF_WEED,
		ItemID.GRIMY_TORSTOL,
		ItemID.TORSTOL,
		ItemID.GRIMY_NOXIFER,
		ItemID.NOXIFER,
		ItemID.GRIMY_GOLPAR,
		ItemID.GOLPAR,
		ItemID.GRIMY_BUCHU_LEAF,
		ItemID.BUCHU_LEAF,
		ItemID.GRIMY_HUASCA,
		ItemID.HUASCA
	);
	private static final Set<Integer> ALLOTMENT_SEED_ITEMS = immutableItemSet(
		ItemID.POTATO_SEED,
		ItemID.ONION_SEED,
		ItemID.CABBAGE_SEED,
		ItemID.TOMATO_SEED,
		ItemID.SWEETCORN_SEED,
		ItemID.STRAWBERRY_SEED,
		ItemID.WATERMELON_SEED,
		ItemID.SNAPE_GRASS_SEED
	);
	private static final Set<Integer> ALLOTMENT_HARVEST_ITEMS = immutableItemSet(
		ItemID.POTATO,
		ItemID.ONION,
		ItemID.CABBAGE,
		ItemID.TOMATO,
		ItemID.SWEETCORN,
		ItemID.STRAWBERRY,
		ItemID.WATERMELON,
		ItemID.SNAPE_GRASS
	);
	private static final Set<Integer> HOPS_SEED_ITEMS = immutableItemSet(
		ItemID.BARLEY_SEED,
		ItemID.HAMMERSTONE_SEED,
		ItemID.ASGARNIAN_SEED,
		ItemID.JUTE_SEED,
		ItemID.YANILLIAN_SEED,
		ItemID.KRANDORIAN_SEED,
		ItemID.WILDBLOOD_SEED,
		ItemID.KELDA_SEED
	);
	private static final Set<Integer> HOPS_HARVEST_ITEMS = immutableItemSet(
		ItemID.BARLEY,
		ItemID.HAMMERSTONE_HOPS,
		ItemID.ASGARNIAN_HOPS,
		ItemID.JUTE_FIBRE,
		ItemID.YANILLIAN_HOPS,
		ItemID.KRANDORIAN_HOPS,
		ItemID.WILDBLOOD_HOPS,
		ItemID.KELDA_HOPS
	);
	private final Map<WorldPoint, CompostPatch> compostedPatches = new HashMap<>();
	private final Set<WorldPoint> needsCompostPatches = new HashSet<>();
	private final Map<WorldPoint, Integer> protectedPatches = new HashMap<>();
	private final Map<WorldPoint, PatchTimer> patchTimers = new HashMap<>();
	private final Set<WorldPoint> seededBirdhouses = new HashSet<>();
	private final Map<WorldPoint, Long> birdhouseReadyAtMillis = new HashMap<>();
	private final Set<WorldPoint> visibleFarmingPatches = new HashSet<>();
	private final Set<WorldPoint> visibleAnimaPatches = new HashSet<>();
	private final Set<WorldPoint> activeProtectableFarmingPatches = new HashSet<>();
	private final Map<Integer, String> chatTabOriginalTexts = new HashMap<>();
	private final Map<Integer, ChatTabTextStyle> chatTabOriginalStyles = new HashMap<>();
	private final Map<Integer, String> activeChatTabTexts = new HashMap<>();
	private final Map<Integer, ChatTabTrackerType> activeChatTabTrackerSlots = new HashMap<>();
	private final Set<Integer> activeChatTabOverrides = new HashSet<>();
	private final List<ChatTabTrackerType> activeChatTabTrackerOrder = new ArrayList<>();
	private final EnumMap<Skill, Integer> lastSkillExperience = new EnumMap<>(Skill.class);
	private final EnumMap<Skill, Integer> trackerBaseExperience = new EnumMap<>(Skill.class);
	private final EnumMap<Skill, Long> xpGainedBySkill = new EnumMap<>(Skill.class);
	private final EnumMap<SkillingToolkitStatsPeriod, SkillingToolkitStats> statsByPeriod = new EnumMap<>(SkillingToolkitStatsPeriod.class);
	private final EnumMap<FarmingActivityType, Long> lastFarmingStatsRunAtMillis = new EnumMap<>(FarmingActivityType.class);
	private final Map<Integer, Long> lastInventoryQuantities = new HashMap<>();
	private final Map<Integer, Long> ignoredInventoryReturns = new HashMap<>();
	private final Map<Integer, Integer> ignoredInventoryReturnExpiryTicks = new HashMap<>();
	private final List<PendingInventoryValueChange> pendingInventoryValueChanges = new ArrayList<>();
	private final List<PendingHerbCleaning> pendingHerbCleanings = new ArrayList<>();
	private final HerbSackContents herbSackContents = new HerbSackContents();
	private final EnumMap<HerbSackHerb, Integer> pendingHerbSackQuantities = new EnumMap<>(HerbSackHerb.class);

	@Inject
	private Client client;

	@Inject
	private SkillingToolkitConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SkillingToolkitOverlay overlay;

	@Inject
	private HerbSackOverlay herbSackOverlay;

	private InventorySpacesInfoBox inventorySpacesInfoBox;
	private SkillingToolkitPanel toolkitPanel;
	private NavigationButton toolkitNavigationButton;
	private FarmingPatch currentFarmingPatch;
	private FarmingPatch currentCompostPatch;
	private WorldPoint lastPatchInteraction;
	private int lastPatchInteractionTick;
	private WorldPoint lastBirdhouseInteraction;
	private int lastBirdhouseInteractionTick;
	private long lastBirdhouseStatsRunAtMillis;
	private WorldPoint lastProtectionInteraction;
	private WorldPoint lastProtectionPatchCandidate;
	private int lastProtectionInteractionTick;
	private int lastProtectionPatchNumber = -1;
	private int tickCount;
	private int lastHarvestSceneScanTick = -HARVEST_SCENE_SCAN_INTERVAL;
	private boolean trackerValuesLoaded;
	private boolean inventoryTrackerBaselineReady;
	private int inventoryTrackerIgnoreTicks;
	private int pendingEquipmentTransferTick = Integer.MIN_VALUE;
	private Skill activeSkillingSkill;
	private int lastSkillingXpTick = Integer.MIN_VALUE;
	private boolean readingHerbSackContents;
	private int herbSackCheckTick = Integer.MIN_VALUE;
	private long profitValue;
	private long costValue;
	private long actionsValue;
	private String lastTradeClockText = "";
	private String lastProtectionDialogText = "";

	@Provides
	SkillingToolkitConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillingToolkitConfig.class);
	}

	private void addToolkitNavigation()
	{
		if (toolkitNavigationButton != null)
		{
			return;
		}

		toolkitPanel = new SkillingToolkitPanel(this);
		toolkitNavigationButton = NavigationButton.builder()
			.tooltip("Skilling Toolkit")
			.icon(loadNavigationIcon())
			.panel(toolkitPanel)
			.priority(TOOLKIT_NAVIGATION_PRIORITY)
			.build();
		clientToolbar.addNavigation(toolkitNavigationButton);
		if (isDeveloperMode())
		{
			SwingUtilities.invokeLater(() -> clientToolbar.openPanel(toolkitNavigationButton));
		}
	}

	private void removeToolkitNavigation()
	{
		if (toolkitNavigationButton != null)
		{
			clientToolbar.removeNavigation(toolkitNavigationButton);
			toolkitNavigationButton = null;
		}

		toolkitPanel = null;
	}

	private BufferedImage loadNavigationIcon()
	{
		try
		{
			BufferedImage icon = ImageUtil.loadImageResource(SkillingToolkitPlugin.class, "/com/arber/skillingtoolkit/icon.png");
			if (icon == null)
			{
				icon = ImageUtil.loadImageResource(SkillingToolkitPlugin.class, "/icon.png");
			}

			if (icon != null)
			{
				return ImageUtil.resizeImage(icon, TOOLKIT_NAVIGATION_ICON_SIZE, TOOLKIT_NAVIGATION_ICON_SIZE, true);
			}
		}
		catch (RuntimeException ignored)
		{
			// Missing resources should not stop the plugin from starting.
		}

		return itemManager.getImage(ItemID.BOTTOMLESS_COMPOST_BUCKET_22997);
	}

	private boolean isDeveloperMode()
	{
		return System.getProperty("sun.java.command", "").contains("--developer-mode");
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(herbSackOverlay);
		if (isLoggedIn())
		{
			ignoreInventoryTrackingBriefly();
			syncInventoryTrackerBaseline();
		}
		loadCompostPatches();
		loadNeedsCompostPatches();
		loadProtectedPatches();
		loadPatchTimers();
		loadSeededBirdhouses();
		loadTrackerValues();
		loadStatsValues();
		addToolkitNavigation();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(herbSackOverlay);
		removeToolkitNavigation();
		removeInventoryInfoBox();
		restoreChatTabOverridesLater();
		persistCompostPatches();
		persistNeedsCompostPatches();
		persistProtectedPatches();
		persistPatchTimers();
		persistSeededBirdhouses();
		currentFarmingPatch = null;
		currentCompostPatch = null;
		lastPatchInteraction = null;
		lastBirdhouseInteraction = null;
		lastBirdhouseStatsRunAtMillis = 0L;
		lastProtectionInteraction = null;
		lastProtectionPatchCandidate = null;
		lastProtectionPatchNumber = -1;
		lastProtectionDialogText = "";
		visibleFarmingPatches.clear();
		visibleAnimaPatches.clear();
		activeProtectableFarmingPatches.clear();
		lastInventoryQuantities.clear();
		ignoredInventoryReturns.clear();
		ignoredInventoryReturnExpiryTicks.clear();
		pendingInventoryValueChanges.clear();
		pendingHerbCleanings.clear();
		lastFarmingStatsRunAtMillis.clear();
		pendingEquipmentTransferTick = Integer.MIN_VALUE;
		activeSkillingSkill = null;
		lastSkillingXpTick = Integer.MIN_VALUE;
		resetPendingHerbSackCheck();
		herbSackContents.forget();
		inventoryTrackerBaselineReady = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SkillingToolkitConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("tradeButtonClock".equals(event.getKey()) && !config.tradeButtonClock())
		{
			restoreChatTabOverrideLater(ComponentID.CHATBOX_TAB_TRADE);
		}

		if ("showInventorySpaces".equals(event.getKey()) && !config.showInventorySpaces())
		{
			removeInventoryInfoBox();
		}

		if ("trackerMode".equals(event.getKey()))
		{
			loadTrackerValues();
		}

		if ("resetSelectedTracker".equals(event.getKey()) && config.resetSelectedTracker())
		{
			resetSelectedTracker();
		}

		if ("resetSelectedMarkers".equals(event.getKey()) && config.resetSelectedMarkers())
		{
			resetSelectedMarkers();
		}

		if (isChatTabTrackerConfigKey(event.getKey()))
		{
			updateChatTabTrackersLater();
		}

		refreshToolkitPanel();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			ignoreInventoryTrackingBriefly();
			syncTrackerSkillBaselines();
			syncInventoryInfoBox();
			syncInventoryTrackerBaseline();
			return;
		}

		if (isResetGameState(event.getGameState()))
		{
			restoreChatTabOverrides();
			removeInventoryInfoBox();
			currentFarmingPatch = null;
			currentCompostPatch = null;
			lastPatchInteraction = null;
			lastBirdhouseInteraction = null;
			lastProtectionInteraction = null;
			lastProtectionPatchCandidate = null;
			lastProtectionPatchNumber = -1;
			lastProtectionDialogText = "";
			visibleFarmingPatches.clear();
			visibleAnimaPatches.clear();
			activeProtectableFarmingPatches.clear();
			lastInventoryQuantities.clear();
			ignoredInventoryReturns.clear();
			ignoredInventoryReturnExpiryTicks.clear();
			pendingInventoryValueChanges.clear();
			pendingHerbCleanings.clear();
			lastFarmingStatsRunAtMillis.clear();
			pendingEquipmentTransferTick = Integer.MIN_VALUE;
			activeSkillingSkill = null;
			lastSkillingXpTick = Integer.MIN_VALUE;
			resetPendingHerbSackCheck();
			inventoryTrackerBaselineReady = false;
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		tickCount++;
		if (!isLoggedIn())
		{
			return;
		}

		ensureToolkitOwnerAvailable();

		if (inventoryTrackerIgnoreTicks > 0)
		{
			inventoryTrackerIgnoreTicks--;
		}

		if (isInventoryTransferInterfaceOpen())
		{
			ignoreInventoryTrackingForStorage();
		}

		cleanupPendingInventoryValueChanges();
		cleanupPendingHerbCleanings();
		cleanupIgnoredInventoryReturns();
		pruneReadyBirdhouses();
		pruneExpiredPatchTimers();
		syncVisibleAnimaPatchTimer();
		finishPendingHerbSackCheckIfReady();
		syncInventoryInfoBox();
		syncTrackerSkillBaselines();
		detectCropProtectionDialog();
		syncVisibleFarmingPatchesAndReadyMarkers();
		refreshToolkitPanel();
	}

	@Subscribe
	public void onPostClientTick(PostClientTick event)
	{
		if (!isLoggedIn())
		{
			restoreChatTabOverrides();
			return;
		}

		updateTradeButtonClock();
		updateChatTabTrackers();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!isLoggedIn())
		{
			return;
		}

		updateTrackedSkillXp(event.getSkill(), event.getXp());
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}

		syncInventoryInfoBox();
		updateInventoryValueTrackers(event.getItemContainer());
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (handleToolkitTabToggle(event))
		{
			return;
		}

		recordHerbSackCheck(event);
		recordProtectionPaymentSelection(event);
		recordCompostPatchSelection(event);
		if (isEquipmentTransferInteraction(event))
		{
			pendingEquipmentTransferTick = tickCount;
		}
		boolean farmingWorldInteraction = isFarmingWorldInteraction(event.getMenuAction());
		String target = cleanFarmingText(event.getMenuTarget());
		boolean farmingMenuTarget = farmingWorldInteraction
			&& (isFarmingPatchName(target) || isFarmingCropName(target) || isAnimaPatchText(target));
		FarmingPatch menuPatch = farmingWorldInteraction
			? resolveMenuPatch(event.getId(), event.getParam0(), event.getParam1(), farmingMenuTarget)
			: null;
		if (menuPatch != null)
		{
			currentFarmingPatch = menuPatch;
		}
		else if (farmingMenuTarget)
		{
			currentFarmingPatch = null;
		}

		if (isStorageOrTradeInteraction(event))
		{
			ignoreInventoryTrackingForStorage();
		}
		else if (isHerbSackInventoryTransferInteraction(event))
		{
			ignoreInventoryTrackingBriefly();
		}

		recordBirdhouseInteraction(event);

		clearReadyPatchMarkerFromMenu(event.getMenuOption(), event.getMenuTarget(), event.getParam0(), event.getParam1(), menuPatch);

		if (isProtectionPaymentInteraction(event))
		{
			rememberProtectionInteraction(menuPatch);
		}

		if (menuPatch == null && !farmingMenuTarget)
		{
			return;
		}

		WorldPoint point = menuPatch == null
			? WorldPoint.fromScene(client, event.getParam0(), event.getParam1(), client.getPlane())
			: menuPatch.getTile();
		if (point == null)
		{
			return;
		}

		markSkillingActivity();
		lastPatchInteraction = normalizeToKnownPatchPoint(point);
		lastPatchInteractionTick = tickCount;

		if (!config.showCompostIndicator() && !config.showCropProtectionIndicator() && !config.showPatchTimers())
		{
			return;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		clearReadyPatchMarkerFromMenu(event.getOption(), event.getTarget(), event.getActionParam0(), event.getActionParam1(), null);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		handleHerbSackChatMessage(event);

		if (!isFarmingStatusMessageType(event.getType())
			|| (!config.showCompostIndicator()
				&& !config.showCropProtectionIndicator()
				&& !config.showBirdhouseSeedIndicator()
				&& !config.showPatchTimers()))
		{
			return;
		}

		String message = cleanFarmingText(event.getMessage());
		if (config.showBirdhouseSeedIndicator() && isBirdhouseSeededMessage(message))
		{
			markSkillingActivity();
			markLastBirdhouseSeeded();
			return;
		}

		if (config.showBirdhouseSeedIndicator() && isBirdhouseClearMessage(message))
		{
			clearLastBirdhouseSeeded();
			return;
		}

		if (config.showCropProtectionIndicator() && isCropProtectionMessage(message))
		{
			markSkillingActivity();
			markProtectedPatch(true);
		}

		if (config.showCompostIndicator() && isPatchNeedsCompostMessage(message))
		{
			markSkillingActivity();
			markNeedsCompostPatch();
			return;
		}

		CompostType compostType = config.showCompostIndicator() ? parseCompostType(message) : null;
		if (compostType != null)
		{
			markSkillingActivity();
			markLastPatch(compostType);
			return;
		}

		if ((config.clearCompostOnHarvest() || config.showPatchTimers() || config.showCropProtectionIndicator())
			&& isPatchClearMessage(message))
		{
			clearLastPatch(shouldShowNeedsCompostAfterClear(message), config.clearCompostOnHarvest());
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject object = event.getGameObject();
		ObjectComposition composition = getObjectComposition(object);
		if (composition != null && isFarmingObject(composition))
		{
			WorldPoint point = getKnownPatchPoint(object, composition);
			visibleFarmingPatches.add(point);
			WorldPoint activeProtectablePoint = getActiveProtectablePatchPoint(object, composition);
			if (activeProtectablePoint != null)
			{
				activeProtectableFarmingPatches.add(activeProtectablePoint);
			}

			if ((config.clearCompostOnHarvest() || config.showPatchTimers() || config.showCropProtectionIndicator())
				&& isHarvestableFarmingObject(composition))
			{
				markReadyPatch(point);
			}
		}

		if (composition != null && isDeadAnimaPatchObject(composition))
		{
			visibleAnimaPatches.remove(object.getWorldLocation());
			clearAnimaPatchTimer(object.getWorldLocation());
		}
		else if (composition != null && isAnimaPatchObject(composition))
		{
			visibleAnimaPatches.add(object.getWorldLocation());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject object = event.getGameObject();
		ObjectComposition composition = getObjectComposition(object);
		if (composition != null && isFarmingObject(composition))
		{
			WorldPoint point = getKnownPatchPoint(object, composition);
			visibleFarmingPatches.remove(point);
			WorldPoint activeProtectablePoint = getActiveProtectablePatchPoint(object, composition);
			if (activeProtectablePoint != null)
			{
				activeProtectableFarmingPatches.remove(activeProtectablePoint);
			}
		}

		if (composition != null && isAnimaPatchText(composition.getName()))
		{
			visibleAnimaPatches.remove(object.getWorldLocation());
		}
	}

	Map<WorldPoint, CompostPatch> getCompostedPatches()
	{
		return compostedPatches;
	}

	Set<WorldPoint> getNeedsCompostPatches()
	{
		return needsCompostPatches;
	}

	boolean shouldRenderOwnCompostIndicators()
	{
		return !isToolkitPluginEnabled(DID_I_COMPOST_PLUGIN_CLASS_NAME);
	}

	Map<WorldPoint, Integer> getProtectedPatches()
	{
		return protectedPatches;
	}

	List<PatchTimer> getPatchTimers()
	{
		List<PatchTimer> timers = new ArrayList<>();
		for (PatchTimer timer : patchTimers.values())
		{
			if (isAnimaCropName(timer.getCropName()))
			{
				timers.add(timer);
			}
		}

		return timers;
	}

	boolean shouldRenderProtectedPatch(WorldPoint patchPoint)
	{
		if (activeProtectableFarmingPatches.contains(patchPoint))
		{
			return true;
		}

		return !visibleFarmingPatches.contains(patchPoint);
	}

	boolean isAllotmentPatch(WorldPoint patchPoint)
	{
		FarmingPatch patch = FarmingPatch.nearestTo(patchPoint, KNOWN_PATCH_NORMALIZE_DISTANCE);
		return patch != null && patch.isAllotment();
	}

	Set<WorldPoint> getSeededBirdhouses()
	{
		return seededBirdhouses;
	}

	boolean isFarmingPatch(GameObject object)
	{
		ObjectComposition composition = getObjectComposition(object);
		return composition != null && isFarmingObject(composition);
	}

	private void syncInventoryInfoBox()
	{
		if (!isLoggedIn() || !config.showInventorySpaces() || !isToolkitUiReady())
		{
			removeInventoryInfoBox();
			return;
		}

		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			removeInventoryInfoBox();
			return;
		}

		int occupiedSlots = getOccupiedInventorySlots(inventory);
		if (occupiedSlots == 0)
		{
			removeInventoryInfoBox();
			return;
		}

		int freeSpaces = Math.max(0, INVENTORY_SIZE - occupiedSlots);
		long inventoryValue = getInventoryValue(inventory);
		if (inventorySpacesInfoBox == null)
		{
			inventorySpacesInfoBox = new InventorySpacesInfoBox(freeSpaces, this);
			infoBoxManager.addInfoBox(inventorySpacesInfoBox);
		}
		else if (inventorySpacesInfoBox.setFreeSpaces(freeSpaces))
		{
			infoBoxManager.updateInfoBoxImage(inventorySpacesInfoBox);
		}

		inventorySpacesInfoBox.setTooltip(buildInventoryTooltip(inventoryValue));
	}

	private void removeInventoryInfoBox()
	{
		if (inventorySpacesInfoBox != null)
		{
			infoBoxManager.removeInfoBox(inventorySpacesInfoBox);
			inventorySpacesInfoBox = null;
		}
	}

	private void syncInventoryTrackerBaseline()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			lastInventoryQuantities.clear();
			inventoryTrackerBaselineReady = false;
			return;
		}

		lastInventoryQuantities.clear();
		lastInventoryQuantities.putAll(snapshotInventoryQuantities(inventory));
		inventoryTrackerBaselineReady = true;
	}

	private void updateInventoryValueTrackers(ItemContainer inventory)
	{
		if (!isLoggedIn() || inventory == null)
		{
			lastInventoryQuantities.clear();
			inventoryTrackerBaselineReady = false;
			return;
		}

		Map<Integer, Long> currentQuantities = snapshotInventoryQuantities(inventory);
		if (!inventoryTrackerBaselineReady)
		{
			lastInventoryQuantities.clear();
			lastInventoryQuantities.putAll(currentQuantities);
			inventoryTrackerBaselineReady = true;
			return;
		}

		if (shouldIgnoreEquipmentTransferChange())
		{
			lastInventoryQuantities.clear();
			lastInventoryQuantities.putAll(currentQuantities);
			return;
		}

		Set<Integer> itemIds = new HashSet<>();
		itemIds.addAll(lastInventoryQuantities.keySet());
		itemIds.addAll(currentQuantities.keySet());

		Map<Integer, Long> gainedQuantities = new HashMap<>();
		Map<Integer, Long> spentQuantities = new HashMap<>();
		for (int itemId : itemIds)
		{
			long previousQuantity = lastInventoryQuantities.getOrDefault(itemId, 0L);
			long currentQuantity = currentQuantities.getOrDefault(itemId, 0L);
			long diff = currentQuantity - previousQuantity;
			if (diff > 0)
			{
				long ignoredQuantity = consumeIgnoredInventoryReturn(itemId, diff);
				long countedQuantity = Math.max(0L, diff - ignoredQuantity);
				if (countedQuantity > 0)
				{
					gainedQuantities.merge(itemId, countedQuantity, Long::sum);
				}
			}
			else if (diff < 0)
			{
				spentQuantities.merge(itemId, -diff, Long::sum);
			}
		}

		if (shouldIgnoreInventoryValueChange() || isSuspiciousEmptyInventoryRefresh(currentQuantities))
		{
			recordIgnoredInventoryMovement(lastInventoryQuantities, currentQuantities);
			pendingInventoryValueChanges.clear();
			lastInventoryQuantities.clear();
			lastInventoryQuantities.putAll(currentQuantities);
			return;
		}

		markBirdhouseSeededFromInventoryChange(spentQuantities);
		markPatchTimerFromInventoryChange(spentQuantities);
		neutralizeCleanedHerbChanges(gainedQuantities, spentQuantities);
		neutralizeBirdHouseCrafting(gainedQuantities, spentQuantities);
		recordBirdhouseLootStats(gainedQuantities, spentQuantities);
		neutralizeOpenedBirdNestChanges(gainedQuantities, spentQuantities, this::getItemName);
		recordFarmingActivityStats(gainedQuantities, spentQuantities);
		InventoryValueAdjustment chargedItemAdjustment = neutralizeChargedItemChanges(
			gainedQuantities,
			spentQuantities,
			this::getExactItemPrice,
			this::getItemName
		);
		long gainedValue = chargedItemAdjustment.gainedValue + getGainedInventoryChangeValue(gainedQuantities);
		long spentValue = chargedItemAdjustment.spentValue + getSpentInventoryChangeValue(spentQuantities);
		if (shouldIgnoreSuspiciousInventoryValueChange(gainedQuantities, spentQuantities, gainedValue, spentValue))
		{
			pendingInventoryValueChanges.clear();
			lastInventoryQuantities.clear();
			lastInventoryQuantities.putAll(currentQuantities);
			return;
		}

		queueOrCommitInventoryValueChange(gainedValue, spentValue, false);

		lastInventoryQuantities.clear();
		lastInventoryQuantities.putAll(currentQuantities);
	}

	private void neutralizeCleanedHerbChanges(Map<Integer, Long> gainedQuantities, Map<Integer, Long> spentQuantities)
	{
		cleanupPendingHerbCleanings();
		if (!gainedQuantities.isEmpty())
		{
			consumePendingHerbCleanings(gainedQuantities);
		}

		if (spentQuantities.isEmpty())
		{
			return;
		}

		for (Map.Entry<Integer, Long> entry : new ArrayList<>(spentQuantities.entrySet()))
		{
			int cleanHerbId = getCleanHerbIdForGrimy(entry.getKey());
			if (cleanHerbId == -1)
			{
				continue;
			}

			cleanHerbId = itemManager.canonicalize(cleanHerbId);
			long cleanedQuantity = Math.min(entry.getValue(), gainedQuantities.getOrDefault(cleanHerbId, 0L));
			if (cleanedQuantity > 0)
			{
				subtractQuantity(gainedQuantities, cleanHerbId, cleanedQuantity);
			}

			long pendingQuantity = entry.getValue() - cleanedQuantity;
			if (pendingQuantity > 0)
			{
				pendingHerbCleanings.add(new PendingHerbCleaning(cleanHerbId, pendingQuantity, tickCount));
			}

			spentQuantities.remove(entry.getKey());
		}
	}

	private void consumePendingHerbCleanings(Map<Integer, Long> gainedQuantities)
	{
		for (Map.Entry<Integer, Long> entry : new ArrayList<>(gainedQuantities.entrySet()))
		{
			long remainingGained = entry.getValue();
			for (PendingHerbCleaning pendingCleaning : new ArrayList<>(pendingHerbCleanings))
			{
				if (remainingGained <= 0)
				{
					break;
				}

				if (pendingCleaning.cleanHerbId != entry.getKey())
				{
					continue;
				}

				long neutralizedQuantity = Math.min(remainingGained, pendingCleaning.quantity);
				remainingGained -= neutralizedQuantity;
				pendingCleaning.quantity -= neutralizedQuantity;
				if (pendingCleaning.quantity <= 0)
				{
					pendingHerbCleanings.remove(pendingCleaning);
				}
			}

			if (remainingGained <= 0)
			{
				gainedQuantities.remove(entry.getKey());
			}
			else
			{
				gainedQuantities.put(entry.getKey(), remainingGained);
			}
		}
	}

	static void neutralizeBirdHouseCrafting(Map<Integer, Long> gainedQuantities, Map<Integer, Long> spentQuantities)
	{
		for (Map.Entry<Integer, Long> entry : new ArrayList<>(gainedQuantities.entrySet()))
		{
			int logId = getBirdHouseLogId(entry.getKey());
			if (logId == -1)
			{
				continue;
			}

			long craftedQuantity = Math.min(entry.getValue(), spentQuantities.getOrDefault(logId, 0L));
			craftedQuantity = Math.min(craftedQuantity, spentQuantities.getOrDefault(ItemID.CLOCKWORK, 0L));
			if (craftedQuantity <= 0)
			{
				continue;
			}

			subtractQuantity(gainedQuantities, entry.getKey(), craftedQuantity);
			subtractQuantity(spentQuantities, logId, craftedQuantity);
			subtractQuantity(spentQuantities, ItemID.CLOCKWORK, craftedQuantity);
		}
	}

	static void neutralizeOpenedBirdNestChanges(
		Map<Integer, Long> gainedQuantities,
		Map<Integer, Long> spentQuantities,
		IntFunction<String> itemName)
	{
		long openedNests = 0L;
		for (Map.Entry<Integer, Long> entry : new ArrayList<>(spentQuantities.entrySet()))
		{
			if (isBirdNestItemName(itemName.apply(entry.getKey())))
			{
				openedNests += Math.max(0L, entry.getValue());
				spentQuantities.remove(entry.getKey());
			}
		}

		if (openedNests <= 0 || gainedQuantities.isEmpty())
		{
			return;
		}

		for (Map.Entry<Integer, Long> entry : new ArrayList<>(gainedQuantities.entrySet()))
		{
			if (isBirdNestOpeningOutputName(itemName.apply(entry.getKey())))
			{
				gainedQuantities.remove(entry.getKey());
			}
		}
	}

	static InventoryValueAdjustment neutralizeChargedItemChanges(
		Map<Integer, Long> gainedQuantities,
		Map<Integer, Long> spentQuantities,
		IntUnaryOperator exactPrice,
		IntFunction<String> itemName)
	{
		long gainedValue = 0L;
		long spentValue = 0L;
		for (int spentItemId : new ArrayList<>(spentQuantities.keySet()))
		{
			ChargedItemVariant spentVariant = parseChargedItemVariant(itemName.apply(spentItemId));
			if (spentVariant == null)
			{
				continue;
			}

			for (int gainedItemId : new ArrayList<>(gainedQuantities.keySet()))
			{
				long remainingSpent = spentQuantities.getOrDefault(spentItemId, 0L);
				if (remainingSpent <= 0)
				{
					break;
				}
				long remainingGained = gainedQuantities.getOrDefault(gainedItemId, 0L);
				if (remainingGained <= 0)
				{
					continue;
				}
				long quantity = Math.min(remainingSpent, remainingGained);

				ChargedItemVariant gainedVariant = parseChargedItemVariant(itemName.apply(gainedItemId));
				if (gainedVariant == null
					|| !spentVariant.baseName.equals(gainedVariant.baseName)
					|| spentVariant.charges == gainedVariant.charges)
				{
					continue;
				}

				long valueDifference = getChargedItemValueDifference(
					exactPrice.applyAsInt(spentItemId),
					exactPrice.applyAsInt(gainedItemId),
					spentVariant.charges,
					gainedVariant.charges,
					quantity
				);
				if (spentVariant.charges > gainedVariant.charges)
				{
					spentValue += valueDifference;
				}
				else
				{
					gainedValue += valueDifference;
				}

				subtractQuantity(spentQuantities, spentItemId, quantity);
				subtractQuantity(gainedQuantities, gainedItemId, quantity);
			}
		}

		return new InventoryValueAdjustment(gainedValue, spentValue);
	}

	private static ChargedItemVariant parseChargedItemVariant(String itemName)
	{
		if (itemName == null)
		{
			return null;
		}

		Matcher matcher = CHARGED_ITEM_NAME.matcher(itemName.trim());
		if (!matcher.matches())
		{
			String baseName = itemName.trim().toLowerCase(Locale.ROOT);
			return baseName.isEmpty() ? null : new ChargedItemVariant(baseName, 0);
		}

		try
		{
			return new ChargedItemVariant(
				matcher.group(1).trim().toLowerCase(Locale.ROOT),
				Integer.parseInt(matcher.group(2))
			);
		}
		catch (NumberFormatException ignored)
		{
			return null;
		}
	}

	private static long getChargedItemValueDifference(
		int spentPrice,
		int gainedPrice,
		int spentCharges,
		int gainedCharges,
		long quantity)
	{
		long exactDifference = spentCharges > gainedCharges
			? (long) spentPrice - gainedPrice
			: (long) gainedPrice - spentPrice;
		if (spentPrice > 0 && gainedPrice > 0 && exactDifference > 0)
		{
			return exactDifference * quantity;
		}

		int knownPrice = spentPrice > 0 ? spentPrice : gainedPrice;
		int knownCharges = spentPrice > 0 ? spentCharges : gainedCharges;
		if (knownPrice <= 0 || knownCharges <= 0)
		{
			return 0L;
		}

		long changedCharges = Math.abs((long) spentCharges - gainedCharges);
		long estimatedValue = ((long) knownPrice * changedCharges + knownCharges - 1L) / knownCharges;
		return estimatedValue * quantity;
	}

	private void cleanupPendingHerbCleanings()
	{
		pendingHerbCleanings.removeIf(cleaning -> tickCount - cleaning.tick > PENDING_HERB_CLEANING_TICKS);
	}

	private long getGainedInventoryChangeValue(Map<Integer, Long> gainedQuantities)
	{
		long value = 0L;
		for (Map.Entry<Integer, Long> entry : gainedQuantities.entrySet())
		{
			value += getItemProfitValue(entry.getKey(), entry.getValue());
		}

		return value;
	}

	private long getSpentInventoryChangeValue(Map<Integer, Long> spentQuantities)
	{
		long value = 0L;
		for (Map.Entry<Integer, Long> entry : spentQuantities.entrySet())
		{
			value += getItemCostValue(entry.getKey(), entry.getValue());
		}

		return value;
	}

	private boolean shouldIgnoreSuspiciousInventoryValueChange(
		Map<Integer, Long> gainedQuantities,
		Map<Integer, Long> spentQuantities,
		long gainedValue,
		long spentValue)
	{
		return isSuspiciousInventoryValueReplacement(
			gainedValue,
			spentValue,
			countPositiveItemTypes(gainedQuantities),
			countPositiveItemTypes(spentQuantities));
	}

	static boolean isSuspiciousInventoryValueReplacement(long gainedValue, long spentValue, int gainedTypes, int spentTypes)
	{
		if (gainedValue <= 0 || spentValue <= 0)
		{
			return false;
		}

		long lowerValue = Math.min(gainedValue, spentValue);
		long higherValue = Math.max(gainedValue, spentValue);
		if (lowerValue >= SUSPICIOUS_VALUE_REPLACEMENT_VERY_LARGE_GP)
		{
			return true;
		}

		boolean closeValues = lowerValue * 100L >= higherValue * SUSPICIOUS_VALUE_REPLACEMENT_CLOSE_PERCENT;
		if (!closeValues)
		{
			return false;
		}

		boolean broadReplacement = lowerValue >= SUSPICIOUS_VALUE_REPLACEMENT_MIN_GP
			&& gainedTypes >= SUSPICIOUS_VALUE_REPLACEMENT_MIN_TYPES
			&& spentTypes >= SUSPICIOUS_VALUE_REPLACEMENT_MIN_TYPES;
		return broadReplacement;
	}

	private static int countPositiveItemTypes(Map<Integer, Long> quantities)
	{
		int count = 0;
		for (long quantity : quantities.values())
		{
			if (quantity > 0)
			{
				count++;
			}
		}

		return count;
	}

	private static void subtractQuantity(Map<Integer, Long> quantities, int itemId, long quantity)
	{
		long remaining = quantities.getOrDefault(itemId, 0L) - quantity;
		if (remaining <= 0)
		{
			quantities.remove(itemId);
		}
		else
		{
			quantities.put(itemId, remaining);
		}
	}

	private Map<Integer, Long> snapshotInventoryQuantities(ItemContainer inventory)
	{
		Map<Integer, Long> quantities = new HashMap<>();
		for (Item item : inventory.getItems())
		{
			if (!isOccupiedInventorySlot(item))
			{
				continue;
			}

			int itemId = itemManager.canonicalize(item.getId());
			quantities.merge(itemId, (long) item.getQuantity(), Long::sum);
		}

		return quantities;
	}

	private boolean shouldIgnoreInventoryValueChange()
	{
		return inventoryTrackerIgnoreTicks > 0 || isInventoryTransferInterfaceOpen();
	}

	private boolean isSuspiciousEmptyInventoryRefresh(Map<Integer, Long> currentQuantities)
	{
		if (!currentQuantities.isEmpty())
		{
			return false;
		}

		return lastInventoryQuantities.size() >= SUSPICIOUS_EMPTY_INVENTORY_ITEM_TYPES;
	}

	private void recordIgnoredInventoryMovement(Map<Integer, Long> previousQuantities, Map<Integer, Long> currentQuantities)
	{
		for (Map.Entry<Integer, Long> entry : previousQuantities.entrySet())
		{
			int itemId = entry.getKey();
			long previousQuantity = entry.getValue();
			long currentQuantity = currentQuantities.getOrDefault(itemId, 0L);
			long removedQuantity = previousQuantity - currentQuantity;
			if (removedQuantity > 0)
			{
				ignoredInventoryReturns.merge(itemId, removedQuantity, Long::sum);
				ignoredInventoryReturnExpiryTicks.put(itemId, tickCount + IGNORED_INVENTORY_RETURN_TICKS);
			}
		}
	}

	private long consumeIgnoredInventoryReturn(int itemId, long gainedQuantity)
	{
		Integer expiryTick = ignoredInventoryReturnExpiryTicks.get(itemId);
		if (expiryTick == null || tickCount > expiryTick)
		{
			ignoredInventoryReturns.remove(itemId);
			ignoredInventoryReturnExpiryTicks.remove(itemId);
			return 0L;
		}

		long ignoredQuantity = ignoredInventoryReturns.getOrDefault(itemId, 0L);
		if (ignoredQuantity <= 0)
		{
			return 0L;
		}

		long consumedQuantity = Math.min(gainedQuantity, ignoredQuantity);
		long remainingQuantity = ignoredQuantity - consumedQuantity;
		if (remainingQuantity <= 0)
		{
			ignoredInventoryReturns.remove(itemId);
			ignoredInventoryReturnExpiryTicks.remove(itemId);
		}
		else
		{
			ignoredInventoryReturns.put(itemId, remainingQuantity);
		}

		return consumedQuantity;
	}

	private void cleanupIgnoredInventoryReturns()
	{
		ignoredInventoryReturnExpiryTicks.entrySet().removeIf(entry ->
		{
			if (tickCount <= entry.getValue())
			{
				return false;
			}

			ignoredInventoryReturns.remove(entry.getKey());
			return true;
		});
	}

	private int getOccupiedInventorySlots(ItemContainer inventory)
	{
		int occupiedSlots = 0;
		for (Item item : inventory.getItems())
		{
			if (isOccupiedInventorySlot(item))
			{
				occupiedSlots++;
			}
		}

		return occupiedSlots;
	}

	private long getInventoryValue(ItemContainer inventory)
	{
		long value = 0L;
		for (Item item : inventory.getItems())
		{
			if (!isOccupiedInventorySlot(item))
			{
				continue;
			}

			value += getItemValue(item.getId(), item.getQuantity());
		}

		return value;
	}

	private long getItemValue(int itemId, long quantity)
	{
		int canonicalItemId = itemManager.canonicalize(itemId);
		if (canonicalItemId == ItemID.COINS_995)
		{
			return quantity;
		}

		if (canonicalItemId == ItemID.PLATINUM_TOKEN)
		{
			return quantity * 1000L;
		}

		int price = getExactItemPrice(canonicalItemId);
		if (price <= 0)
		{
			int variationItemId = ItemVariationMapping.map(canonicalItemId);
			if (variationItemId != canonicalItemId)
			{
				price = getExactItemPrice(variationItemId);
			}
		}
		return price > 0 ? (long) price * quantity : 0L;
	}

	private int getExactItemPrice(int itemId)
	{
		return getConfiguredItemPrice(itemManager.canonicalize(itemId));
	}

	private String getItemName(int itemId)
	{
		try
		{
			return itemManager.getItemComposition(itemManager.canonicalize(itemId)).getName();
		}
		catch (RuntimeException ignored)
		{
			return "";
		}
	}

	String getStatsItemName(int itemId)
	{
		String name = getItemName(itemId);
		return name == null || name.isBlank() ? "Item " + itemId : name;
	}

	private int getConfiguredItemPrice(int itemId)
	{
		if (config.priceSource() == ToolkitPriceSource.RUNELITE)
		{
			return itemManager.getItemPrice(itemId);
		}

		return itemManager.getItemPriceWithSource(itemId, true);
	}

	private long getItemCostValue(int itemId, long quantity)
	{
		int canonicalItemId = itemManager.canonicalize(itemId);
		if (canonicalItemId == ItemID.CLOCKWORK)
		{
			return 0L;
		}

		long value = getItemValue(canonicalItemId, quantity);
		if (isBirdHouseItem(canonicalItemId))
		{
			value -= getItemValue(ItemID.CLOCKWORK, quantity);
		}

		return Math.max(0L, value);
	}

	private long getItemProfitValue(int itemId, long quantity)
	{
		int canonicalItemId = itemManager.canonicalize(itemId);
		if (canonicalItemId == ItemID.CLOCKWORK)
		{
			return 0L;
		}

		return getItemValue(canonicalItemId, quantity);
	}

	private static int getCleanHerbIdForGrimy(int itemId)
	{
		switch (itemId)
		{
			case ItemID.GRIMY_GUAM_LEAF:
				return ItemID.GUAM_LEAF;
			case ItemID.GRIMY_MARRENTILL:
				return ItemID.MARRENTILL;
			case ItemID.GRIMY_TARROMIN:
				return ItemID.TARROMIN;
			case ItemID.GRIMY_HARRALANDER:
				return ItemID.HARRALANDER;
			case ItemID.GRIMY_RANARR_WEED:
				return ItemID.RANARR_WEED;
			case ItemID.GRIMY_TOADFLAX:
				return ItemID.TOADFLAX;
			case ItemID.GRIMY_IRIT_LEAF:
				return ItemID.IRIT_LEAF;
			case ItemID.GRIMY_AVANTOE:
				return ItemID.AVANTOE;
			case ItemID.GRIMY_KWUARM:
				return ItemID.KWUARM;
			case ItemID.GRIMY_SNAPDRAGON:
				return ItemID.SNAPDRAGON;
			case ItemID.GRIMY_CADANTINE:
				return ItemID.CADANTINE;
			case ItemID.GRIMY_LANTADYME:
				return ItemID.LANTADYME;
			case ItemID.GRIMY_DWARF_WEED:
				return ItemID.DWARF_WEED;
			case ItemID.GRIMY_TORSTOL:
				return ItemID.TORSTOL;
			case ItemID.GRIMY_SNAKE_WEED:
				return ItemID.SNAKE_WEED;
			case ItemID.GRIMY_ARDRIGAL:
				return ItemID.ARDRIGAL;
			case ItemID.GRIMY_SITO_FOIL:
				return ItemID.SITO_FOIL;
			case ItemID.GRIMY_VOLENCIA_MOSS:
				return ItemID.VOLENCIA_MOSS;
			case ItemID.GRIMY_ROGUES_PURSE:
				return ItemID.ROGUES_PURSE;
			case ItemID.GRIMY_NOXIFER:
				return ItemID.NOXIFER;
			case ItemID.GRIMY_GOLPAR:
				return ItemID.GOLPAR;
			case ItemID.GRIMY_BUCHU_LEAF:
				return ItemID.BUCHU_LEAF;
			case ItemID.GRIMY_HUASCA:
				return ItemID.HUASCA;
			default:
				return -1;
		}
	}

	private static int getBirdHouseLogId(int itemId)
	{
		switch (itemId)
		{
			case ItemID.BIRD_HOUSE:
				return ItemID.LOGS;
			case ItemID.OAK_BIRD_HOUSE:
				return ItemID.OAK_LOGS;
			case ItemID.WILLOW_BIRD_HOUSE:
				return ItemID.WILLOW_LOGS;
			case ItemID.TEAK_BIRD_HOUSE:
				return ItemID.TEAK_LOGS;
			case ItemID.MAPLE_BIRD_HOUSE:
				return ItemID.MAPLE_LOGS;
			case ItemID.MAHOGANY_BIRD_HOUSE:
				return ItemID.MAHOGANY_LOGS;
			case ItemID.YEW_BIRD_HOUSE:
				return ItemID.YEW_LOGS;
			case ItemID.MAGIC_BIRD_HOUSE:
				return ItemID.MAGIC_LOGS;
			case ItemID.REDWOOD_BIRD_HOUSE:
				return ItemID.REDWOOD_LOGS;
			default:
				return -1;
		}
	}

	private static boolean isBirdHouseItem(int itemId)
	{
		return getBirdHouseLogId(itemId) != -1;
	}

	private String buildInventoryTooltip(long inventoryValue)
	{
		return "Inventory value: " + QuantityFormatter.quantityToStackSize(inventoryValue) + " gp";
	}

	private boolean isOccupiedInventorySlot(Item item)
	{
		return item != null && item.getId() != -1 && item.getQuantity() > 0;
	}

	private void updateTrackedSkillXp(Skill skill, int currentXp)
	{
		if (!trackerValuesLoaded)
		{
			loadTrackerValues();
		}

		if (!isSkillingTrackerSkill(skill))
		{
			return;
		}

		Integer previousXp = lastSkillExperience.get(skill);
		if (!trackerBaseExperience.containsKey(skill) || previousXp == null)
		{
			trackerBaseExperience.put(skill, currentXp);
			lastSkillExperience.put(skill, currentXp);
			return;
		}

		lastSkillExperience.put(skill, currentXp);
		int gainedXp = currentXp - previousXp;
		if (gainedXp <= 0)
		{
			if (currentXp < trackerBaseExperience.getOrDefault(skill, currentXp))
			{
				trackerBaseExperience.put(skill, currentXp);
			}
			return;
		}

		markSkillingActivity();
		long gainedSinceBase = Math.max(0L, (long) currentXp - trackerBaseExperience.getOrDefault(skill, currentXp));
		long savedOffset = getXpGainedOffset(skill);
		boolean wasInactive = getXpGained() <= 0;
		xpGainedBySkill.put(skill, savedOffset + gainedSinceBase);
		if (wasInactive && getXpGained() > 0)
		{
			activateChatTabTracker(ChatTabTrackerType.XP);
			activateChatTabTracker(ChatTabTrackerType.TOP_SKILL_XP);
		}

		activeSkillingSkill = skill;
		lastSkillingXpTick = tickCount;
		addXpStats(skill, gainedXp);
		if (skill == Skill.HUNTER && hasRecentBirdhouseXpInteraction())
		{
			addBirdhouseXpStats(gainedXp);
		}
		commitPendingInventoryValueChanges();
		if (isForeverTrackerMode())
		{
			persistXpTrackerValues();
		}

		addAction();
	}

	private void syncTrackerSkillBaselines()
	{
		if (!isLoggedIn())
		{
			return;
		}

		for (Skill skill : SKILLING_TRACKER_SKILLS)
		{
			syncTrackerSkillBaseline(skill);
		}
	}

	private void syncTrackerSkillBaseline(Skill skill)
	{
		if (trackerBaseExperience.containsKey(skill) && lastSkillExperience.containsKey(skill))
		{
			return;
		}

		int xp = client.getSkillExperience(skill);
		if (xp <= 0)
		{
			return;
		}

		lastSkillExperience.put(skill, xp);
		trackerBaseExperience.put(skill, xp);
	}

	private void loadTrackerValues()
	{
		loadXpTrackerValues();
		profitValue = isForeverTrackerMode() ? parseLongConfig(config.savedProfitTrackerValue()) : 0L;
		costValue = isForeverTrackerMode() ? parseLongConfig(config.savedCostTrackerValue()) : 0L;
		actionsValue = isForeverTrackerMode() ? parseLongConfig(config.savedActionsTrackerValue()) : 0L;
		loadChatTabTrackerOrder();
		syncTrackerSkillBaselines();
		trackerValuesLoaded = true;
	}

	private void loadStatsValues()
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			statsByPeriod.put(period, SkillingToolkitStats.deserialize(getSavedStatsValue(period), getCurrentStatsPeriodId(period)));
		}
		syncAllTimeStatsWithTrackerValues();
		refreshToolkitPanel();
	}

	SkillingToolkitStats getStatsSnapshot(SkillingToolkitStatsPeriod period)
	{
		return getStats(period).copy();
	}

	private SkillingToolkitStats getStats(SkillingToolkitStatsPeriod period)
	{
		String currentPeriodId = getCurrentStatsPeriodId(period);
		SkillingToolkitStats stats = statsByPeriod.get(period);
		if (stats == null || !currentPeriodId.equals(stats.getPeriodId()))
		{
			stats = SkillingToolkitStats.deserialize(getSavedStatsValue(period), currentPeriodId);
			statsByPeriod.put(period, stats);
			persistStatsValue(period);
		}

		return stats;
	}

	private String getCurrentStatsPeriodId(SkillingToolkitStatsPeriod period)
	{
		return period.getCurrentPeriodId(LocalDate.now());
	}

	private String getSavedStatsValue(SkillingToolkitStatsPeriod period)
	{
		String key = getSavedStatsKey(period);
		String savedValue = configManager.getConfiguration(SkillingToolkitConfig.GROUP, key);
		if (savedValue != null)
		{
			return savedValue;
		}

		switch (period)
		{
			case DAY:
				return config.savedStatsDay();
			case WEEK:
				return config.savedStatsWeek();
			case MONTH:
				return config.savedStatsMonth();
			case YEAR:
				return config.savedStatsYear();
			case ALL_TIME:
			default:
				return config.savedStatsAllTime();
		}
	}

	private String getSavedStatsKey(SkillingToolkitStatsPeriod period)
	{
		switch (period)
		{
			case DAY:
				return SAVED_STATS_DAY_KEY;
			case WEEK:
				return SAVED_STATS_WEEK_KEY;
			case MONTH:
				return SAVED_STATS_MONTH_KEY;
			case YEAR:
				return SAVED_STATS_YEAR_KEY;
			case ALL_TIME:
			default:
				return SAVED_STATS_ALL_TIME_KEY;
		}
	}

	private void addXpStats(Skill skill, long xp)
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			getStats(period).addXp(skill, xp);
			persistStatsValue(period);
		}
		refreshToolkitPanel();
	}

	private void addProfitStats(long value)
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			getStats(period).addProfit(value);
			persistStatsValue(period);
		}
		refreshToolkitPanel();
	}

	private void addCostStats(long value)
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			getStats(period).addCost(value);
			persistStatsValue(period);
		}
		refreshToolkitPanel();
	}

	private void addActionStats()
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			getStats(period).addAction();
			persistStatsValue(period);
		}
		refreshToolkitPanel();
	}

	private void addBirdhouseLootStats(long nests, long nestValue, boolean countRun)
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			getStats(period).addBirdhouseLoot(nests, nestValue, countRun);
			persistStatsValue(period);
		}
		refreshToolkitPanel();
	}

	private void addBirdhouseXpStats(long xp)
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			getStats(period).addBirdhouseXp(xp);
			persistStatsValue(period);
		}
		refreshToolkitPanel();
	}

	private void addFarmingActivityStats(
		FarmingActivityType type,
		long seedsUsed,
		long seedCost,
		long harvested,
		long harvestValue,
		boolean countRun)
	{
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			getStats(period).addFarmingActivity(type, seedsUsed, seedCost, harvested, harvestValue, countRun);
			persistStatsValue(period);
		}
		refreshToolkitPanel();
	}

	private void persistStatsValue(SkillingToolkitStatsPeriod period)
	{
		configManager.setConfiguration(SkillingToolkitConfig.GROUP, getSavedStatsKey(period), getStats(period).serialize());
	}

	private void syncAllTimeStatsWithTrackerValues()
	{
		SkillingToolkitStats allTimeStats = getStats(SkillingToolkitStatsPeriod.ALL_TIME);
		boolean changed = allTimeStats.ensureProfitAtLeast(profitValue);
		changed |= allTimeStats.ensureCostAtLeast(costValue);
		changed |= allTimeStats.ensureActionsAtLeast(actionsValue);
		for (Skill skill : SKILLING_TRACKER_SKILLS)
		{
			changed |= allTimeStats.ensureXpAtLeast(skill, xpGainedBySkill.getOrDefault(skill, 0L));
		}

		if (changed)
		{
			persistStatsValue(SkillingToolkitStatsPeriod.ALL_TIME);
		}
	}

	private void loadXpTrackerValues()
	{
		xpGainedBySkill.clear();
		trackerBaseExperience.clear();
		for (Skill skill : SKILLING_TRACKER_SKILLS)
		{
			xpGainedBySkill.put(skill, 0L);
		}

		if (!isForeverTrackerMode())
		{
			return;
		}

		String savedValues = config.savedXpTrackerValues();
		if (savedValues == null || savedValues.isBlank())
		{
			return;
		}

		for (String savedValue : savedValues.split(";"))
		{
			String[] parts = savedValue.split("=", 2);
			if (parts.length != 2)
			{
				continue;
			}

			try
			{
				Skill skill = Skill.valueOf(parts[0]);
				if (isSkillingTrackerSkill(skill))
				{
					xpGainedBySkill.put(skill, Math.max(0L, parseLongConfig(parts[1])));
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore stale config values from older versions or manual edits.
			}
		}
	}

	private void addProfit(long value)
	{
		boolean wasInactive = profitValue <= 0;
		profitValue += value;
		addProfitStats(value);
		if (wasInactive && profitValue > 0)
		{
			activateChatTabTracker(ChatTabTrackerType.PROFIT);
		}

		if (isForeverTrackerMode())
		{
			persistProfitTrackerValue();
		}
	}

	private void queueOrCommitInventoryValueChange(long gainedValue, long spentValue, boolean forceCommit)
	{
		if (gainedValue <= 0 && spentValue <= 0)
		{
			return;
		}

		boolean activeSkillingInventoryWindow = isWithinActiveSkillingInventoryWindow();
		boolean recentDirectSkillingInteraction = hasRecentDirectSkillingInventoryInteraction();
		if (!shouldAcceptInventoryValueChange(
			forceCommit,
			activeSkillingInventoryWindow,
			recentDirectSkillingInteraction,
			isOtherToolkitUiOwnerActive()))
		{
			return;
		}

		if (forceCommit || activeSkillingInventoryWindow)
		{
			commitInventoryValueChange(gainedValue, spentValue);
			return;
		}

		pendingInventoryValueChanges.add(new PendingInventoryValueChange(gainedValue, spentValue, tickCount));
		cleanupPendingInventoryValueChanges();
	}

	private void commitPendingInventoryValueChanges()
	{
		if (pendingInventoryValueChanges.isEmpty())
		{
			return;
		}

		pendingInventoryValueChanges.removeIf(change ->
		{
			if (tickCount - change.tick > PENDING_INVENTORY_VALUE_TICKS)
			{
				return true;
			}

			commitInventoryValueChange(change.gainedValue, change.spentValue);
			return true;
		});
	}

	private void cleanupPendingInventoryValueChanges()
	{
		pendingInventoryValueChanges.removeIf(change -> tickCount - change.tick > PENDING_INVENTORY_VALUE_TICKS);
	}

	private boolean isWithinActiveSkillingInventoryWindow()
	{
		return activeSkillingSkill != null && tickCount - lastSkillingXpTick <= ACTIVE_SKILL_INVENTORY_TICKS;
	}

	private boolean hasRecentDirectSkillingInventoryInteraction()
	{
		return hasRecentFarmingStatsInteraction() || hasRecentBirdhouseInteraction();
	}

	static boolean shouldAcceptInventoryValueChange(
		boolean forceCommit,
		boolean activeSkillingInventoryWindow,
		boolean recentDirectSkillingInteraction,
		boolean otherToolkitOwnerActive)
	{
		if (forceCommit)
		{
			return true;
		}

		if (otherToolkitOwnerActive && !activeSkillingInventoryWindow)
		{
			return false;
		}

		return activeSkillingInventoryWindow || recentDirectSkillingInteraction;
	}

	private void commitInventoryValueChange(long gainedValue, long spentValue)
	{
		if (gainedValue > 0)
		{
			addProfit(gainedValue);
		}

		if (spentValue > 0)
		{
			addCost(spentValue);
		}
	}

	private void addCost(long value)
	{
		boolean wasInactive = costValue <= 0;
		costValue += value;
		addCostStats(value);
		if (wasInactive && costValue > 0)
		{
			activateChatTabTracker(ChatTabTrackerType.COST);
		}

		if (isForeverTrackerMode())
		{
			persistCostTrackerValue();
		}
	}

	private void addAction()
	{
		boolean wasInactive = actionsValue <= 0;
		actionsValue++;
		addActionStats();
		if (wasInactive && actionsValue > 0)
		{
			activateChatTabTracker(ChatTabTrackerType.ACTIONS);
		}

		if (isForeverTrackerMode())
		{
			persistActionsTrackerValue();
		}
	}

	private void resetSelectedTracker()
	{
		switch (config.resetTrackerTarget())
		{
			case XP:
				resetXpTracker();
				break;
			case PROFIT:
				resetProfitTracker();
				break;
			case COST:
				resetCostTracker();
				break;
			case ACTIONS:
				resetActionsTracker();
				break;
			case ALL:
				resetXpTracker();
				resetProfitTracker();
				resetCostTracker();
				resetActionsTracker();
				break;
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, "resetSelectedTracker", false);
		updateChatTabTrackersLater();
	}

	private void resetXpTracker()
	{
		for (Skill skill : SKILLING_TRACKER_SKILLS)
		{
			xpGainedBySkill.put(skill, 0L);
			if (isLoggedIn())
			{
				trackerBaseExperience.put(skill, client.getSkillExperience(skill));
			}
		}
		persistXpTrackerValues();
	}

	private void resetProfitTracker()
	{
		profitValue = 0L;
		ignoredInventoryReturns.clear();
		ignoredInventoryReturnExpiryTicks.clear();
		persistProfitTrackerValue();
	}

	private void resetCostTracker()
	{
		costValue = 0L;
		ignoredInventoryReturns.clear();
		ignoredInventoryReturnExpiryTicks.clear();
		persistCostTrackerValue();
	}

	private void resetActionsTracker()
	{
		actionsValue = 0L;
		persistActionsTrackerValue();
	}

	private void resetSelectedMarkers()
	{
		switch (config.markerResetTarget())
		{
			case COMPOST:
				resetCompostMarkers();
				break;
			case NEEDS_COMPOST:
				resetNeedsCompostMarkers();
				break;
			case PROTECTION:
				resetProtectionMarkers();
				break;
			case PATCH_TIMERS:
				resetPatchTimers();
				break;
			case BIRDHOUSES:
				resetBirdhouseMarkers();
				break;
			case ALL:
				resetCompostMarkers();
				resetNeedsCompostMarkers();
				resetProtectionMarkers();
				resetPatchTimers();
				resetBirdhouseMarkers();
				break;
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, "resetSelectedMarkers", false);
	}

	private void resetCompostMarkers()
	{
		compostedPatches.clear();
		currentCompostPatch = null;
		persistCompostPatches();
	}

	private void resetNeedsCompostMarkers()
	{
		needsCompostPatches.clear();
		persistNeedsCompostPatches();
	}

	private void resetProtectionMarkers()
	{
		protectedPatches.clear();
		lastProtectionPatchCandidate = null;
		lastProtectionDialogText = "";
		persistProtectedPatches();
	}

	private void resetPatchTimers()
	{
		patchTimers.clear();
		persistPatchTimers();
	}

	private void resetBirdhouseMarkers()
	{
		seededBirdhouses.clear();
		birdhouseReadyAtMillis.clear();
		lastBirdhouseInteraction = null;
		persistSeededBirdhouses();
	}

	String getQuickXpText()
	{
		return formatShortValue(getXpGained()) + " xp";
	}

	String getQuickTopSkillText()
	{
		String skillName = getTopXpSkillText();
		if (skillName.isEmpty())
		{
			return "-";
		}

		return skillName + " " + formatShortValue(getTopXpSkillValue());
	}

	String getQuickProfitText()
	{
		return formatShortValue(profitValue) + " gp";
	}

	String getQuickCostText()
	{
		return formatShortValue(costValue) + " gp";
	}

	String getQuickActionsText()
	{
		return formatShortValue(actionsValue);
	}

	boolean isCompostQuickEnabled()
	{
		return config.showCompostIndicator();
	}

	void setCompostQuickEnabled(boolean enabled)
	{
		setConfigBoolean("showCompostIndicator", enabled);
	}

	boolean isProtectionQuickEnabled()
	{
		return config.showCropProtectionIndicator();
	}

	void setProtectionQuickEnabled(boolean enabled)
	{
		setConfigBoolean("showCropProtectionIndicator", enabled);
	}

	boolean isPatchTimerQuickEnabled()
	{
		return config.showPatchTimers();
	}

	void setPatchTimerQuickEnabled(boolean enabled)
	{
		setConfigBoolean("showPatchTimers", enabled);
	}

	boolean isBirdhouseQuickEnabled()
	{
		return config.showBirdhouseSeedIndicator();
	}

	void setBirdhouseQuickEnabled(boolean enabled)
	{
		setConfigBoolean("showBirdhouseSeedIndicator", enabled);
	}

	boolean isInventoryQuickEnabled()
	{
		return config.showInventorySpaces();
	}

	void setInventoryQuickEnabled(boolean enabled)
	{
		setConfigBoolean("showInventorySpaces", enabled);
		if (enabled)
		{
			clientThread.invokeLater(this::syncInventoryInfoBox);
		}
		else
		{
			removeInventoryInfoBox();
		}
	}

	boolean isHerbSackViewerQuickEnabled()
	{
		return config.showHerbSackViewer();
	}

	void setHerbSackViewerQuickEnabled(boolean enabled)
	{
		setConfigBoolean("showHerbSackViewer", enabled);
	}

	boolean isClockQuickEnabled()
	{
		return config.tradeButtonClock();
	}

	void setClockQuickEnabled(boolean enabled)
	{
		setConfigBoolean("tradeButtonClock", enabled);
		clientThread.invokeLater(this::updateTradeButtonClock);
	}

	boolean isChatTrackersQuickEnabled()
	{
		return config.xpTracker()
			|| config.topSkillTracker()
			|| config.profitTracker()
			|| config.costTracker()
			|| config.actionsTracker();
	}

	void setChatTrackersQuickEnabled(boolean enabled)
	{
		setConfigBoolean("xpTracker", enabled);
		setConfigBoolean("topSkillTracker", enabled);
		setConfigBoolean("profitTracker", enabled);
		setConfigBoolean("costTracker", enabled);
		setConfigBoolean("actionsTracker", enabled);
		updateChatTabTrackersLater();
	}

	void resetAllTrackersQuick()
	{
		resetXpTracker();
		resetProfitTracker();
		resetCostTracker();
		resetActionsTracker();
		updateChatTabTrackersLater();
		refreshToolkitPanel();
	}

	void resetGpStatsPeriodQuick(SkillingToolkitStatsPeriod period)
	{
		if (period == null)
		{
			return;
		}

		getStats(period).resetGp();
		persistStatsValue(period);
		refreshToolkitPanel();
	}

	HerbSackContents getHerbSackContents()
	{
		return herbSackContents;
	}

	long getItemValueForDisplay(int itemId, long quantity)
	{
		return getItemValue(itemId, quantity);
	}

	void resetAllMarkersQuick()
	{
		resetCompostMarkers();
		resetNeedsCompostMarkers();
		resetProtectionMarkers();
		resetPatchTimers();
		resetBirdhouseMarkers();
		refreshToolkitPanel();
	}

	private void setConfigBoolean(String key, boolean enabled)
	{
		configManager.setConfiguration(SkillingToolkitConfig.GROUP, key, enabled);
		refreshToolkitPanel();
	}

	private void refreshToolkitPanel()
	{
		if (toolkitPanel != null)
		{
			toolkitPanel.refresh();
		}
	}

	private void persistXpTrackerValues()
	{
		StringBuilder savedValues = new StringBuilder();
		for (Skill skill : SKILLING_TRACKER_SKILLS)
		{
			if (savedValues.length() > 0)
			{
				savedValues.append(';');
			}

			savedValues.append(skill.name())
				.append('=')
				.append(xpGainedBySkill.getOrDefault(skill, 0L));
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_XP_TRACKER_KEY, savedValues.toString());
	}

	private void persistProfitTrackerValue()
	{
		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_PROFIT_TRACKER_KEY, Long.toString(profitValue));
	}

	private void persistCostTrackerValue()
	{
		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_COST_TRACKER_KEY, Long.toString(costValue));
	}

	private void persistActionsTrackerValue()
	{
		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_ACTIONS_TRACKER_KEY, Long.toString(actionsValue));
	}

	private long getXpGainedOffset(Skill skill)
	{
		long currentGained = xpGainedBySkill.getOrDefault(skill, 0L);
		if (!isLoggedIn() || !trackerBaseExperience.containsKey(skill))
		{
			return currentGained;
		}

		int currentXp = client.getSkillExperience(skill);
		int baseXp = trackerBaseExperience.get(skill);
		return Math.max(0L, currentGained - Math.max(0, currentXp - baseXp));
	}

	private long getXpGained()
	{
		long total = 0L;
		for (long skillXp : xpGainedBySkill.values())
		{
			total += skillXp;
		}
		return total;
	}

	private Skill getTopXpSkill()
	{
		Skill topSkill = null;
		long topXp = 0L;
		for (Skill skill : SKILLING_TRACKER_SKILLS)
		{
			long skillXp = xpGainedBySkill.getOrDefault(skill, 0L);
			if (skillXp > topXp)
			{
				topXp = skillXp;
				topSkill = skill;
			}
		}

		return topSkill;
	}

	private long getTopXpSkillValue()
	{
		Skill topSkill = getTopXpSkill();
		return topSkill == null ? 0L : xpGainedBySkill.getOrDefault(topSkill, 0L);
	}

	private boolean isSkillingTrackerSkill(Skill skill)
	{
		for (Skill skillingSkill : SKILLING_TRACKER_SKILLS)
		{
			if (skillingSkill == skill)
			{
				return true;
			}
		}

		return false;
	}

	private boolean isForeverTrackerMode()
	{
		return config.trackerMode() == TrackerPersistenceMode.FOREVER;
	}

	private void updateTradeButtonClock()
	{
		if (!config.tradeButtonClock())
		{
			restoreChatTabOverride(ComponentID.CHATBOX_TAB_TRADE);
			return;
		}

		String clockText = LocalTime.now().format(TRADE_CLOCK_FORMATTER);
		applyChatTabOverride(ComponentID.CHATBOX_TAB_TRADE, "trade", clockText, DEFAULT_CHAT_TAB_COLOR);
		lastTradeClockText = clockText;
	}

	private void updateChatTabTrackers()
	{
		if (!isToolkitUiReady())
		{
			if (isOtherToolkitUiOwnerActive())
			{
				forgetChatTabOverrideState();
			}
			else
			{
				restoreChatTabTrackerOverrides();
			}
			return;
		}

		updateChatTabTrackerOrder();

		for (int i = 0; i < CHAT_TAB_TRACKER_SLOT_COMPONENTS.length; i++)
		{
			int componentId = CHAT_TAB_TRACKER_SLOT_COMPONENTS[i];
			ChatTabTrackerType trackerType = i < activeChatTabTrackerOrder.size()
				? activeChatTabTrackerOrder.get(i)
				: null;

			if (trackerType == null)
			{
				activeChatTabTrackerSlots.remove(componentId);
				restoreChatTabOverride(componentId);
				continue;
			}

			activeChatTabTrackerSlots.put(componentId, trackerType);
			applyChatTabTrackerOverride(componentId, CHAT_TAB_TRACKER_SLOT_NAMES[i], getChatTabTrackerText(trackerType), getChatTabTrackerLabel(trackerType), getChatTabTrackerColor(trackerType));
		}
	}

	private void restoreChatTabTrackerOverrides()
	{
		for (int componentId : CHAT_TAB_TRACKER_SLOT_COMPONENTS)
		{
			restoreChatTabOverride(componentId);
		}
	}

	private void updateChatTabTrackerOrder()
	{
		boolean changed = false;
		for (ChatTabTrackerType trackerType : ChatTabTrackerType.values())
		{
			boolean active = isChatTabTrackerActive(trackerType);
			if (active && !activeChatTabTrackerOrder.contains(trackerType))
			{
				activeChatTabTrackerOrder.add(trackerType);
				changed = true;
			}
			else if (!active && activeChatTabTrackerOrder.remove(trackerType))
			{
				changed = true;
			}
		}

		if (changed)
		{
			persistChatTabTrackerOrder();
		}
	}

	private void activateChatTabTracker(ChatTabTrackerType trackerType)
	{
		if (isChatTabTrackerActive(trackerType) && !activeChatTabTrackerOrder.contains(trackerType))
		{
			activeChatTabTrackerOrder.add(trackerType);
			persistChatTabTrackerOrder();
		}
	}

	private void loadChatTabTrackerOrder()
	{
		activeChatTabTrackerOrder.clear();
		String savedOrder = config.savedChatTabTrackerOrder();
		if (savedOrder == null || savedOrder.isBlank())
		{
			return;
		}

		for (String savedType : savedOrder.split(","))
		{
			try
			{
				ChatTabTrackerType trackerType = ChatTabTrackerType.valueOf(savedType.trim());
				if (!activeChatTabTrackerOrder.contains(trackerType))
				{
					activeChatTabTrackerOrder.add(trackerType);
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// Ignore stale values from older plugin versions.
			}
		}
	}

	private void persistChatTabTrackerOrder()
	{
		StringBuilder savedOrder = new StringBuilder();
		for (ChatTabTrackerType trackerType : activeChatTabTrackerOrder)
		{
			if (savedOrder.length() > 0)
			{
				savedOrder.append(',');
			}

			savedOrder.append(trackerType.name());
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_CHAT_TAB_TRACKER_ORDER_KEY, savedOrder.toString());
	}

	private boolean isChatTabTrackerActive(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case XP:
				return config.xpTracker() && getXpGained() > 0;
			case TOP_SKILL_XP:
				return config.topSkillTracker() && getTopXpSkill() != null;
			case PROFIT:
				return config.profitTracker() && profitValue > 0;
			case COST:
				return config.costTracker() && costValue > 0;
			case ACTIONS:
				return config.actionsTracker() && actionsValue > 0;
			default:
				return false;
		}
	}

	private String getChatTabTrackerText(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case XP:
				return formatShortValue(getXpGained());
			case TOP_SKILL_XP:
				return formatShortValue(getTopXpSkillValue());
			case PROFIT:
				return formatShortValue(profitValue);
			case COST:
				return formatShortValue(costValue);
			case ACTIONS:
				return formatShortValue(actionsValue);
			default:
				return "";
		}
	}

	private String getTopXpSkillText()
	{
		Skill topSkill = getTopXpSkill();
		if (topSkill == null)
		{
			return "";
		}

		switch (topSkill)
		{
			case CONSTRUCTION:
				return "Cons";
			case FIREMAKING:
				return "FM";
			case RUNECRAFT:
				return "RC";
			case WOODCUTTING:
				return "WC";
			default:
				String name = topSkill.getName();
				return name == null || name.isBlank() ? topSkill.name() : name;
		}
	}

	private int getChatTabTrackerColor(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case XP:
				return XP_TRACKER_COLOR;
			case TOP_SKILL_XP:
				return XP_TRACKER_COLOR;
			case PROFIT:
				return PROFIT_TRACKER_COLOR;
			case COST:
				return COST_TRACKER_COLOR;
			case ACTIONS:
				return ACTIONS_TRACKER_COLOR;
			default:
				return DEFAULT_CHAT_TAB_COLOR;
		}
	}

	private String getChatTabTrackerLabel(ChatTabTrackerType trackerType)
	{
		switch (trackerType)
		{
			case XP:
				return "XP";
			case TOP_SKILL_XP:
				return getTopXpSkillText();
			case PROFIT:
				return "Profit";
			case COST:
				return "Cost";
			case ACTIONS:
				return "Actions";
			default:
				return "";
		}
	}

	private void applyChatTabOverride(int componentId, String tabName, String text, int textColor)
	{
		Widget tab = client.getWidget(componentId);
		if (tab == null)
		{
			return;
		}

		List<Widget> widgets = new ArrayList<>();
		collectChatTabWidgets(tab, widgets, Collections.newSetFromMap(new IdentityHashMap<>()));
		Widget textWidget = findChatTabTextWidget(widgets, tabName);
		boolean appliedToTextWidget = false;

		for (Widget widget : widgets)
		{
			String widgetText = widget.getText();
			if (widgetText == null || widgetText.isEmpty())
			{
				continue;
			}

			rememberChatTabText(widget, widgetText);
			rememberChatTabStyle(widget);
			if (!appliedToTextWidget && widget == textWidget)
			{
				if (!text.equals(widgetText))
				{
					widget.setText(text);
				}

				applyChatTabStyle(widget, textColor);
				appliedToTextWidget = true;
			}
			else
			{
				widget.setText("");
			}
		}

		if (!appliedToTextWidget)
		{
			rememberChatTabText(tab, tab.getText());
			rememberChatTabStyle(tab);
			tab.setText(text);
			applyChatTabStyle(tab, textColor);
		}

		activeChatTabTexts.put(componentId, text);
		activeChatTabOverrides.add(componentId);
	}

	private void applyChatTabTrackerOverride(int componentId, String tabName, String valueText, String labelText, int textColor)
	{
		Widget tab = client.getWidget(componentId);
		if (tab == null)
		{
			return;
		}

		List<Widget> widgets = new ArrayList<>();
		collectChatTabWidgets(tab, widgets, Collections.newSetFromMap(new IdentityHashMap<>()));
		List<Widget> textWidgets = findChatTabTextWidgets(widgets, tabName);
		if (textWidgets.isEmpty())
		{
			rememberChatTabText(tab, tab.getText());
			rememberChatTabStyle(tab);
			tab.setText(valueText);
			applyChatTabValueStyle(tab, textColor);
			activeChatTabTexts.put(componentId, valueText);
			activeChatTabOverrides.add(componentId);
			return;
		}

		Widget valueWidget = textWidgets.get(0);
		Widget labelWidget = textWidgets.size() > 1 ? textWidgets.get(1) : null;
		int textX = valueWidget.getOriginalX();
		int textWidth = valueWidget.getOriginalWidth() > 0 ? valueWidget.getOriginalWidth() : valueWidget.getWidth();

		for (Widget widget : widgets)
		{
			String widgetText = widget.getText();
			if (widgetText == null || widgetText.isEmpty())
			{
				continue;
			}

			rememberChatTabText(widget, widgetText);
			rememberChatTabStyle(widget);
			if (widget == valueWidget)
			{
				if (!valueText.equals(widgetText))
				{
					widget.setText(valueText);
				}

				applyChatTabValueStyle(widget, textColor);
				positionChatTabTrackerText(widget, textX, CHAT_TAB_VALUE_Y_OFFSET, textWidth);
			}
			else if (widget == labelWidget)
			{
				if (!labelText.equals(widgetText))
				{
					widget.setText(labelText);
				}

				applyChatTabLabelStyle(widget);
				positionChatTabTrackerText(widget, textX, CHAT_TAB_LABEL_Y_OFFSET, textWidth);
			}
			else
			{
				widget.setText("");
			}
		}

		if (labelWidget == null)
		{
			valueWidget.setText(valueText + "<br>" + labelText);
			applyChatTabValueStyle(valueWidget, textColor);
		}

		activeChatTabTexts.put(componentId, valueText);
		activeChatTabTexts.put(-componentId, labelText);
		activeChatTabOverrides.add(componentId);
	}

	private void collectChatTabWidgets(Widget widget, List<Widget> widgets, Set<Widget> visitedWidgets)
	{
		if (widget == null || !visitedWidgets.add(widget))
		{
			return;
		}

		widgets.add(widget);
		collectChatTabWidgets(widget.getChildren(), widgets, visitedWidgets);
		collectChatTabWidgets(widget.getDynamicChildren(), widgets, visitedWidgets);
		collectChatTabWidgets(widget.getStaticChildren(), widgets, visitedWidgets);
		collectChatTabWidgets(widget.getNestedChildren(), widgets, visitedWidgets);
	}

	private void collectChatTabWidgets(Widget[] widgets, List<Widget> collectedWidgets, Set<Widget> visitedWidgets)
	{
		if (widgets == null)
		{
			return;
		}

		for (Widget widget : widgets)
		{
			collectChatTabWidgets(widget, collectedWidgets, visitedWidgets);
		}
	}

	private Widget findChatTabTextWidget(List<Widget> widgets, String tabName)
	{
		Widget firstTextWidget = null;
		for (Widget widget : widgets)
		{
			String text = widget.getText();
			if (text == null || text.isBlank())
			{
				continue;
			}

			if (firstTextWidget == null)
			{
				firstTextWidget = widget;
			}

			String cleanText = Text.removeTags(text).toLowerCase(Locale.ENGLISH);
			if (cleanText.contains(tabName))
			{
				return widget;
			}
		}

		return firstTextWidget;
	}

	private List<Widget> findChatTabTextWidgets(List<Widget> widgets, String tabName)
	{
		List<Widget> textWidgets = new ArrayList<>();
		List<Widget> matchingWidgets = new ArrayList<>();
		for (Widget widget : widgets)
		{
			String text = widget.getText();
			if (text == null || text.isBlank())
			{
				continue;
			}

			textWidgets.add(widget);
			String cleanText = Text.removeTags(text).toLowerCase(Locale.ENGLISH);
			if (cleanText.contains(tabName))
			{
				matchingWidgets.add(widget);
			}
		}

		if (matchingWidgets.size() >= 2)
		{
			return matchingWidgets;
		}

		if (matchingWidgets.size() == 1 && textWidgets.size() >= 2)
		{
			int index = textWidgets.indexOf(matchingWidgets.get(0));
			if (index >= 0 && index + 1 < textWidgets.size())
			{
				return textWidgets.subList(index, index + 2);
			}
		}

		return textWidgets.size() > 2 ? textWidgets.subList(0, 2) : textWidgets;
	}

	private void rememberChatTabText(Widget widget, String text)
	{
		if (text == null || text.isEmpty() || isChatTabOverrideText(text))
		{
			return;
		}

		chatTabOriginalTexts.putIfAbsent(widget.getId(), text);
	}

	private void rememberChatTabStyle(Widget widget)
	{
		chatTabOriginalStyles.putIfAbsent(widget.getId(), new ChatTabTextStyle(widget));
	}

	private void applyChatTabStyle(Widget widget, int textColor)
	{
		applyChatTabValueStyle(widget, textColor);
	}

	private void applyChatTabValueStyle(Widget widget, int textColor)
	{
		widget.setFontId(FontID.PLAIN_12);
		widget.setTextColor(textColor);
		widget.setTextShadowed(true);
		widget.setXTextAlignment(1);
		widget.setYTextAlignment(1);
	}

	private void applyChatTabLabelStyle(Widget widget)
	{
		widget.setFontId(FontID.PLAIN_11);
		widget.setTextColor(CHAT_TAB_LABEL_COLOR);
		widget.setTextShadowed(true);
		widget.setXTextAlignment(1);
		widget.setYTextAlignment(1);
	}

	private void positionChatTabTrackerText(Widget widget, int x, int y, int width)
	{
		widget.setOriginalX(x);
		widget.setRelativeX(x);
		widget.setOriginalY(y);
		widget.setRelativeY(y);
		if (width > 0)
		{
			widget.setOriginalWidth(width);
			widget.setWidth(width);
		}

		widget.setOriginalHeight(CHAT_TAB_TRACKER_TEXT_HEIGHT);
		widget.setHeight(CHAT_TAB_TRACKER_TEXT_HEIGHT);
	}

	private boolean isChatTabOverrideText(String text)
	{
		String cleanText = Text.removeTags(text);
		return activeChatTabTexts.containsValue(cleanText) || cleanText.equals(lastTradeClockText);
	}

	private void restoreChatTabOverrideLater(int componentId)
	{
		clientThread.invokeLater(() -> restoreChatTabOverride(componentId));
	}

	private void updateChatTabTrackersLater()
	{
		clientThread.invokeLater(this::updateChatTabTrackers);
	}

	private void restoreChatTabOverridesLater()
	{
		clientThread.invokeLater(this::restoreChatTabOverrides);
	}

	private void restoreChatTabOverride(int componentId)
	{
		if (!activeChatTabOverrides.contains(componentId))
		{
			return;
		}

		Widget tab = client.getWidget(componentId);
		if (tab == null)
		{
			return;
		}

		List<Widget> widgets = new ArrayList<>();
		collectChatTabWidgets(tab, widgets, Collections.newSetFromMap(new IdentityHashMap<>()));
		for (Widget widget : widgets)
		{
			String originalText = chatTabOriginalTexts.remove(widget.getId());
			if (originalText != null)
			{
				widget.setText(originalText);
			}

			ChatTabTextStyle originalStyle = chatTabOriginalStyles.remove(widget.getId());
			if (originalStyle != null)
			{
				originalStyle.restore(widget);
			}
		}

		activeChatTabTexts.remove(componentId);
		activeChatTabTexts.remove(-componentId);
		activeChatTabTrackerSlots.remove(componentId);
		activeChatTabOverrides.remove(componentId);
		if (componentId == ComponentID.CHATBOX_TAB_TRADE)
		{
			lastTradeClockText = "";
		}
	}

	private void restoreChatTabOverrides()
	{
		for (Map.Entry<Integer, String> entry : chatTabOriginalTexts.entrySet())
		{
			Widget widget = client.getWidget(entry.getKey());
			if (widget != null)
			{
				widget.setText(entry.getValue());
			}
		}

		for (Map.Entry<Integer, ChatTabTextStyle> entry : chatTabOriginalStyles.entrySet())
		{
			Widget widget = client.getWidget(entry.getKey());
			if (widget != null)
			{
				entry.getValue().restore(widget);
			}
		}

		chatTabOriginalTexts.clear();
		chatTabOriginalStyles.clear();
		activeChatTabTexts.clear();
		activeChatTabTrackerSlots.clear();
		activeChatTabOverrides.clear();
		lastTradeClockText = "";
	}

	private void forgetChatTabOverrideState()
	{
		chatTabOriginalTexts.clear();
		chatTabOriginalStyles.clear();
		activeChatTabTexts.clear();
		activeChatTabTrackerSlots.clear();
		activeChatTabOverrides.clear();
		lastTradeClockText = "";
	}

	private boolean isChatTabTrackerConfigKey(String key)
	{
		return "xpTracker".equals(key)
			|| "profitTracker".equals(key)
			|| "costTracker".equals(key)
			|| "actionsTracker".equals(key);
	}

	private void recordHerbSackCheck(MenuOptionClicked event)
	{
		if (!config.showHerbSackViewer()
			|| !"check".equals(cleanFarmingText(event.getMenuOption()))
			|| !isHerbSackItem(event.getItemId()))
		{
			return;
		}

		readingHerbSackContents = true;
		herbSackCheckTick = tickCount;
		pendingHerbSackQuantities.clear();
	}

	private void handleHerbSackChatMessage(ChatMessage event)
	{
		if (!readingHerbSackContents || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = cleanFarmingText(event.getMessage());
		if (message.contains("herb sack is empty"))
		{
			herbSackContents.clearLoaded();
			resetPendingHerbSackCheck();
			return;
		}

		HerbSackContents.ParsedHerbLine parsedLine = HerbSackContents.parseChatLine(message);
		if (parsedLine != null)
		{
			pendingHerbSackQuantities.put(parsedLine.getHerb(), parsedLine.getQuantity());
		}
	}

	private void finishPendingHerbSackCheckIfReady()
	{
		if (!readingHerbSackContents)
		{
			return;
		}

		int age = tickCount - herbSackCheckTick;
		if (!pendingHerbSackQuantities.isEmpty() && age > 0)
		{
			herbSackContents.setQuantities(pendingHerbSackQuantities);
			resetPendingHerbSackCheck();
			return;
		}

		if (age > HERB_SACK_CHECK_TIMEOUT_TICKS)
		{
			resetPendingHerbSackCheck();
		}
	}

	private void resetPendingHerbSackCheck()
	{
		readingHerbSackContents = false;
		herbSackCheckTick = Integer.MIN_VALUE;
		pendingHerbSackQuantities.clear();
	}

	static boolean isHerbSackItem(int itemId)
	{
		return itemId == ItemID.HERB_SACK
			|| itemId == ItemID.OPEN_HERB_SACK
			|| itemId == ItemID.SILKLINED_HERB_SACK
			|| itemId == ItemID.OPEN_SILKLINED_HERB_SACK;
	}

	private void ignoreInventoryTrackingBriefly()
	{
		inventoryTrackerIgnoreTicks = INVENTORY_TRACKER_IGNORE_TICKS;
		pendingInventoryValueChanges.clear();
		pendingHerbCleanings.clear();
	}

	private void ignoreInventoryTrackingForStorage()
	{
		inventoryTrackerIgnoreTicks = Math.max(inventoryTrackerIgnoreTicks, STORAGE_INVENTORY_TRACKER_IGNORE_TICKS);
		pendingInventoryValueChanges.clear();
		pendingHerbCleanings.clear();
	}

	private boolean isHerbSackInventoryTransferInteraction(MenuOptionClicked event)
	{
		return isHerbSackInventoryTransferOption(event.getItemId(), event.getMenuOption());
	}

	static boolean isHerbSackInventoryTransferOption(int itemId, String menuOption)
	{
		String option = cleanFarmingText(menuOption);
		return isHerbSackItem(itemId)
			&& (option.equals("empty")
			|| option.equals("fill")
			|| option.equals("open")
			|| option.equals("close"));
	}

	private boolean shouldIgnoreEquipmentTransferChange()
	{
		if (pendingEquipmentTransferTick == Integer.MIN_VALUE)
		{
			return false;
		}

		int age = tickCount - pendingEquipmentTransferTick;
		pendingEquipmentTransferTick = Integer.MIN_VALUE;
		return age >= 0 && age <= 2;
	}

	private static boolean isEquipmentTransferInteraction(MenuOptionClicked event)
	{
		String option = cleanFarmingText(event.getMenuOption());
		return "wear".equals(option)
			|| "wield".equals(option)
			|| "equip".equals(option)
			|| ("remove".equals(option) && event.getWidget() != null);
	}

	private boolean isStorageOrTradeInteraction(MenuOptionClicked event)
	{
		if (isPriceCheckerWidgetId(event.getWidgetId()))
		{
			return true;
		}

		String option = Text.removeTags(event.getMenuOption()).toLowerCase(Locale.ROOT);
		String target = Text.removeTags(event.getMenuTarget()).toLowerCase(Locale.ROOT);
		return option.contains("bank")
			|| option.contains("deposit")
			|| option.contains("withdraw")
			|| option.contains("collect")
			|| option.contains("exchange")
			|| option.contains("price-check")
			|| option.contains("trade with")
			|| option.contains("sell")
			|| option.contains("buy")
			|| target.contains("bank")
			|| target.contains("grand exchange")
			|| target.contains("guide price")
			|| target.contains("price checker")
			|| target.contains("shop");
	}

	private boolean isInventoryTransferInterfaceOpen()
	{
		return isPriceCheckerOpen()
			|| isVisibleWidget(client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.BANK_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.GRAND_EXCHANGE_WINDOW_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.GRAND_EXCHANGE_INVENTORY_INVENTORY_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.SHOP_INVENTORY_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.SEED_VAULT_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.SEED_VAULT_INVENTORY_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.GROUP_STORAGE_UI))
			|| isVisibleWidget(client.getWidget(ComponentID.GROUP_STORAGE_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.CHAMBERS_OF_XERIC_STORAGE_UNIT_PRIVATE_ITEM_CONTAINER))
			|| isVisibleWidget(client.getWidget(ComponentID.LOOTING_BAG_LOOTING_BAG_INVENTORY));
	}

	private boolean isPriceCheckerOpen()
	{
		Widget inventoryWidget = client.getWidget(ComponentID.GUIDE_PRICES_INVENTORY_ITEM_CONTAINER);
		Widget priceWidget = client.getWidget(ComponentID.GUIDE_PRICES_ITEM_CONTAINER);
		return isVisibleWidget(inventoryWidget) || isVisibleWidget(priceWidget);
	}

	private boolean isVisibleWidget(Widget widget)
	{
		return widget != null && !widget.isHidden();
	}

	private boolean isPriceCheckerWidgetId(int widgetId)
	{
		int groupId = widgetId >>> 16;
		return groupId == InterfaceID.GE_PRICECHECKER
			|| groupId == InterfaceID.GE_PRICECHECKER_SIDE;
	}

	private void detectCropProtectionDialog()
	{
		if (!config.showCropProtectionIndicator())
		{
			lastProtectionDialogText = "";
			return;
		}

		Widget textWidget = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
		if (!isVisibleWidget(textWidget))
		{
			lastProtectionDialogText = "";
			return;
		}

		String message = cleanFarmingText(textWidget.getText());
		if (!isCropProtectionMessage(message))
		{
			lastProtectionDialogText = "";
			return;
		}

		if (message.equals(lastProtectionDialogText))
		{
			return;
		}

		lastProtectionDialogText = message;
		WorldPoint paymentPatch = resolveProtectionPatchFromDialogNpc();
		if (paymentPatch != null)
		{
			markProtectedPatch(paymentPatch, true);
		}
		else
		{
			markProtectedPatch(true);
		}
	}

	private void syncVisibleFarmingPatchesAndReadyMarkers()
	{
		if (!config.showCompostIndicator()
			&& !config.showCropProtectionIndicator()
			&& !config.showBirdhouseSeedIndicator()
			&& !config.showPatchTimers())
		{
			return;
		}

		if (tickCount - lastHarvestSceneScanTick < HARVEST_SCENE_SCAN_INTERVAL)
		{
			return;
		}

		lastHarvestSceneScanTick = tickCount;
		Set<WorldPoint> patchPoints = scanVisibleFarmingPatchPoints(
			config.clearCompostOnHarvest() || config.showPatchTimers() || config.showCropProtectionIndicator());
		visibleFarmingPatches.clear();
		visibleFarmingPatches.addAll(patchPoints);
	}

	private boolean isProtectionPaymentInteraction(MenuOptionClicked event)
	{
		String option = cleanFarmingText(event.getMenuOption());
		String target = cleanFarmingText(event.getMenuTarget());
		Widget widget = event.getWidget();
		return option.contains("pay")
			|| option.contains("protect")
			|| target.contains("gardener")
			|| target.contains("farmer")
			|| isPatchPaymentOption(widget == null ? null : widget.getText());
	}

	private void recordProtectionPaymentSelection(MenuOptionClicked event)
	{
		MenuAction action = event.getMenuAction();
		Widget widget = event.getWidget();
		if (action == MenuAction.WIDGET_CONTINUE && widget != null && widget.getIndex() > 0 && isPatchPaymentOption(widget.getText()))
		{
			lastProtectionPatchNumber = widget.getIndex() - 1;
			return;
		}

		String option = cleanFarmingText(event.getMenuOption());
		if (!option.startsWith("pay"))
		{
			return;
		}

		lastProtectionPatchNumber = -1;
		if (action == MenuAction.NPC_THIRD_OPTION)
		{
			lastProtectionPatchNumber = 0;
		}
		else if (action == MenuAction.NPC_FOURTH_OPTION)
		{
			lastProtectionPatchNumber = 1;
		}
	}

	private WorldPoint resolveProtectionPatchFromDialogNpc()
	{
		WorldPoint anchor = lastProtectionInteraction;
		if (anchor == null || tickCount - lastProtectionInteractionTick > PROTECTION_INTERACTION_TICK_WINDOW)
		{
			return null;
		}

		Widget npcModel = client.getWidget(ComponentID.DIALOG_NPC_HEAD_MODEL);
		FarmingPatch patch = null;
		if (isVisibleWidget(npcModel) && npcModel.getModelType() == 2)
		{
			patch = FarmingPatch.fromFarmer(npcModel.getModelId(), lastProtectionPatchNumber, anchor);
		}
		if (patch == null)
		{
			patch = FarmingPatch.nearestProtectableTo(anchor, PROTECTION_PAYMENT_FALLBACK_DISTANCE);
		}
		return patch == null ? null : patch.getTile();
	}

	private void rememberProtectionInteraction(FarmingPatch menuPatch)
	{
		if (!isLoggedIn() || client.getLocalPlayer() == null)
		{
			return;
		}

		markSkillingActivity();
		lastProtectionInteraction = client.getLocalPlayer().getWorldLocation();
		lastProtectionInteractionTick = tickCount;
		lastProtectionPatchCandidate = null;
		if (menuPatch != null && menuPatch.supportsProtection())
		{
			lastProtectionPatchCandidate = menuPatch.getTile();
		}
	}

	private void recordCompostPatchSelection(MenuOptionClicked event)
	{
		if (!config.showCompostIndicator())
		{
			return;
		}

		FarmingPatch patch = resolveExactCompostPatch(event.getId());
		if (patch != null)
		{
			currentCompostPatch = patch;
		}
	}

	private FarmingPatch resolveExactCompostPatch(int objectId)
	{
		if (objectId <= 0)
		{
			return null;
		}

		try
		{
			ObjectComposition patchDef = client.getObjectDefinition(objectId);
			if (patchDef == null)
			{
				return null;
			}

			return FarmingPatch.fromObjectId(patchDef.getId());
		}
		catch (RuntimeException ignored)
		{
			return null;
		}
	}

	private void recordBirdhouseInteraction(MenuOptionClicked event)
	{
		String option = cleanFarmingText(event.getMenuOption());
		String target = cleanFarmingText(event.getMenuTarget());
		if (!isBirdhouseMenuTarget(option, target, event.getId()))
		{
			return;
		}

		WorldPoint point = resolveBirdhousePoint(event.getId(), event.getParam0(), event.getParam1());
		if (point == null)
		{
			return;
		}

		markSkillingActivity();
		lastBirdhouseInteraction = normalizeToTrackedBirdhousePoint(point);
		lastBirdhouseInteractionTick = tickCount;
		if (isBirdhouseClearAction(option))
		{
			clearBirdhouseSeeded(point);
		}
	}

	private void recordBirdhouseLootStats(Map<Integer, Long> gainedQuantities, Map<Integer, Long> spentQuantities)
	{
		if (gainedQuantities.isEmpty()
			|| !hasRecentBirdhouseInteraction()
			|| hasBirdNestItem(spentQuantities))
		{
			return;
		}

		long nests = 0L;
		long nestValue = 0L;
		for (Map.Entry<Integer, Long> entry : gainedQuantities.entrySet())
		{
			if (!isBirdNestItem(entry.getKey()))
			{
				continue;
			}

			long quantity = Math.max(0L, entry.getValue());
			nests += quantity;
			nestValue += getItemProfitValue(entry.getKey(), quantity);
		}

		if (nests <= 0)
		{
			return;
		}

		long nowMillis = System.currentTimeMillis();
		boolean countRun = lastBirdhouseStatsRunAtMillis <= 0L
			|| nowMillis - lastBirdhouseStatsRunAtMillis > BIRDHOUSE_STATS_RUN_GROUP_MILLIS;
		lastBirdhouseStatsRunAtMillis = nowMillis;
		addBirdhouseLootStats(nests, nestValue, countRun);
	}

	private void recordFarmingActivityStats(Map<Integer, Long> gainedQuantities, Map<Integer, Long> spentQuantities)
	{
		boolean withinFarmingStatsWindow = isWithinFarmingStatsWindow();
		if (!withinFarmingStatsWindow)
		{
			return;
		}

		Map<Integer, Long> seedQuantities = hasRecentBirdhouseInteraction() ? Collections.emptyMap() : spentQuantities;
		recordFarmingActivityStats(FarmingActivityType.HERBS, seedQuantities, gainedQuantities, HERB_SEED_ITEMS, HERB_HARVEST_ITEMS);
		recordFarmingActivityStats(FarmingActivityType.ALLOTMENTS, seedQuantities, gainedQuantities, ALLOTMENT_SEED_ITEMS, ALLOTMENT_HARVEST_ITEMS);
		recordFarmingActivityStats(FarmingActivityType.HOPS, seedQuantities, gainedQuantities, HOPS_SEED_ITEMS, HOPS_HARVEST_ITEMS);
	}

	private boolean isWithinFarmingStatsWindow()
	{
		return hasRecentFarmingStatsInteraction()
			|| (activeSkillingSkill == Skill.FARMING && tickCount - lastSkillingXpTick <= ACTIVE_SKILL_INVENTORY_TICKS);
	}

	private boolean hasRecentFarmingStatsInteraction()
	{
		return lastPatchInteraction != null
			&& tickCount - lastPatchInteractionTick <= FARMING_STATS_INTERACTION_TICK_WINDOW;
	}

	private void recordFarmingActivityStats(
		FarmingActivityType type,
		Map<Integer, Long> spentQuantities,
		Map<Integer, Long> gainedQuantities,
		Set<Integer> seedItems,
		Set<Integer> harvestItems)
	{
		long seedsUsed = sumQuantities(spentQuantities, seedItems);
		long seedCost = sumItemValues(spentQuantities, seedItems, false);
		long harvested = sumQuantities(gainedQuantities, harvestItems);
		long harvestValue = sumItemValues(gainedQuantities, harvestItems, true);
		if (seedsUsed <= 0 && harvested <= 0)
		{
			return;
		}

		addFarmingActivityStats(type, seedsUsed, seedCost, harvested, harvestValue, shouldCountFarmingRun(type));
	}

	private boolean shouldCountFarmingRun(FarmingActivityType type)
	{
		long nowMillis = System.currentTimeMillis();
		long lastRunAtMillis = lastFarmingStatsRunAtMillis.getOrDefault(type, 0L);
		lastFarmingStatsRunAtMillis.put(type, nowMillis);
		return lastRunAtMillis <= 0L || nowMillis - lastRunAtMillis > FARMING_STATS_RUN_GROUP_MILLIS;
	}

	private long sumQuantities(Map<Integer, Long> quantities, Set<Integer> itemIds)
	{
		long total = 0L;
		for (int itemId : itemIds)
		{
			total += quantities.getOrDefault(itemManager.canonicalize(itemId), 0L);
		}
		return total;
	}

	private long sumItemValues(Map<Integer, Long> quantities, Set<Integer> itemIds, boolean profitValue)
	{
		long total = 0L;
		for (int itemId : itemIds)
		{
			int canonicalItemId = itemManager.canonicalize(itemId);
			long quantity = quantities.getOrDefault(canonicalItemId, 0L);
			if (quantity <= 0)
			{
				continue;
			}

			total += profitValue
				? getItemProfitValue(canonicalItemId, quantity)
				: getItemCostValue(canonicalItemId, quantity);
		}
		return total;
	}

	private boolean hasBirdNestItem(Map<Integer, Long> quantities)
	{
		for (int itemId : quantities.keySet())
		{
			if (isBirdNestItem(itemId))
			{
				return true;
			}
		}

		return false;
	}

	private boolean isBirdNestItem(int itemId)
	{
		return isBirdNestItemName(getItemName(itemId));
	}

	private void markBirdhouseSeededFromInventoryChange(Map<Integer, Long> spentQuantities)
	{
		if (!config.showBirdhouseSeedIndicator()
			|| spentQuantities.isEmpty()
			|| !hasRecentBirdhouseInteraction())
		{
			return;
		}

		for (int itemId : spentQuantities.keySet())
		{
			if (isSeedItem(itemId))
			{
				markLastBirdhouseSeeded();
				return;
			}
		}
	}

	private void markPatchTimerFromInventoryChange(Map<Integer, Long> spentQuantities)
	{
		if (!config.showPatchTimers()
			|| spentQuantities.isEmpty()
			|| !hasRecentPatchInteraction())
		{
			return;
		}

		WorldPoint patchPoint = resolveRecentPatchPoint();
		if (patchPoint == null)
		{
			return;
		}

		PatchGrowthInfo bestGrowthInfo = null;
		for (int itemId : spentQuantities.keySet())
		{
			PatchGrowthInfo growthInfo = getPatchGrowthInfo(itemId);
			if (growthInfo != null
				&& isAnimaCropName(growthInfo.cropName)
				&& (bestGrowthInfo == null || growthInfo.growthMinutes > bestGrowthInfo.growthMinutes))
			{
				bestGrowthInfo = growthInfo;
			}
		}

		if (bestGrowthInfo == null)
		{
			return;
		}

		long doneAtMillis = System.currentTimeMillis() + bestGrowthInfo.growthMinutes * 60_000L;
		patchTimers.put(patchPoint, new PatchTimer(patchPoint, doneAtMillis, bestGrowthInfo.cropName));
		persistPatchTimers();
	}

	private void syncVisibleAnimaPatchTimer()
	{
		if (!config.showPatchTimers() || visibleAnimaPatches.isEmpty())
		{
			return;
		}

		PatchGrowthInfo growthInfo = getAnimaGrowthInfoFromVarbit();
		if (growthInfo == null)
		{
			clearVisibleAnimaPatchTimers();
			return;
		}

		for (WorldPoint point : visibleAnimaPatches)
		{
			syncAnimaPatchTimer(point, growthInfo);
		}
	}

	private void syncAnimaPatchTimer(WorldPoint point, PatchGrowthInfo growthInfo)
	{
		if (point == null || growthInfo == null || !isAnimaCropName(growthInfo.cropName))
		{
			return;
		}

		WorldPoint timerPoint = normalizeToTrackedAnimaPoint(point);
		PatchTimer timer = patchTimers.get(timerPoint);
		WorldPoint existingPoint = null;
		if (timer == null)
		{
			existingPoint = findAnimaTimerPoint(growthInfo.cropName);
			timer = existingPoint == null ? null : patchTimers.get(existingPoint);
		}

		long nowMillis = System.currentTimeMillis();
		long maxDoneAtMillis = nowMillis + growthInfo.growthMinutes * 60_000L;
		long doneAtMillis = maxDoneAtMillis;
		if (timer != null && growthInfo.cropName.equals(timer.getCropName()) && !timer.isExpired(nowMillis))
		{
			doneAtMillis = Math.min(timer.getDoneAtMillis(), maxDoneAtMillis);
		}

		if (existingPoint != null && !existingPoint.equals(timerPoint))
		{
			patchTimers.remove(existingPoint);
		}

		PatchTimer currentTimer = patchTimers.get(timerPoint);
		if (currentTimer == null
			|| !growthInfo.cropName.equals(currentTimer.getCropName())
			|| currentTimer.getDoneAtMillis() != doneAtMillis)
		{
			patchTimers.put(timerPoint, new PatchTimer(timerPoint, doneAtMillis, growthInfo.cropName));
			persistPatchTimers();
		}
	}

	private WorldPoint findAnimaTimerPoint(String cropName)
	{
		WorldPoint bestPoint = null;
		long bestDoneAtMillis = Long.MAX_VALUE;
		long nowMillis = System.currentTimeMillis();
		for (Map.Entry<WorldPoint, PatchTimer> entry : patchTimers.entrySet())
		{
			PatchTimer timer = entry.getValue();
			if (!cropName.equals(timer.getCropName()) || timer.isExpired(nowMillis))
			{
				continue;
			}

			if (timer.getDoneAtMillis() < bestDoneAtMillis)
			{
				bestDoneAtMillis = timer.getDoneAtMillis();
				bestPoint = entry.getKey();
			}
		}

		return bestPoint;
	}

	private void clearVisibleAnimaPatchTimers()
	{
		clearAnimaPatchTimers(visibleAnimaPatches);
	}

	private void clearAnimaPatchTimer(WorldPoint point)
	{
		if (point != null)
		{
			clearAnimaPatchTimers(Collections.singleton(point));
		}
	}

	private void clearAnimaPatchTimers(Set<WorldPoint> points)
	{
		boolean changed = false;
		for (WorldPoint point : points)
		{
			WorldPoint timerPoint = normalizeToTrackedAnimaPoint(point);
			changed |= removeNearestTrackedPatch(patchTimers.keySet(), timerPoint, TRACKED_PATCH_NORMALIZE_DISTANCE);
			changed |= patchTimers.remove(timerPoint) != null;
		}

		if (changed)
		{
			persistPatchTimers();
		}
	}

	private WorldPoint normalizeToTrackedAnimaPoint(WorldPoint point)
	{
		WorldPoint timerPoint = findNearestPoint(patchTimers.keySet(), point, TRACKED_PATCH_NORMALIZE_DISTANCE);
		return timerPoint == null ? point : timerPoint;
	}

	private PatchGrowthInfo getAnimaGrowthInfoFromVarbit()
	{
		int varbitValue = client.getVarbitValue(ANIMA_PATCH_VARBIT);
		if (varbitValue >= 8 && varbitValue <= 16)
		{
			return getAnimaGrowthInfo(ANIMA_ATTAS, varbitValue - 8);
		}

		if (varbitValue >= 17 && varbitValue <= 25)
		{
			return getAnimaGrowthInfo(ANIMA_IASOR, varbitValue - 17);
		}

		if (varbitValue >= 26 && varbitValue <= 34)
		{
			return getAnimaGrowthInfo(ANIMA_KRONOS, varbitValue - 26);
		}

		return null;
	}

	private static PatchGrowthInfo getAnimaGrowthInfo(String cropName, int stage)
	{
		int remainingStages = Math.max(1, ANIMA_STAGE_COUNT - stage);
		return new PatchGrowthInfo(cropName, remainingStages * ANIMA_STAGE_MINUTES);
	}

	private boolean hasRecentPatchInteraction()
	{
		return lastPatchInteraction != null
			&& tickCount - lastPatchInteractionTick <= PATCH_TIMER_INTERACTION_TICK_WINDOW;
	}

	private WorldPoint resolveRecentPatchPoint()
	{
		if (!hasRecentPatchInteraction())
		{
			return null;
		}

		return normalizeToKnownPatchPoint(lastPatchInteraction);
	}

	private void markLastBirdhouseSeeded()
	{
		WorldPoint birdhousePoint = resolveRecentBirdhousePoint();
		if (birdhousePoint == null)
		{
			return;
		}

		seededBirdhouses.add(birdhousePoint);
		birdhouseReadyAtMillis.put(birdhousePoint, System.currentTimeMillis() + BIRDHOUSE_DURATION_MILLIS);
		persistSeededBirdhouses();
	}

	private void clearLastBirdhouseSeeded()
	{
		WorldPoint birdhousePoint = resolveRecentBirdhousePoint();
		if (birdhousePoint == null)
		{
			return;
		}

		clearBirdhouseSeeded(birdhousePoint);
	}

	private void clearBirdhouseSeeded(WorldPoint point)
	{
		WorldPoint birdhousePoint = normalizeToTrackedBirdhousePoint(point);
		WorldPoint trackedPoint = findNearestPoint(seededBirdhouses, birdhousePoint, TRACKED_BIRDHOUSE_NORMALIZE_DISTANCE);
		boolean removed = removeNearestTrackedPatch(seededBirdhouses, birdhousePoint, TRACKED_BIRDHOUSE_NORMALIZE_DISTANCE);
		removed |= seededBirdhouses.remove(birdhousePoint);
		boolean removedTimestamp = false;
		if (trackedPoint != null)
		{
			removedTimestamp = birdhouseReadyAtMillis.remove(trackedPoint) != null;
		}
		removedTimestamp |= birdhouseReadyAtMillis.remove(birdhousePoint) != null;
		if (removed || removedTimestamp)
		{
			persistSeededBirdhouses();
		}
	}

	private void pruneReadyBirdhouses()
	{
		if (birdhouseReadyAtMillis.isEmpty())
		{
			return;
		}

		long nowMillis = System.currentTimeMillis();
		Set<WorldPoint> readyBirdhouses = new HashSet<>();
		for (Map.Entry<WorldPoint, Long> entry : birdhouseReadyAtMillis.entrySet())
		{
			if (entry.getValue() > 0L && entry.getValue() <= nowMillis)
			{
				readyBirdhouses.add(entry.getKey());
			}
		}

		if (readyBirdhouses.isEmpty())
		{
			return;
		}

		seededBirdhouses.removeAll(readyBirdhouses);
		for (WorldPoint point : readyBirdhouses)
		{
			birdhouseReadyAtMillis.remove(point);
		}
		persistSeededBirdhouses();
	}

	private WorldPoint resolveRecentBirdhousePoint()
	{
		if (!hasRecentBirdhouseInteraction())
		{
			return null;
		}

		return normalizeToTrackedBirdhousePoint(lastBirdhouseInteraction);
	}

	private boolean hasRecentBirdhouseInteraction()
	{
		return lastBirdhouseInteraction != null
			&& tickCount - lastBirdhouseInteractionTick <= BIRDHOUSE_INTERACTION_TICK_WINDOW;
	}

	private boolean hasRecentBirdhouseXpInteraction()
	{
		return lastBirdhouseInteraction != null
			&& tickCount - lastBirdhouseInteractionTick <= BIRDHOUSE_XP_INTERACTION_TICK_WINDOW;
	}

	private WorldPoint normalizeToTrackedBirdhousePoint(WorldPoint point)
	{
		WorldPoint trackedPoint = findNearestPoint(seededBirdhouses, point, TRACKED_BIRDHOUSE_NORMALIZE_DISTANCE);
		return trackedPoint == null ? point : trackedPoint;
	}

	private WorldPoint resolveBirdhousePoint(int objectId, int sceneX, int sceneY)
	{
		WorldPoint point = WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
		if (point != null)
		{
			return point;
		}

		if (client.getLocalPlayer() != null && isBirdhouseObject(objectId))
		{
			return client.getLocalPlayer().getWorldLocation();
		}

		return null;
	}

	private void markLastPatch(CompostType compostType)
	{
		WorldPoint patchPoint = resolveCompostMessagePatchPoint();
		if (patchPoint == null)
		{
			return;
		}

		compostedPatches.put(patchPoint, new CompostPatch(patchPoint, compostType));
		removeNearestTrackedPatch(needsCompostPatches, patchPoint, TRACKED_PATCH_NORMALIZE_DISTANCE);
		needsCompostPatches.remove(patchPoint);
		persistCompostPatches();
		persistNeedsCompostPatches();
	}

	private void markNeedsCompostPatch()
	{
		WorldPoint patchPoint = resolveCompostMessagePatchPoint();
		if (patchPoint == null)
		{
			return;
		}

		removeNearestTrackedPatch(compostedPatches.keySet(), patchPoint, TRACKED_PATCH_NORMALIZE_DISTANCE);
		compostedPatches.remove(patchPoint);
		needsCompostPatches.add(patchPoint);
		persistCompostPatches();
		persistNeedsCompostPatches();
	}

	private void markProtectedPatch(boolean trustedPaymentConfirmation)
	{
		WorldPoint patchPoint = resolveProtectionPatchPoint();
		if (patchPoint == null)
		{
			return;
		}

		markProtectedPatch(patchPoint, trustedPaymentConfirmation);
	}

	private void markProtectedPatch(WorldPoint patchPoint, boolean trustedPaymentConfirmation)
	{
		patchPoint = normalizeToProtectablePatchPoint(patchPoint);
		if (patchPoint == null)
		{
			return;
		}

		if (!trustedPaymentConfirmation
			&& visibleFarmingPatches.contains(patchPoint)
			&& !activeProtectableFarmingPatches.contains(patchPoint))
		{
			return;
		}

		protectedPatches.put(patchPoint, tickCount);
		persistProtectedPatches();
	}

	private WorldPoint resolveProtectionPatchPoint()
	{
		if (lastProtectionPatchCandidate != null && tickCount - lastProtectionInteractionTick <= PROTECTION_INTERACTION_TICK_WINDOW)
		{
			return lastProtectionPatchCandidate;
		}

		if (currentFarmingPatch != null
			&& currentFarmingPatch.supportsProtection()
			&& tickCount - lastPatchInteractionTick <= PROTECTION_INTERACTION_TICK_WINDOW)
		{
			return currentFarmingPatch.getTile();
		}

		return null;
	}

	private Set<WorldPoint> scanVisibleFarmingPatchPoints(boolean clearReadyMarkers)
	{
		Set<WorldPoint> patchPoints = new HashSet<>();
		Set<WorldPoint> visibleProtectablePoints = new HashSet<>();
		Set<WorldPoint> activeProtectablePoints = new HashSet<>();
		Set<WorldPoint> animaPatchPoints = new HashSet<>();
		Set<WorldPoint> deadAnimaPatchPoints = new HashSet<>();
		Scene scene = client.getScene();
		if (scene == null)
		{
			return patchPoints;
		}

		Tile[][][] tiles = scene.getTiles();
		int plane = client.getPlane();
		if (tiles == null || plane < 0 || plane >= tiles.length)
		{
			return patchPoints;
		}

		for (Tile[] row : tiles[plane])
		{
			if (row == null)
			{
				continue;
			}

			for (Tile tile : row)
			{
				if (tile == null || tile.getGameObjects() == null)
				{
					continue;
				}

				for (GameObject gameObject : tile.getGameObjects())
				{
					ObjectComposition composition = getObjectComposition(gameObject);
					if (composition != null && isFarmingObject(composition))
					{
						WorldPoint point = getKnownPatchPoint(gameObject, composition);
						patchPoints.add(point);
						WorldPoint visibleProtectablePoint = getVisibleProtectablePatchPoint(gameObject, composition);
						if (visibleProtectablePoint != null)
						{
							visibleProtectablePoints.add(visibleProtectablePoint);
						}

						WorldPoint activeProtectablePoint = getActiveProtectablePatchPoint(gameObject, composition);
						if (activeProtectablePoint != null)
						{
							activeProtectablePoints.add(activeProtectablePoint);
						}

						if (clearReadyMarkers && isHarvestableFarmingObject(composition))
						{
							markReadyPatch(point);
						}
					}

					if (composition != null && isDeadAnimaPatchObject(composition))
					{
						deadAnimaPatchPoints.add(gameObject.getWorldLocation());
					}
					else if (composition != null && isAnimaPatchObject(composition))
					{
						animaPatchPoints.add(gameObject.getWorldLocation());
					}
				}
			}
		}

		activeProtectableFarmingPatches.clear();
		activeProtectableFarmingPatches.addAll(activeProtectablePoints);
		visibleAnimaPatches.clear();
		visibleAnimaPatches.addAll(animaPatchPoints);
		clearAnimaPatchTimers(deadAnimaPatchPoints);
		pruneVisibleInactiveProtectedPatches(visibleProtectablePoints, activeProtectablePoints);
		return patchPoints;
	}

	private WorldPoint getVisibleProtectablePatchPoint(GameObject object, ObjectComposition composition)
	{
		FarmingPatch exactPatch = FarmingPatch.fromObjectId(composition.getId());
		if (exactPatch != null)
		{
			return exactPatch.supportsProtection() ? exactPatch.getTile() : null;
		}

		String name = composition.getName();
		String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
		if (!lowerName.contains("fake")
			&& !hasAction(composition, "rake")
			&& !isActiveProtectableFarmingObject(composition))
		{
			return null;
		}

		FarmingPatch patch = FarmingPatch.nearestProtectableTo(object.getWorldLocation(), getPatchObjectMatchDistance(composition));
		return patch == null ? null : patch.getTile();
	}

	private WorldPoint getActiveProtectablePatchPoint(GameObject object, ObjectComposition composition)
	{
		if (!isActiveProtectableFarmingObject(composition))
		{
			return null;
		}

		FarmingPatch patch = FarmingPatch.nearestProtectableTo(object.getWorldLocation(), getPatchObjectMatchDistance(composition));
		return patch == null ? null : patch.getTile();
	}

	private void pruneVisibleInactiveProtectedPatches(Set<WorldPoint> visiblePatchPoints, Set<WorldPoint> activeProtectablePoints)
	{
		if (!config.showCropProtectionIndicator() || protectedPatches.isEmpty())
		{
			return;
		}

		boolean changed = false;
		for (WorldPoint visiblePoint : visiblePatchPoints)
		{
			WorldPoint protectablePoint = normalizeToKnownProtectablePatchPoint(visiblePoint);
			if (protectablePoint != null
				&& !activeProtectablePoints.contains(protectablePoint)
				&& protectedPatches.remove(protectablePoint) != null)
			{
				changed = true;
			}
		}

		if (changed)
		{
			persistProtectedPatches();
		}
	}

	private void clearLastPatch(boolean showNeedsCompostAfterClear, boolean clearCompost)
	{
		WorldPoint patchPoint = resolveClearMessagePatchPoint();
		if (patchPoint == null)
		{
			return;
		}

		clearPatchMarkers(patchPoint, showNeedsCompostAfterClear, clearCompost);
	}

	private WorldPoint resolveCompostMessagePatchPoint()
	{
		return currentCompostPatch == null ? null : currentCompostPatch.getTile();
	}

	private WorldPoint resolveClearMessagePatchPoint()
	{
		if (currentFarmingPatch != null && tickCount - lastPatchInteractionTick <= PATCH_CLEAR_MESSAGE_TICK_WINDOW)
		{
			return currentFarmingPatch.getTile();
		}

		if (lastPatchInteraction != null && tickCount - lastPatchInteractionTick <= PATCH_CLEAR_MESSAGE_TICK_WINDOW)
		{
			return normalizeToTrackedPatchPoint(lastPatchInteraction);
		}

		return resolveCompostMessagePatchPoint();
	}

	private void clearPatchMarkers(WorldPoint point, boolean showNeedsCompostAfterClear, boolean clearCompost)
	{
		WorldPoint patchPoint = normalizeToTrackedPatchPoint(point);
		boolean changedCompost = clearCompost && removeTrackedMarker(compostedPatches.keySet(), patchPoint);
		boolean changedProtection = removeTrackedMarker(protectedPatches.keySet(), patchPoint);
		boolean changedNeeds = clearCompost && removeTrackedMarker(needsCompostPatches, patchPoint);
		boolean changedTimer = removeTrackedMarker(patchTimers.keySet(), patchPoint);
		if (clearCompost && showNeedsCompostAfterClear)
		{
			changedNeeds |= needsCompostPatches.add(patchPoint);
		}

		if (changedCompost)
		{
			persistCompostPatches();
		}

		if (changedProtection)
		{
			persistProtectedPatches();
		}

		if (changedNeeds)
		{
			persistNeedsCompostPatches();
		}

		if (changedTimer)
		{
			persistPatchTimers();
		}
	}

	private WorldPoint normalizeToTrackedPatchPoint(WorldPoint point)
	{
		WorldPoint knownPoint = normalizeToKnownPatchPoint(point);
		if (knownPoint != point)
		{
			return knownPoint;
		}

		WorldPoint compostPoint = findNearestPoint(compostedPatches.keySet(), point, TRACKED_PATCH_NORMALIZE_DISTANCE);
		if (compostPoint != null)
		{
			return compostPoint;
		}

		WorldPoint needsCompostPoint = findNearestPoint(needsCompostPatches, point, TRACKED_PATCH_NORMALIZE_DISTANCE);
		if (needsCompostPoint != null)
		{
			return needsCompostPoint;
		}

		WorldPoint timerPoint = findNearestPoint(patchTimers.keySet(), point, TRACKED_PATCH_NORMALIZE_DISTANCE);
		if (timerPoint != null)
		{
			return timerPoint;
		}

		return point;
	}

	private WorldPoint normalizeToProtectablePatchPoint(WorldPoint point)
	{
		return normalizeToKnownProtectablePatchPoint(point);
	}

	private WorldPoint normalizeToKnownProtectablePatchPoint(WorldPoint point)
	{
		FarmingPatch patch = FarmingPatch.nearestProtectableTo(point, TRACKED_PATCH_NORMALIZE_DISTANCE);
		return patch == null ? null : patch.getTile();
	}

	private WorldPoint normalizeToKnownPatchPoint(WorldPoint point)
	{
		FarmingPatch patch = FarmingPatch.nearestTo(point, KNOWN_PATCH_NORMALIZE_DISTANCE);
		return patch == null ? point : patch.getTile();
	}

	private void markReadyPatch(WorldPoint point)
	{
		WorldPoint patchPoint = normalizeToTrackedPatchPoint(point);
		clearReadyPatchMarkers(patchPoint);
	}

	private void clearReadyPatchMarkers(WorldPoint patchPoint)
	{
		boolean removedProtected = removeTrackedMarker(protectedPatches.keySet(), patchPoint);
		if (removedProtected)
		{
			persistProtectedPatches();
		}

		boolean removedTimer = removeTrackedMarker(patchTimers.keySet(), patchPoint);
		if (removedTimer)
		{
			persistPatchTimers();
		}

		if (!config.clearCompostOnHarvest())
		{
			return;
		}

		boolean removedCompost = removeTrackedMarker(compostedPatches.keySet(), patchPoint);
		if (removedCompost)
		{
			persistCompostPatches();
		}

		boolean removedNeedsCompost = removeTrackedMarker(needsCompostPatches, patchPoint);
		if (removedNeedsCompost)
		{
			persistNeedsCompostPatches();
		}
	}

	private boolean removeTrackedMarker(Set<WorldPoint> points, WorldPoint anchor)
	{
		if (points.remove(anchor))
		{
			return true;
		}

		return removeNearestTrackedPatch(points, anchor, KNOWN_PATCH_NORMALIZE_DISTANCE);
	}

	private void pruneExpiredPatchTimers()
	{
		if (patchTimers.isEmpty())
		{
			return;
		}

		long nowMillis = System.currentTimeMillis();
		boolean changed = patchTimers.values().removeIf(timer -> timer.isExpired(nowMillis));
		if (changed)
		{
			persistPatchTimers();
		}
	}

	private boolean removeNearestTrackedPatch(Set<WorldPoint> points, WorldPoint anchor, int maxDistance)
	{
		WorldPoint point = findNearestPoint(points, anchor, maxDistance);
		return point != null && points.remove(point);
	}

	private WorldPoint findNearestPoint(Set<WorldPoint> points, WorldPoint anchor, int maxDistance)
	{
		if (anchor == null)
		{
			return null;
		}

		WorldPoint nearestPoint = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (WorldPoint point : points)
		{
			if (point.getPlane() != anchor.getPlane())
			{
				continue;
			}

			int distance = point.distanceTo(anchor);
			if (distance < nearestDistance)
			{
				nearestDistance = distance;
				nearestPoint = point;
			}
		}

		return nearestDistance <= maxDistance ? nearestPoint : null;
	}

	private int getPatchObjectMatchDistance(ObjectComposition composition)
	{
		return Math.max(PATCH_READY_CLEAR_DISTANCE, Math.max(composition.getSizeX(), composition.getSizeY()) + 2);
	}

	private FarmingPatch resolveMenuPatch(int objectId, int sceneX, int sceneY, boolean allowSceneFallback)
	{
		FarmingPatch patch = FarmingPatch.fromObjectId(objectId);
		if (patch != null)
		{
			return patch;
		}

		ObjectComposition composition = getObjectComposition(objectId);
		if (composition != null)
		{
			patch = FarmingPatch.fromObjectId(composition.getId());
			if (patch != null)
			{
				return patch;
			}
		}

		if (!allowSceneFallback)
		{
			return null;
		}

		WorldPoint scenePoint = WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane());
		return FarmingPatch.nearestTo(scenePoint, KNOWN_PATCH_NORMALIZE_DISTANCE);
	}

	private WorldPoint getKnownPatchPoint(GameObject object, ObjectComposition composition)
	{
		FarmingPatch patch = composition == null ? null : FarmingPatch.fromObjectId(composition.getId());
		if (patch == null)
		{
			int maxDistance = composition == null ? KNOWN_PATCH_NORMALIZE_DISTANCE : getPatchObjectMatchDistance(composition);
			patch = FarmingPatch.nearestTo(object.getWorldLocation(), maxDistance);
		}

		return patch == null ? object.getWorldLocation() : patch.getTile();
	}

	private void loadCompostPatches()
	{
		compostedPatches.clear();
		String savedPatches = config.savedCompostPatches();
		if (savedPatches == null || savedPatches.isBlank())
		{
			return;
		}

		boolean changed = false;
		for (String savedPatch : savedPatches.split(";"))
		{
			String[] parts = savedPatch.split(",", 4);
			if (parts.length != 4)
			{
				changed = true;
				continue;
			}

			try
			{
				int x = Integer.parseInt(parts[0]);
				int y = Integer.parseInt(parts[1]);
				int plane = Integer.parseInt(parts[2]);
				CompostType compostType = CompostType.valueOf(parts[3]);
				WorldPoint point = normalizeToKnownPatchPoint(new WorldPoint(x, y, plane));
				if (isReasonableWorldPoint(point))
				{
					compostedPatches.put(point, new CompostPatch(point, compostType));
				}
				else
				{
					changed = true;
				}
			}
			catch (IllegalArgumentException ignored)
			{
				changed = true;
				// Ignore stale config values from older versions or manual edits.
			}
		}

		if (changed)
		{
			persistCompostPatches();
		}
	}

	private void persistCompostPatches()
	{
		StringBuilder savedPatches = new StringBuilder();
		for (CompostPatch patch : compostedPatches.values())
		{
			WorldPoint point = patch.getWorldPoint();
			if (savedPatches.length() > 0)
			{
				savedPatches.append(';');
			}

			savedPatches.append(point.getX())
				.append(',')
				.append(point.getY())
				.append(',')
				.append(point.getPlane())
				.append(',')
				.append(patch.getCompostType().name());
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_COMPOST_PATCHES_KEY, savedPatches.toString());
	}

	private void loadNeedsCompostPatches()
	{
		needsCompostPatches.clear();
		String savedPatches = config.savedNeedsCompostPatches();
		if (savedPatches == null || savedPatches.isBlank())
		{
			return;
		}

		boolean changed = false;
		for (String savedPatch : savedPatches.split(";"))
		{
			WorldPoint point = parseSavedPatchPoint(savedPatch);
			if (point != null)
			{
				needsCompostPatches.add(point);
			}
			else
			{
				changed = true;
			}
		}

		if (changed)
		{
			persistNeedsCompostPatches();
		}
	}

	private void persistNeedsCompostPatches()
	{
		configManager.setConfiguration(
			SkillingToolkitConfig.GROUP,
			SAVED_NEEDS_COMPOST_PATCHES_KEY,
			formatSavedPatchPoints(needsCompostPatches)
		);
	}

	private void loadProtectedPatches()
	{
		protectedPatches.clear();
		String savedPatches = config.savedProtectedPatches();
		if (savedPatches == null || savedPatches.isBlank())
		{
			return;
		}

		boolean changed = false;
		for (String savedPatch : savedPatches.split(";"))
		{
			String[] parts = savedPatch.split(",", 3);
			if (parts.length != 3)
			{
				changed = true;
				continue;
			}

			try
			{
				WorldPoint point = normalizeToProtectablePatchPoint(new WorldPoint(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2])
				));
				if (point != null)
				{
					protectedPatches.put(point, tickCount);
				}
				else
				{
					changed = true;
				}
			}
			catch (IllegalArgumentException ignored)
			{
				changed = true;
				// Ignore stale config values from older versions or manual edits.
			}
		}

		if (changed)
		{
			persistProtectedPatches();
		}
	}

	private void persistProtectedPatches()
	{
		StringBuilder savedPatches = new StringBuilder();
		for (WorldPoint point : protectedPatches.keySet())
		{
			if (savedPatches.length() > 0)
			{
				savedPatches.append(';');
			}

			savedPatches.append(point.getX())
				.append(',')
				.append(point.getY())
				.append(',')
				.append(point.getPlane());
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_PROTECTED_PATCHES_KEY, savedPatches.toString());
	}

	private void loadPatchTimers()
	{
		patchTimers.clear();
		String savedTimers = config.savedPatchTimers();
		if (savedTimers == null || savedTimers.isBlank())
		{
			return;
		}

		long nowMillis = System.currentTimeMillis();
		boolean changed = false;
		for (String savedTimer : savedTimers.split(";"))
		{
			String[] parts = savedTimer.split(",", 5);
			if (parts.length != 5)
			{
				changed = true;
				continue;
			}

			try
			{
				WorldPoint point = normalizeToKnownPatchPoint(new WorldPoint(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2])
				));
				long doneAtMillis = Long.parseLong(parts[3]);
				String cropName = parts[4];
				if (isReasonableWorldPoint(point) && doneAtMillis > nowMillis && isAnimaCropName(cropName))
				{
					patchTimers.put(point, new PatchTimer(point, doneAtMillis, cropName));
				}
				else
				{
					changed = true;
				}
			}
			catch (IllegalArgumentException ignored)
			{
				changed = true;
				// Ignore stale config values from older versions or manual edits.
			}
		}

		if (changed)
		{
			persistPatchTimers();
		}
	}

	private void persistPatchTimers()
	{
		StringBuilder savedTimers = new StringBuilder();
		long nowMillis = System.currentTimeMillis();
		for (PatchTimer timer : patchTimers.values())
		{
			if (timer.isExpired(nowMillis) || !isAnimaCropName(timer.getCropName()))
			{
				continue;
			}

			WorldPoint point = timer.getWorldPoint();
			if (savedTimers.length() > 0)
			{
				savedTimers.append(';');
			}

			savedTimers.append(point.getX())
				.append(',')
				.append(point.getY())
				.append(',')
				.append(point.getPlane())
				.append(',')
				.append(timer.getDoneAtMillis())
				.append(',')
				.append(timer.getCropName());
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_PATCH_TIMERS_KEY, savedTimers.toString());
	}

	private void loadSeededBirdhouses()
	{
		seededBirdhouses.clear();
		birdhouseReadyAtMillis.clear();
		String savedBirdhouses = config.savedSeededBirdhouses();
		if (savedBirdhouses == null || savedBirdhouses.isBlank())
		{
			return;
		}

		long nowMillis = System.currentTimeMillis();
		boolean changed = false;
		for (String savedBirdhouse : savedBirdhouses.split(";"))
		{
			String[] parts = savedBirdhouse.split(",", 4);
			if (parts.length < 3)
			{
				changed = true;
				continue;
			}

			try
			{
				WorldPoint point = new WorldPoint(
					Integer.parseInt(parts[0]),
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2])
				);
				if (!isReasonableWorldPoint(point))
				{
					changed = true;
					continue;
				}

				long readyAtMillis = parts.length == 4 ? Long.parseLong(parts[3]) : 0L;
				if (readyAtMillis > 0L && readyAtMillis <= nowMillis)
				{
					changed = true;
					continue;
				}

				seededBirdhouses.add(point);
				if (readyAtMillis > 0L)
				{
					birdhouseReadyAtMillis.put(point, readyAtMillis);
				}
			}
			catch (IllegalArgumentException ignored)
			{
				changed = true;
			}
		}

		if (changed)
		{
			persistSeededBirdhouses();
		}
	}

	private void persistSeededBirdhouses()
	{
		StringBuilder savedBirdhouses = new StringBuilder();
		for (WorldPoint point : seededBirdhouses)
		{
			if (savedBirdhouses.length() > 0)
			{
				savedBirdhouses.append(';');
			}

			savedBirdhouses.append(point.getX())
				.append(',')
				.append(point.getY())
				.append(',')
				.append(point.getPlane())
				.append(',')
				.append(birdhouseReadyAtMillis.getOrDefault(point, 0L));
		}

		configManager.setConfiguration(SkillingToolkitConfig.GROUP, SAVED_SEEDED_BIRDHOUSES_KEY, savedBirdhouses.toString());
	}

	private WorldPoint parseSavedPatchPoint(String savedPatch)
	{
		String[] parts = savedPatch.split(",", 3);
		if (parts.length != 3)
		{
			return null;
		}

		try
		{
			WorldPoint point = normalizeToKnownPatchPoint(new WorldPoint(
				Integer.parseInt(parts[0]),
				Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2])
			));
			return isReasonableWorldPoint(point) ? point : null;
		}
		catch (IllegalArgumentException ignored)
		{
			return null;
		}
	}

	private WorldPoint parseSavedWorldPoint(String savedPoint)
	{
		String[] parts = savedPoint.split(",", 3);
		if (parts.length != 3)
		{
			return null;
		}

		try
		{
			WorldPoint point = new WorldPoint(
				Integer.parseInt(parts[0]),
				Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2])
			);
			return isReasonableWorldPoint(point) ? point : null;
		}
		catch (IllegalArgumentException ignored)
		{
			return null;
		}
	}

	private static boolean isReasonableWorldPoint(WorldPoint point)
	{
		return point != null
			&& point.getX() >= 0
			&& point.getY() >= 0
			&& point.getX() < 20000
			&& point.getY() < 20000
			&& point.getPlane() >= 0
			&& point.getPlane() <= 3;
	}

	private String formatSavedPatchPoints(Set<WorldPoint> points)
	{
		StringBuilder savedPatches = new StringBuilder();
		for (WorldPoint point : points)
		{
			if (savedPatches.length() > 0)
			{
				savedPatches.append(';');
			}

			savedPatches.append(point.getX())
				.append(',')
				.append(point.getY())
				.append(',')
				.append(point.getPlane());
		}

		return savedPatches.toString();
	}

	private static CompostType parseCompostType(String message)
	{
		CompostType compostType = parseCompostType(COMPOST_USED_ON_PATCH.matcher(message));
		if (compostType != null)
		{
			return compostType;
		}

		compostType = parseCompostType(FERTILE_SOIL_CAST.matcher(message));
		if (compostType != null)
		{
			return compostType;
		}

		compostType = parseCompostType(ALREADY_TREATED.matcher(message));
		if (compostType != null)
		{
			return compostType;
		}

		return parseCompostType(INSPECT_PATCH_COMPOSTED.matcher(message));
	}

	private static CompostType parseCompostType(Matcher matcher)
	{
		if (!matcher.matches())
		{
			return null;
		}

		String compostType = matcher.group("compostType");
		if ("ultra".equals(compostType))
		{
			return CompostType.ULTRACOMPOST;
		}

		if ("super".equals(compostType))
		{
			return CompostType.SUPERCOMPOST;
		}

		return CompostType.COMPOST;
	}

	private static boolean isCropProtectionMessage(String message)
	{
		boolean paymentConfirmation = message.contains("leave it with me")
			&& (message.contains("grow")
				|| message.contains("safe")
				|| message.contains("protect")
				|| message.contains("look after")
				|| message.contains("take care")
				|| message.contains("nursery"));
		boolean alreadyProtected = message.contains("already looking after")
			|| message.contains("already looked after")
			|| message.contains("already taking care of")
			|| message.contains("already protecting");
		return paymentConfirmation || alreadyProtected;
	}

	private static boolean isPatchNeedsCompostMessage(String message)
	{
		return INSPECT_PATCH_NEEDS_COMPOST.matcher(message).matches()
			|| message.contains("soil has not been treated")
			|| message.contains("has not been treated with compost")
			|| message.contains("has not been treated with supercompost")
			|| message.contains("has not been treated with ultracompost");
	}

	private static boolean isFarmingStatusMessageType(ChatMessageType type)
	{
		return type == ChatMessageType.GAMEMESSAGE
			|| type == ChatMessageType.DIALOG
			|| type == ChatMessageType.MESBOX
			|| type == ChatMessageType.NPC_SAY
			|| type == ChatMessageType.SPAM;
	}

	private static boolean isPatchClearMessage(String message)
	{
		return CLEAR_HERB_PATCH.matcher(message).matches()
			|| CLEAR_ALLOTMENT.matcher(message).matches()
			|| CLEAR_PATCH.matcher(message).matches()
			|| CLEAR_TREE.matcher(message).matches()
			|| CLEAR_SEAWEED.matcher(message).matches()
			|| CLEAR_BELLADONNA.matcher(message).matches()
			|| CLEAR_MUSHROOM.matcher(message).matches()
			|| message.contains("harvest")
			|| message.contains("clear")
			|| message.contains("dig up")
			|| message.contains("remove the dead");
	}

	private static boolean shouldShowNeedsCompostAfterClear(String message)
	{
		return message.contains("soil has not been treated")
			|| message.contains("patch is now empty")
			|| message.contains("allotment is now empty")
			|| message.contains("successfully cleared this patch");
	}

	private static boolean isFarmingPatchName(String name)
	{
		if (name == null)
		{
			return false;
		}

		String lowerName = name.toLowerCase(Locale.ROOT);
		for (String patchNamePart : PATCH_NAME_PARTS)
		{
			if (lowerName.contains(patchNamePart))
			{
				return true;
			}
		}

		return false;
	}

	private static boolean isFarmingWorldInteraction(MenuAction action)
	{
		switch (action)
		{
			case ITEM_USE_ON_GAME_OBJECT:
			case WIDGET_TARGET_ON_GAME_OBJECT:
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case EXAMINE_OBJECT:
			case WORLD_ENTITY_FIRST_OPTION:
			case WORLD_ENTITY_SECOND_OPTION:
			case WORLD_ENTITY_THIRD_OPTION:
			case WORLD_ENTITY_FOURTH_OPTION:
			case WORLD_ENTITY_FIFTH_OPTION:
			case EXAMINE_WORLD_ENTITY:
				return true;
			default:
				return false;
		}
	}

	private ObjectComposition getObjectComposition(GameObject object)
	{
		if (object == null)
		{
			return null;
		}

		return getObjectComposition(object.getId());
	}

	private ObjectComposition getObjectComposition(int objectId)
	{
		if (objectId <= 0)
		{
			return null;
		}

		ObjectComposition composition = client.getObjectDefinition(objectId);
		return getActiveObjectComposition(composition);
	}

	private static ObjectComposition getActiveObjectComposition(ObjectComposition composition)
	{
		if (composition == null)
		{
			return null;
		}

		ObjectComposition impostor;
		try
		{
			impostor = composition.getImpostor();
		}
		catch (RuntimeException ignored)
		{
			return composition;
		}

		return impostor == null ? composition : impostor;
	}

	private static boolean isFarmingObject(ObjectComposition composition)
	{
		String name = composition.getName();
		return isFarmingPatchName(name)
			|| hasStrongFarmingAction(composition)
			|| (hasAction(composition, "inspect") && isFarmingCropName(name));
	}

	private static boolean isHarvestableFarmingObject(ObjectComposition composition)
	{
		if (!isFarmingObject(composition))
		{
			return false;
		}

		for (String actionPart : HARVESTABLE_FARMING_ACTION_PARTS)
		{
			if (hasAction(composition, actionPart))
			{
				return true;
			}
		}

		return false;
	}

	private static boolean isActiveProtectableFarmingObject(ObjectComposition composition)
	{
		if (!isFarmingObject(composition))
		{
			return false;
		}

		String name = composition.getName();
		String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
		if (lowerName.contains("fake") || lowerName.contains("dead") || lowerName.contains("diseased"))
		{
			return false;
		}

		if (hasAction(composition, "rake"))
		{
			return false;
		}

		if (isFarmingPatchName(lowerName) && !isFarmingCropName(lowerName))
		{
			return false;
		}

		return isFarmingCropName(lowerName)
			|| hasAction(composition, "inspect")
			|| hasAction(composition, "check-health")
			|| hasAction(composition, "harvest")
			|| hasAction(composition, "pick")
			|| hasAction(composition, "prune");
	}

	private void clearReadyPatchMarkerFromMenu(String optionText, String targetText, int sceneX, int sceneY, FarmingPatch menuPatch)
	{
		if (!config.clearCompostOnHarvest() && !config.showPatchTimers() && !config.showCropProtectionIndicator())
		{
			return;
		}

		String option = cleanFarmingText(optionText);
		String target = cleanFarmingText(targetText);
		if (!isHarvestableFarmingAction(option) || (!isFarmingPatchName(target) && !isFarmingCropName(target)))
		{
			return;
		}

		WorldPoint point = menuPatch == null
			? normalizeToKnownPatchPoint(WorldPoint.fromScene(client, sceneX, sceneY, client.getPlane()))
			: menuPatch.getTile();
		if (point != null)
		{
			markReadyPatch(point);
		}
	}

	private static boolean isHarvestableFarmingAction(String option)
	{
		for (String actionPart : HARVESTABLE_FARMING_ACTION_PARTS)
		{
			if (option.contains(actionPart))
			{
				return true;
			}
		}

		return false;
	}

	private static boolean hasStrongFarmingAction(ObjectComposition composition)
	{
		for (String actionPart : STRONG_FARMING_ACTION_PARTS)
		{
			if (hasAction(composition, actionPart))
			{
				return true;
			}
		}

		return false;
	}

	private static boolean hasAction(ObjectComposition composition, String actionPart)
	{
		String[] actions = composition.getActions();
		if (actions == null)
		{
			return false;
		}

		for (String action : actions)
		{
			if (action != null && action.toLowerCase(Locale.ROOT).contains(actionPart))
			{
				return true;
			}
		}

		return false;
	}

	private static boolean isFarmingCropName(String name)
	{
		if (name == null)
		{
			return false;
		}

		String lowerName = name.toLowerCase(Locale.ROOT);
		for (String cropNamePart : FARMING_CROP_NAME_PARTS)
		{
			if (lowerName.contains(cropNamePart))
			{
				return true;
			}
		}

		return false;
	}

	private static boolean isPatchPaymentOption(String text)
	{
		String option = cleanFarmingText(text);
		return option.contains("patch")
			|| option.contains("allotment")
			|| option.contains("protect")
			|| option.contains("tree")
			|| option.contains("hardwood")
			|| option.contains("teak")
			|| option.contains("mahogany")
			|| option.contains("crop");
	}

	private boolean isBirdhouseMenuTarget(String option, String target, int objectId)
	{
		return isBirdhouseText(option)
			|| isBirdhouseText(target)
			|| isBirdhouseObject(objectId);
	}

	private boolean isBirdhouseObject(int objectId)
	{
		ObjectComposition composition = getObjectComposition(objectId);
		return composition != null && isBirdhouseText(composition.getName());
	}

	private boolean isBirdhouseSeededMessage(String message)
	{
		boolean birdhouseMessage = isBirdhouseText(message);
		if (!birdhouseMessage && !hasRecentBirdhouseInteraction())
		{
			return false;
		}

		if (isBirdhouseNeedsSeedMessage(message))
		{
			return false;
		}

		boolean seedAction = message.contains("seed")
			&& (message.contains("add")
				|| message.contains("fill")
				|| message.contains("filled")
				|| message.contains("place")
				|| message.contains("put")
				|| message.contains("bait"));
		boolean baited = message.contains("baited")
			|| message.contains("bait the")
			|| message.contains("has bait");
		return seedAction || (birdhouseMessage && baited);
	}

	private boolean isBirdhouseClearMessage(String message)
	{
		boolean birdhouseMessage = isBirdhouseText(message);
		if (!birdhouseMessage && !hasRecentBirdhouseInteraction())
		{
			return false;
		}

		return isBirdhouseNeedsSeedMessage(message)
			|| message.contains("dismantle")
			|| message.contains("dismantled")
			|| message.contains("take down")
			|| message.contains("taken down")
			|| message.contains("remove the bird")
			|| message.contains("removed the bird")
			|| message.contains("retrieve the clockwork")
			|| message.contains("get your clockwork back");
	}

	private static boolean isBirdhouseNeedsSeedMessage(String message)
	{
		return isBirdhouseText(message)
			&& (message.contains("no seed")
				|| message.contains("no bait")
				|| message.contains("not baited")
				|| message.contains("not been baited")
				|| message.contains("needs seed")
				|| message.contains("need seed")
				|| message.contains("empty"));
	}

	private static boolean isBirdhouseClearAction(String option)
	{
		return option.contains("dismantle")
			|| option.contains("empty")
			|| option.contains("remove")
			|| option.contains("take-down")
			|| option.contains("take down");
	}

	private PatchGrowthInfo getPatchGrowthInfo(int itemId)
	{
		try
		{
			String itemName = itemManager.getItemComposition(itemManager.canonicalize(itemId)).getName();
			String cropName = normalizePlantingCropName(itemName);
			if (cropName.isEmpty())
			{
				return null;
			}

			if (!isAnimaCropName(cropName))
			{
				return null;
			}

			return PATCH_GROWTH_TIMES.get(cropName);
		}
		catch (RuntimeException ignored)
		{
			return null;
		}
	}

	private static String normalizePlantingCropName(String itemName)
	{
		String name = cleanFarmingText(itemName);
		if (name.isEmpty())
		{
			return "";
		}

		if (name.equals("acorn"))
		{
			return "oak";
		}

		if (name.equals("crystal acorn"))
		{
			return "crystal tree";
		}

		if (!isPlantingItemName(name))
		{
			return "";
		}

		name = removePlantingSuffix(name, " seeds");
		name = removePlantingSuffix(name, " seed");
		name = removePlantingSuffix(name, " sapling");
		name = removePlantingSuffix(name, " spores");
		name = removePlantingSuffix(name, " spore");

		if (name.equals("spirit"))
		{
			return "spirit tree";
		}

		if (name.equals("crystal"))
		{
			return "crystal tree";
		}

		if (name.endsWith(" tree"))
		{
			name = name.substring(0, name.length() - " tree".length()).trim();
		}

		return name;
	}

	private static boolean isAnimaPatchObject(ObjectComposition composition)
	{
		return composition != null
			&& isAnimaPatchText(composition.getName())
			&& !isDeadAnimaPatchObject(composition);
	}

	private static boolean isDeadAnimaPatchObject(ObjectComposition composition)
	{
		if (composition == null)
		{
			return false;
		}

		String name = cleanFarmingText(composition.getName());
		return name.contains("dead") && isAnimaPatchText(name);
	}

	private static boolean isAnimaPatchText(String text)
	{
		String name = cleanFarmingText(text);
		return name.contains("anima patch")
			|| name.contains("attas plant")
			|| name.contains("iasor plant")
			|| name.contains("kronos plant");
	}

	private static boolean isAnimaCropName(String cropName)
	{
		String name = cleanFarmingText(cropName);
		return ANIMA_ATTAS.equals(name)
			|| ANIMA_IASOR.equals(name)
			|| ANIMA_KRONOS.equals(name);
	}

	private static boolean isPlantingItemName(String name)
	{
		return name.contains("seed")
			|| name.contains("sapling")
			|| name.contains("spore")
			|| name.contains("acorn");
	}

	private static String removePlantingSuffix(String name, String suffix)
	{
		if (name.endsWith(suffix))
		{
			return name.substring(0, name.length() - suffix.length()).trim();
		}

		return name;
	}

	private static Map<String, PatchGrowthInfo> createPatchGrowthTimes()
	{
		Map<String, PatchGrowthInfo> times = new HashMap<>();
		addGrowthTime(times, "attas", 5760);
		addGrowthTime(times, "iasor", 5760);
		addGrowthTime(times, "kronos", 5760);
		return Collections.unmodifiableMap(times);
	}

	private static void addGrowthTime(Map<String, PatchGrowthInfo> times, String cropName, int growthMinutes)
	{
		times.put(cropName, new PatchGrowthInfo(cropName, growthMinutes));
	}

	private static Set<Integer> immutableItemSet(int... itemIds)
	{
		Set<Integer> set = new HashSet<>();
		for (int itemId : itemIds)
		{
			set.add(itemId);
		}
		return Collections.unmodifiableSet(set);
	}

	private boolean isSeedItem(int itemId)
	{
		try
		{
			String itemName = itemManager.getItemComposition(itemManager.canonicalize(itemId)).getName();
			return cleanFarmingText(itemName).contains("seed");
		}
		catch (RuntimeException ignored)
		{
			return false;
		}
	}

	private static boolean isBirdNestItemName(String itemName)
	{
		String name = cleanFarmingText(itemName);
		return name.contains("nest") && !name.contains("nest box");
	}

	private static boolean isBirdNestOpeningOutputName(String itemName)
	{
		String name = cleanFarmingText(itemName);
		return isBirdNestItemName(name)
			|| name.contains("seed")
			|| name.contains("clue scroll")
			|| name.endsWith(" egg")
			|| name.contains(" egg ")
			|| name.startsWith("ring of")
			|| name.endsWith(" ring")
			|| name.contains(" ring ");
	}

	private static boolean isBirdhouseText(String text)
	{
		String option = cleanFarmingText(text);
		return option.contains("bird house") || option.contains("birdhouse");
	}

	private static String cleanFarmingText(String text)
	{
		if (text == null)
		{
			return "";
		}

		return Text.removeTags(text.replace("<br>", " ")).toLowerCase(Locale.ROOT);
	}

	private long parseLongConfig(String value)
	{
		if (value == null || value.isBlank())
		{
			return 0L;
		}

		try
		{
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private String formatShortValue(long value)
	{
		return QuantityFormatter.quantityToStackSize(value).toLowerCase(Locale.ENGLISH);
	}

	private void markSkillingActivity()
	{
		if (!ownsToolkitUi() && !isRecentToolkitOwner(TOOLKIT_UI_OWNER_PVM, TOOLKIT_UI_OWNER_GRACE_TICKS))
		{
			setToolkitUiOwner(TOOLKIT_UI_OWNER_SKILLING);
		}
	}

	private boolean handleToolkitTabToggle(MenuOptionClicked event)
	{
		if (!isAllChatTabClick(event))
		{
			return false;
		}

		int currentTick = client.getTickCount();
		long lastToggleTick = parseLongConfig(configManager.getConfiguration(
			TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_LAST_TOGGLE_TICK_KEY));
		if (lastToggleTick == currentTick)
		{
			return true;
		}

		configManager.setConfiguration(
			TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_LAST_TOGGLE_TICK_KEY, Integer.toString(currentTick));
		boolean skillingEnabled = isToolkitPluginEnabled(SKILLING_PLUGIN_CLASS_NAME);
		boolean pvmEnabled = isToolkitPluginEnabled(PVM_PLUGIN_CLASS_NAME);
		String owner = configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY);
		String nextOwner;
		if (skillingEnabled && pvmEnabled)
		{
			nextOwner = TOOLKIT_UI_OWNER_PVM.equals(owner) ? TOOLKIT_UI_OWNER_SKILLING : TOOLKIT_UI_OWNER_PVM;
		}
		else
		{
			nextOwner = pvmEnabled ? TOOLKIT_UI_OWNER_PVM : TOOLKIT_UI_OWNER_SKILLING;
		}

		setToolkitUiOwner(nextOwner);
		return true;
	}

	private static boolean isAllChatTabClick(MenuOptionClicked event)
	{
		Widget widget = event.getWidget();
		return event.getParam1() == ComponentID.CHATBOX_TAB_ALL
			|| (widget != null && (widget.getId() == ComponentID.CHATBOX_TAB_ALL
				|| widget.getParentId() == ComponentID.CHATBOX_TAB_ALL));
	}

	private void ensureToolkitOwnerAvailable()
	{
		String owner = configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY);
		if (TOOLKIT_UI_OWNER_PVM.equals(owner) && isToolkitPluginEnabled(PVM_PLUGIN_CLASS_NAME))
		{
			return;
		}

		if (TOOLKIT_UI_OWNER_SKILLING.equals(owner) && isToolkitPluginEnabled(SKILLING_PLUGIN_CLASS_NAME))
		{
			return;
		}

		setToolkitUiOwner(TOOLKIT_UI_OWNER_SKILLING);
	}

	private boolean isToolkitPluginEnabled(String className)
	{
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (className.equals(plugin.getClass().getName()) && pluginManager.isPluginEnabled(plugin))
			{
				return true;
			}
		}

		return false;
	}

	private void setToolkitUiOwner(String owner)
	{
		configManager.setConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY, owner);
		configManager.setConfiguration(
			TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_TICK_KEY, Integer.toString(client.getTickCount()));
		clientThread.invokeLater(() ->
		{
			syncInventoryInfoBox();
			updateChatTabTrackers();
		});
	}

	private boolean ownsToolkitUi()
	{
		return TOOLKIT_UI_OWNER_SKILLING.equals(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY));
	}

	private boolean isOtherToolkitUiOwnerActive()
	{
		return TOOLKIT_UI_OWNER_PVM.equals(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY))
			&& isToolkitPluginEnabled(PVM_PLUGIN_CLASS_NAME);
	}

	private boolean isRecentToolkitOwner(String owner, int graceTicks)
	{
		if (!owner.equals(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_KEY)))
		{
			return false;
		}

		long ownerTick = parseLongConfig(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_TICK_KEY));
		int currentTick = client.getTickCount();
		return ownerTick <= currentTick && currentTick - ownerTick <= graceTicks;
	}

	private boolean isToolkitUiReady()
	{
		if (!ownsToolkitUi())
		{
			return false;
		}

		long ownerTick = parseLongConfig(configManager.getConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_TICK_KEY));
		int currentTick = client.getTickCount();
		if (ownerTick > currentTick)
		{
			configManager.setConfiguration(TOOLKIT_UI_COORDINATION_GROUP, TOOLKIT_UI_OWNER_TICK_KEY, Integer.toString(Math.max(0, currentTick - 1)));
			return true;
		}

		return ownerTick < currentTick;
	}

	private boolean isLoggedIn()
	{
		return client.getGameState() == GameState.LOGGED_IN;
	}

	private boolean isResetGameState(GameState gameState)
	{
		return gameState == GameState.STARTING
			|| gameState == GameState.LOGIN_SCREEN
			|| gameState == GameState.LOGIN_SCREEN_AUTHENTICATOR
			|| gameState == GameState.LOGGING_IN
			|| gameState == GameState.CONNECTION_LOST
			|| gameState == GameState.UNKNOWN;
	}

	private enum ChatTabTrackerType
	{
		XP,
		TOP_SKILL_XP,
		PROFIT,
		COST,
		ACTIONS
	}

	private static final class ChatTabTextStyle
	{
		private final int fontId;
		private final int textColor;
		private final boolean textShadowed;
		private final int xTextAlignment;
		private final int yTextAlignment;

		private ChatTabTextStyle(Widget widget)
		{
			fontId = widget.getFontId();
			textColor = widget.getTextColor();
			textShadowed = widget.getTextShadowed();
			xTextAlignment = widget.getXTextAlignment();
			yTextAlignment = widget.getYTextAlignment();
		}

		private void restore(Widget widget)
		{
			widget.setFontId(fontId);
			widget.setTextColor(textColor);
			widget.setTextShadowed(textShadowed);
			widget.setXTextAlignment(xTextAlignment);
			widget.setYTextAlignment(yTextAlignment);
		}
	}

	private static final class PendingInventoryValueChange
	{
		private final long gainedValue;
		private final long spentValue;
		private final int tick;

		private PendingInventoryValueChange(long gainedValue, long spentValue, int tick)
		{
			this.gainedValue = gainedValue;
			this.spentValue = spentValue;
			this.tick = tick;
		}
	}

	static final class InventoryValueAdjustment
	{
		final long gainedValue;
		final long spentValue;

		private InventoryValueAdjustment(long gainedValue, long spentValue)
		{
			this.gainedValue = gainedValue;
			this.spentValue = spentValue;
		}
	}

	private static final class ChargedItemVariant
	{
		private final String baseName;
		private final int charges;

		private ChargedItemVariant(String baseName, int charges)
		{
			this.baseName = baseName;
			this.charges = charges;
		}
	}

	private static final class PatchGrowthInfo
	{
		private final String cropName;
		private final int growthMinutes;

		private PatchGrowthInfo(String cropName, int growthMinutes)
		{
			this.cropName = cropName;
			this.growthMinutes = growthMinutes;
		}
	}

	private static final class PendingHerbCleaning
	{
		private final int cleanHerbId;
		private long quantity;
		private final int tick;

		private PendingHerbCleaning(int cleanHerbId, long quantity, int tick)
		{
			this.cleanHerbId = cleanHerbId;
			this.quantity = quantity;
			this.tick = tick;
		}
	}
}
