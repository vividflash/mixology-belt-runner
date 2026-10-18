package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;

/**
 * Follows each potion by inventory slot, so the plugin knows which station a
 * finished potion went through. The item id does not say, and the game keeps no
 * client-side record of it.
 * <p>
 * Inventory snapshots are compared against the slot map. A changed slot has to be
 * explained by an event (a station loading or clearing, a deposit, a failed deposit,
 * a drop, a drag);
 * either side may arrive first. A change nothing explains within {@link #GRACE_TICKS}
 * stops tracking until the inventory is cleared of potions. The tracker never guesses.
 */
@Slf4j
public final class InventoryTracker
{
	/**
	 * Picks the planned station for a potion that has just come out of the vessel.
	 */
	public interface StationAssigner
	{
		Station assign(SlotMap map, Map<Station, PotionSlot> inStations, PotionType type);
	}

	/**
	 * How long a slot may stay unexplained before tracking stops.
	 */
	static final int GRACE_TICKS = 2;
	/**
	 * How long after the last belt click finished potions may leave as deposits.
	 */
	static final int DEPOSIT_TICKS = 20;
	/**
	 * How long a drop or destroy click waits; destroy asks for confirmation first.
	 */
	static final int DROP_TICKS = 50;
	/**
	 * How long station, failed deposit and digweed events wait for their inventory change.
	 */
	static final int EVENT_TICKS = 5;

	private static final int NONE = Integer.MIN_VALUE;
	private static final int UNSTAMPED = Integer.MIN_VALUE + 1;
	private static final int DIGWEED = ItemID.MM_LAB_SPECIAL_HERB;

	private static final String NOT_STARTED = "Tracking has not started.";
	private static final String HELD_AT_START = "Potions were already in the inventory when tracking started.";
	private static final String FAILED_DEPOSIT_MISMATCH = "The belt took a potion the tracker did not expect.";

	/**
	 * A one-sided event waiting for the inventory change it explains. Events carry
	 * no tick of their own, so each is stamped by the next tick the tracker sees.
	 */
	private static final class Pending
	{
		/**
		 * Load: what went in. Clear: what is coming out. Null for the rest.
		 */
		private final PotionSlot entry;
		/**
		 * Load: the first unprocessed slot when it went in. Failed deposit: the first
		 * finished slot when the game named it. Drop, digweed: the clicked slot. Follows
		 * drags.
		 */
		private int slot;
		private int tick = UNSTAMPED;

		private Pending(PotionSlot entry, int slot)
		{
			this.entry = entry;
			this.slot = slot;
		}
	}

	private final StationAssigner assigner;

	private SlotMap map = new SlotMap();
	private final Map<Station, PotionSlot> stations = new EnumMap<>(Station.class);
	/**
	 * The last inventory seen, with known drags already applied.
	 */
	private int[] lastIds = emptyInventory();
	/**
	 * Tick each slot first disagreed with the map, or NONE.
	 */
	private final int[] firstSeen = new int[SlotMap.SIZE];

	private final List<Pending> loads = new ArrayList<>();
	private final List<Pending> clears = new ArrayList<>();
	private final List<Pending> drops = new ArrayList<>();
	private final List<Pending> digweeds = new ArrayList<>();
	/**
	 * Each belongs to one inventory change: dropped once a change is reconciled
	 * without it, or straight away if its change already came this tick.
	 */
	private final List<Pending> failedDeposits = new ArrayList<>();
	/**
	 * True from an inventory change until the next tick.
	 */
	private boolean changedThisTick;
	/**
	 * True when an order claimed a finished potion this tick, so a failed deposit that
	 * arrives after its inventory change can see the contradiction.
	 */
	private boolean claimedThisTick;
	/**
	 * The order board as last passed in; each deposit is claimed against the board
	 * live when its inventory change arrives.
	 */
	private List<Order> orders = Collections.emptyList();
	/**
	 * True from a belt click until {@link #DEPOSIT_TICKS} after the last one.
	 */
	private boolean depositArmed;
	private int depositTick;

	private TrackerState state = TrackerState.NEEDS_CLEAR;
	private String pauseReason = NOT_STARTED;
	private boolean pauseEvent;
	private int revision;
	private int lastTick = UNSTAMPED;

