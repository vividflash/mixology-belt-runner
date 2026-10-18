package com.mixologybeltrunner;

import static com.mixologybeltrunner.PotionComponent.AGA;
import static com.mixologybeltrunner.PotionComponent.LYE;
import static com.mixologybeltrunner.PotionComponent.MOX;
import static com.mixologybeltrunner.PotionType.AQUALUX_AMALGAM;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.PotionType.MYSTIC_MANA_AMALGAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class VesselTest
{
	@Test
	public void pullNumbersFollowTheRecipe()
	{
		assertEquals(Map.of(MOX, List.of(1, 2), AGA, List.of(3)), Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, new int[]{0, 0, 0}));
		assertEquals(Map.of(MOX, List.of(2), AGA, List.of(3)), Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, new int[]{1, 0, 0}));
		assertEquals(Map.of(MOX, List.of(3)), Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, new int[]{2, 1, 0}));   // M then A
		assertEquals(Map.of(AGA, List.of(1, 3), LYE, List.of(2)), Vessel.pullNumbers(AQUALUX_AMALGAM, new int[]{0, 0, 0}));
		assertEquals(Map.of(MOX, List.of(1), AGA, List.of(2), LYE, List.of(3)), Vessel.pullNumbers(MIXALOT, new int[]{0, 0, 0}));
		assertTrue(Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, new int[]{2, 1, 1}).isEmpty());
	}

	@Test
	public void wrongPullStartsAgain()
	{
		// MMA: M then L leaves nothing usable, so all three pulls again.
		assertEquals(Map.of(MOX, List.of(1, 2), AGA, List.of(3)), Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, new int[]{3, 1, 0}));
		assertTrue(Vessel.wrongPull(MYSTIC_MANA_AMALGAM, new int[]{3, 1, 0}));
		// L then M: the M still counts.
		assertEquals(Map.of(MOX, List.of(2), AGA, List.of(3)), Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, new int[]{1, 3, 0}));
		assertTrue(Vessel.wrongPull(MYSTIC_MANA_AMALGAM, new int[]{1, 3, 0}));
		// Full queue L, M, M: the last two count, one A to go.
		assertEquals(Map.of(AGA, List.of(3)), Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, new int[]{1, 1, 3}));
		assertFalse(Vessel.wrongPull(MYSTIC_MANA_AMALGAM, new int[]{1, 0, 0}));
	}

	@Test
	public void pullNumbersIterateMoxAgaLye()
	{
		assertEquals(List.of(AGA, LYE), new ArrayList<>(Vessel.pullNumbers(AQUALUX_AMALGAM, new int[]{0, 0, 0}).keySet()));
	}
}
