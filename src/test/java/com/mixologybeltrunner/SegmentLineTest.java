package com.mixologybeltrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.ToIntFunction;
import net.runelite.client.ui.FontManager;
import org.junit.Test;

public class SegmentLineTest
{
	/**
	 * Six pixels a character.
	 */
	private static final ToIntFunction<String> MONO = s -> s.length() * 6;
	/**
	 * The guide window is 200 wide with a 4 pixel border each side.
	 */
	private static final int LINE_WIDTH = 192;

	@Test
	public void shortTextStaysOnOneLine()
	{
		assertEquals(List.of("Click the belt"), SegmentLine.wrap("Click the belt", LINE_WIDTH, MONO));
	}

	@Test
	public void longTextBreaksBetweenWords()
	{
		assertEquals(List.of("Deposit or destroy your potions", "to start"),
			SegmentLine.wrap("Deposit or destroy your potions to start", LINE_WIDTH, MONO));
	}

	@Test
	public void overlongWordGetsItsOwnLine()
	{
		assertEquals(List.of("a", "abcdefgh", "b"), SegmentLine.wrap("a abcdefgh b", 30, MONO));
	}

	@Test
	public void emptyTextIsOneEmptyLine()
	{
		assertEquals(List.of(""), SegmentLine.wrap("", LINE_WIDTH, MONO));
	}

	@Test
	public void windowTextFitsInTheRunescapeFonts()
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		FontMetrics bold = g.getFontMetrics(FontManager.getRunescapeBoldFont());
		FontMetrics small = g.getFontMetrics(FontManager.getRunescapeSmallFont());
		assertFits(bold, "Deposit or destroy your potions to start", 2);
		assertFits(bold, "Click the belt (skipping LLL, AAA)", 2);
		assertFits(bold, "Belt Runner needs Herblore 81", 2);
		assertFits(bold, "Make MML → Retort", 1);
		assertFits(small, "Potions were already in the inventory when tracking started.", 2);
		assertFits(small, "Restocking · round 1 of 3 · stock 0 / 21 · digweed ready", 2);
		g.dispose();
	}

	private static void assertFits(FontMetrics metrics, String text, int expectedLines)
	{
		List<String> lines = SegmentLine.wrap(text, LINE_WIDTH, metrics::stringWidth);
		assertEquals(text, expectedLines, lines.size());
		for (String line : lines)
		{
			assertTrue(line + " is " + metrics.stringWidth(line) + " px", metrics.stringWidth(line) <= LINE_WIDTH);
		}
	}
}
