package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Turns the tracked state into one instruction per tick: restock, process, then
 * work the belt until it locks, then top up or restock.
 */
public final class Guide
{
	private static final String SEPARATOR = " · ";

	private Phase phase;
	/**
	 * The cells the current make batch should cover.
	 */
	private final Set<Cell> targets = new LinkedHashSet<>();
	/**
	 * The order cells the batch makes potions for; empty when none.
	 */
	private final Set<Cell> orderCells = new LinkedHashSet<>();
	/**
	 * True once the batch knows which orders it makes potions for.
	 */
	private boolean orderCellsDecided;
	/**
	 * True when the batch refills the plan, false for a top up of the orders alone.
	 */
	private boolean restocking;

	public Step next(GuideInput in)
	{
		if (in.rival != null)
		{
			reset();
			return Step.builder(Phase.DORMANT, "Belt Runner is paused")
				.status("Turn off " + in.rival + " to use it")
				.warning(warning(in))
				.build();
		}
		if (in.trackerState != TrackerState.TRACKING)
		{
			reset();
			return Step.builder(Phase.NEEDS_CLEAR, "Deposit or destroy your potions to start")
				.status(in.pauseReason != null ? in.pauseReason : "Tracking starts from an inventory with no potions")
				.warning(warning(in))
				.build();
		}

		if (phase == null)
		{
			startBatch(in.plan.missing(in.map, in.inStations), true, Collections.emptySet());
			phase = Phase.MAKE;
		}

		Step.Builder b = advance(in);
		phase = b.phase();
		headline(b, in);
		return b.rows(StockSummary.rows(in.plan, in.map, in.inStations))
			.stock(stockHave(in), in.plan.size())
			.warning(warning(in))
			.build();
	}

	private static final String LOW_LEVEL_WARNING = "Not tested below Herblore 81. The XP rate assumes every potion is unlocked.";

	private static String warning(GuideInput in)
	{
		return in.lowLevelWarning ? LOW_LEVEL_WARNING : "";
	}

	/**
	 * Picks the station for a potion that has just come out of the vessel: the one the
	 * current step asked for when the potion is the type it asked for, otherwise the
	 * plan's best guess.
	 */
	static InventoryTracker.StationAssigner assigner(Supplier<Step> step, Supplier<StockPlan> plan, Supplier<List<Order>> orders)
	{
		return (map, inStations, type) -> stationFor(step.get(), plan.get(), orders.get(), map, inStations, type);
	}

	static Station stationFor(Step step, StockPlan plan, List<Order> orders, SlotMap map,
		Map<Station, PotionSlot> inStations, PotionType type)
	{
		if (step != null && step.make() == type && step.station() != null)
		{
			return step.station();
		}
		return plan.assignStation(map, inStations, type, orders);
	}

	public void reset()
	{
		phase = null;
		targets.clear();
		orderCells.clear();
		orderCellsDecided = false;
		restocking = false;
	}

	private Step.Builder advance(GuideInput in)
	{
		if (phase == Phase.MAKE)
		{
			Step.Builder b = make(in);
			if (b != null)
			{
				return b;
			}
			phase = Phase.PROCESS;
		}
		if (phase == Phase.PROCESS)
		{
			Step.Builder b = process(in);
			if (b != null)
			{
				return b;
			}
			phase = Phase.BELT;
		}
		return belt(in);
	}

