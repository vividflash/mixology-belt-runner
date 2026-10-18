package com.mixologybeltrunner;

import static com.mixologybeltrunner.Fixtures.orders;
import static com.mixologybeltrunner.PotionType.ALCO_AUGMENTATOR;
import static com.mixologybeltrunner.PotionType.ANTI_LEECH_LOTION;
import static com.mixologybeltrunner.PotionType.AZURE_AURA_MIX;
import static com.mixologybeltrunner.PotionType.LIPLACK_LIQUOR;
import static com.mixologybeltrunner.PotionType.MAMMOTH_MIGHT_MIX;
import static com.mixologybeltrunner.PotionType.MEGALITE_LIQUID;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.PotionType.MYSTIC_MANA_AMALGAM;
import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * Drives the guide, the tracker and the station assigner together, wired the way the plugin wires them.
 */
public class RestockFlowTest
{
	private static final int DIGWEED = ItemID.MM_LAB_SPECIAL_HERB;

	private final Guide guide = new Guide();
	private final StockPlan plan = StockPlan.create(25);
	private List<Order> orders = Collections.emptyList();
	private Step current;
	private InventoryTracker tracker;
	private final int[] inv = emptyInventory();
	private int tick = 1;

	@Test
	public void freshRestockIsGroupedByStation()
	{
		orders = orders(MIXALOT, ALEMBIC, MYSTIC_MANA_AMALGAM, RETORT, ANTI_LEECH_LOTION, AGITATOR);
		startTracking(null);
		makeUntilDone();
		List<Station> expected = new ArrayList<>();
		for (Station s : Station.PROCESS_ORDER)
		{
			expected.addAll(Collections.nCopies(7, s));
		}
		assertEquals(expected, unprocessedStations());
		assertEquals(List.of("Alembic ×7", "Retort ×7", "Agitator ×7"), processAll());
		assertEquals(Phase.BELT, current.phase());
	}

	@Test
	public void restockAfterABeltRunIsGroupedByStation()
	{
		SlotMap seed = afterBeltRun();
		orders = orders(MIXALOT, RETORT, LIPLACK_LIQUOR, AGITATOR, MEGALITE_LIQUID, AGITATOR);
		startTracking(seed);
		makeUntilDone();
		List<Station> made = unprocessedStations();
		// The 11 missing cells; the unfilled triple is skipped.
		assertEquals(11, made.size());
		assertGrouped(made);
		assertEquals(Phase.PROCESS, current.phase());
	}

	@Test
	public void orderPotionOnATripleBoardSitsInItsStationBlock()
	{
		orders = orders(ALCO_AUGMENTATOR, RETORT, LIPLACK_LIQUOR, ALEMBIC, MAMMOTH_MIGHT_MIX, AGITATOR);
		startTracking(null);
		makeUntilDone();
		List<Station> made = unprocessedStations();
		assertEquals(22, made.size());
		assertGrouped(made);
		assertEquals(List.of("Alembic ×7", "Retort ×8", "Agitator ×7"), processAll());
	}

	/**
	 * The live session's order: digweed picked up late in the Alembic block takes the next slot,
	 * the last Alembic potion (the Mixalot) lands after it, and the digweed would free that
	 * slot under the Retort block if it were used before the making is done.
	 */
	@Test
	public void digweedTakingAndFreeingASlotKeepsStationsGrouped()
	{
		orders = orders(MIXALOT, ALEMBIC, MYSTIC_MANA_AMALGAM, RETORT, ANTI_LEECH_LOTION, AGITATOR);
		startTracking(null);
		for (int i = 0; i < 6; i++)
		{
			assertTrue(makeOne());
		}
		inv[firstEmpty()] = DIGWEED;
		changeInventory();
		makeUntilDone();
		List<Station> made = unprocessedStations();
		assertEquals(21, made.size());
		assertGrouped(made);
		assertEquals(Phase.PROCESS, current.phase());
		assertEquals("Use digweed on MAL", current.headline());
		useDigweed(current.digweedSlot());
		assertEquals(List.of("Alembic ×7", "Retort ×7", "Agitator ×7"), processAll());
	}

