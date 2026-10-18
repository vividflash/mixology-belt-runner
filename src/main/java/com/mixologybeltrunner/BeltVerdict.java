package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of checking the orders: the state, the order to make next when the
 * belt is closed, the orders being skipped, the orders left unfilled, and how many
 * orders can be filled.
 */
public final class BeltVerdict
{
	private final BeltState state;
	private final Order toMake;
	private final List<Order> skipped;
	private final List<Order> unfilled;
	private final int fills;

	BeltVerdict(BeltState state, Order toMake, List<Order> skipped, List<Order> unfilled, int fills)
	{
		this.state = state;
		this.toMake = toMake;
		this.skipped = new ArrayList<>(skipped);
		this.unfilled = new ArrayList<>(unfilled);
		this.fills = fills;
	}

	public BeltState state()
	{
		return state;
	}

	public Order toMake()
	{
		return toMake;
	}

	public List<Order> skipped()
	{
		return new ArrayList<>(skipped);
	}

	/**
	 * Every order the held potions cannot fill, in board order.
	 */
	public List<Order> unfilled()
	{
		return new ArrayList<>(unfilled);
	}

	public int fills()
	{
		return fills;
	}
}
