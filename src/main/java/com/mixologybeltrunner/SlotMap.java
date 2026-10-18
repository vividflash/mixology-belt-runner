package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Which potion sits in each inventory slot and which station it went through.
 * The game does not expose that, so the plugin tracks it here.
 */
public final class SlotMap
{
	public static final int SIZE = 28;

	private final PotionSlot[] slots = new PotionSlot[SIZE];

	public PotionSlot get(int slot)
	{
		return slots[slot];
	}

	public void put(int slot, PotionSlot p)
	{
		slots[slot] = p;
	}

	public PotionSlot remove(int slot)
	{
		PotionSlot old = slots[slot];
		slots[slot] = null;
		return old;
	}

	/**
	 * Swaps two slots; either side may be empty.
	 */
	public void move(int from, int to)
	{
		PotionSlot tmp = slots[from];
		slots[from] = slots[to];
		slots[to] = tmp;
	}

	public int firstUnprocessed()
	{
		for (int i = 0; i < SIZE; i++)
		{
			if (slots[i] != null && !slots[i].processed())
			{
				return i;
			}
		}
		return -1;
	}

	public List<Integer> occupiedSlots()
	{
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < SIZE; i++)
		{
			if (slots[i] != null)
			{
				out.add(i);
			}
		}
		return out;
	}

	public int[] expectedItemIds()
	{
		int[] ids = new int[SIZE];
		for (int i = 0; i < SIZE; i++)
		{
			ids[i] = slots[i] == null ? -1 : slots[i].expectedItemId();
		}
		return ids;
	}

	public boolean isEmpty()
	{
		for (PotionSlot p : slots)
		{
			if (p != null)
			{
				return false;
			}
		}
		return true;
	}

	public int countCovered(Cell c)
	{
		return count(c, false);
	}

	public int countFinished(Cell c)
	{
		return count(c, true);
	}

	private int count(Cell c, boolean finishedOnly)
	{
		int n = 0;
		for (PotionSlot p : slots)
		{
			if (p != null && p.type() == c.type() && p.station() == c.station()
				&& (!finishedOnly || p.processed()))
			{
				n++;
			}
		}
		return n;
	}

	public SlotMap copy()
	{
		SlotMap m = new SlotMap();
		System.arraycopy(slots, 0, m.slots, 0, SIZE);
		return m;
	}

	/**
	 * A copy with every unprocessed entry marked processed at its planned station.
	 */
	public SlotMap assumingProcessed()
	{
		SlotMap m = copy();
		for (int i = 0; i < SIZE; i++)
		{
			PotionSlot p = m.slots[i];
			if (p != null && !p.processed())
			{
				m.slots[i] = p.processedAt(p.station());
			}
		}
		return m;
	}
}
