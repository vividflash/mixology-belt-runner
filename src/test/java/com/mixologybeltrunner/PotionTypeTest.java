package com.mixologybeltrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class PotionTypeTest
{
	@Test
	public void indexMatchesVarbitOrder()
	{
		assertEquals(PotionType.MAMMOTH_MIGHT_MIX, PotionType.fromIndex(0));
		assertEquals(PotionType.AZURE_AURA_MIX, PotionType.fromIndex(4));
		assertEquals(PotionType.AQUALUX_AMALGAM, PotionType.fromIndex(5));
		assertEquals(PotionType.MIXALOT, PotionType.fromIndex(9));
		assertNull(PotionType.fromIndex(10));
		assertNull(PotionType.fromIndex(-1));
	}

	@Test
	public void itemIdsResolveBothForms()
	{
		assertEquals(PotionType.AQUALUX_AMALGAM, PotionType.fromItemId(ItemID.MM_POTION_AAL_UNFINISHED));
		assertEquals(PotionType.AQUALUX_AMALGAM, PotionType.fromItemId(ItemID.MM_POTION_AAL_FINISHED));
		assertEquals(PotionType.MEGALITE_LIQUID, PotionType.fromItemId(ItemID.MM_POTION_LLM_FINISHED));
		assertEquals(PotionType.ANTI_LEECH_LOTION, PotionType.fromItemId(ItemID.MM_POTION_LLA_UNFINISHED));
		assertTrue(PotionType.MIXALOT.isFinishedItem(ItemID.MM_POTION_MAL_FINISHED));
		assertFalse(PotionType.MIXALOT.isFinishedItem(ItemID.MM_POTION_MAL_UNFINISHED));
		assertNull(PotionType.fromItemId(ItemID.MM_LAB_SPECIAL_HERB));
	}

	@Test
	public void xpAndCodes()
	{
		assertEquals(2150, PotionType.MYSTIC_MANA_AMALGAM.xp());
		assertEquals(3650, PotionType.MIXALOT.xp());
		assertEquals("ALA", PotionType.AQUALUX_AMALGAM.code());
		assertEquals("MLL", PotionType.MEGALITE_LIQUID.code());
		assertTrue(PotionType.ALCO_AUGMENTATOR.isTriple());
		assertFalse(PotionType.MIXALOT.isTriple());
	}

	@Test
	public void allXpAndCodes()
	{
		int[] xp = {1900, 2150, 2400, 1900, 2650, 2900, 1900, 3150, 3400, 3650};
		String[] codes = {"MMM", "MMA", "MML", "AAA", "AAM", "ALA", "LLL", "MLL", "ALL", "MAL"};
		for (int i = 0; i < xp.length; i++)
		{
			assertEquals(xp[i], PotionType.fromIndex(i).xp());
			assertEquals(codes[i], PotionType.fromIndex(i).code());
			assertEquals(i, PotionType.fromIndex(i).index());
		}
	}
}