	/**
	 * The next make step, or null when the batch is done.
	 */
	private Step.Builder make(GuideInput in)
	{
		decideOrderCells(in);
		Cell next = nextCell(in, null);
		if (in.vesselReady != null)
		{
			Cell taken = next != null && next.type() == in.vesselReady ? next : nextCell(in, in.vesselReady);
			if (taken != null || next == null)
			{
				return Step.builder(Phase.MAKE, "Take the potion")
					.make(in.vesselReady)
					.station(taken == null ? null : taken.station())
					.highlightVessel(true);
			}
			// The vessel made a potion nobody wants: keep pulling for the next one instead.
		}
		if (next == null)
		{
			return null;
		}

		PotionType type = next.type();
		boolean planGap = restocking && in.plan.contains(type) && StockPlan.covered(in.map, in.inStations, next) == 0;
		Step.Builder b;
		if (orderCells.contains(next) && !planGap)
		{
			b = Step.builder(Phase.MAKE, makeOrderHeadline(next))
				.status(in.plan.contains(type) ? "Top up" + SEPARATOR + "for the current order"
					: "Not stocked" + SEPARATOR + "just for this order");
		}
		else
		{
			b = Step.builder(Phase.MAKE, "Make " + type.code())
				.status(join("Restocking", "round " + in.plan.round(in.map, in.inStations) + " of 3", stockPart(in)));
		}
		b.make(type).station(next.station());
		if (Vessel.wrongPull(type, in.vesselSlots))
		{
			b.status(join(b.status(), "wrong lever, start again"));
		}
		return b.leverNumbers(Vessel.pullNumbers(type, in.vesselSlots));
	}

	/**
	 * The next batch cell to make, or null when every one has its potions; only cells of
	 * {@code type} when it is not null. A new potion lands in the lowest empty slot, so the
	 * pick is the first cell, in make order, whose station fits between the unprocessed
	 * potions below and above that slot. When none fits, or the inventory is full, it is
	 * simply the first cell in make order.
	 */
	private Cell nextCell(GuideInput in, PotionType type)
	{
		List<Cell> candidates = new ArrayList<>();
		for (Cell cell : in.plan.makeOrder(targets))
		{
			if ((type == null || cell.type() == type) && StockPlan.covered(in.map, in.inStations, cell) < need(in, cell))
			{
				candidates.add(cell);
			}
		}
		if (candidates.isEmpty())
		{
			return null;
		}
		if (in.landingSlot >= 0)
		{
			int lo = 0;
			int hi = Station.PROCESS_ORDER.size() - 1;
			for (int slot : in.map.occupiedSlots())
			{
				PotionSlot p = in.map.get(slot);
				if (p.processed())
				{
					continue;
				}
				int rank = rank(p.station());
				if (slot < in.landingSlot)
				{
					lo = Math.max(lo, rank);
				}
				else if (slot > in.landingSlot)
				{
					hi = Math.min(hi, rank);
				}
			}
			for (Cell cell : candidates)
			{
				int rank = rank(cell.station());
				if (lo <= rank && rank <= hi)
				{
					return cell;
				}
			}
		}
		return candidates.get(0);
	}

	private static int rank(Station station)
	{
		return Station.PROCESS_ORDER.indexOf(station);
	}

	/**
	 * Potions the batch wants for a cell: one, or as many as the orders ask for an order cell.
	 */
	private int need(GuideInput in, Cell cell)
	{
		return orderCells.contains(cell) ? Math.max(1, StockPlan.demand(in.orders, cell)) : 1;
	}

	/**
	 * A restock started without a locked belt works out whether the belt will lock once the
	 * batch is made. If so, the potions those orders need join the batch now, so each is
	 * made inside its station's block instead of after the last one.
	 */
	private void decideOrderCells(GuideInput in)
	{
		if (orderCellsDecided)
		{
			return;
		}
		BeltVerdict verdict = BeltRules.evaluate(in.orders, afterBatch(in), in.mode);
		if (verdict == null)
		{
			// Orders unknown: ask again next time.
			return;
		}
		orderCellsDecided = true;
		Set<Cell> needed = BeltRules.neededCells(verdict, in.mode);
		orderCells.addAll(needed);
		targets.addAll(needed);
	}

	/**
	 * The stock as it will be once every potion is processed and the batch's missing cells
	 * are made, as far as the inventory has room.
	 */
	private SlotMap afterBatch(GuideInput in)
	{
		SlotMap out = in.map.assumingProcessed();
		List<Cell> toAdd = new ArrayList<>();
		for (Station station : Station.PROCESS_ORDER)
		{
			PotionSlot loaded = in.inStations.get(station);
			if (loaded != null)
			{
				toAdd.add(new Cell(loaded.type(), station));
			}
		}
		for (Cell cell : targets)
		{
			if (StockPlan.covered(in.map, in.inStations, cell) == 0)
			{
				toAdd.add(cell);
			}
		}
		int slot = 0;
		for (Cell cell : toAdd)
		{
			while (slot < SlotMap.SIZE && out.get(slot) != null)
			{
				slot++;
			}
			if (slot == SlotMap.SIZE)
			{
				break;
			}
			out.put(slot, new PotionSlot(cell.type(), true, cell.station(), false));
		}
		return out;
	}

