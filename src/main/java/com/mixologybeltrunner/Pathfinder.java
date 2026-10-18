package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiPredicate;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * Scene-local A* that can treat extra tiles as blocked (except the goal).
 */
final class Pathfinder
{
	private static final int MAX_NODES = 6000;
	private static final int WALKABLE_SEARCH = 6;

	private Pathfinder()
	{
	}

	/** A one-click crossing between two tiles, such as an Agility shortcut, and its cost in ticks. */
	static final class Jump
	{
		final WorldPoint a;
		final WorldPoint b;
		final int ticks;

		Jump(WorldPoint a, WorldPoint b, int ticks)
		{
			this.a = a;
			this.b = b;
			this.ticks = ticks;
		}
	}

	/** Walking costs 2 per straight tile and 3 per diagonal; running covers 2 tiles a tick, so a tick is 4. */
	static final int COST_PER_TICK = 4;

	static List<WorldPoint> find(WorldView worldView, WorldPoint start, WorldPoint end, Set<WorldPoint> blocked)
	{
		return find(worldView, start, end, blocked, null);
	}

	static List<WorldPoint> find(WorldView worldView, WorldPoint start, WorldPoint end, Set<WorldPoint> blocked,
		Jump jump)
	{
		if (worldView == null || start == null || end == null || start.getPlane() != end.getPlane())
		{
			return Collections.emptyList();
		}

		CollisionData[] maps = worldView.getCollisionMaps();
		if (maps == null)
		{
			return Collections.emptyList();
		}
		int plane = worldView.getPlane();
		if (plane < 0 || plane >= maps.length || maps[plane] == null)
		{
			return Collections.emptyList();
		}

		int[][] flags = maps[plane].getFlags();
		if (flags == null || flags.length == 0)
		{
			return Collections.emptyList();
		}

		LocalPoint startLocal = LocalPoint.fromWorld(worldView, start);
		if (startLocal == null)
		{
			return Collections.emptyList();
		}

		LocalPoint endLocal = LocalPoint.fromWorld(worldView, end);
		if (endLocal == null)
		{
			return Collections.emptyList();
		}

		int size = flags.length;
		int sx = startLocal.getSceneX();
		int sy = startLocal.getSceneY();
		int[] goal = nearestWalkable(worldView, flags, endLocal.getSceneX(), endLocal.getSceneY(),
			sx, sy, size, blocked, plane);
		if (!inBounds(sx, sy, size) || goal == null)
		{
			return Collections.emptyList();
		}
		int ex = goal[0];
		int ey = goal[1];
		if (sx == ex && sy == ey)
		{
			return Collections.emptyList();
		}

		int[] link = null;
		if (jump != null)
		{
			LocalPoint a = LocalPoint.fromWorld(worldView, jump.a);
			LocalPoint b = LocalPoint.fromWorld(worldView, jump.b);
			if (a != null && b != null)
			{
				link = new int[]{a.getSceneX(), a.getSceneY(), b.getSceneX(), b.getSceneY(), jump.ticks * COST_PER_TICK};
			}
		}
		List<int[]> nodes = search(flags, sx, sy, ex, ey, (x, y) -> isBlocked(worldView, x, y, plane, blocked), link);
		List<WorldPoint> path = new ArrayList<>();
		for (int[] n : nodes)
		{
			path.add(WorldPoint.fromScene(worldView, n[0], n[1], plane));
		}
		return path;
	}

