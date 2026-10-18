package com.mixologybeltrunner;

/**
 * Decides which clicks to swallow before they reach the game.
 */
public final class ClickGuard
{
	private ClickGuard()
	{
	}

	/**
	 * True for a click on an idle station when the next potion is planned for another one.
	 * A station that already holds a potion is never blocked: its clicks drive the process.
	 */
	public static boolean blockStation(Station clicked, boolean clickedOccupied, Station plannedForFirstUnprocessed)
	{
		return plannedForFirstUnprocessed != null && !clickedOccupied && clicked != plannedForFirstUnprocessed;
	}

	/**
	 * True for a lever pull that is not the next one the potion needs, and for any pull while
	 * the finished potion waits in the vessel. No numbers and nothing waiting never blocks.
	 */
	public static boolean blockLever(java.util.Map<PotionComponent, java.util.List<Integer>> numbers,
		boolean potionWaiting, PotionComponent clicked)
	{
		if (potionWaiting)
		{
			return true;
		}
		PotionComponent next = null;
		int lowest = Integer.MAX_VALUE;
		for (java.util.Map.Entry<PotionComponent, java.util.List<Integer>> e : numbers.entrySet())
		{
			for (int n : e.getValue())
			{
				if (n < lowest)
				{
					lowest = n;
					next = e.getKey();
				}
			}
		}
		return next != null && clicked != next;
	}

	/**
	 * True when the belt is locked. No verdict (orders unknown) never blocks.
	 */
	public static boolean blockBelt(BeltVerdict verdict)
	{
		return verdict != null && !verdict.state().open();
	}

	/**
	 * Also blocks a second belt click in the same tick. A tick hands in once, and a click
	 * that reaches the server late lands after that hand-in has brought new orders, where
	 * it can fail to fulfil and lose a potion.
	 */
	public static boolean blockBelt(BeltVerdict verdict, boolean clickedThisTick)
	{
		return clickedThisTick || blockBelt(verdict);
	}
}