	public InventoryTracker(StationAssigner assigner)
	{
		this.assigner = assigner;
		Arrays.fill(firstSeen, NONE);
	}

	/**
	 * Starts fresh. Tracks only if the inventory holds no mixology potion, since
	 * potions already there have an unknown history.
	 */
	public void start(int[] inventoryIds, Map<Station, PotionType> loadedStations)
	{
		begin(inventoryIds, loadedStations);
		if (hasPotion(lastIds))
		{
			state = TrackerState.NEEDS_CLEAR;
			pauseReason = HELD_AT_START;
		}
		else
		{
			state = TrackerState.TRACKING;
			pauseReason = null;
		}
	}

	/**
	 * Picks up a saved map if it still matches the inventory slot for slot,
	 * otherwise starts fresh.
	 */
	public void restore(SlotMap saved, int[] inventoryIds, Map<Station, PotionType> loadedStations)
	{
		int[] ids = normalise(inventoryIds);
		if (saved == null || !Arrays.equals(saved.expectedItemIds(), potionIds(ids)))
		{
			start(inventoryIds, loadedStations);
			return;
		}
		begin(inventoryIds, loadedStations);
		map = saved.copy();
		state = TrackerState.TRACKING;
		pauseReason = null;
	}

	/**
	 * A station took a potion of {@code type} (read from the station varbit). Its
	 * contents are recorded straight away, paused or not. While tracking, the load
	 * also explains the loss of the inventory's first unfinished potion if that is
	 * the same type; any other loss stays unexplained.
	 */
	public void onStationLoaded(Station s, PotionType type)
	{
		if (s == null || type == null || stations.containsKey(s))
		{
			return;
		}
		PotionSlot entry = new PotionSlot(type, false, s, false);
		stations.put(s, entry);
		if (state != TrackerState.TRACKING)
		{
			return;
		}
		int slot = map.firstUnprocessed();
		if (slot != -1 && map.get(slot).type() == type)
		{
			loads.add(new Pending(entry, slot));
			reconcile();
		}
	}

	public void onStationCleared(Station s)
	{
		PotionSlot entry = stations.remove(s);
		if (state != TrackerState.TRACKING || entry == null)
		{
			return;
		}
		clears.add(new Pending(entry, -1));
		reconcile();
	}

	/**
	 * A belt click. Every finished potion that leaves within {@link #DEPOSIT_TICKS} of the
	 * last click is explained if the board live at the time asks for it, so a double click
	 * that the game runs as two deposits, the second against a refreshed board, is followed.
	 */
	public void onDepositClick(List<Order> orders)
	{
		if (state != TrackerState.TRACKING)
		{
			return;
		}
		this.orders = new ArrayList<>(orders);
		depositArmed = true;
		depositTick = UNSTAMPED;
		reconcile();
	}

	/**
	 * The game said "You deposit some X, but fail to fulfil an order". The belt then takes
	 * the first finished potion in the inventory, so this explains one loss: the lowest
	 * slot holding a finished potion, when that potion is an X and no unclaimed order on
	 * the live board wants it. While it waits for its inventory change, no claim explains
	 * a loss. Anything else, including a claim already made this tick, means the game has
	 * contradicted the map: tracking stops at once, so no later claim can absorb the loss.
	 */
	public void onFailedDeposit(PotionType type)
	{
		if (state != TrackerState.TRACKING || type == null)
		{
			return;
		}
		int slot = firstProcessed();
		// claimedThisTick: an inventory change came first and an order claimed a potion.
		if (claimedThisTick || slot == -1 || map.get(slot).type() != type)
		{
			pause(FAILED_DEPOSIT_MISMATCH);
			return;
		}
		failedDeposits.add(new Pending(null, slot));
		reconcile();
		if (changedThisTick)
		{
			// Its inventory change came first and needed no failed deposit.
			failedDeposits.clear();
		}
	}

	public void onDropClick(int slot)
	{
		if (state != TrackerState.TRACKING || !validSlot(slot))
		{
			return;
		}
		drops.add(new Pending(null, slot));
		reconcile();
	}

