package com.mixologybeltrunner;

import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * When a previously computed walk can be kept. Reusing a path after you have stepped off it
 * draws a V: from your new tile, back to the old waypoint, then on to the destination.
 */
final class PathPolicy
{
	static final int DRIFT_TILES = 1;
	static final int DETOUR_SLACK = 4;

	private PathPolicy()
	{
	}

	static int nearestIndex(List<WorldPoint> path, WorldPoint start)
	{
		int nearest = 0;
		int best = Integer.MAX_VALUE;
		for (int i = 0; i < path.size(); i++)
		{
			int d = path.get(i).distanceTo(start);
			if (d < best)
			{
				best = d;
				nearest = i;
			}
		}
		return nearest;
	}

	static boolean canReuse(WorldPoint start, WorldPoint end, List<WorldPoint> cached)
	{
		if (start == null || end == null || cached == null || cached.size() < 2)
		{
			return false;
		}
		int nearest = nearestIndex(cached, start);
		if (nearest >= cached.size() - 1)
		{
			return false;
		}
		int dist = cached.get(nearest).distanceTo(start);
		if (dist > DRIFT_TILES)
		{
			return false;
		}
		WorldPoint first = cached.get(nearest);
		if (first.distanceTo(end) > start.distanceTo(end))
		{
			return false;
		}
		int via = start.distanceTo(first) + polylineLength(cached, nearest);
		return via <= start.distanceTo(end) + DETOUR_SLACK;
	}

	static int polylineLength(List<WorldPoint> path, int from)
	{
		int length = 0;
		for (int i = from; i < path.size() - 1; i++)
		{
			length += path.get(i).distanceTo(path.get(i + 1));
		}
		return length;
	}
}
