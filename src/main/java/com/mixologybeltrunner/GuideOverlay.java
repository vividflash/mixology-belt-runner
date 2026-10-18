package com.mixologybeltrunner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * The guide window: the current instruction and the stock, in the style picked in the config.
 */
public class GuideOverlay extends OverlayPanel
{
	private static final Color TITLE = new Color(0xff981f);
	private static final Color WARNING = new Color(0xffd54f);
	private static final Color FINISHED = new Color(0x4caf50);
	private static final Color UNPROCESSED = new Color(0xffd54f);
	private static final Color MISSING = new Color(0x9e9e9e);
	private static final Color DIGWEED = new Color(0xb388ff);
	private static final Color STATUS = new Color(0xbdbdbd);
	private static final int WIDTH = 200;
	private static final int COLUMN_WIDTH = 36;

	private final BeltRunnerPlugin plugin;
	private final BeltRunnerConfig config;

	@Inject
	GuideOverlay(BeltRunnerPlugin plugin, BeltRunnerConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Step step = plugin.currentStep();
		if (step == null || !plugin.inLab())
		{
			return null;
		}
		panelComponent.setPreferredSize(new Dimension(WIDTH, 0));
		panelComponent.getChildren().add(TitleComponent.builder().text("Belt Runner").color(TITLE).build());
		if (!step.warning().isEmpty())
		{
			panelComponent.getChildren().add(SegmentLine.wrapped(FontManager.getRunescapeSmallFont(), step.warning(), WARNING));
		}
		if (step.phase() == Phase.DORMANT || step.phase() == Phase.NEEDS_CLEAR)
		{
			addHeadline(step);
			addStatus(step);
		}
		else
		{
			switch (config.windowStyle())
			{
				case TODO_LIST:
					addHeadline(step);
					addTodo(step.rows());
					break;
				case FULL_GRID:
					addHeadline(step);
					addGrid(step.rows());
					break;
				default:
					addStatus(step);
					addHeadline(step);
					addStockBar(step);
					break;
			}
		}
		return super.render(graphics);
	}

	private void addStatus(Step step)
	{
		if (!step.status().isEmpty())
		{
			panelComponent.getChildren().add(SegmentLine.wrapped(FontManager.getRunescapeSmallFont(), step.status(), STATUS));
		}
	}

	private void addHeadline(Step step)
	{
		panelComponent.getChildren().add(SegmentLine.wrapped(FontManager.getRunescapeBoldFont(), step.headline(), Color.WHITE));
	}

	private void addStockBar(Step step)
	{
		if (step.stockTotal() == 0)
		{
			return;
		}
		ProgressBarComponent bar = new ProgressBarComponent();
		bar.setMinimum(0);
		bar.setMaximum(step.stockTotal());
		bar.setValue(step.stockHave());
		bar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.FULL);
		bar.setForegroundColor(FINISHED);
		bar.setBackgroundColor(new Color(0x303030));
		panelComponent.getChildren().add(bar);
	}

	private void addTodo(List<StockSummary.Row> rows)
	{
		List<TodoLines.Make> make = TodoLines.toMake(rows);
		if (!make.isEmpty())
		{
			addSubtitle("To make");
			for (TodoLines.Make m : make)
			{
				SegmentLine line = new SegmentLine(FontManager.getRunescapeSmallFont())
					.leftCode(m.type())
					.left(" ×" + m.count(), Color.WHITE)
					.right(stationNames(m.stations()), STATUS);
				panelComponent.getChildren().add(line);
			}
		}
		Map<Station, List<PotionType>> process = TodoLines.toProcess(rows);
		if (!process.isEmpty())
		{
			addSubtitle("To process");
			for (Map.Entry<Station, List<PotionType>> e : process.entrySet())
			{
				StringBuilder codes = new StringBuilder();
				for (PotionType type : e.getValue())
				{
					if (codes.length() > 0)
					{
						codes.append(' ');
					}
					codes.append(type.code());
				}
				SegmentLine line = new SegmentLine(FontManager.getRunescapeSmallFont())
					.left(e.getKey().displayName() + " ×" + e.getValue().size(), Color.WHITE)
					.right(codes.toString(), UNPROCESSED);
				panelComponent.getChildren().add(line);
			}
		}
	}

	private void addSubtitle(String text)
	{
		panelComponent.getChildren().add(new SegmentLine(FontManager.getRunescapeSmallFont()).left(text, TITLE));
	}

	private static String stationNames(List<Station> stations)
	{
		StringBuilder sb = new StringBuilder();
		for (Station s : stations)
		{
			if (sb.length() > 0)
			{
				sb.append(' ');
			}
			sb.append(s.shortName());
		}
		return sb.toString();
	}

	private void addGrid(List<StockSummary.Row> rows)
	{
		SegmentLine header = new SegmentLine(FontManager.getRunescapeSmallFont());
		for (Station station : Station.PROCESS_ORDER)
		{
			header.cell(station.shortName(), STATUS, columnCentre(station));
		}
		panelComponent.getChildren().add(header);
		for (StockSummary.Row row : rows)
		{
			SegmentLine line = new SegmentLine(FontManager.getRunescapeSmallFont()).leftCode(row.type());
			for (Station station : Station.PROCESS_ORDER)
			{
				boolean digweed = row.digweed(station);
				line.cell(glyph(row.state(station), digweed), digweed ? DIGWEED : stateColour(row.state(station)), columnCentre(station));
			}
			panelComponent.getChildren().add(line);
		}
	}

	private static int columnCentre(Station station)
	{
		return COLUMN_WIDTH / 2 + (Station.PROCESS_ORDER.size() - 1 - station.ordinal()) * COLUMN_WIDTH;
	}

	private static String glyph(CellState state, boolean digweed)
	{
		if (digweed)
		{
			return "★";
		}
		switch (state)
		{
			case FINISHED:
				return "✓";
			case UNPROCESSED:
				return "○";
			default:
				return "·";
		}
	}

	private static Color stateColour(CellState state)
	{
		switch (state)
		{
			case FINISHED:
				return FINISHED;
			case UNPROCESSED:
				return UNPROCESSED;
			default:
				return MISSING;
		}
	}
}