	public void onDigweedUsed(int slot)
	{
		if (state != TrackerState.TRACKING || !validSlot(slot))
		{
			return;
		}
		digweeds.add(new Pending(null, slot));
	}

	/**
	 * Applies a drag at once, to the map and to the expected inventory, so the
	 * inventory change that follows (if any; identical swaps send none) matches.
	 */
	public void onDrag(int from, int to)
	{
		if (state != TrackerState.TRACKING || !validSlot(from) || !validSlot(to) || from == to)
		{
			return;
		}
		map.move(from, to);
		swap(lastIds, from, to);
		swap(firstSeen, from, to);
		followDrag(loads, from, to);
		followDrag(drops, from, to);
		followDrag(digweeds, from, to);
		followDrag(failedDeposits, from, to);
		revision++;
	}

	/**
	 * @param orders the order board live when the change arrived, which is the board any
	 *               deposit in it was made against
	 */
	public void onInventory(int[] ids, int tick, List<Order> orders)
	{
		this.orders = new ArrayList<>(orders);
		onInventory(ids, tick);
	}

	/**
	 * An inventory change with the order board unchanged since it was last passed in.
	 */
	public void onInventory(int[] ids, int tick)
	{
		stamp(tick);
		int[] next = normalise(ids);
		int digweedUsed = count(lastIds, DIGWEED) - count(next, DIGWEED);
		lastIds = next;
		if (state == TrackerState.TRACKING)
		{
			applyDigweed(digweedUsed);
			reconcile();
			failedDeposits.clear();
			changedThisTick = true;
		}
		else if (!hasPotion(next))
		{
			resume();
		}
		expire(tick);
	}

	public void onTick(int tick)
	{
		stamp(tick);
		expire(tick);
		changedThisTick = false;
		claimedThisTick = false;
		if (state != TrackerState.TRACKING)
		{
			return;
		}
		for (int slot = 0; slot < SlotMap.SIZE; slot++)
		{
			int seen = firstSeen[slot];
			if (seen != NONE && seen != UNSTAMPED && tick - seen >= GRACE_TICKS)
			{
				pause(lostTrack(slot));
				return;
			}
		}
	}

	public TrackerState state()
	{
		return state;
	}

	/**
	 * A copy of the slot map.
	 */
	public SlotMap map()
	{
		return map.copy();
	}

	public PotionSlot inStation(Station s)
	{
		return stations.get(s);
	}

	public Map<Station, PotionSlot> stationContents()
	{
		return Collections.unmodifiableMap(new EnumMap<>(stations));
	}

	public String pauseReason()
	{
		return pauseReason;
	}

	/**
	 * True once after each stop, so the plugin can warn in chat.
	 */
	public boolean consumePauseEvent()
	{
		boolean was = pauseEvent;
		pauseEvent = false;
		return was;
	}

	/**
	 * Goes up on every change to the slot map.
	 */
	public int revision()
	{
		return revision;
	}

	private void begin(int[] inventoryIds, Map<Station, PotionType> loadedStations)
	{
		clearPending();
		stations.clear();
		for (Map.Entry<Station, PotionType> e : loadedStations.entrySet())
		{
			if (e.getKey() != null && e.getValue() != null)
			{
				stations.put(e.getKey(), new PotionSlot(e.getValue(), false, e.getKey(), false));
			}
		}
		lastIds = normalise(inventoryIds);
		map = new SlotMap();
		pauseEvent = false;
		revision++;
	}