	/**
	 * The next station step, or null when nothing is left to process. The headline counts
	 * the run of potions the station takes before the next one is needed.
	 */
	private Step.Builder process(GuideInput in)
	{
		int first = in.map.firstUnprocessed();
		if (first >= 0)
		{
			List<String> runs = runs(in.map);
			return Step.builder(Phase.PROCESS, runs.get(0))
				.status(then(runs.subList(1, runs.size())))
				.station(in.map.get(first).station())
				.next(nextAfter(in.map, in.map.get(first).station()))
				.stationCount(firstRun(in.map))
				.nextPotionSlot(first);
		}
		Station busy = busyStation(in);
		return busy == null ? null : Step.builder(Phase.PROCESS, "Finish at " + busy.displayName()).station(busy).stationCount(0)
			.next(LabObject.CONVEYOR_BELT);
	}

	/**
	 * Where to go once the current station's run is done: the next station with unprocessed
	 * potions, in slot order, or the belt when there is none.
	 */
	static LabObject nextAfter(SlotMap map, Station current)
	{
		for (int slot : map.occupiedSlots())
		{
			PotionSlot p = map.get(slot);
			if (!p.processed() && p.station() != current)
			{
				return p.station().object();
			}
		}
		return LabObject.CONVEYOR_BELT;
	}

	/**
	 * How many unprocessed potions in a row, from the first one, share its station.
	 */
	static int firstRun(SlotMap map)
	{
		int first = map.firstUnprocessed();
		if (first < 0)
		{
			return 0;
		}
		Station station = map.get(first).station();
		int count = 0;
		for (int slot : map.occupiedSlots())
		{
			PotionSlot p = map.get(slot);
			if (p.processed())
			{
				continue;
			}
			if (p.station() != station)
			{
				break;
			}
			count++;
		}
		return count;
	}

	/**
	 * Potions still to load into a busy station: the run that follows, if it is the same station.
	 */
	private static int stillToLoad(SlotMap map, Station busy)
	{
		int first = map.firstUnprocessed();
		return first >= 0 && map.get(first).station() == busy ? firstRun(map) : 0;
	}

	/**
	 * The unprocessed potions as runs in slot order, each "Station ×count".
	 */
	private static List<String> runs(SlotMap map)
	{
		List<Station> stations = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		for (int slot : map.occupiedSlots())
		{
			PotionSlot p = map.get(slot);
			if (p.processed())
			{
				continue;
			}
			int last = stations.size() - 1;
			if (last >= 0 && stations.get(last) == p.station())
			{
				counts.set(last, counts.get(last) + 1);
			}
			else
			{
				stations.add(p.station());
				counts.add(1);
			}
		}
		List<String> out = new ArrayList<>();
		for (int i = 0; i < stations.size(); i++)
		{
			out.add(stations.get(i).displayName() + " ×" + counts.get(i));
		}
		return out;
	}

	private static String then(List<String> runs)
	{
		return runs.isEmpty() ? "" : "then " + String.join(", ", runs);
	}

	/**
	 * The first station, in process order, holding a potion; null when all are free.
	 */
	private static Station busyStation(GuideInput in)
	{
		for (Station station : Station.PROCESS_ORDER)
		{
			if (in.inStations.get(station) != null)
			{
				return station;
			}
		}
		return null;
	}

