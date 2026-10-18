package com.mixologybeltrunner;

import java.util.Objects;

/**
 * A potion type paired with the station it is made at.
 */
public final class Cell
{
	private final PotionType type;
	private final Station station;

	public Cell(PotionType type, Station station)
	{
		this.type = type;
		this.station = station;
	}

	public PotionType type()
	{
		return type;
	}

	public Station station()
	{
		return station;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Cell))
		{
			return false;
		}
		Cell other = (Cell) o;
		return type == other.type && station == other.station;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(type, station);
	}
}