	/**
	 * Explains as many slot differences as the pending events allow. Losses go
	 * first so a slot they free can take a new potion in the same pass.
	 */
	private void reconcile()
	{
		int[] seen = potionIds(lastIds);
		boolean[] claimed = depositArmed ? new boolean[orders.size()] : null;
		// A pending failed deposit says no order took a potion in this change.
		boolean mayClaim = claimed != null && failedDeposits.isEmpty();

		for (int slot = 0; slot < SlotMap.SIZE; slot++)
		{
			PotionSlot p = map.get(slot);
			if (p == null || p.expectedItemId() == seen[slot])
			{
				continue;
			}
			Pending load;
			if (take(drops, slot) != null)
			{
				map.remove(slot);
				revision++;
			}
			else if (!p.processed() && (load = take(loads, slot)) != null)
			{
				Station s = load.entry.station();
				map.remove(slot);
				if (stations.get(s) == load.entry)
				{
					// Same potion, now with its digweed.
					stations.put(s, new PotionSlot(p.type(), false, s, p.digweed()));
				}
				revision++;
			}
			else if (p.processed() && take(failedDeposits, slot) != null)
			{
				// Ahead of the claim: the game says no order took this one.
				if (wanted(orders, claimed, p))
				{
					pause(FAILED_DEPOSIT_MISMATCH);
					return;
				}
				map.remove(slot);
				revision++;
			}
			else if (p.processed() && mayClaim && claim(orders, claimed, p))
			{
				claimedThisTick = true;
				map.remove(slot);
				revision++;
			}
		}

		for (int slot = 0; slot < SlotMap.SIZE; slot++)
		{
			if (map.get(slot) != null || seen[slot] == -1)
			{
				continue;
			}
			PotionType type = PotionType.fromItemId(seen[slot]);
			if (!type.isFinishedItem(seen[slot]))
			{
				// Unfinished potions only come from the vessel.
				map.put(slot, new PotionSlot(type, false, assigner.assign(map.copy(), stationContents(), type), false));
				revision++;
				continue;
			}
			Pending clear = onlyClearFor(type);
			if (clear != null)
			{
				clears.remove(clear);
				map.put(slot, clear.entry.processedAt(clear.entry.station()));
				revision++;
			}
		}

		int[] expected = map.expectedItemIds();
		for (int slot = 0; slot < SlotMap.SIZE; slot++)
		{
			if (expected[slot] == seen[slot])
			{
				firstSeen[slot] = NONE;
			}
			else if (firstSeen[slot] == NONE)
			{
				firstSeen[slot] = lastTick;
			}
		}
	}

	/**
	 * The pending clear a finished potion of {@code type} came from, or null when
	 * none or several match: the tracker does not pick between them.
	 */
	private Pending onlyClearFor(PotionType type)
	{
		Pending found = null;
		for (Pending c : clears)
		{
			if (c.entry.type() == type)
			{
				if (found != null)
				{
					return null;
				}
				found = c;
			}
		}
		return found;
	}

	/**
	 * The lowest slot in the map holding a finished potion, or -1.
	 */
	private int firstProcessed()
	{
		for (int slot = 0; slot < SlotMap.SIZE; slot++)
		{
			PotionSlot p = map.get(slot);
			if (p != null && p.processed())
			{
				return slot;
			}
		}
		return -1;
	}

	/**
	 * True when an order not yet claimed in this pass asks for {@code p}.
	 */
	private static boolean wanted(List<Order> orders, boolean[] claimed, PotionSlot p)
	{
		for (int i = 0; i < orders.size(); i++)
		{
			Order o = orders.get(i);
			if ((claimed == null || !claimed[i]) && o.type() == p.type() && o.station() == p.station())
			{
				return true;
			}
		}
		return false;
	}

	private static String lostTrack(int slot)
	{
		return "Lost track of the potion in inventory slot " + (slot + 1) + ".";
	}

	private static boolean claim(List<Order> orders, boolean[] claimed, PotionSlot p)
	{
		for (int i = 0; i < orders.size(); i++)
		{
			Order o = orders.get(i);
			if (!claimed[i] && o.type() == p.type() && o.station() == p.station())
			{
				claimed[i] = true;
				return true;
			}
		}
		return false;
	}

	private void applyDigweed(int used)
	{
		for (int i = 0; i < used && !digweeds.isEmpty(); i++)
		{
			int slot = digweeds.remove(0).slot;
			PotionSlot p = map.get(slot);
			if (p != null)
			{
				map.put(slot, p.withDigweed());
				revision++;
			}
		}
	}

	private void pause(String reason)
	{
		log.debug("Tracking paused: {}", reason);
		state = TrackerState.NEEDS_CLEAR;
		pauseReason = reason;
		pauseEvent = true;
		map = new SlotMap();
		clearPending();
		revision++;
	}

