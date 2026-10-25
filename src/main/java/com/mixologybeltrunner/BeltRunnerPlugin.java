package com.mixologybeltrunner;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetDrag;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Mixology Belt Runner",
	description = "Guides the pre-make style of Mastering Mixology",
	tags = {"mixology", "herblore", "minigame"}
)
public class BeltRunnerPlugin extends Plugin
{
	static final String SLOT_MAP_KEY = "slotMap";

	private static final int[] ORDER_TYPE_VARBITS = {
		VarbitID.MM_LAB_ORDER_1_TYPE, VarbitID.MM_LAB_ORDER_2_TYPE, VarbitID.MM_LAB_ORDER_3_TYPE,
	};
	private static final int[] ORDER_MODIFIER_VARBITS = {
		VarbitID.MM_LAB_ORDER_1_MODIFIER, VarbitID.MM_LAB_ORDER_2_MODIFIER, VarbitID.MM_LAB_ORDER_3_MODIFIER,
	};
	private static final int[] VESSEL_SLOT_VARBITS = {
		VarbitID.MM_LAB_MIXER_SLOT_0, VarbitID.MM_LAB_MIXER_SLOT_1, VarbitID.MM_LAB_MIXER_SLOT_2,
	};
	private static final int[] DIGWEED_VARBITS = {
		VarbitID.MM_HERB_READY_1, VarbitID.MM_HERB_READY_2, VarbitID.MM_HERB_READY_3, VarbitID.MM_HERB_READY_4,
	};
	private static final LabObject[] DIGWEED_SPOTS = {
		LabObject.DIGWEED_NORTH_EAST, LabObject.DIGWEED_SOUTH_EAST, LabObject.DIGWEED_SOUTH_WEST, LabObject.DIGWEED_NORTH_WEST,
	};
	private static final String BELT_OPTION = "Fulfil-order";
	private static final String BELT_TARGET = "Conveyor belt";
	private static final String DIGWEED_TARGET_PREFIX = "Digweed -> ";
	private static final String DIGWEED_TARGET_SUFFIX = " -> Digweed";
	/**
	 * Ticks to wait for the login inventory before starting with whatever is there.
	 */
	private static final int LOGIN_INVENTORY_WAIT = 5;
	private static final int ALL_RECIPES_LEVEL = 81;
	private static final String VESSEL_TARGET = "Mixing vessel";
	private static final String LEVER_OPTION = "Operate";
	private static final int PENDING_PULL_TICKS = 3;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Gson gson;

	@Inject
	private GuideOverlay guideOverlay;

	@Inject
	private LabOverlay labOverlay;

	@Inject
	private LabLabelsOverlay labLabelsOverlay;

	@Inject
	private PathOverlay pathOverlay;

	@Inject
	private PathMinimapOverlay pathMinimapOverlay;

	@Inject
	private PathRouter pathRouter;

	@Inject
	private InventoryOverlay inventoryOverlay;

	@Inject
	private BeltRunnerConfig config;

	private final Guide guide = new Guide();
	private InventoryTracker tracker;
	private StockPlan plan = StockPlan.create(SlotMap.SIZE);
	private List<Order> orders = Collections.emptyList();

	/**
	 * Name of the active rival plugin, or null.
	 */
	private String rival;
	private boolean rivalChecked;
	/**
	 * True once this login has been handled; reset on logout and shutdown.
	 */
	private boolean loginHandled;
	/**
	 * Waiting for the inventory to load before tracking starts.
	 */
	private boolean startPending;
	private boolean restoreOnStart;
	/**
	 * Set when the saved map went stale while dormant or switched off: the next start must
	 * not restore it, even if unsetting the key could not reach the profile at the time.
	 */
	private boolean discardSaved;
	/**
	 * Ticks left to wait for the inventory to arrive after a login, or 0 to start on the next tick.
	 */
	private int startWait;
	/**
	 * True while the tracker is fed game events.
	 */
	private boolean tracking;
	/**
	 * True once a potion has been taken from the vessel since the plugin started.
	 */
	private boolean madeFirstPotion;
	/**
	 * The tick of the last belt click let through.
	 */
	private int beltClickTick = -1;
	/**
	 * Lever pulls clicked but not yet shown in the vessel slots, oldest first.
	 */
	private final List<PotionComponent> pendingPulls = new ArrayList<>();
	private int pendingPullTick;
	private int savedRevision = -1;
	private boolean ordersQueued;
	private boolean dragReleaseSeen;
	private final QuickActionWindow quickWindow = new QuickActionWindow();
	private int alembicProgress;
	private int agitatorProgress;

