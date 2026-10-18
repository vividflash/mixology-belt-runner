package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The per-type, per-station grid the window shows.
 */
public final class StockSummary
{
	private StockSummary()
	{
	}

	public static final class Row
	{
		private final PotionType type;
		private final Map<Station, CellState> states = new EnumMap<>(Station.class);
		private final Map<Station, Boolean> digweeds = new EnumMap<>(Station.class);

		private Row(PotionType type)
		{
			this.type = type;
			for (Station station : Station.PROCESS_ORDER)
			{
				states.put(station, CellState.MISSING);
				digweeds.put(station, false);
			}
		}

		public PotionType type()
		{
			return type;
		}

		public CellState state(Station s)
		{
			return states.get(s);
		}

		public boolean digweed(Station s)
		{
			return digweeds.get(s);
		}

		private void note(Station station, CellState state, boolean digweed)
		{
			if (state == CellState.FINISHED || states.get(station) == CellState.MISSING)
			{
				states.put(station, state);
			}
			if (digweed)
			{
				digweeds.put(station, true);
			}
		}
	}

	/**
	 * One row per planned type, in plan order. A potion inside a station counts
	 * as unprocessed at that station.
	 */
	public static List<Row> rows(StockPlan plan, SlotMap map, Map<Station, PotionSlot> inStations)
	{
		List<Row> rows = new ArrayList<>();
		for (PotionType type : plan.types())
		{
			Row row = new Row(type);
			for (int i : map.occupiedSlots())
			{
				PotionSlot slot = map.get(i);
				if (slot.type() == type && slot.station() != null)
				{
					row.note(slot.station(), slot.processed() ? CellState.FINISHED : CellState.UNPROCESSED, slot.digweed());
				}
			}
			for (Map.Entry<Station, PotionSlot> entry : inStations.entrySet())
			{
				PotionSlot slot = entry.getValue();
				if (slot != null && slot.type() == type)
				{
					row.note(entry.getKey(), CellState.UNPROCESSED, slot.digweed());
				}
			}
			rows.add(row);
		}
		return Collections.unmodifiableList(rows);
	}
}
