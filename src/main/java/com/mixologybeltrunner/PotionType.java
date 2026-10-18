package com.mixologybeltrunner;

import static com.mixologybeltrunner.PotionComponent.AGA;
import static com.mixologybeltrunner.PotionComponent.LYE;
import static com.mixologybeltrunner.PotionComponent.MOX;
import net.runelite.api.gameval.ItemID;

/**
 * Declared in game index order: index is the order/station varbit value minus one.
 */
public enum PotionType
{
	MAMMOTH_MIGHT_MIX("Mammoth-might mix", ItemID.MM_POTION_MMM_UNFINISHED, ItemID.MM_POTION_MMM_FINISHED, 1900, MOX, MOX, MOX),
	MYSTIC_MANA_AMALGAM("Mystic mana amalgam", ItemID.MM_POTION_MMA_UNFINISHED, ItemID.MM_POTION_MMA_FINISHED, 2150, MOX, MOX, AGA),
	MARLEYS_MOONLIGHT("Marley's moonlight", ItemID.MM_POTION_MML_UNFINISHED, ItemID.MM_POTION_MML_FINISHED, 2400, MOX, MOX, LYE),
	ALCO_AUGMENTATOR("Alco-augmentator", ItemID.MM_POTION_AAA_UNFINISHED, ItemID.MM_POTION_AAA_FINISHED, 1900, AGA, AGA, AGA),
	AZURE_AURA_MIX("Azure aura mix", ItemID.MM_POTION_AAM_UNFINISHED, ItemID.MM_POTION_AAM_FINISHED, 2650, AGA, AGA, MOX),
	AQUALUX_AMALGAM("Aqualux amalgam", ItemID.MM_POTION_AAL_UNFINISHED, ItemID.MM_POTION_AAL_FINISHED, 2900, AGA, LYE, AGA),
	LIPLACK_LIQUOR("Liplack liquor", ItemID.MM_POTION_LLL_UNFINISHED, ItemID.MM_POTION_LLL_FINISHED, 1900, LYE, LYE, LYE),
	MEGALITE_LIQUID("Megalite liquid", ItemID.MM_POTION_LLM_UNFINISHED, ItemID.MM_POTION_LLM_FINISHED, 3150, MOX, LYE, LYE),
	ANTI_LEECH_LOTION("Anti-leech lotion", ItemID.MM_POTION_LLA_UNFINISHED, ItemID.MM_POTION_LLA_FINISHED, 3400, AGA, LYE, LYE),
	MIXALOT("Mixalot", ItemID.MM_POTION_MAL_UNFINISHED, ItemID.MM_POTION_MAL_FINISHED, 3650, MOX, AGA, LYE);

	private static final PotionType[] TYPES = values();

	private final String itemName;
	private final int unfinishedId;
	private final int finishedId;
	private final int xp;
	private final PotionComponent[] components;
	private final String code;

	PotionType(String itemName, int unfinishedId, int finishedId, int xp, PotionComponent... components)
	{
		this.itemName = itemName;
		this.unfinishedId = unfinishedId;
		this.finishedId = finishedId;
		this.xp = xp;
		this.components = components;
		this.code = "" + components[0].letter() + components[1].letter() + components[2].letter();
	}

	/**
	 * The name the game uses in chat, such as "Mystic mana amalgam".
	 */
	public String itemName()
	{
		return itemName;
	}

	public int unfinishedId()
	{
		return unfinishedId;
	}

	public int finishedId()
	{
		return finishedId;
	}

	/**
	 * The Herblore level the potion needs.
	 */
	public int level()
	{
		switch (this)
		{
			case MYSTIC_MANA_AMALGAM:
				return 63;
			case MARLEYS_MOONLIGHT:
				return 66;
			case AZURE_AURA_MIX:
				return 69;
			case AQUALUX_AMALGAM:
				return 72;
			case MEGALITE_LIQUID:
				return 75;
			case ANTI_LEECH_LOTION:
				return 78;
			case MIXALOT:
				return 81;
			default:
				return 60;
		}
	}

	public int xp()
	{
		return xp;
	}

	public PotionComponent[] components()
	{
		return components.clone();
	}

	public String code()
	{
		return code;
	}

	public boolean isTriple()
	{
		return components[0] == components[1] && components[1] == components[2];
	}

	public int index()
	{
		return ordinal();
	}

	public boolean isFinishedItem(int id)
	{
		return id == finishedId;
	}

	public static PotionType fromIndex(int index)
	{
		return index >= 0 && index < TYPES.length ? TYPES[index] : null;
	}

	public static PotionType fromItemId(int id)
	{
		for (PotionType type : TYPES)
		{
			if (type.unfinishedId == id || type.finishedId == id)
			{
				return type;
			}
		}
		return null;
	}

	/**
	 * The type with that in-game name, ignoring case, or null.
	 */
	public static PotionType fromItemName(String name)
	{
		for (PotionType type : TYPES)
		{
			if (type.itemName.equalsIgnoreCase(name))
			{
				return type;
			}
		}
		return null;
	}
}
