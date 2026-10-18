package com.mixologybeltrunner;

import static com.mixologybeltrunner.Fixtures.fin;
import static com.mixologybeltrunner.Fixtures.unf;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.PotionType.MYSTIC_MANA_AMALGAM;
import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;

public class SlotMapTest
{
	@Test
	public void swapOfIdenticalPotionsSwapsStations()
	{
		SlotMap m = new SlotMap();
		m.put(0, fin(MYSTIC_MANA_AMALGAM, ALEMBIC));
		m.put(1, fin(MYSTIC_MANA_AMALGAM, RETORT));
		m.move(0, 1);
		assertEquals(RETORT, m.get(0).station());
		assertEquals(ALEMBIC, m.get(1).station());
	}

	@Test
	public void moveIntoEmptySlot()
	{
		SlotMap m = new SlotMap();
		m.put(2, fin(MYSTIC_MANA_AMALGAM, AGITATOR));
		m.move(2, 3);
		assertNull(m.get(2));
		assertEquals(AGITATOR, m.get(3).station());
	}

	@Test
	public void firstUnprocessedSkipsFinished()
	{
		SlotMap m = new SlotMap();
		m.put(0, fin(MIXALOT, ALEMBIC));
		m.put(4, unf(MIXALOT, RETORT));
		m.put(6, unf(MIXALOT, AGITATOR));
		assertEquals(4, m.firstUnprocessed());
	}

	@Test
	public void expectedIdsAndAssumingProcessed()
	{
		SlotMap m = new SlotMap();
		m.put(1, unf(MIXALOT, RETORT));
		assertEquals(MIXALOT.unfinishedId(), m.expectedItemIds()[1]);
		assertEquals(-1, m.expectedItemIds()[0]);
		SlotMap a = m.assumingProcessed();
		assertTrue(a.get(1).processed());
		assertFalse(m.get(1).processed());
		assertEquals(1, a.countFinished(new Cell(MIXALOT, RETORT)));
	}

	@Test
	public void occupiedEmptyRemoveAndCounts()
	{
		SlotMap m = new SlotMap();
		assertTrue(m.isEmpty());
		assertEquals(-1, m.firstUnprocessed());
		m.put(5, fin(MIXALOT, RETORT));
		m.put(2, unf(MIXALOT, RETORT));
		assertEquals(Arrays.asList(2, 5), m.occupiedSlots());
		Cell c = new Cell(MIXALOT, RETORT);
		assertEquals(2, m.countCovered(c));
		assertEquals(1, m.countFinished(c));
		assertEquals(MIXALOT.finishedId(), m.expectedItemIds()[5]);
		assertEquals(2, m.remove(2).type() == MIXALOT ? 2 : 0);
		assertNull(m.get(2));
		assertFalse(m.isEmpty());
	}
}