	private volatile Station quickAction;
	private volatile Step step;
	private volatile List<WorldPoint> path = Collections.emptyList();
	private volatile boolean inLab;

	@Override
	protected void startUp()
	{
		tracker = new InventoryTracker(Guide.assigner(() -> step, () -> plan, () -> orders));
		guide.reset();
		plan = StockPlan.create(SlotMap.SIZE);
		orders = Collections.emptyList();
		rival = null;
		rivalChecked = false;
		loginHandled = false;
		startPending = false;
		pendingPulls.clear();
		madeFirstPotion = false;
		tracking = false;
		savedRevision = -1;
		ordersQueued = false;
		dragReleaseSeen = false;
		step = null;
		inLab = false;
		quickWindow.reset();
		quickAction = null;
		alembicProgress = 0;
		agitatorProgress = 0;
		path = Collections.emptyList();
		pathRouter.reset();
		overlayManager.add(guideOverlay);
		overlayManager.add(labOverlay);
		overlayManager.add(labLabelsOverlay);
		overlayManager.add(pathOverlay);
		overlayManager.add(pathMinimapOverlay);
		overlayManager.add(inventoryOverlay);
		clientThread.invokeLater(() ->
		{
			checkRivals();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				// Already logged in: the inventory is loaded.
				onLogin(0);
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(guideOverlay);
		overlayManager.remove(labOverlay);
		overlayManager.remove(labLabelsOverlay);
		overlayManager.remove(pathOverlay);
		overlayManager.remove(pathMinimapOverlay);
		overlayManager.remove(inventoryOverlay);
		// Turning the plugin off forgets the map, so turning it back on starts fresh. Closing
		// the client does not call shutDown, so a relog still restores.
		discardSavedMap();
		tracking = false;
		startPending = false;
		loginHandled = false;
		rival = null;
		step = null;
		inLab = false;
		quickWindow.reset();
		quickAction = null;
	}

	/**
	 * The station with a bonus click open right now, or null.
	 */
	public Station quickActionStation()
	{
		return quickAction;
	}

	/**
	 * The instruction built on the last game tick, or null before the first one.
	 */
	public Step currentStep()
	{
		return step;
	}

	/**
	 * The walking path to the next thing to click, or empty.
	 */
	public List<WorldPoint> currentPath()
	{
		return path;
	}

	public boolean inLab()
	{
		return inLab;
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		clientThread.invokeLater(this::checkRivals);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN)
		{
			if (!loginHandled)
			{
				onLogin(LOGIN_INVENTORY_WAIT);
			}
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			loginHandled = false;
			startPending = false;
			tracking = false;
			inLab = false;
			quickWindow.reset();
			quickAction = null;
			alembicProgress = 0;
			agitatorProgress = 0;
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		int id = event.getGraphicsObject().getId();
		if (id == SpotanimID.VFX_MACHINERY_ALCHEMY01_ALEMBIC01)
		{
			quickWindow.onSpotAnim(Station.ALEMBIC,
				client.getVarbitValue(VarbitID.MM_LAB_ALEMBIC_POTION) != 0, client.getTickCount());
		}
		else if (id == SpotanimID.VFX_MACHINERY_ALCHEMY01_AGITATOR01)
		{
			quickWindow.onSpotAnim(Station.AGITATOR,
				client.getVarbitValue(VarbitID.MM_LAB_AGITATOR_POTION) != 0, client.getTickCount());
		}
		else
		{
			return;
		}
		quickAction = quickWindow.active();
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarbitId();
		if (isVesselSlot(id))
		{
			// Any slot moving means a pull landed. A repeated lever (L after L) leaves slot 0 alone
			// and only shifts the older slots, so slot 0 on its own is not enough.
			pendingPulls.clear();
		}
		if (trackQuickAction(id, event.getValue()))
		{
			return;
		}
		Station station = stationForVarbit(id);
		if (station != null)
		{
			if (tracking)
			{
				int value = event.getValue();
				if (value == 0)
				{
					tracker.onStationCleared(station);
				}
				else
				{
					tracker.onStationLoaded(station, PotionType.fromIndex(value - 1));
				}
			}
			return;
		}
		if (isOrderVarbit(id) && !ordersQueued)
		{
			ordersQueued = true;
			clientThread.invokeAtTickEnd(() ->
			{
				ordersQueued = false;
				orders = readOrders();
			});
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}
		if (startPending && startWait > 0)
		{
			// The login inventory has arrived; starting reads it.
			startTracking();
			return;
		}
		if (!tracking)
		{
			return;
		}
		// The orders field refreshes at tick end, so it still holds the board any deposit
		// in this change was made against.
		tracker.onInventory(itemIds(event.getItemContainer()), client.getTickCount(), orders);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType chatType = event.getType();
		if (!tracking || (chatType != ChatMessageType.GAMEMESSAGE && chatType != ChatMessageType.SPAM))
		{
			return;
		}
		PotionType type = ChatParse.failedDeposit(event.getMessage());
		if (type != null)
		{
			tracker.onFailedDeposit(type);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (!event.isConsumed() && "Take-from".equals(Text.removeTags(event.getMenuOption()))
			&& Text.removeTags(event.getMenuTarget()).startsWith(VESSEL_TARGET))
		{
			madeFirstPotion = true;
		}
		if (event.isConsumed() || !tracking)
		{
			return;
		}
		String option = Text.removeTags(event.getMenuOption());
		String target = Text.removeTags(event.getMenuTarget());
		PotionComponent lever = leverFromTarget(target);
		if (lever != null && LEVER_OPTION.equals(option))
		{
			if (config.leverGuard() && guarding() && blockLever(lever))
			{
				event.consume();
				return;
			}
			pendingPulls.add(lever);
			pendingPullTick = client.getTickCount();
			return;
		}

		Station station = Station.fromMenuTarget(target);
		if (station != null && station.menuOption().equals(option))
		{
			if (config.stationGuard() && guarding() && blockStation(station))
			{
				event.consume();
			}
			return;
		}

		if (BELT_OPTION.equals(option) && BELT_TARGET.equals(target))
		{
			int tick = client.getTickCount();
			if (config.beltLock() && guarding()
				&& ClickGuard.blockBelt(BeltRules.evaluate(orders, tracker.map(), config.mode()), beltClickTick == tick))
			{
				event.consume();
				return;
			}
			beltClickTick = tick;
			tracker.onDepositClick(orders);
			return;
		}

		boolean potion = PotionType.fromItemId(event.getItemId()) != null;
		if (potion && ("Drop".equals(option) || "Destroy".equals(option)))
		{
			tracker.onDropClick(event.getParam0());
		}
		else if (event.getMenuAction() == MenuAction.WIDGET_TARGET_ON_WIDGET && target != null)
		{
			if (potion && target.startsWith(DIGWEED_TARGET_PREFIX))
			{
				// Digweed used on a potion: param0 is the potion's slot.
				tracker.onDigweedUsed(event.getParam0());
			}
			else if (event.getItemId() == ItemID.MM_LAB_SPECIAL_HERB && target.endsWith(DIGWEED_TARGET_SUFFIX))
			{
				// A potion used on the digweed: the potion is the selected item.
				int slot = selectedPotionSlot();
				if (slot >= 0)
				{
					tracker.onDigweedUsed(slot);
				}
			}
		}
	}

	@Subscribe
	public void onWidgetDrag(WidgetDrag event)
	{
		if (client.getMouseCurrentButton() != 0)
		{
			dragReleaseSeen = false;
			return;
		}
		if (dragReleaseSeen)
		{
			return;
		}
		dragReleaseSeen = true;
		Widget dragged = client.getDraggedWidget();
		Widget on = client.getDraggedOnWidget();
		if (tracking && inInventory(dragged) && inInventory(on))
		{
			tracker.onDrag(dragged.getIndex(), on.getIndex());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();
		if (!pendingPulls.isEmpty() && tick - pendingPullTick > PENDING_PULL_TICKS)
		{
			pendingPulls.clear();
		}
		quickWindow.onTick(tick);
		quickAction = quickWindow.active();
		inLab = readInLab();
		if (startPending && (startWait == 0 || --startWait == 0))
		{
			startTracking();
		}
		if (tracking)
		{
			tracker.onTick(tick);
			if (tracker.consumePauseEvent())
			{
				chat("Belt Runner stopped tracking: " + sentence(tracker.pauseReason())
					+ " Use up your potions to start again.");
			}
		}

		int[] ids = itemIds(client.getItemContainer(InventoryID.INV));
		int herblore = client.getRealSkillLevel(Skill.HERBLORE);
		plan = StockPlan.create(SlotMap.SIZE - nonPotionCount(ids), herblore, config.mode().stockTypes());

		GuideInput in = new GuideInput();
		in.rival = rival;
		in.herbloreLevel = herblore;
		in.lowLevelWarning = herblore < ALL_RECIPES_LEVEL && !madeFirstPotion;
		if (tracking)
		{
			in.trackerState = tracker.state();
			in.pauseReason = tracker.pauseReason();
			in.map = tracker.map();
			in.inStations = tracker.stationContents();
		}
		in.plan = plan;
		in.orders = orders;
		for (int i = 0; i < VESSEL_SLOT_VARBITS.length; i++)
		{
			in.vesselSlots[i] = client.getVarbitValue(VESSEL_SLOT_VARBITS[i]);
		}
		in.vesselReady = PotionType.fromIndex(client.getVarbitValue(VarbitID.MM_LAB_VESSEL_READY) - 1);
		in.landingSlot = lowestEmptySlot(ids);
		in.digweedHeld = contains(ids, ItemID.MM_LAB_SPECIAL_HERB);
		in.digweedSpawn = digweedSpawn();
		in.holdDigweed = config.holdDigweed();
		in.restockThreshold = config.restockThreshold();
		in.mode = config.mode();
		step = guide.next(in);
		path = walkingPath(step);

		if (tracking)
		{
			save();
		}
	}

	@Provides
	BeltRunnerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BeltRunnerConfig.class);
	}

	/**
	 * Re-reads which rival is on and goes dormant or wakes up when that changes.
	 */
	private void checkRivals()
	{
		String found = RivalPlugins.activeRival(pluginManager.getPlugins(), pluginManager::isPluginActive);
		boolean wasDormant = rival != null;
		rival = found;
		boolean first = !rivalChecked;
		rivalChecked = true;
		if (first || wasDormant == (found != null))
		{
			return;
		}
		if (found != null)
		{
			log.debug("Going dormant: {} is on", found);
			guide.reset();
			stopTracking();
			discardSavedMap();
		}
		else if (client.getGameState() == GameState.LOGGED_IN)
		{
			log.debug("Waking up");
			// Waking is a fresh start: potions held now have an unknown history.
			startPending = true;
			restoreOnStart = false;
			startWait = 0;
		}
		else
		{
			// Mid hop or reconnect: let the next LOGGED_IN start tracking.
			log.debug("Waking up on the next login");
			loginHandled = false;
		}
	}

	private void onLogin(int inventoryWait)
	{
		loginHandled = true;
		if (rival != null)
		{
			discardSavedMap();
			chat("Mixology Belt Runner is paused while " + rival + " is on.");
			return;
		}
		// Right after a login the inventory may not have arrived yet, so tracking starts
		// on its first update, or after a few ticks without one.
		startPending = true;
		restoreOnStart = true;
		startWait = inventoryWait;
	}

	private void startTracking()
	{
		startPending = false;
		if (rival != null)
		{
			return;
		}
		int[] ids = itemIds(client.getItemContainer(InventoryID.INV));
		Map<Station, PotionType> loaded = new EnumMap<>(Station.class);
		for (Station s : Station.values())
		{
			PotionType type = PotionType.fromIndex(client.getVarbitValue(potionVarbit(s)) - 1);
			if (type != null)
			{
				loaded.put(s, type);
			}
		}
		orders = readOrders();
		SlotMap saved = restoreOnStart && !discardSaved
			? SlotMapCodec.decode(gson, configManager.getRSProfileConfiguration(BeltRunnerConfig.GROUP, SLOT_MAP_KEY))
			: null;
		if (saved != null)
		{
			tracker.restore(saved, ids, loaded);
		}
		else
		{
			tracker.start(ids, loaded);
		}
		tracking = true;
		// The first save overwrites any stale key.
		discardSaved = false;
		savedRevision = -1;
		guide.reset();
		log.debug("Tracking started: {}", tracker.state());
	}

	/**
	 * The map goes stale while dormant or switched off, so it must not come back on a later
	 * login. The unset is a no-op without an RS profile; the flag covers that case.
	 */
	private void discardSavedMap()
	{
		discardSaved = true;
		configManager.unsetRSProfileConfiguration(BeltRunnerConfig.GROUP, SLOT_MAP_KEY);
	}

	/**
	 * The inventory slot of the selected item when it is a mixology potion, or -1.
	 */
	private int selectedPotionSlot()
	{
		Widget selected = client.getSelectedWidget();
		if (!inInventory(selected) || PotionType.fromItemId(selected.getItemId()) == null)
		{
			return -1;
		}
		return selected.getIndex();
	}

	private void stopTracking()
	{
		tracking = false;
		startPending = false;
	}

	/**
	 * Writes the slot map whenever it changed; an empty map removes the key.
	 */
	private void save()
	{
		int revision = tracker.revision();
		if (revision == savedRevision)
		{
			return;
		}
		savedRevision = revision;
		SlotMap map = tracker.map();
		if (map.isEmpty())
		{
			configManager.unsetRSProfileConfiguration(BeltRunnerConfig.GROUP, SLOT_MAP_KEY);
		}
		else
		{
			configManager.setRSProfileConfiguration(BeltRunnerConfig.GROUP, SLOT_MAP_KEY, SlotMapCodec.encode(gson, map));
		}
	}

	/**
	 * The path from the player to where the step wants them next, or empty.
	 */
	private List<WorldPoint> walkingPath(Step current)
	{
		Player player = client.getLocalPlayer();
		WorldPoint target = PathTargets.target(current);
		boolean show = config.pathDisplay() != PathDisplay.OFF && inLab && player != null;
		return pathRouter.pathTo(client.getTopLevelWorldView(), player == null ? null : player.getWorldLocation(),
			target, Collections.emptySet(), show && target != null);
	}

	private static boolean isVesselSlot(int varbitId)
	{
		for (int slot : VESSEL_SLOT_VARBITS)
		{
			if (slot == varbitId)
			{
				return true;
			}
		}
		return false;
	}

	private static PotionComponent leverFromTarget(String target)
	{
		switch (target)
		{
			case "Mox lever":
				return PotionComponent.MOX;
			case "Aga lever":
				return PotionComponent.AGA;
			case "Lye lever":
				return PotionComponent.LYE;
			default:
				return null;
		}
	}

	/**
	 * Only levers the potion still needs may be pulled while making it, counting pulls already
	 * clicked this tick, and none while the finished potion waits to be taken.
	 */
	private boolean blockLever(PotionComponent lever)
	{
		Step current = step;
		if (current == null || current.phase() != Phase.MAKE || current.make() == null)
		{
			return false;
		}
		int[] slots = new int[VESSEL_SLOT_VARBITS.length];
		for (int i = 0; i < slots.length; i++)
		{
			slots[i] = client.getVarbitValue(VESSEL_SLOT_VARBITS[i]);
		}
		for (PotionComponent pull : pendingPulls)
		{
			slots = Vessel.pushed(slots, pull);
		}
		Map<PotionComponent, List<Integer>> numbers = Vessel.pullNumbers(current.make(), slots);
		// Nothing owed means the potion is complete (or will be once a pull this tick lands).
		return ClickGuard.blockLever(numbers, current.highlightVessel() || numbers.isEmpty(), lever);
	}

	/**
	 * Guards act only while the map is trusted, the guide is running, and the player is in the lab.
	 */
	private boolean guarding()
	{
		Step current = step;
		return rival == null
			&& tracker.state() == TrackerState.TRACKING
			&& current != null && current.phase() != Phase.DORMANT
			&& readInLab();
	}

	/**
	 * Feeds the bonus-click window its skillshot hits and progress updates. True if the varbit was one of those.
	 */
	private boolean trackQuickAction(int id, int value)
	{
		if (id == VarbitID.MM_LAB_HIT_SKILLSHOT_ALEMBIC)
		{
			quickWindow.onSkillshotHit(Station.ALEMBIC);
		}
		else if (id == VarbitID.MM_LAB_HIT_SKILLSHOT_AGITATOR)
		{
			quickWindow.onSkillshotHit(Station.AGITATOR);
		}
		else if (id == VarbitID.MM_ALEMBIC_PROGRESS)
		{
			quickWindow.onProgress(Station.ALEMBIC);
			if (value < alembicProgress)
			{
				quickWindow.onProgressDropped(Station.ALEMBIC);
			}
			alembicProgress = value;
		}
		else if (id == VarbitID.MM_AGITATOR_PROGRESS)
		{
			quickWindow.onProgress(Station.AGITATOR);
			if (value < agitatorProgress)
			{
				quickWindow.onProgressDropped(Station.AGITATOR);
			}
			agitatorProgress = value;
		}
		else
		{
			return false;
		}
		quickAction = quickWindow.active();
		return true;
	}

	private boolean blockStation(Station clicked)
	{
		boolean occupied = client.getVarbitValue(potionVarbit(clicked)) != 0;
		SlotMap map = tracker.map();
		int first = map.firstUnprocessed();
		Station planned = first < 0 ? null : map.get(first).station();
		return ClickGuard.blockStation(clicked, occupied, planned);
	}

	private boolean readInLab()
	{
		Widget overlay = client.getWidget(InterfaceID.MM_OVERLAY, 0);
		return overlay != null && !overlay.isHidden();
	}

	private List<Order> readOrders()
	{
		List<Order> out = new ArrayList<>(ORDER_TYPE_VARBITS.length);
		for (int i = 0; i < ORDER_TYPE_VARBITS.length; i++)
		{
			PotionType type = PotionType.fromIndex(client.getVarbitValue(ORDER_TYPE_VARBITS[i]) - 1);
			Station station = Station.fromOrderModifier(client.getVarbitValue(ORDER_MODIFIER_VARBITS[i]));
			if (type != null && station != null)
			{
				out.add(new Order(i, type, station));
			}
		}
		return Collections.unmodifiableList(out);
	}

	private LabObject digweedSpawn()
	{
		for (int i = 0; i < DIGWEED_VARBITS.length; i++)
		{
			if (client.getVarbitValue(DIGWEED_VARBITS[i]) == 1)
			{
				return DIGWEED_SPOTS[i];
			}
		}
		return null;
	}

	private void chat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder().append(message).build())
			.build());
	}

	private static boolean inInventory(Widget w)
	{
		return w != null && WidgetUtil.componentToInterface(w.getId()) == InterfaceID.INVENTORY;
	}

	private static int potionVarbit(Station s)
	{
		switch (s)
		{
			case ALEMBIC:
				return VarbitID.MM_LAB_ALEMBIC_POTION;
			case RETORT:
				return VarbitID.MM_LAB_RETORT_POTION;
			default:
				return VarbitID.MM_LAB_AGITATOR_POTION;
		}
	}

	private static Station stationForVarbit(int varbitId)
	{
		for (Station s : Station.values())
		{
			if (potionVarbit(s) == varbitId)
			{
				return s;
			}
		}
		return null;
	}

	private static boolean isOrderVarbit(int varbitId)
	{
		for (int i = 0; i < ORDER_TYPE_VARBITS.length; i++)
		{
			if (ORDER_TYPE_VARBITS[i] == varbitId || ORDER_MODIFIER_VARBITS[i] == varbitId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Item ids for all 28 slots, -1 for an empty slot.
	 */
	private static int[] itemIds(ItemContainer container)
	{
		int[] ids = new int[SlotMap.SIZE];
		Arrays.fill(ids, -1);
		if (container != null)
		{
			Item[] items = container.getItems();
			for (int i = 0; i < Math.min(items.length, SlotMap.SIZE); i++)
			{
				ids[i] = items[i].getId();
			}
		}
		return ids;
	}

	/**
	 * The lowest empty inventory slot, or -1 when the inventory is full.
	 */
	private static int lowestEmptySlot(int[] ids)
	{
		for (int i = 0; i < ids.length; i++)
		{
			if (ids[i] == -1)
			{
				return i;
			}
		}
		return -1;
	}

	private static int nonPotionCount(int[] ids)
	{
		int n = 0;
		for (int id : ids)
		{
			if (id != -1 && PotionType.fromItemId(id) == null)
			{
				n++;
			}
		}
		return n;
	}

	private static boolean contains(int[] ids, int itemId)
	{
		for (int id : ids)
		{
			if (id == itemId)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The reason as a full sentence ending in a full stop.
	 */
	private static String sentence(String s)
	{
		if (s == null || s.isEmpty())
		{
			return "";
		}
		return s.endsWith(".") ? s : s + ".";
	}
}
