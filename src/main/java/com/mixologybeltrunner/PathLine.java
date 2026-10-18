package com.mixologybeltrunner;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Function;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;

/**
 * The path line shared by the floor and minimap overlays; only the projection and stroke widths differ.
 */
final class PathLine
{
	private static final Color OUTLINE = new Color(0, 0, 0, 140);
	private static final int LINE_ALPHA = 220;

	private PathLine()
	{
	}

	/**
	 * Joins the projected tiles into one line, starting at the player. The tile the player is standing
	 * on is skipped, and a tile that cannot be projected breaks the line rather than bridging the gap.
	 * Returns null when nothing could be projected.
	 */
	static Path2D.Float build(List<WorldPoint> path, WorldPoint here, Point start, Function<WorldPoint, Point> project)
	{
		Path2D.Float line = new Path2D.Float();
		boolean started = false;
		if (start != null)
		{
			line.moveTo(start.getX(), start.getY());
			started = true;
		}
		boolean pendingGap = false;
		for (int i = 0; i < path.size(); i++)
		{
			WorldPoint tile = path.get(i);
			if (i == 0 && here != null && tile.distanceTo(here) <= 1 && path.size() > 1)
			{
				continue;
			}
			Point point = project.apply(tile);
			if (point == null)
			{
				pendingGap = true;
				continue;
			}
			if (!started || pendingGap)
			{
				line.moveTo(point.getX(), point.getY());
				started = true;
			}
			else
			{
				line.lineTo(point.getX(), point.getY());
			}
			pendingGap = false;
		}
		return started ? line : null;
	}

	/** A dark outline under the coloured line, anti-aliased, leaving the graphics state as it found it. */
	static void draw(Graphics2D graphics, Shape line, Stroke outline, Stroke stroke, Color base)
	{
		Stroke oldStroke = graphics.getStroke();
		Object oldHint = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setStroke(outline);
		graphics.setColor(OUTLINE);
		graphics.draw(line);
		graphics.setStroke(stroke);
		graphics.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), LINE_ALPHA));
		graphics.draw(line);
		graphics.setStroke(oldStroke);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			oldHint != null ? oldHint : RenderingHints.VALUE_ANTIALIAS_OFF);
	}
}
