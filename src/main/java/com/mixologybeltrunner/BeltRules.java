package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides whether clicking the conveyor belt is worth it for the current orders. Any click
 * that fills one order brings a whole new set, so the belt is worth clicking whenever
 * something fills and no unfilled order is worth making first (see {@link Mode#worthMaking}).
 */
public final class BeltRules
{
	private static final int ORDER_COUNT = 3;
	private static final Comparator<Order> BEST_FIRST = Comparator.comparingInt((Order o) -> o.type().xp()).reversed();

	private BeltRules()
	{
	}

	/**
	 * Returns null unless all three orders are known.
	 */
	public static BeltVerdict evaluate(List<Order> orders, SlotMap map, Mode mode)
	{
		if (orders == null || orders.size() != ORDER_COUNT)
		{
			return null;
		}

		boolean[] claimed = new boolean[SlotMap.SIZE];
		List<Order> unfilled = new ArrayList<>();
		List<Order> worthMaking = new ArrayList<>();
		int fills = 0;
		for (Order o : orders)
		{
			if (claim(o, map, claimed))
			{
				fills++;
			}
			else
			{
				unfilled.add(o);
				if (mode.worthMaking(o.type()))
				{
					worthMaking.add(o);
				}
			}
		}
		if (unfilled.isEmpty())
		{
			return new BeltVerdict(BeltState.ALL_FILLABLE, null, Collections.emptyList(), unfilled, fills);
		}
		if (worthMaking.isEmpty() && fills >= 1)
		{
			return new BeltVerdict(BeltState.SKIPPING, null, unfilled, unfilled, fills);
		}
		if (!worthMaking.isEmpty())
		{
			return new BeltVerdict(BeltState.NEED_ORDER, best(worthMaking), Collections.emptyList(), unfilled, fills);
		}
		return new BeltVerdict(BeltState.NOTHING_FILLABLE, best(unfilled), Collections.emptyList(), unfilled, fills);
	}

	/**
	 * The cells a closed belt needs made, in board order: every unfilled order worth making,
	 * or the best unfilled order when nothing can be filled. Empty for an open belt or
	 * unknown orders. Two orders for the same cell give one entry; the guide counts the
	 * demand itself.
	 */
	public static Set<Cell> neededCells(BeltVerdict v, Mode mode)
	{
		Set<Cell> out = new LinkedHashSet<>();
		if (v == null)
		{
			return out;
		}
		switch (v.state())
		{
			case NEED_ORDER:
				for (Order o : v.unfilled())
				{
					if (mode.worthMaking(o.type()))
					{
						out.add(new Cell(o.type(), o.station()));
					}
				}
				break;
			case NOTHING_FILLABLE:
				out.add(new Cell(v.toMake().type(), v.toMake().station()));
				break;
			default:
				break;
		}
		return out;
	}

	private static Order best(List<Order> orders)
	{
		List<Order> sorted = new ArrayList<>(orders);
		sorted.sort(BEST_FIRST);
		return sorted.get(0);
	}

	private static boolean claim(Order o, SlotMap map, boolean[] claimed)
	{
		for (int i = 0; i < SlotMap.SIZE; i++)
		{
			PotionSlot p = map.get(i);
			if (!claimed[i] && p != null && p.processed() && p.type() == o.type() && p.station() == o.station())
			{
				claimed[i] = true;
				return true;
			}
		}
		return false;
	}
}
