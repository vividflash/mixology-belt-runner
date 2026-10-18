package com.mixologybeltrunner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/**
 * One line of the guide window made of coloured pieces of text: some pinned left, some pinned right.
 * A line made with {@link #wrapped} is a single piece of text that breaks between words to fit.
 */
final class SegmentLine implements LayoutableRenderableEntity
{
	private static final int LINE_PADDING = 2;

	private final Font font;
	private final List<String> leftText = new ArrayList<>();
	private final List<Color> leftColour = new ArrayList<>();
	private final List<String> rightText = new ArrayList<>();
	private final List<Color> rightColour = new ArrayList<>();
	private final List<String> cellText = new ArrayList<>();
	private final List<Color> cellColour = new ArrayList<>();
	private final List<Integer> cellCentre = new ArrayList<>();
	private boolean wrap;
	private final Rectangle bounds = new Rectangle();
	private Point location = new Point();
	private Dimension size = new Dimension(0, 0);

	SegmentLine(Font font)
	{
		this.font = font;
	}

	/**
	 * Text in one colour, broken onto as many lines as the window width needs.
	 */
	static SegmentLine wrapped(Font font, String text, Color colour)
	{
		SegmentLine line = new SegmentLine(font).left(text, colour);
		line.wrap = true;
		return line;
	}

	/**
	 * Breaks text between words so each line fits {@code maxWidth}. A word wider than
	 * that gets a line to itself rather than being cut.
	 */
	static List<String> wrap(String text, int maxWidth, ToIntFunction<String> widthOf)
	{
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			if (word.isEmpty())
			{
				continue;
			}
			if (line.length() > 0 && widthOf.applyAsInt(line + " " + word) > maxWidth)
			{
				lines.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0)
			{
				line.append(' ');
			}
			line.append(word);
		}
		if (line.length() > 0 || lines.isEmpty())
		{
			lines.add(line.toString());
		}
		return lines;
	}

	SegmentLine left(String text, Color colour)
	{
		leftText.add(text);
		leftColour.add(colour);
		return this;
	}

	/**
	 * Adds a potion code, one letter per component in that component's colour.
	 */
	SegmentLine leftCode(PotionType type)
	{
		for (PotionComponent component : type.components())
		{
			left(String.valueOf(component.letter()), component.color());
		}
		return this;
	}

	SegmentLine right(String text, Color colour)
	{
		rightText.add(text);
		rightColour.add(colour);
		return this;
	}

	/**
	 * Adds text centred a fixed distance in from the right edge, so rows line up as columns.
	 */
	SegmentLine cell(String text, Color colour, int centreFromRight)
	{
		cellText.add(text);
		cellColour.add(colour);
		cellCentre.add(centreFromRight);
		return this;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		int height = metrics.getHeight() + LINE_PADDING;
		int width = size.width;
		int baseline = location.y + metrics.getAscent() + LINE_PADDING / 2;
		if (wrap)
		{
			List<String> lines = wrap(leftText.get(0), width, metrics::stringWidth);
			for (int i = 0; i < lines.size(); i++)
			{
				draw(graphics, lines.get(i), leftColour.get(0), location.x, baseline + i * metrics.getHeight());
			}
			return finish(width, height + (lines.size() - 1) * metrics.getHeight());
		}
		int x = location.x;
		for (int i = 0; i < leftText.size(); i++)
		{
			x += draw(graphics, leftText.get(i), leftColour.get(i), x, baseline);
		}
		int rightWidth = 0;
		for (String text : rightText)
		{
			rightWidth += metrics.stringWidth(text);
		}
		x = location.x + width - rightWidth;
		for (int i = 0; i < rightText.size(); i++)
		{
			x += draw(graphics, rightText.get(i), rightColour.get(i), x, baseline);
		}
		for (int i = 0; i < cellText.size(); i++)
		{
			int cellX = location.x + width - cellCentre.get(i) - metrics.stringWidth(cellText.get(i)) / 2;
			draw(graphics, cellText.get(i), cellColour.get(i), cellX, baseline);
		}
		return finish(width, height);
	}

	private Dimension finish(int width, int height)
	{
		Dimension used = new Dimension(width, height);
		bounds.setLocation(location);
		bounds.setSize(used);
		return used;
	}

	private static int draw(Graphics2D graphics, String text, Color colour, int x, int y)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(colour);
		graphics.drawString(text, x, y);
		return graphics.getFontMetrics().stringWidth(text);
	}

	@Override
	public Rectangle getBounds()
	{
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point position)
	{
		location = position;
	}

	@Override
	public void setPreferredSize(Dimension dimension)
	{
		size = dimension;
	}
}
