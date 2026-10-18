package com.mixologybeltrunner;

import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class StockSummaryTest
{
	@Test
	public void rowsReflectEveryState()
	{
		StockPlan plan = StockPlan.create(25);
		SlotMap m = new SlotMap();
		m.put(0, new PotionSlot(MIXALOT, true, ALEMBIC, false));
		m.put(1, new PotionSlot(MIXALOT, true, RETORT, true));
		List<StockSummary.Row> rows = StockSummary.rows(plan, m,
			Map.of(AGITATOR, new PotionSlot(MIXALOT, false, AGITATOR, false)));
		StockSummary.Row mal = rows.get(6);
		assertEquals(MIXALOT, mal.type());
		assertEquals(CellState.FINISHED, mal.state(ALEMBIC));
		assertTrue(mal.digweed(RETORT));
		assertFalse(mal.digweed(ALEMBIC));
		assertEquals(CellState.UNPROCESSED, mal.state(AGITATOR));
		assertEquals(CellState.MISSING, rows.get(0).state(ALEMBIC));
		assertEquals(plan.types().size(), rows.size());
	}

	@Test
	public void unprocessedInInventoryCountsAsUnprocessed()
	{
		StockPlan plan = StockPlan.create(25);
		SlotMap m = Fixtures.map(Fixtures.unf(MIXALOT, RETORT));
		StockSummary.Row mal = StockSummary.rows(plan, m, Map.of()).get(6);
		assertEquals(CellState.UNPROCESSED, mal.state(RETORT));
	}

	@Test
	public void finishedWinsOverUnprocessed()
	{
		StockPlan plan = StockPlan.create(25);
		SlotMap m = Fixtures.map(Fixtures.fin(MIXALOT, RETORT), Fixtures.unf(MIXALOT, RETORT));
		StockSummary.Row mal = StockSummary.rows(plan, m, Map.of()).get(6);
		assertEquals(CellState.FINISHED, mal.state(RETORT));
	}
}
