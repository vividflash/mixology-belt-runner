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

public class TodoLinesTest
{
	@Test
	public void missingCellsAreGroupedByType()
	{
		StockPlan plan = StockPlan.create(25);
		SlotMap m = Fixtures.map(Fixtures.fin(MIXALOT, ALEMBIC));
		List<StockSummary.Row> rows = StockSummary.rows(plan, m, Map.of());
		List<TodoLines.Make> make = TodoLines.toMake(rows);
		assertEquals(plan.types().size(), make.size());
		TodoLines.Make mal = make.get(6);
		assertEquals(MIXALOT, mal.type());
		assertEquals(2, mal.count());
		assertEquals(List.of(RETORT, AGITATOR), mal.stations());
	}

	@Test
	public void unprocessedAreGroupedByStation()
	{
		StockPlan plan = StockPlan.create(25);
		SlotMap m = Fixtures.map(Fixtures.unf(MIXALOT, RETORT));
		Map<Station, List<PotionType>> process = TodoLines.toProcess(StockSummary.rows(plan, m, Map.of()));
		assertEquals(List.of(MIXALOT), process.get(RETORT));
		assertFalse(process.containsKey(ALEMBIC));
		assertEquals(1, process.size());
	}

	@Test
	public void nothingToDoWhenEveryCellIsFinished()
	{
		StockPlan plan = StockPlan.create(25);
		List<StockSummary.Row> rows = StockSummary.rows(plan, new SlotMap(), Map.of());
		assertTrue(TodoLines.toProcess(rows).isEmpty());
	}
}
