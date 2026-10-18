package com.mixologybeltrunner;

import java.util.Objects;

public final class Order
{
	private final int boardIndex;
	private final PotionType type;
	private final Station station;

	public Order(int boardIndex, PotionType type, Station station)
	{
		this.boardIndex = boardIndex;
		this.type = type;
		this.station = station;
	}

	public int boardIndex()
	{
		return boardIndex;
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
		if (!(o instanceof Order))
		{
			return false;
		}
		Order other = (Order) o;
		return boardIndex == other.boardIndex && type == other.type && station == other.station;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(boardIndex, type, station);
	}

	@Override
	public String toString()
	{
		return "Order[" + boardIndex + ", " + type + ", " + station + "]";
	}
}
