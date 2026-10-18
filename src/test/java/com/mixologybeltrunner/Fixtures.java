package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared builders for tests.
 */
final class Fixtures
{
	private Fixtures()
	{
	}

	static PotionSlot fin(PotionType type, Station station)
	{
		return new PotionSlot(type, true, station, false);
	}

	static PotionSlot unf(PotionType type, Station station)
	{
		return new PotionSlot(type, false, station, false);
	}

	/**
	 * A slot map with the given slots at 0, 1, 2...
	 */
	static SlotMap map(PotionSlot... slots)
	{
		SlotMap m = new SlotMap();
		for (int i = 0; i < slots.length; i++)
		{
			m.put(i, slots[i]);
		}
		return m;
	}

	/**
	 * Removes the first finished potion of that type and station.
	 */
	static void removeType(SlotMap map, PotionType type, Station station)
	{
		for (int slot : map.occupiedSlots())
		{
			PotionSlot p = map.get(slot);
			if (p.processed() && p.type() == type && p.station() == station)
			{
				map.remove(slot);
				return;
			}
		}
		throw new AssertionError("no finished " + type + " at " + station);
	}

	/**
	 * The lowest empty slot, or -1 when the map is full.
	 */
	static int firstEmpty(SlotMap map)
	{
		for (int slot = 0; slot < SlotMap.SIZE; slot++)
		{
			if (map.get(slot) == null)
			{
				return slot;
			}
		}
		return -1;
	}

	/**
	 * Orders from alternating type and station arguments, with board indices 0..n.
	 */
	static List<Order> orders(Object... typeStationPairs)
	{
		List<Order> out = new ArrayList<>();
		for (int i = 0; i + 1 < typeStationPairs.length; i += 2)
		{
			out.add(new Order(i / 2, (PotionType) typeStationPairs[i], (Station) typeStationPairs[i + 1]));
		}
		return out;
	}
}
