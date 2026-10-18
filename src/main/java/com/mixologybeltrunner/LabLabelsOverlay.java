package com.mixologybeltrunner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Text drawn in the scene: the pull numbers over the levers, the potion being made by the
 * vessel, the countdown over the station being worked and "Deposit" over the belt. Drawn on
 * the top layer, large and outlined. Each label goes to the far side of its object from the
 * player when it first appears and stays there while it is shown, so it never sits over the
 * character and does not drift as they walk.
 */
class LabLabelsOverlay extends Overlay
{
	private static final int HEIGHT = 150;
	private static final float SIZE = 24f;
	private static final float NEXT_SIZE = 18f;
	private static final BasicStroke OUTLINE = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	/**
	 * How far past the object the label sits, in local units (128 per tile).
	 */
	static final int AWAY = 176;
	/**
	 * Beyond this distance from the object (local units) the label stays over the object.
	 */
	static final int NEAR = 128 * 12;

	private final Client client;
	private final BeltRunnerPlugin plugin;
	private final BeltRunnerConfig config;
	/**
	 * Where each shown label sits, fixed when it first appears.
	 */
	private final Map<WorldPoint, int[]> anchors = new HashMap<>();
	private final Set<WorldPoint> shown = new HashSet<>();

	@Inject
	LabLabelsOverlay(Client client, BeltRunnerPlugin plugin, BeltRunnerConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Step step = plugin.currentStep();
		WorldView worldView = client.getTopLevelWorldView();
		if (step == null || !plugin.inLab() || worldView == null)
		{
			anchors.clear();
			return null;
		}
		shown.clear();
		graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont(SIZE));
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		LocalPoint player = playerPoint(worldView);

		for (Map.Entry<PotionComponent, List<Integer>> entry : step.leverNumbers().entrySet())
		{
			String text = LabOverlay.leverLabel(step.leverNumbers(), entry.getKey());
			label(graphics, worldView, player, LabOverlay.leverFor(entry.getKey()).coordinate(), segments(text, entry.getKey().color()));
		}
		if (step.phase() == Phase.MAKE && step.make() != null && !step.leverNumbers().isEmpty())
		{
			label(graphics, worldView, player, LabObject.MIXING_VESSEL.coordinate(), makeLabel(step.make()));
		}

