package com.mixologybeltrunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One instruction for the player, built by the guide each tick and read by the overlays.
 */
public final class Step
{
	private final Phase phase;
	private final String headline;
	private final String status;
	private final String warning;
	private final PotionType make;
	private final Station station;
	private final Map<PotionComponent, List<Integer>> leverNumbers;
	private final boolean highlightVessel;
	private final int nextPotionSlot;
	private final int stationCount;
	private final int digweedSlot;
	private final PotionType digweedTarget;
	private final LabObject digweedSpot;
	private final boolean digweedPickup;
	private final LabObject next;
	private final BeltColour belt;
	private final BeltVerdict verdict;
	private final List<StockSummary.Row> rows;
	private final int stockHave;
	private final int stockTotal;

	private Step(Builder b)
	{
		phase = b.phase;
		headline = b.headline;
		status = b.status;
		warning = b.warning;
		make = b.make;
		station = b.station;
		leverNumbers = b.leverNumbers;
		highlightVessel = b.highlightVessel;
		nextPotionSlot = b.nextPotionSlot;
		stationCount = b.stationCount;
		digweedSlot = b.digweedSlot;
		digweedTarget = b.digweedTarget;
		digweedSpot = b.digweedSpot;
		digweedPickup = b.digweedPickup;
		next = b.next;
		belt = b.belt;
		verdict = b.verdict;
		rows = b.rows;
		stockHave = b.stockHave;
		stockTotal = b.stockTotal;
	}

	public Phase phase()
	{
		return phase;
	}

	public String headline()
	{
		return headline;
	}

	public String status()
	{
		return status;
	}

	/**
	 * A notice shown above the instruction, or empty.
	 */
	public String warning()
	{
		return warning;
	}

	/**
	 * The potion to make, or null.
	 */
	public PotionType make()
	{
		return make;
	}

	/**
	 * The station to use, or for a make step the station the new potion is for; null otherwise.
	 */
	public Station station()
	{
		return station;
	}

	/**
	 * The pulls still owed per lever, as 1-based recipe positions; empty when none.
	 */
	public Map<PotionComponent, List<Integer>> leverNumbers()
	{
		return leverNumbers;
	}

	public boolean highlightVessel()
	{
		return highlightVessel;
	}

	/**
	 * The inventory slot the station will take next, or -1.
	 */
	/**
	 * Potions still to put into the step's station in this run, or -1 when the step has no
	 * station count. Zero means the potion inside is the last one there.
	 */
	public int stationCount()
	{
		return stationCount;
	}

	public int nextPotionSlot()
	{
		return nextPotionSlot;
	}

	/**
	 * The potion the digweed should go on, or null.
	 */
	public PotionType digweedTarget()
	{
		return digweedTarget;
	}

	/**
	 * The inventory slot to use the digweed on, or -1.
	 */
	public int digweedSlot()
	{
		return digweedSlot;
	}

	public LabObject digweedSpot()
	{
		return digweedSpot;
	}

	/**
	 * True when the headline asks the player to pick up the digweed.
	 */
	public boolean digweedPickup()
	{
		return digweedPickup;
	}

	/**
	 * Where the player goes after the current station run: the next station, or the belt
	 * once nothing is left to process. Null outside processing.
	 */
	public LabObject next()
	{
		return next;
	}

	public BeltColour belt()
	{
		return belt;
	}

	/**
	 * The belt check made this tick, or null.
	 */
	public BeltVerdict verdict()
	{
		return verdict;
	}

	public List<StockSummary.Row> rows()
	{
		return rows;
	}

	public int stockHave()
	{
		return stockHave;
	}

	public int stockTotal()
	{
		return stockTotal;
	}

	static Builder builder(Phase phase, String headline)
	{
		return new Builder(phase, headline);
	}

	static final class Builder
	{
		private Phase phase;
		private String headline;
		private String status = "";
		private String warning = "";
		private PotionType make;
		private Station station;
		private Map<PotionComponent, List<Integer>> leverNumbers = Collections.emptyMap();
		private boolean highlightVessel;
		private int nextPotionSlot = -1;
		private int stationCount = -1;
		private int digweedSlot = -1;
		private PotionType digweedTarget;
		private LabObject next;
		private LabObject digweedSpot;
		private boolean digweedPickup;
		private BeltColour belt = BeltColour.NONE;
		private BeltVerdict verdict;
		private List<StockSummary.Row> rows = Collections.emptyList();
		private int stockHave;
		private int stockTotal;

		private Builder(Phase phase, String headline)
		{
			this.phase = phase;
			this.headline = headline;
		}

		Phase phase()
		{
			return phase;
		}

		Builder phase(Phase v)
		{
			phase = v;
			return this;
		}

		String status()
		{
			return status;
		}

		Builder headline(String v)
		{
			headline = v;
			return this;
		}

		Builder warning(String v)
		{
			warning = v == null ? "" : v;
			return this;
		}

		Builder status(String v)
		{
			status = v == null ? "" : v;
			return this;
		}

		Builder make(PotionType v)
		{
			make = v;
			return this;
		}

		Builder station(Station v)
		{
			station = v;
			return this;
		}

		Builder leverNumbers(Map<PotionComponent, List<Integer>> v)
		{
			leverNumbers = v;
			return this;
		}

		Builder highlightVessel(boolean v)
		{
			highlightVessel = v;
			return this;
		}

		Builder stationCount(int v)
		{
			stationCount = v;
			return this;
		}

		Builder nextPotionSlot(int v)
		{
			nextPotionSlot = v;
			return this;
		}

		Builder digweedTarget(PotionType v)
		{
			digweedTarget = v;
			return this;
		}

		Builder digweedSlot(int v)
		{
			digweedSlot = v;
			return this;
		}

		Builder digweedSpot(LabObject v)
		{
			digweedSpot = v;
			return this;
		}

		Builder next(LabObject v)
		{
			next = v;
			return this;
		}

		Builder digweedPickup(boolean v)
		{
			digweedPickup = v;
			return this;
		}

		Builder belt(BeltColour v)
		{
			belt = v;
			return this;
		}

		Builder verdict(BeltVerdict v)
		{
			verdict = v;
			return this;
		}

		Builder rows(List<StockSummary.Row> v)
		{
			rows = v;
			return this;
		}

		Builder stock(int have, int total)
		{
			stockHave = have;
			stockTotal = total;
			return this;
		}

		Step build()
		{
			return new Step(this);
		}
	}
}
