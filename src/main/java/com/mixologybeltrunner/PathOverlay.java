package com.mixologybeltrunner;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
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
 * The walking path drawn on the floor.
 */
class PathOverlay extends Overlay
{
	private static final Stroke PATH_OUTLINE = new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PATH_LINE = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final Client client;
	private final BeltRunnerPlugin plugin;
	private final BeltRunnerConfig config;

	@Inject
	PathOverlay(Client client, BeltRunnerPlugin plugin, BeltRunnerConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<WorldPoint> path = plugin.currentPath();
		WorldView worldView = client.getTopLevelWorldView();
		if (!config.pathDisplay().showsFloor() || !plugin.inLab() || path.size() < 2 || worldView == null)
		{
			return null;
		}
		Player player = client.getLocalPlayer();
		int plane = worldView.getPlane();
		LocalPoint playerLocal = player == null ? null : player.getLocalLocation();
		Point start = playerLocal == null ? null : Perspective.localToCanvas(client, playerLocal, plane);
		Path2D.Float line = PathLine.build(path, player == null ? null : player.getWorldLocation(), start, tile ->
		{
			LocalPoint local = LocalPoint.fromWorld(worldView, tile);
			return local == null ? null : Perspective.localToCanvas(client, local, plane);
		});
		if (line != null)
		{
			PathLine.draw(graphics, line, PATH_OUTLINE, PATH_LINE, config.highlightColour());
		}
		return null;
	}
}