	/**
	 * An Agitator potion made off-script before the Retort block: the rest of the Agitator
	 * block lands next to it, so each station is visited once.
	 */
	@Test
	public void offScriptPotionAtALaterStationIsRoutedAround()
	{
		orders = orders(MIXALOT, RETORT, LIPLACK_LIQUOR, AGITATOR, MEGALITE_LIQUID, AGITATOR);
		startTracking(afterBeltRun());
		for (int i = 0; i < 3; i++)
		{
			assertTrue(makeOne());
		}
		current = guide.next(input(null));
		assertEquals(RETORT, current.station());
		// AAM is only short at the Agitator.
		takeFromVessel(AZURE_AURA_MIX);
		assertEquals(AGITATOR, tracker.map().get(4).station());
		makeUntilDone();
		assertEquals(11, unprocessedStations().size());
		assertEquals(List.of("Alembic ×3", "Agitator ×4", "Retort ×4"), processAll());
	}

	@Test
	public void lockedBoardNeedingTwoIsOneTrip()
	{
		SlotMap seed = new SlotMap();
		int slot = 0;
		for (Station s : Station.PROCESS_ORDER)
		{
			for (PotionType type : plan.types())
			{
				seed.put(slot++, new PotionSlot(type, true, s, false));
			}
		}
		for (int s : seed.occupiedSlots())
		{
			inv[s] = seed.get(s).type().finishedId();
		}
		orders = orders(MIXALOT, ALEMBIC, MYSTIC_MANA_AMALGAM, AGITATOR, ANTI_LEECH_LOTION, RETORT);
		startTracking(seed);
		current = guide.next(input(null));
		assertEquals(Phase.BELT, current.phase());
		deposit();

		// Two orders worth making are missing, not enough for a restock.
		orders = orders(ANTI_LEECH_LOTION, RETORT, MIXALOT, ALEMBIC, AZURE_AURA_MIX, RETORT);
		makeUntilDone();
		assertEquals(List.of(ALEMBIC, RETORT), unprocessedStations());
		assertEquals(Phase.PROCESS, current.phase());
		List<String> runs = processAll();
		assertEquals(List.of("Alembic ×1", "Retort ×1"), runs);
		assertEquals(Phase.BELT, current.phase());
		assertEquals("Click the belt", current.headline());
	}

	private void startTracking(SlotMap seed)
	{
		tracker = new InventoryTracker(Guide.assigner(() -> current, () -> plan, () -> orders));
		if (seed == null)
		{
			tracker.start(inv.clone(), Map.of());
		}
		else
		{
			tracker.restore(seed, inv.clone(), Map.of());
		}
		assertEquals(TrackerState.TRACKING, tracker.state());
	}

	/**
	 * Follows make steps until the guide moves on. Each potion is taken from the vessel
	 * after the guide has published its "Take the potion" step, as in the game.
	 */
	private void makeUntilDone()
	{
		for (int i = 0; i < 40; i++)
		{
			if (!makeOne())
			{
				return;
			}
		}
		throw new AssertionError("the make batch never ended");
	}

	/**
	 * Makes the potion the guide asks for. False, with nothing made, once the guide has moved on.
	 */
	private boolean makeOne()
	{
		current = guide.next(input(null));
		if (current.phase() != Phase.MAKE)
		{
			return false;
		}
		if (current.headline().startsWith("Use digweed on"))
		{
			useDigweed(current.digweedSlot());
			return true;
		}
		takeFromVessel(current.make());
		return true;
	}

	/**
	 * A potion of {@code type} is ready in the vessel: the guide publishes its step, then
	 * the potion lands in the lowest empty slot.
	 */
	private void takeFromVessel(PotionType type)
	{
		current = guide.next(input(type));
		assertEquals("Take the potion", current.headline());
		inv[firstEmpty()] = type.unfinishedId();
		changeInventory();
	}

	private void useDigweed(int slot)
	{
		tracker.onDigweedUsed(slot);
		for (int i = 0; i < inv.length; i++)
		{
			if (inv[i] == DIGWEED)
			{
				inv[i] = -1;
				break;
			}
		}
		changeInventory();
		assertTrue(tracker.map().get(slot).digweed());
	}

	private void changeInventory()
	{
		tracker.onInventory(inv.clone(), tick);
		tracker.onTick(tick++);
		assertEquals(TrackerState.TRACKING, tracker.state());
	}

