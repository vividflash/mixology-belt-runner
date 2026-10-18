package com.mixologybeltrunner;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The walking path drawn on the minimap.
 */
class PathMinimapOverlay extends Overlay
{
	private static final int MINIMAP_RANGE = 6400;
	/** RuneLite's zoom-aware minimap range works out at 80 pixels; a little less stays inside the frame. */
	private static final float MINIMAP_CLIP_RADIUS = 76f;
	private static final Stroke PATH_OUTLINE = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PATH_LINE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final Client client;
	private final BeltRunnerConfig config;
	private final BeltRunnerPlugin plugin;

	@Inject
	PathMinimapOverlay(Client client, BeltRunnerConfig config, BeltRunnerPlugin plugin)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(Overlay.PRIORITY_LOW);
		this.client = client;
		this.config = config;
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<WorldPoint> path = plugin.currentPath();
		if (!config.pathDisplay().showsMinimap() || !plugin.inLab() || path.size() < 2)
		{
			return null;
		}
		renderPath(graphics, path);
		return null;
	}

	private void renderPath(Graphics2D graphics, List<WorldPoint> path)
	{
		Player player = client.getLocalPlayer();
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}
		Point centre = player == null ? null : toMinimap(player.getWorldLocation(), worldView, player);
		Path2D.Float line = PathLine.build(path, player == null ? null : player.getWorldLocation(), centre,
			tile -> toMinimap(tile, worldView, player));
		if (line == null)
		{
			return;
		}
		Shape oldClip = graphics.getClip();
		if (centre != null)
		{
			// The path is projected well past the minimap's edge, so keep the line inside the circle.
			graphics.clip(new Ellipse2D.Float(centre.getX() - MINIMAP_CLIP_RADIUS, centre.getY() - MINIMAP_CLIP_RADIUS,
				MINIMAP_CLIP_RADIUS * 2, MINIMAP_CLIP_RADIUS * 2));
		}
		PathLine.draw(graphics, line, PATH_OUTLINE, PATH_LINE, config.highlightColour());
		graphics.setClip(oldClip);
	}

	private Point toMinimap(WorldPoint tile, WorldView worldView, Player player)
	{
		if (tile == null)
		{
			return null;
		}
		LocalPoint local = LocalPoint.fromWorld(worldView, tile);
		if (local == null && player != null && player.getWorldLocation() != null)
		{
			LocalPoint here = player.getLocalLocation();
			WorldPoint playerWorld = player.getWorldLocation();
			if (here != null && playerWorld.getPlane() == tile.getPlane())
			{
				int x = here.getX() + (tile.getX() - playerWorld.getX()) * Perspective.LOCAL_TILE_SIZE;
				int y = here.getY() + (tile.getY() - playerWorld.getY()) * Perspective.LOCAL_TILE_SIZE;
				local = new LocalPoint(x, y, worldView);
			}
		}
		if (local == null)
		{
			return null;
		}
		return Perspective.localToMinimap(client, local, MINIMAP_RANGE);
	}
}