	private void resume()
	{
		state = TrackerState.TRACKING;
		pauseReason = null;
		map = new SlotMap();
		clearPending();
		revision++;
	}

	/**
	 * Drops pending events and unexplained slots. Known station contents stay:
	 * the game has not touched them.
	 */
	private void clearPending()
	{
		loads.clear();
		clears.clear();
		drops.clear();
		digweeds.clear();
		failedDeposits.clear();
		claimedThisTick = false;
		depositArmed = false;
		Arrays.fill(firstSeen, NONE);
	}

	private void stamp(int tick)
	{
		lastTick = tick;
		stampAll(loads, tick);
		stampAll(clears, tick);
		stampAll(drops, tick);
		stampAll(digweeds, tick);
		stampAll(failedDeposits, tick);
		if (depositArmed && depositTick == UNSTAMPED)
		{
			depositTick = tick;
		}
		for (int slot = 0; slot < SlotMap.SIZE; slot++)
		{
			if (firstSeen[slot] == UNSTAMPED)
			{
				firstSeen[slot] = tick;
			}
		}
	}

	private void expire(int tick)
	{
		expireAll(loads, tick, EVENT_TICKS);
		expireAll(digweeds, tick, EVENT_TICKS);
		expireAll(clears, tick, EVENT_TICKS);
		expireAll(failedDeposits, tick, EVENT_TICKS);
		expireAll(drops, tick, DROP_TICKS);
		if (depositArmed && expired(depositTick, tick, DEPOSIT_TICKS))
		{
			depositArmed = false;
		}
	}

	private static void stampAll(List<Pending> list, int tick)
	{
		for (Pending p : list)
		{
			if (p.tick == UNSTAMPED)
			{
				p.tick = tick;
			}
		}
	}

	private static void expireAll(List<Pending> list, int tick, int window)
	{
		list.removeIf(p -> expired(p.tick, tick, window));
	}

	private static boolean expired(int stamped, int tick, int window)
	{
		return stamped != UNSTAMPED && tick - stamped > window;
	}

	/**
	 * Removes and returns the first pending event pointing at {@code slot}, or null.
	 */
	private static Pending take(List<Pending> list, int slot)
	{
		for (Iterator<Pending> it = list.iterator(); it.hasNext(); )
		{
			Pending p = it.next();
			if (p.slot == slot)
			{
				it.remove();
				return p;
			}
		}
		return null;
	}

	private static void followDrag(List<Pending> list, int from, int to)
	{
		for (Pending p : list)
		{
			if (p.slot == from)
			{
				p.slot = to;
			}
			else if (p.slot == to)
			{
				p.slot = from;
			}
		}
	}

	private static int[] emptyInventory()
	{
		int[] ids = new int[SlotMap.SIZE];
		Arrays.fill(ids, -1);
		return ids;
	}

	/**
	 * Pads or trims to 28 slots; the client's array can be shorter than the inventory.
	 */
	private static int[] normalise(int[] ids)
	{
		int[] out = emptyInventory();
		if (ids != null)
		{
			System.arraycopy(ids, 0, out, 0, Math.min(ids.length, SlotMap.SIZE));
		}
		return out;
	}

	/**
	 * Potion ids per slot; anything that is not a mixology potion becomes -1.
	 */
	private static int[] potionIds(int[] ids)
	{
		int[] out = new int[SlotMap.SIZE];
		for (int i = 0; i < SlotMap.SIZE; i++)
		{
			out[i] = PotionType.fromItemId(ids[i]) == null ? -1 : ids[i];
		}
		return out;
	}

	private static boolean hasPotion(int[] ids)
	{
		for (int id : ids)
		{
			if (PotionType.fromItemId(id) != null)
			{
				return true;
			}
		}
		return false;
	}

	private static int count(int[] ids, int itemId)
	{
		int n = 0;
		for (int id : ids)
		{
			if (id == itemId)
			{
				n++;
			}
		}
		return n;
	}

	private static boolean validSlot(int slot)
	{
		return slot >= 0 && slot < SlotMap.SIZE;
	}

	private static void swap(int[] a, int i, int j)
	{
		int tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}
}
