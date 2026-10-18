package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The finished potions to keep in stock: three stations of each chosen type,
 * made one station at a time, so every type gains a station before any gains a second.
 */
public final class StockPlan
{
	private static final int ORDER_RESERVE = 3;
	private static final int STATIONS = 3;

	private final List<PotionType> types;

	private StockPlan(List<PotionType> types)
	{
		this.types = Collections.unmodifiableList(types);
	}

	/**
	 * @param freeSlots 28 minus the non-potion items held
	 */
	public static StockPlan create(int freeSlots)
	{
		return create(freeSlots, Integer.MAX_VALUE);
	}

	public static StockPlan create(int freeSlots, int herbloreLevel)
	{
		return create(freeSlots, herbloreLevel, Integer.MAX_VALUE);
	}

	/**
	 * A plan of the best non-triples the player's Herblore level can make, at most maxTypes of them.
	 */
	public static StockPlan create(int freeSlots, int herbloreLevel, int maxTypes)
	{
		List<PotionType> candidates = new ArrayList<>();
		for (PotionType type : PotionType.values())
		{
			if (!type.isTriple() && type.level() <= herbloreLevel)
			{
				candidates.add(type);
			}
		}
		candidates.sort(Comparator.comparingInt(PotionType::xp));
		int keep = Math.max(0, Math.min(Math.min(candidates.size(), maxTypes), (freeSlots - ORDER_RESERVE) / STATIONS));
		return new StockPlan(new ArrayList<>(candidates.subList(candidates.size() - keep, candidates.size())));
	}

	public List<PotionType> types()
	{
		return types;
	}

	public int size()
	{
		return types.size() * STATIONS;
	}

	public boolean contains(PotionType type)
	{
		return types.contains(type);
	}

	/**
	 * Planned cells with nothing in stock, in make order.
	 */
	public Set<Cell> missing(SlotMap map, Map<Station, PotionSlot> inStations)
	{
		Set<Cell> out = new LinkedHashSet<>();
		for (Station station : Station.PROCESS_ORDER)
		{
			for (PotionType type : types)
			{
				Cell cell = new Cell(type, station);
				if (covered(map, inStations, cell) == 0)
				{
					out.add(cell);
				}
			}
		}
		return out;
	}

	/**
	 * The cells in the order to make them: station by station (Alembic, Retort, Agitator),
	 * planned types in plan order, then any other type. New potions fill the first empty
	 * slot, so making in this order keeps the unprocessed potions grouped by station.
	 */
	public List<Cell> makeOrder(Collection<Cell> cells)
	{
		List<Cell> out = new ArrayList<>(cells);
		out.sort(Comparator.comparingInt((Cell c) -> c.station().ordinal()).thenComparingInt(this::typeRank));
		return out;
	}

	/**
	 * The station for a new potion when the guide did not ask for it: an order still
	 * short of it, then a planned cell with nothing in stock, then the least used station.
	 */
	public Station assignStation(SlotMap map, Map<Station, PotionSlot> inStations, PotionType type, List<Order> orders)
	{
		for (Order order : orders)
		{
			Cell cell = new Cell(type, order.station());
			if (order.type() == type && covered(map, inStations, cell) < demand(orders, cell))
			{
				return order.station();
			}
		}
		if (contains(type))
		{
			for (Station station : Station.PROCESS_ORDER)
			{
				if (covered(map, inStations, new Cell(type, station)) == 0)
				{
					return station;
				}
			}
		}
		Station best = Station.PROCESS_ORDER.get(0);
		int bestCount = Integer.MAX_VALUE;
		for (Station station : Station.PROCESS_ORDER)
		{
			int count = covered(map, inStations, new Cell(type, station));
			if (count < bestCount)
			{
				best = station;
				bestCount = count;
			}
		}
		return best;
	}

	/**
	 * The restock round in progress, 1 to 3.
	 */
	public int round(SlotMap map, Map<Station, PotionSlot> inStations)
	{
		if (types.isEmpty())
		{
			return 1;
		}
		int min = STATIONS;
		for (PotionType type : types)
		{
			min = Math.min(min, coveredCells(map, inStations, type));
		}
		return Math.min(STATIONS, 1 + min);
	}

	public boolean needsRestock(SlotMap map, Map<Station, PotionSlot> inStations, int thresholdPercent)
	{
		return size() > 0 && missing(map, inStations).size() * 100 >= thresholdPercent * size();
	}

	/**
	 * Potions held for this cell, finished or not, plus one if a station holds it.
	 */
	public static int covered(SlotMap map, Map<Station, PotionSlot> inStations, Cell cell)
	{
		PotionSlot loaded = inStations.get(cell.station());
		return map.countCovered(cell) + (loaded != null && loaded.type() == cell.type() ? 1 : 0);
	}

	/**
	 * How many of the orders ask for exactly this type and station.
	 */
	public static int demand(List<Order> orders, Cell cell)
	{
		int n = 0;
		for (Order order : orders)
		{
			if (order.type() == cell.type() && order.station() == cell.station())
			{
				n++;
			}
		}
		return n;
	}

	private int typeRank(Cell cell)
	{
		int i = types.indexOf(cell.type());
		return i >= 0 ? i : types.size() + cell.type().ordinal();
	}

	private static int coveredCells(SlotMap map, Map<Station, PotionSlot> inStations, PotionType type)
	{
		int n = 0;
		for (Station station : Station.PROCESS_ORDER)
		{
			if (covered(map, inStations, new Cell(type, station)) > 0)
			{
				n++;
			}
		}
		return n;
	}
}
