package com.mixologybeltrunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.client.util.Text;

public enum Station
{
	ALEMBIC("Crystalise-potion", "Alembic", "Alem", LabObject.ALEMBIC),
	RETORT("Concentrate-potion", "Retort", "Reto", LabObject.RETORT),
	AGITATOR("Homogenise-potion", "Agitator", "Agit", LabObject.AGITATOR);

	public static final List<Station> PROCESS_ORDER =
		Collections.unmodifiableList(Arrays.asList(ALEMBIC, RETORT, AGITATOR));

	private final String menuOption;
	private final String displayName;
	private final String shortName;
	private final LabObject object;

	Station(String menuOption, String displayName, String shortName, LabObject object)
	{
		this.menuOption = menuOption;
		this.displayName = displayName;
		this.shortName = shortName;
		this.object = object;
	}

	/**
	 * The right-click option the game shows on the station object.
	 */
	public String menuOption()
	{
		return menuOption;
	}

	public String displayName()
	{
		return displayName;
	}

	public String shortName()
	{
		return shortName;
	}

	public LabObject object()
	{
		return object;
	}

	/**
	 * Maps the order modifier varbit value to the station it asks for, or null.
	 */
	public static Station fromOrderModifier(int varbitValue)
	{
		switch (varbitValue)
		{
			case 1:
				return AGITATOR;
			case 2:
				return RETORT;
			case 3:
				return ALEMBIC;
			default:
				return null;
		}
	}

	/**
	 * Resolves a raw menu target, colour tags included, to a station, or null.
	 */
	public static Station fromMenuTarget(String rawTarget)
	{
		if (rawTarget == null)
		{
			return null;
		}
		String target = Text.removeTags(rawTarget);
		for (Station station : values())
		{
			if (station.displayName.equalsIgnoreCase(target))
			{
				return station;
			}
		}
		return null;
	}
}