	private Step.Builder belt(GuideInput in)
	{
		BeltVerdict verdict = BeltRules.evaluate(in.orders, in.map, in.mode);
		if (verdict == null)
		{
			return Step.builder(Phase.BELT, "Waiting for orders").status(stockPart(in));
		}
		if (verdict.state().open())
		{
			String headline = "Click the belt";
			if (verdict.state() == BeltState.SKIPPING)
			{
				List<String> codes = new ArrayList<>();
				for (Order o : verdict.skipped())
				{
					codes.add(o.type().code());
				}
				headline += " (skipping " + String.join(", ", codes) + ")";
			}
			return Step.builder(Phase.BELT, headline)
				.status(join("fills " + verdict.fills() + " of 3", stockPart(in)))
				.belt(BeltColour.GREEN)
				.verdict(verdict);
		}

		Cell toMake = cell(verdict.toMake());
		boolean restock = in.plan.needsRestock(in.map, in.inStations, in.restockThreshold);
		startBatch(restock ? in.plan.missing(in.map, in.inStations) : new LinkedHashSet<>(), restock,
			BeltRules.neededCells(verdict, in.mode));
		phase = Phase.MAKE;
		Step.Builder b = make(in);
		if (b == null)
		{
			// Unprocessed potions made off-script may already cover the orders.
			b = process(in);
		}
		if (b == null)
		{
			// Nothing to make or process: stay on the belt rather than loop back to it.
			phase = Phase.BELT;
			b = Step.builder(Phase.BELT, makeOrderHeadline(toMake))
				.status("Top up" + SEPARATOR + "for the current order")
				.make(toMake.type())
				.station(toMake.station());
		}
		return b.verdict(verdict);
	}

	/**
	 * @param orders the order cells a locked belt needs; empty when the batch works them
	 *               out itself once the orders are known
	 */
	private void startBatch(Set<Cell> cells, boolean restock, Set<Cell> orders)
	{
		targets.clear();
		targets.addAll(cells);
		targets.addAll(orders);
		orderCells.clear();
		orderCells.addAll(orders);
		orderCellsDecided = !orders.isEmpty();
		restocking = restock;
	}

	/**
	 * Lets the steps that outrank the phase step take the headline: a busy station, a
	 * ready vessel, digweed to use (only once the batch has nothing left to make), then
	 * digweed to pick up (only with a free slot to take it). The phase step underneath has
	 * already moved the guide's phase on.
	 */
	private void headline(Step.Builder b, GuideInput in)
	{
		LabObject spawn = in.digweedHeld ? null : in.digweedSpawn;
		String direction = spawn == null ? null : digweedDirection(spawn);
		b.digweedSpot(spawn);

		Station busy = busyStation(in);
		if (busy != null)
		{
			takeOver(b.phase(Phase.PROCESS), "Finish at " + busy.displayName())
				.status(join(then(runs(in.map)), direction == null ? "" : "digweed ready " + direction))
				.station(busy)
				.stationCount(stillToLoad(in.map, busy))
				.next(nextAfter(in.map, busy));
			return;
		}
		if (in.vesselReady != null && (b.phase() != Phase.MAKE || nextCell(in, in.vesselReady) != null))
		{
			if (b.phase() != Phase.MAKE)
			{
				takeOver(b, "Take the potion")
					.make(in.vesselReady)
					.highlightVessel(true)
					.next(null);
			}
			return;
		}
		if (in.digweedHeld)
		{
			int slot = digweedTarget(in);
			if (slot >= 0 && b.phase() == Phase.MAKE)
			{
				// A make step means potions are still to come. Using the digweed would free
				// its slot and land one of them out of station order; holding it costs nothing,
				// since the spawn count only moves on deliveries.
				b.status(join(b.status(), "use digweed after making"));
			}
			else if (slot >= 0)
			{
				b.headline("Use digweed on " + in.map.get(slot).type().code())
					.digweedSlot(slot)
					.digweedTarget(in.map.get(slot).type())
					.station(null)
					.nextPotionSlot(-1)
					.leverNumbers(Collections.emptyMap())
					.highlightVessel(false)
					.next(null)
					.belt(BeltColour.NONE);
			}
			else if (in.holdDigweed)
			{
				b.status(join(b.status(), "holding digweed for a Mixalot"));
			}
			return;
		}
		if (spawn != null && (in.landingSlot < 0 || mixing(in)))
		{
			// A full inventory has no room for it, and a potion half made in the vessel is
			// finished first; the spot stays marked.
			b.status(join(b.status(), "digweed ready " + direction));
		}
		else if (spawn != null)
		{
			b.headline("Pick up the digweed (" + direction + ")")
				.digweedPickup(true)
				.leverNumbers(Collections.emptyMap())
				.highlightVessel(false)
				.nextPotionSlot(-1)
				.next(null)
				.belt(BeltColour.NONE);
			if (b.phase() == Phase.PROCESS)
			{
				// No station outline competing with the pickup.
				b.station(null);
			}
		}
	}

