package com.mixologybeltrunner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Outlines the lab objects the current step points at.
 */
public class LabOverlay extends Overlay
{
	private static final int OUTLINE_WIDTH = 4;
	private static final int NEXT_WIDTH = 1;
	private static final Color NEXT_COLOUR = new Color(255, 255, 255, 170);
	private static final int FEATHER = 4;
	private static final int SPOT_ALPHA = 80;

	private final Client client;
	private final BeltRunnerPlugin plugin;
	private final BeltRunnerConfig config;
	private final ModelOutlineRenderer outliner;

	@Inject
	LabOverlay(Client client, BeltRunnerPlugin plugin, BeltRunnerConfig config, ModelOutlineRenderer outliner)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outliner = outliner;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Step step = plugin.currentStep();
		if (step == null || !plugin.inLab())
		{
			return null;
		}
		LabObject station = stationToOutline(step);
		if (station != null)
		{
			outline(station, stationColour(step.station(), plugin.quickActionStation(),
				config.highlightColour(), config.quickActionColour()));
		}
		LabObject next = nextToOutline(step);
		if (next != null)
		{
			outline(next, NEXT_COLOUR, NEXT_WIDTH);
		}
		// Only the levers this potion still needs; their numbers are drawn by LabLabelsOverlay.
		for (PotionComponent component : step.leverNumbers().keySet())
		{
			outline(leverFor(component), component.color());
		}
		if (step.highlightVessel())
		{
			outline(LabObject.MIXING_VESSEL, config.highlightColour());
		}
		if (step.belt() == BeltColour.GREEN)
		{
			outline(LabObject.CONVEYOR_BELT, config.beltOpenColour());
		}
		if (step.digweedSpot() != null)
		{
			outline(step.digweedSpot(), config.digweedColour());
			drawSpot(graphics, step.digweedSpot().coordinate(), config.digweedColour());
		}
		return null;
	}

	/**
	 * The station to outline: only while processing. Make steps carry a station too, but
	 * there the player is pointed at the levers instead.
	 */
	static LabObject stationToOutline(Step step)
	{
		return step.phase() == Phase.PROCESS && step.station() != null ? step.station().object() : null;
	}

	/**
	 * Where the player goes next, outlined thinly: only while processing, never while mixing.
	 */
	static LabObject nextToOutline(Step step)
	{
		return step.phase() == Phase.PROCESS ? step.next() : null;
	}

	/**
	 * The lever's numbers joined by spaces, or null when it has none.
	 */
	static String leverLabel(Map<PotionComponent, List<Integer>> numbers, PotionComponent component)
	{
		List<Integer> list = numbers.get(component);
		if (list == null || list.isEmpty())
		{
			return null;
		}
		StringBuilder text = new StringBuilder();
		for (int number : list)
		{
			if (text.length() > 0)
			{
				text.append(' ');
			}
			text.append(number);
		}
		return text.toString();
	}

	/**
	 * The bonus colour while the station is the one to click for the bonus, else the normal one.
	 */
	static Color stationColour(Station station, Station bonusStation, Color normal, Color bonus)
	{
		return station != null && station == bonusStation ? bonus : normal;
	}

	static LabObject leverFor(PotionComponent component)
	{
		switch (component)
		{
			case MOX:
				return LabObject.MOX_LEVER;
			case AGA:
				return LabObject.AGA_LEVER;
			default:
				return LabObject.LYE_LEVER;
		}
	}

	private void outline(LabObject object, Color colour)
	{
		outline(object, colour, OUTLINE_WIDTH);
	}

	private void outline(LabObject object, Color colour, int width)
	{
		TileObject found = LabObjects.find(client, object);
		if (found != null)
		{
			outliner.drawOutline(found, width, colour, FEATHER);
		}
	}

	private void drawSpot(Graphics2D graphics, WorldPoint tile, Color colour)
	{
		Polygon polygon = tilePolygon(tile);
		if (polygon != null)
		{
			OverlayUtil.renderPolygon(graphics, polygon, colour, withAlpha(colour, SPOT_ALPHA),
				new java.awt.BasicStroke(2));
		}
	}

	private Polygon tilePolygon(WorldPoint tile)
	{
		WorldView worldView = client.getTopLevelWorldView();
		LocalPoint point = worldView == null ? null : LocalPoint.fromWorld(worldView, tile);
		return point == null ? null : Perspective.getCanvasTilePoly(client, point);
	}

	private static Color withAlpha(Color colour, int alpha)
	{
		return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
	}
}
