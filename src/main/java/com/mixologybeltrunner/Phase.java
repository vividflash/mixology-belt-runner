package com.mixologybeltrunner;

/**
 * Where the player is in the loop.
 */
public enum Phase
{
	/**
	 * Another mixology plugin is on, or the player cannot make every recipe yet.
	 */
	DORMANT,
	/**
	 * Tracking waits for an inventory with no potions.
	 */
	NEEDS_CLEAR,
	MAKE,
	PROCESS,
	BELT
}
