package com.mixologybeltrunner;

import static com.mixologybeltrunner.Fixtures.fin;
import static com.mixologybeltrunner.Fixtures.unf;
import static com.mixologybeltrunner.PotionType.LIPLACK_LIQUOR;
import static com.mixologybeltrunner.PotionType.MARLEYS_MOONLIGHT;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.PotionType.MYSTIC_MANA_AMALGAM;
import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class StockPlanTest
{
	private static final Map<Station, PotionSlot> NONE = Map.of();

	@Test
	public void fullPlanWith25Free()
	{
		StockPlan p = StockPlan.create(25);
		assertEquals(21, p.size());
		assertEquals(MYSTIC_MANA_AMALGAM, p.types().get(0));
		assertEquals(MIXALOT, p.types().get(6));
	}

	@Test
	public void tinySpaceMeansEmptyPlan()
	{
		assertEquals(0, StockPlan.create(5).size());
		assertEquals(0, StockPlan.create(2).size());
	}

	@Test
	public void shortSpaceCutsLowestXpFirst()
	{
		StockPlan p = StockPlan.create(22);
		assertEquals(18, p.size());
		assertFalse(p.contains(MYSTIC_MANA_AMALGAM));
		assertTrue(p.contains(MARLEYS_MOONLIGHT));
	}

	@Test
	public void makeOrderIsStationMajor()
	{
		StockPlan p = StockPlan.create(25);
		List<Cell> order = p.makeOrder(p.missing(new SlotMap(), NONE));
		assertEquals(21, order.size());
		assertEquals(new Cell(MYSTIC_MANA_AMALGAM, ALEMBIC), order.get(0));
		assertEquals(new Cell(MARLEYS_MOONLIGHT, ALEMBIC), order.get(1));
		assertEquals(new Cell(MIXALOT, ALEMBIC), order.get(6));
		assertEquals(new Cell(MYSTIC_MANA_AMALGAM, RETORT), order.get(7));
		assertEquals(new Cell(MIXALOT, AGITATOR), order.get(20));
	}

	@Test
	public void restockRunsInRounds()
	{
		StockPlan p = StockPlan.create(25);
		SlotMap m = new SlotMap();
		assertEquals(ALEMBIC, p.assignStation(m, NONE, MYSTIC_MANA_AMALGAM, List.of()));
		int slot = 0;
		for (PotionType type : p.types())
		{
			m.put(slot++, unf(type, ALEMBIC));
		}
		assertEquals(RETORT, p.assignStation(m, NONE, MYSTIC_MANA_AMALGAM, List.of()));
		assertEquals(2, p.round(m, NONE));
	}

	@Test
	public void potionInAStationCountsAsCovered()
	{
		StockPlan p = StockPlan.create(25);
		Map<Station, PotionSlot> loaded = Map.of(ALEMBIC, unf(MIXALOT, ALEMBIC));
		assertFalse(p.missing(new SlotMap(), loaded).contains(new Cell(MIXALOT, ALEMBIC)));
		assertEquals(20, p.missing(new SlotMap(), loaded).size());
		assertEquals(RETORT, p.assignStation(new SlotMap(), loaded, MIXALOT, List.of()));
		assertEquals(1, StockPlan.covered(new SlotMap(), loaded, new Cell(MIXALOT, ALEMBIC)));
		assertEquals(0, StockPlan.covered(new SlotMap(), loaded, new Cell(MIXALOT, RETORT)));
	}

	@Test
	public void orderCellWinsStationAssignment()
	{
		StockPlan p = StockPlan.create(25);
		List<Order> orders = List.of(new Order(0, MARLEYS_MOONLIGHT, AGITATOR));
		assertEquals(AGITATOR, p.assignStation(new SlotMap(), NONE, MARLEYS_MOONLIGHT, orders));
		assertEquals(AGITATOR,
			p.assignStation(new SlotMap(), NONE, LIPLACK_LIQUOR, List.of(new Order(1, LIPLACK_LIQUOR, AGITATOR))));
	}

	@Test
	public void duplicateOrdersAskForASecondCopy()
	{
		StockPlan p = StockPlan.create(25);
		SlotMap m = new SlotMap();
		m.put(0, fin(MIXALOT, ALEMBIC));
		List<Order> orders = List.of(new Order(0, MIXALOT, ALEMBIC), new Order(1, MIXALOT, ALEMBIC),
			new Order(2, MYSTIC_MANA_AMALGAM, RETORT));
		assertEquals(ALEMBIC, p.assignStation(m, NONE, MIXALOT, orders));
		assertEquals(RETORT, p.assignStation(m, NONE, MIXALOT, List.of()));
	}

	@Test
	public void unplannedTypeUsesLeastUsedStation()
	{
		StockPlan p = StockPlan.create(25);
		SlotMap m = new SlotMap();
		m.put(0, unf(LIPLACK_LIQUOR, ALEMBIC));
		m.put(1, unf(LIPLACK_LIQUOR, ALEMBIC));
		m.put(2, unf(LIPLACK_LIQUOR, RETORT));
		assertEquals(AGITATOR, p.assignStation(m, NONE, LIPLACK_LIQUOR, List.of()));
	}

	@Test
	public void unplannedTypesComeLastInTheirStation()
	{
		StockPlan p = StockPlan.create(25);
		Set<Cell> cells = Set.of(new Cell(MYSTIC_MANA_AMALGAM, RETORT), new Cell(LIPLACK_LIQUOR, ALEMBIC),
			new Cell(MIXALOT, ALEMBIC));
		assertEquals(List.of(new Cell(MIXALOT, ALEMBIC), new Cell(LIPLACK_LIQUOR, ALEMBIC),
			new Cell(MYSTIC_MANA_AMALGAM, RETORT)), p.makeOrder(cells));
	}

	@Test
	public void restockThreshold()
	{
		StockPlan p = StockPlan.create(25);
		SlotMap m = new SlotMap();
		int slot = 0;
		for (Cell c : p.missing(m, NONE))
		{
			if (slot < 11)
			{
				m.put(slot++, fin(c.type(), c.station()));
			}
		}
		assertFalse(p.needsRestock(m, NONE, 50));
		m.remove(0);
		assertTrue(p.needsRestock(m, NONE, 50));
	}
}
