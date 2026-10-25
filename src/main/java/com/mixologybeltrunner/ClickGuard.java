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
	 * True for a lever pull the potion no longer needs, and for any pull while the finished
	 * potion waits in the vessel. The owed pulls may come in any order: the vessel only checks
	 * which three components it holds. No numbers and nothing waiting never blocks.
	 */
	public static boolean blockLever(java.util.Map<PotionComponent, java.util.List<Integer>> numbers,
		boolean potionWaiting, PotionComponent clicked)
	{
		if (potionWaiting)
		{
			return true;
		}
		return !numbers.isEmpty() && !numbers.containsKey(clicked);
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
