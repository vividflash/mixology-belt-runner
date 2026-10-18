package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

@Singleton
class PathRouter
{
	private static final int AT_TARGET = 1;

	private List<WorldPoint> cachedPath = Collections.emptyList();
	private WorldPoint cachedEnd;
	private boolean useDirect;
	private WorldPoint directFrom;
	private Pathfinder.Jump cachedJump;

	@Inject
	PathRouter()
	{
	}

	void reset()
	{
		cachedPath = Collections.emptyList();
		cachedEnd = null;
		useDirect = false;
		directFrom = null;
		cachedJump = null;
	}

	List<WorldPoint> pathTo(WorldView worldView, WorldPoint start, WorldPoint end, Set<WorldPoint> blocked, boolean showPath)
	{
		return pathTo(worldView, start, end, blocked, showPath, null);
	}

	/** As above, allowed to take {@code jump} (null for none). */
	List<WorldPoint> pathTo(WorldView worldView, WorldPoint start, WorldPoint end, Set<WorldPoint> blocked, boolean showPath,
		Pathfinder.Jump jump)
	{
		if (!showPath || start == null || end == null || start.distanceTo(end) <= AT_TARGET)
		{
			reset();
			return Collections.emptyList();
		}
		if (jump != cachedJump)
		{
			// Gaining or losing the shortcut (a boost wearing off) changes the way.
			cachedEnd = null;
			cachedJump = jump;
		}

		Set<WorldPoint> block = blocked == null ? Collections.emptySet() : blocked;
		if (end.equals(cachedEnd) && useDirect && directFrom != null
			&& start.distanceTo(directFrom) < 3)
		{
			return directLine(worldView, start, end);
		}
		if (end.equals(cachedEnd) && PathPolicy.canReuse(start, end, cachedPath))
		{
			int nearest = PathPolicy.nearestIndex(cachedPath, start);
			List<WorldPoint> trimmed = new ArrayList<>(cachedPath.subList(nearest, cachedPath.size()));
			cachedPath = trimmed;
			return Pathfinder.simplify(Pathfinder.smooth(worldView, trimmed, block));
		}

		List<WorldPoint> path = Pathfinder.find(worldView, start, end, block, jump);
		if (path == null || path.isEmpty())
		{
			useDirect = true;
			directFrom = start;
			cachedPath = Collections.emptyList();
			cachedEnd = end;
			return directLine(worldView, start, end);
		}
		useDirect = false;
		directFrom = null;
		// Keep every tile for the reuse check; a smoothed path has too few points to stay near while running.
		cachedPath = path;
		cachedEnd = end;
		return Pathfinder.simplify(Pathfinder.smooth(worldView, path, block));
	}

	private List<WorldPoint> directLine(WorldView worldView, WorldPoint start, WorldPoint end)
	{
		WorldPoint clamped = Pathfinder.clampToScene(worldView, start, end);
		if (clamped == null || start.distanceTo(clamped) <= AT_TARGET)
		{
			return Collections.emptyList();
		}
		return Arrays.asList(start, clamped);
	}

	/** The end of the jump the way ahead starts with or still reaches, or null when it does not cross. */
	WorldPoint jumpAhead()
	{
		return jumpFrom(cachedPath, cachedJump);
	}

	static WorldPoint jumpFrom(List<WorldPoint> path, Pathfinder.Jump jump)
	{
		if (jump == null || path == null)
		{
			return null;
		}
		for (int i = 0; i + 1 < path.size(); i++)
		{
			WorldPoint from = path.get(i);
			WorldPoint to = path.get(i + 1);
			if ((from.equals(jump.a) && to.equals(jump.b)) || (from.equals(jump.b) && to.equals(jump.a)))
			{
				return from;
			}
		}
		return null;
	}
}