	/**
	 * The belt takes a finished potion for every current order.
	 */
	private void deposit()
	{
		tracker.onDepositClick(orders);
		SlotMap m = tracker.map();
		for (Order o : orders)
		{
			for (int slot : m.occupiedSlots())
			{
				PotionSlot p = m.get(slot);
				if (inv[slot] != -1 && p.processed() && p.type() == o.type() && p.station() == o.station())
				{
					inv[slot] = -1;
					break;
				}
			}
		}
		tracker.onInventory(inv.clone(), tick, orders);
		tracker.onTick(tick++);
		assertEquals(TrackerState.TRACKING, tracker.state());
		assertEquals(18, tracker.map().occupiedSlots().size());
	}

	/**
	 * Processes potions one at a time as the guide directs. Returns the headline at the start
	 * of each run of one station.
	 */
	private List<String> processAll()
	{
		List<String> headlines = new ArrayList<>();
		Station last = null;
		for (int i = 0; i < 40; i++)
		{
			current = guide.next(input(null));
			if (current.phase() != Phase.PROCESS)
			{
				return headlines;
			}
			int slot = current.nextPotionSlot();
			PotionSlot p = tracker.map().get(slot);
			assertEquals(current.station(), p.station());
			if (p.station() != last)
			{
				headlines.add(current.headline());
				last = p.station();
			}
			tracker.onStationLoaded(p.station(), p.type());
			inv[slot] = -1;
			tracker.onInventory(inv.clone(), tick);
			tracker.onStationCleared(p.station());
			inv[firstEmpty()] = p.type().finishedId();
			tracker.onInventory(inv.clone(), tick);
			tracker.onTick(tick++);
			assertEquals(TrackerState.TRACKING, tracker.state());
		}
		throw new AssertionError("processing never ended");
	}

	private GuideInput input(PotionType vesselReady)
	{
		GuideInput in = new GuideInput();
		in.herbloreLevel = 99;
		in.trackerState = tracker.state();
		in.map = tracker.map();
		in.plan = plan;
		in.orders = orders;
		in.inStations = tracker.stationContents();
		in.vesselReady = vesselReady;
		in.restockThreshold = 50;
		in.mode = Mode.BALANCED;
		in.landingSlot = landingSlot();
		in.digweedHeld = Arrays.stream(inv).anyMatch(id -> id == DIGWEED);
		return in;
	}

	/**
	 * Finished stock from slot 3 on, with the potions a belt run used taken out.
	 */
	private SlotMap afterBeltRun()
	{
		SlotMap seed = new SlotMap();
		int slot = 3;
		for (Station s : Station.PROCESS_ORDER)
		{
			for (PotionType type : plan.types())
			{
				seed.put(slot++, new PotionSlot(type, true, s, false));
			}
		}
		for (int used : new int[]{4, 6, 8, 10, 11, 13, 16, 17, 19, 21, 23})
		{
			seed.remove(used);
		}
		for (int s : seed.occupiedSlots())
		{
			inv[s] = seed.get(s).type().finishedId();
		}
		return seed;
	}

	private List<Station> unprocessedStations()
	{
		SlotMap m = tracker.map();
		List<Station> out = new ArrayList<>();
		for (int slot : m.occupiedSlots())
		{
			if (!m.get(slot).processed())
			{
				out.add(m.get(slot).station());
			}
		}
		return out;
	}

	private static void assertGrouped(List<Station> stations)
	{
		for (int i = 1; i < stations.size(); i++)
		{
			assertTrue("not grouped by station: " + stations,
				stations.get(i - 1).ordinal() <= stations.get(i).ordinal());
		}
	}

	private int firstEmpty()
	{
		int slot = landingSlot();
		if (slot < 0)
		{
			throw new AssertionError("inventory full");
		}
		return slot;
	}

	/**
	 * Where the next potion lands: the lowest empty slot, -1 when the inventory is full.
	 */
	private int landingSlot()
	{
		for (int slot = 0; slot < inv.length; slot++)
		{
			if (inv[slot] == -1)
			{
				return slot;
			}
		}
		return -1;
	}

	private static int[] emptyInventory()
	{
		int[] ids = new int[SlotMap.SIZE];
		Arrays.fill(ids, -1);
		return ids;
	}
}