	/**
	 * A* over a scene's collision flags from (sx, sy) to (ex, ey), as scene tiles, start first; empty when there
	 * is no way through. {@code link} is an optional jump {ax, ay, bx, by, cost} usable both ways.
	 */
	static List<int[]> search(int[][] flags, int sx, int sy, int ex, int ey, BiPredicate<Integer, Integer> blocked,
		int[] link)
	{
		int size = flags.length;
		boolean[][] seen = new boolean[size][size];
		PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
		open.add(new Node(sx, sy, 0, octile(sx, sy, ex, ey), null));
		seen[sx][sy] = true;

		int visited = 0;
		Node found = null;
		while (!open.isEmpty() && visited < MAX_NODES)
		{
			Node cur = open.poll();
			visited++;
			if (cur.x == ex && cur.y == ey)
			{
				found = cur;
				break;
			}

			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					if (dx == 0 && dy == 0)
					{
						continue;
					}
					tryAdd(open, seen, flags, size, cur, cur.x + dx, cur.y + dy, dx, dy, ex, ey, blocked);
				}
			}
			if (link != null)
			{
				if (cur.x == link[0] && cur.y == link[1])
				{
					jumpTo(open, seen, cur, link[2], link[3], link[4], ex, ey);
				}
				else if (cur.x == link[2] && cur.y == link[3])
				{
					jumpTo(open, seen, cur, link[0], link[1], link[4], ex, ey);
				}
			}
		}

		if (found == null)
		{
			return Collections.emptyList();
		}

		List<int[]> path = new ArrayList<>();
		for (Node n = found; n != null; n = n.parent)
		{
			path.add(new int[]{n.x, n.y});
		}
		Collections.reverse(path);
		return path;
	}

	private static void jumpTo(PriorityQueue<Node> open, boolean[][] seen, Node cur, int nx, int ny, int cost,
		int ex, int ey)
	{
		if (!inBounds(nx, ny, seen.length) || seen[nx][ny])
		{
			return;
		}
		seen[nx][ny] = true;
		int g = cur.g + cost;
		open.add(new Node(nx, ny, g, g + octile(nx, ny, ex, ey), cur));
	}

	static WorldPoint clampToScene(WorldView worldView, WorldPoint start, WorldPoint end)
	{
		if (worldView == null || start == null || end == null)
		{
			return null;
		}
		if (LocalPoint.fromWorld(worldView, end) != null)
		{
			return end;
		}

		int dx = end.getX() - start.getX();
		int dy = end.getY() - start.getY();
		int steps = Math.max(Math.abs(dx), Math.abs(dy));
		if (steps <= 0)
		{
			return null;
		}

		WorldPoint last = null;
		for (int i = 1; i <= steps; i++)
		{
			WorldPoint point = new WorldPoint(
				start.getX() + dx * i / steps,
				start.getY() + dy * i / steps,
				start.getPlane());
			if (LocalPoint.fromWorld(worldView, point) == null)
			{
				break;
			}
			last = point;
		}
		return last;
	}

	static List<WorldPoint> simplify(List<WorldPoint> path)
	{
		if (path == null || path.size() < 3)
		{
			return path == null ? Collections.emptyList() : path;
		}

		List<WorldPoint> out = new ArrayList<>();
		out.add(path.get(0));
		for (int i = 1; i < path.size() - 1; i++)
		{
			WorldPoint prev = out.get(out.size() - 1);
			WorldPoint cur = path.get(i);
			WorldPoint next = path.get(i + 1);
			int dx1 = cur.getX() - prev.getX();
			int dy1 = cur.getY() - prev.getY();
			int dx2 = next.getX() - cur.getX();
			int dy2 = next.getY() - cur.getY();
			if (dx1 * dy2 != dy1 * dx2)
			{
				out.add(cur);
			}
		}
		out.add(path.get(path.size() - 1));
		return out;
	}

	/**
	 * Pulls the walk taut: drops a waypoint when the next one is still reachable in a straight
	 * collision-checked line. Stops chords from cutting across walls.
	 */
	static List<WorldPoint> smooth(WorldView worldView, List<WorldPoint> path, Set<WorldPoint> blocked)
	{
		if (path == null || path.size() < 3 || worldView == null)
		{
			return path == null ? Collections.emptyList() : path;
		}

		CollisionData[] maps = worldView.getCollisionMaps();
		if (maps == null)
		{
			return path;
		}
		int plane = worldView.getPlane();
		if (plane < 0 || plane >= maps.length || maps[plane] == null)
		{
			return path;
		}
		int[][] flags = maps[plane].getFlags();
		if (flags == null || flags.length == 0)
		{
			return path;
		}

		List<WorldPoint> out = new ArrayList<>();
		out.add(path.get(0));
		int anchor = 0;
		for (int i = 1; i < path.size(); i++)
		{
			boolean last = i == path.size() - 1;
			if (last || !lineClear(worldView, flags, path.get(anchor), path.get(i + 1), blocked, plane))
			{
				WorldPoint keep = path.get(i);
				if (!keep.equals(out.get(out.size() - 1)))
				{
					out.add(keep);
				}
				anchor = i;
			}
		}
		return out;
	}

	private static boolean lineClear(WorldView worldView, int[][] flags, WorldPoint from, WorldPoint to,
		Set<WorldPoint> blocked, int plane)
	{
		if (from == null || to == null)
		{
			return false;
		}
		LocalPoint a = LocalPoint.fromWorld(worldView, from);
		LocalPoint b = LocalPoint.fromWorld(worldView, to);
		if (a == null || b == null)
		{
			return false;
		}
		int x0 = a.getSceneX();
		int y0 = a.getSceneY();
		int x1 = b.getSceneX();
		int y1 = b.getSceneY();
		int size = flags.length;
		int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		if (steps <= 0)
		{
			return true;
		}
		int px = x0;
		int py = y0;
		for (int i = 1; i <= steps; i++)
		{
			int nx = x0 + (x1 - x0) * i / steps;
			int ny = y0 + (y1 - y0) * i / steps;
			int dx = nx - px;
			int dy = ny - py;
			if (dx == 0 && dy == 0)
			{
				continue;
			}
			if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || !inBounds(nx, ny, size)
				|| !canMove(flags, px, py, dx, dy, size))
			{
				return false;
			}
			if (isBlocked(worldView, nx, ny, plane, blocked) && !(nx == x1 && ny == y1))
			{
				return false;
			}
			px = nx;
			py = ny;
		}
		return px == x1 && py == y1;
	}

	private static void tryAdd(
		PriorityQueue<Node> open,
		boolean[][] seen,
		int[][] flags,
		int size,
		Node cur,
		int nx,
		int ny,
		int dx,
		int dy,
		int ex,
		int ey,
		BiPredicate<Integer, Integer> blocked)
	{
		if (!inBounds(nx, ny, size) || seen[nx][ny] || (nx == cur.x && ny == cur.y))
		{
			return;
		}
		boolean atGoal = nx == ex && ny == ey;
		if (!canMove(flags, cur.x, cur.y, dx, dy, size, atGoal))
		{
			return;
		}
		if (blocked.test(nx, ny) && !(nx == ex && ny == ey))
		{
			return;
		}
		seen[nx][ny] = true;
		int step = (dx != 0 && dy != 0) ? 3 : 2;
		int g = cur.g + step;
		open.add(new Node(nx, ny, g, g + octile(nx, ny, ex, ey), cur));
	}

	private static boolean isBlocked(WorldView worldView, int sx, int sy, int plane, Set<WorldPoint> blocked)
	{
		if (blocked == null || blocked.isEmpty())
		{
			return false;
		}
		WorldPoint point = WorldPoint.fromScene(worldView, sx, sy, plane);
		return point != null && blocked.contains(point);
	}

	private static int[] nearestWalkable(WorldView worldView, int[][] flags, int x, int y,
		int fromX, int fromY, int size, Set<WorldPoint> blocked, int plane)
	{
		if (inBounds(x, y, size) && isStandable(worldView, flags, x, y, blocked, plane))
		{
			return new int[]{x, y};
		}
		int bestNx = Integer.MIN_VALUE;
		int bestNy = 0;
		int best = Integer.MAX_VALUE;
		for (int r = 1; r <= WALKABLE_SEARCH; r++)
		{
			for (int dx = -r; dx <= r; dx++)
			{
				for (int dy = -r; dy <= r; dy++)
				{
					if (Math.abs(dx) != r && Math.abs(dy) != r)
					{
						continue;
					}
					int nx = x + dx;
					int ny = y + dy;
					if (!inBounds(nx, ny, size)
						|| !isStandable(worldView, flags, nx, ny, blocked, plane))
					{
						continue;
					}
					int score = octile(nx, ny, x, y) * 8 + octile(nx, ny, fromX, fromY);
					if (score < best)
					{
						best = score;
						bestNx = nx;
						bestNy = ny;
					}
				}
			}
		}
		if (bestNx != Integer.MIN_VALUE)
		{
			return new int[]{bestNx, bestNy};
		}
		return inBounds(x, y, size) ? new int[]{x, y} : null;
	}

	private static boolean isStandable(WorldView worldView, int[][] flags, int x, int y,
		Set<WorldPoint> blocked, int plane)
	{
		return (flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
			&& !isBlocked(worldView, x, y, plane, blocked);
	}

	private static int octile(int x, int y, int ex, int ey)
	{
		int dx = Math.abs(x - ex);
		int dy = Math.abs(y - ey);
		return 2 * Math.max(dx, dy) + Math.min(dx, dy);
	}

	private static boolean inBounds(int x, int y, int size)
	{
		return x >= 0 && y >= 0 && x < size && y < size;
	}

	private static boolean canMove(int[][] flags, int x, int y, int dx, int dy, int size)
	{
		return canMove(flags, x, y, dx, dy, size, false);
	}

	private static boolean canMove(int[][] flags, int x, int y, int dx, int dy, int size,
		boolean allowBlockedDest)
	{
		int nx = x + dx;
		int ny = y + dy;
		if (!inBounds(nx, ny, size))
		{
			return false;
		}

		if (dx != 0 && dy != 0)
		{
			return canMove(flags, x, y, dx, 0, size, false) && canMove(flags, x, y, 0, dy, size, false)
				&& (allowBlockedDest || (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0);
		}

		int destFlags = flags[nx][ny];
		if (!allowBlockedDest && (destFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
		{
			return false;
		}

		int cur = flags[x][y];
		if (allowBlockedDest && (destFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0)
		{
			return outgoingClear(cur, dx, dy);
		}
		if (dx == 1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
				&& (destFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
		}
		if (dx == -1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
				&& (destFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
		}
		if (dy == 1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
				&& (destFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
		}
		return (cur & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
			&& (destFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
	}

	private static boolean outgoingClear(int cur, int dx, int dy)
	{
		if (dx == 1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
		}
		if (dx == -1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
		}
		if (dy == 1)
		{
			return (cur & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
		}
		return (cur & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
	}

	private static final class Node
	{
		final int x;
		final int y;
		final int g;
		final int f;
		final Node parent;

		Node(int x, int y, int g, int f, Node parent)
		{
			this.x = x;
			this.y = y;
			this.g = g;
			this.f = f;
			this.parent = parent;
		}
	}
}