		String count = stationLabel(step);
		if (count != null)
		{
			label(graphics, worldView, player, step.station().object().coordinate(), segments(count, Color.WHITE));
		}
		if (step.belt() == BeltColour.GREEN)
		{
			label(graphics, worldView, player, LabObject.CONVEYOR_BELT.coordinate(), segments("Deposit", config.beltOpenColour()));
		}
		if (step.digweedPickup() && step.digweedSpot() != null)
		{
			label(graphics, worldView, player, step.digweedSpot().coordinate(), segments("Pick up", config.digweedColour()));
		}
		LabObject next = LabOverlay.nextToOutline(step);
		if (next != null)
		{
			graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont(NEXT_SIZE));
			label(graphics, worldView, player, next.coordinate(), segments("Next", Color.WHITE));
			graphics.setFont(FontManager.getRunescapeBoldFont().deriveFont(SIZE));
		}
		if (step.digweedTarget() != null)
		{
			overhead(graphics, digweedLabel(step.digweedTarget(), config.digweedColour()));
		}
		anchors.keySet().retainAll(shown);
		return null;
	}

	/**
	 * "Make 7" counting down while potions are still to go into the station, "Last one" once
	 * the potion inside is the last; null when the step has no station count.
	 */
	static String stationLabel(Step step)
	{
		if (step.phase() != Phase.PROCESS || step.station() == null || step.stationCount() < 0)
		{
			return null;
		}
		return step.stationCount() > 0 ? "Make " + step.stationCount() : "Last one";
	}

	/**
	 * "Digweed → " in the digweed colour followed by the potion code in its paste colours.
	 */
	static List<Segment> digweedLabel(PotionType type, Color digweed)
	{
		List<Segment> out = new ArrayList<>();
		out.add(new Segment("Digweed → ", digweed));
		for (PotionComponent component : type.components())
		{
			out.add(new Segment(String.valueOf(component.letter()), component.color()));
		}
		return out;
	}

	/**
	 * "Make " in white followed by the potion code, each letter in its paste colour.
	 */
	static List<Segment> makeLabel(PotionType type)
	{
		List<Segment> out = new ArrayList<>();
		out.add(new Segment("Make ", Color.WHITE));
		for (PotionComponent component : type.components())
		{
			out.add(new Segment(String.valueOf(component.letter()), component.color()));
		}
		return out;
	}

	private static List<Segment> segments(String text, Color colour)
	{
		List<Segment> out = new ArrayList<>();
		if (text != null)
		{
			out.add(new Segment(text, colour));
		}
		return out;
	}

	/**
	 * A run of label text in one colour.
	 */
	static final class Segment
	{
		final String text;
		final Color colour;

		Segment(String text, Color colour)
		{
			this.text = text;
			this.colour = colour;
		}
	}

	/**
	 * The offset (local units) that moves a label from the object to its far side from the
	 * player; zero when the player stands on it or is far away.
	 */
	static int[] awayOffset(int objectX, int objectY, int playerX, int playerY)
	{
		int dx = objectX - playerX;
		int dy = objectY - playerY;
		double length = Math.hypot(dx, dy);
		if (length < 1 || length > NEAR)
		{
			return new int[]{0, 0};
		}
		return new int[]{(int) Math.round(dx / length * AWAY), (int) Math.round(dy / length * AWAY)};
	}

	private LocalPoint playerPoint(WorldView worldView)
	{
		Player player = client.getLocalPlayer();
		return player == null ? null : player.getLocalLocation();
	}

	/**
	 * A label above the player's head, for actions done in the inventory.
	 */
	private void overhead(Graphics2D graphics, List<Segment> parts)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		StringBuilder whole = new StringBuilder();
		for (Segment part : parts)
		{
			whole.append(part.text);
		}
		Point location = player.getCanvasTextLocation(graphics, whole.toString(), player.getLogicalHeight() + 60);
		if (location != null)
		{
			drawSegments(graphics, parts, location);
		}
	}

	private void label(Graphics2D graphics, WorldView worldView, LocalPoint player, WorldPoint tile, List<Segment> parts)
	{
		if (parts.isEmpty())
		{
			return;
		}
		LocalPoint object = LocalPoint.fromWorld(worldView, tile);
		if (object == null)
		{
			return;
		}
		shown.add(tile);
		int[] offset = anchors.computeIfAbsent(tile, t -> player == null
			? new int[]{0, 0}
			: awayOffset(object.getX(), object.getY(), player.getX(), player.getY()));
		LocalPoint anchor = new LocalPoint(object.getX() + offset[0], object.getY() + offset[1], worldView);
		StringBuilder whole = new StringBuilder();
		for (Segment part : parts)
		{
			whole.append(part.text);
		}
		Point location = Perspective.getCanvasTextLocation(client, graphics, anchor, whole.toString(), HEIGHT);
		if (location == null)
		{
			return;
		}
		drawSegments(graphics, parts, location);
	}

	private static void drawSegments(Graphics2D graphics, List<Segment> parts, Point location)
	{
		float x = location.getX();
		for (Segment part : parts)
		{
			TextLayout layout = new TextLayout(part.text, graphics.getFont(), graphics.getFontRenderContext());
			Shape shape = layout.getOutline(AffineTransform.getTranslateInstance(x, location.getY()));
			graphics.setStroke(OUTLINE);
			graphics.setColor(Color.BLACK);
			graphics.draw(shape);
			graphics.setColor(part.colour);
			graphics.fill(shape);
			x += layout.getAdvance();
		}
	}
}