	/**
	 * True while the vessel holds lever pulls: a potion is part made.
	 */
	private static boolean mixing(GuideInput in)
	{
		for (int slot : in.vesselSlots)
		{
			if (slot != 0)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Swaps the phase step's instruction for another one, keeping its belt verdict.
	 */
	private static Step.Builder takeOver(Step.Builder b, String headline)
	{
		return b.headline(headline)
			.status("")
			.make(null)
			.station(null)
			.leverNumbers(Collections.emptyMap())
			.highlightVessel(false)
			.nextPotionSlot(-1)
			.stationCount(-1)
			.digweedSlot(-1)
			.belt(BeltColour.NONE);
	}

	private static int digweedTarget(GuideInput in)
	{
		SlotMap map = in.map;
		if (in.holdDigweed)
		{
			for (int slot : map.occupiedSlots())
			{
				PotionSlot p = map.get(slot);
				if (p.type() == PotionType.MIXALOT && !p.digweed() && ordered(in.orders, p))
				{
					return slot;
				}
			}
			return -1;
		}
		for (int slot : map.occupiedSlots())
		{
			PotionSlot p = map.get(slot);
			if (p.type() == PotionType.MIXALOT && !p.digweed())
			{
				return slot;
			}
		}
		int best = -1;
		for (int slot : map.occupiedSlots())
		{
			PotionSlot p = map.get(slot);
			if (!p.digweed() && ordered(in.orders, p) && (best < 0 || p.type().xp() > map.get(best).type().xp()))
			{
				best = slot;
			}
		}
		return best;
	}

	private static boolean ordered(List<Order> orders, PotionSlot p)
	{
		for (Order o : orders)
		{
			if (o.type() == p.type() && o.station() == p.station())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Planned cells held finished, unprocessed, or loaded into a station.
	 */
	private static int stockHave(GuideInput in)
	{
		int have = 0;
		for (PotionType type : in.plan.types())
		{
			for (Station station : Station.PROCESS_ORDER)
			{
				if (StockPlan.covered(in.map, in.inStations, new Cell(type, station)) > 0)
				{
					have++;
				}
			}
		}
		return have;
	}

	/**
	 * The fixed compass label of a digweed spot.
	 */
	static String digweedDirection(LabObject spot)
	{
		switch (spot)
		{
			case DIGWEED_NORTH_EAST:
				return "north east";
			case DIGWEED_SOUTH_EAST:
				return "south east";
			case DIGWEED_SOUTH_WEST:
				return "south west";
			case DIGWEED_NORTH_WEST:
				return "north west";
			default:
				throw new IllegalArgumentException("Not a digweed spot: " + spot);
		}
	}

	private static String stockPart(GuideInput in)
	{
		int total = in.plan.size();
		return total == 0 ? "" : "stock " + stockHave(in) + " / " + total;
	}

	private static String makeOrderHeadline(Cell cell)
	{
		return "Make " + cell.type().code() + " → " + cell.station().displayName();
	}

	private static Cell cell(Order o)
	{
		return new Cell(o.type(), o.station());
	}

	private static String join(String... parts)
	{
		StringBuilder sb = new StringBuilder();
		for (String part : parts)
		{
			if (part != null && !part.isEmpty())
			{
				if (sb.length() > 0)
				{
					sb.append(SEPARATOR);
				}
				sb.append(part);
			}
		}
		return sb.toString();
	}
}
