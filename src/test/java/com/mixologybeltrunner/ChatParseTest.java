package com.mixologybeltrunner;

import static com.mixologybeltrunner.PotionType.MARLEYS_MOONLIGHT;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.PotionType.MYSTIC_MANA_AMALGAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class ChatParseTest
{
	@Test
	public void parsesTheProbeLine()
	{
		assertEquals(MYSTIC_MANA_AMALGAM, ChatParse.failedDeposit("You deposit some @mes_hl_pur@Mystic mana amalgam, but fail to fulfil an order."));
	}

	@Test
	public void stripsColourTags()
	{
		assertEquals(MIXALOT, ChatParse.failedDeposit("You deposit some <col=ff00ff>Mixalot</col>, but fail to fulfil an order."));
	}

	@Test
	public void unknownNameIsNull()
	{
		assertNull(ChatParse.failedDeposit("You deposit some Tea, but fail to fulfil an order."));
		assertNull(ChatParse.failedDeposit("You deposit some Mixalot and fulfil an order."));
	}

	@Test
	public void namesWithAnApostropheParse()
	{
		assertEquals(MARLEYS_MOONLIGHT, ChatParse.failedDeposit("You deposit some @mes_hl_pur@Marley's moonlight, but fail to fulfil an order."));
	}

	@Test
	public void everyItemNameRoundTrips()
	{
		String[] names = {"Mammoth-might mix", "Mystic mana amalgam", "Marley's moonlight", "Alco-augmentator",
			"Azure aura mix", "Aqualux amalgam", "Liplack liquor", "Megalite liquid", "Anti-leech lotion", "Mixalot"};
		for (int i = 0; i < names.length; i++)
		{
			assertEquals(names[i], PotionType.fromIndex(i).itemName());
			assertEquals(PotionType.fromIndex(i), PotionType.fromItemName(names[i].toUpperCase()));
		}
		assertNull(PotionType.fromItemName("Tea"));
		assertNull(PotionType.fromItemName(null));
	}

	@Test
	public void otherLinesAreNull()
	{
		assertNull(ChatParse.failedDeposit(null));
		assertNull(ChatParse.failedDeposit("You are rewarded @mes_hl_blu@0/@mes_hl_gre@10/@mes_hl_red@0."));
	}
}
