package com.mixologybeltrunner;

/**
 * The short stretch after the game shows a bonus-click effect at the alembic or agitator,
 * during which one extra click on that station earns bonus experience.
 * <p>
 * The window is timed by the station's progress varbit, not by ticks: the alembic's ends
 * on the first progress update after the effect, the agitator's on the second.
 */
public class QuickActionWindow
{
	/**
	 * Ticks after the effect at which the window closes anyway, should a progress update
	 * never arrive.
	 */
	static final int SAFETY_TICKS = 3;

	private Station station;
	private int startTick;
	private int progressSeen;

	/**
	 * Opens a window when the effect shows on a station that holds a potion. The retort has none.
	 */
	public void onSpotAnim(Station s, boolean stationHoldsPotion, int tick)
	{
		if (!stationHoldsPotion || progressUpdates(s) == 0)
		{
			return;
		}
		station = s;
		startTick = tick;
		progressSeen = 0;
	}

	public void onSkillshotHit(Station s)
	{
		end(s);
	}

	public void onProgressDropped(Station s)
	{
		end(s);
	}

	/**
	 * Any update to a station's progress varbit.
	 */
	public void onProgress(Station s)
	{
		if (station == s && ++progressSeen >= progressUpdates(s))
		{
			station = null;
		}
	}

	public void onTick(int tick)
	{
		if (station != null && tick - startTick >= SAFETY_TICKS)
		{
			station = null;
		}
	}

	/**
	 * The station whose window is open, or null.
	 */
	public Station active()
	{
		return station;
	}

	public void reset()
	{
		station = null;
	}

	private void end(Station s)
	{
		if (station == s)
		{
			station = null;
		}
	}

	/**
	 * Progress updates the window lasts for; 0 for a station without one.
	 */
	private static int progressUpdates(Station s)
	{
		if (s == Station.ALEMBIC)
		{
			return 1;
		}
		if (s == Station.AGITATOR)
		{
			return 2;
		}
		return 0;
	}
}
