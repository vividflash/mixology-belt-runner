package com.mixologybeltrunner;

import java.awt.Color;

public enum PotionComponent
{
	MOX('M', new Color(0x03a9f4)),
	AGA('A', new Color(0x00e676)),
	LYE('L', new Color(0xe91e63));

	private final char letter;
	private final Color color;

	PotionComponent(char letter, Color color)
	{
		this.letter = letter;
		this.color = color;
	}

	public char letter()
	{
		return letter;
	}

	public Color color()
	{
		return color;
	}

	/**
	 * Maps the mixing vessel varbit value (1 to 3) to a component, or null.
	 */
	public static PotionComponent fromVesselValue(int v)
	{
		switch (v)
		{
			case 1:
				return MOX;
			case 2:
				return AGA;
			case 3:
				return LYE;
			default:
				return null;
		}
	}
}
