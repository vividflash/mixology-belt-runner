package com.mixologybeltrunner;

import net.runelite.api.coords.WorldPoint;

/**
 * Where the walking path leads for a step.
 */
final class PathTargets
{
	private PathTargets()
	{
	}

	/**
	 * The digweed spot while picking one up, the vessel while making, the station while
	 * processing, the belt when it is time to deposit; null otherwise.
	 */
	static WorldPoint target(Step step)
	{
		if (step == null)
		{
			return null;
		}
		if (step.digweedPickup() && step.digweedSpot() != null)
		{
			return step.digweedSpot().coordinate();
		}
		switch (step.phase())
		{
			case MAKE:
				return LabObject.MIXING_VESSEL.coordinate();
			case PROCESS:
				return step.station() == null ? null : step.station().object().coordinate();
			case BELT:
				return step.belt() == BeltColour.GREEN ? LabObject.CONVEYOR_BELT.coordinate() : null;
			default:
				return null;
		}
	}
}
