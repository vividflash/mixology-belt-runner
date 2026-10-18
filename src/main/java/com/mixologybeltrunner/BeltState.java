package com.mixologybeltrunner;

/**
 * How the three current orders stand against the finished potions held.
 */
public enum BeltState
{
	ALL_FILLABLE(true),
	/**
	 * Something fills and nothing unfilled is worth making first.
	 */
	SKIPPING(true),
	/**
	 * An unfilled order is worth making before the belt is clicked.
	 */
	NEED_ORDER(false),
	NOTHING_FILLABLE(false);

	private final boolean open;

	BeltState(boolean open)
	{
		this.open = open;
	}

	/**
	 * True when clicking the belt is worth it.
	 */
	public boolean open()
	{
		return open;
	}
}
