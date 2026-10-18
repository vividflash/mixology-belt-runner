package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Groups the stock grid into the lines of the to-do list window.
 */
final class TodoLines
{
	private TodoLines()
	{
	}

	/**
	 * A type still to make, with the stations it has no potion at yet, in station order.
	 */
	static final class Make
	{
		private final PotionType type;
		private final List<Station> stations;

		Make(PotionType type, List<Station> stations)
		{
			this.type = type;
			this.stations = Collections.unmodifiableList(stations);
		}

		PotionType type()
		{
			return type;
		}

		List<Station> stations()
		{
			return stations;
		}

		int count()
		{
			return stations.size();
		}
	}

	/**
	 * Types with at least one missing cell, in plan order.
	 */
	static List<Make> toMake(List<StockSummary.Row> rows)
	{
		List<Make> out = new ArrayList<>();
		for (StockSummary.Row row : rows)
		{
			List<Station> missing = new ArrayList<>();
			for (Station station : Station.PROCESS_ORDER)
			{
				if (row.state(station) == CellState.MISSING)
				{
					missing.add(station);
				}
			}
			if (!missing.isEmpty())
			{
				out.add(new Make(row.type(), missing));
			}
		}
		return out;
	}

	/**
	 * Types waiting to be processed, grouped by station. Stations with nothing waiting are left out.
	 */
	static Map<Station, List<PotionType>> toProcess(List<StockSummary.Row> rows)
	{
		Map<Station, List<PotionType>> out = new EnumMap<>(Station.class);
		for (Station station : Station.PROCESS_ORDER)
		{
			List<PotionType> types = new ArrayList<>();
			for (StockSummary.Row row : rows)
			{
				if (row.state(station) == CellState.UNPROCESSED)
				{
					types.add(row.type());
				}
			}
			if (!types.isEmpty())
			{
				out.put(station, types);
			}
		}
		return out;
	}
}
