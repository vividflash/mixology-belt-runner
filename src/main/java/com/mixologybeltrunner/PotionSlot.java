package com.mixologybeltrunner;

/**
 * One potion in an inventory slot: what it is, whether it has been through its
 * station yet, and which station it went (or is planned to go) through.
 */
public final class PotionSlot
{
	private final PotionType type;
	private final boolean processed;
	private final Station station;
	private final boolean digweed;

	public PotionSlot(PotionType type, boolean processed, Station station, boolean digweed)
	{
		this.type = type;
		this.processed = processed;
		this.station = station;
		this.digweed = digweed;
	}

	public PotionType type()
	{
		return type;
	}

	public boolean processed()
	{
		return processed;
	}

	public Station station()
	{
		return station;
	}

	public boolean digweed()
	{
		return digweed;
	}

	public PotionSlot processedAt(Station s)
	{
		return new PotionSlot(type, true, s, digweed);
	}

	public PotionSlot withDigweed()
	{
		return new PotionSlot(type, processed, station, true);
	}

	public int expectedItemId()
	{
		return processed ? type.finishedId() : type.unfinishedId();
	}
}
