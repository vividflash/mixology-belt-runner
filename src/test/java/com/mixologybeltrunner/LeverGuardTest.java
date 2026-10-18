package com.mixologybeltrunner;

import static com.mixologybeltrunner.PotionComponent.AGA;
import static com.mixologybeltrunner.PotionComponent.LYE;
import static com.mixologybeltrunner.PotionComponent.MOX;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.PotionType.MYSTIC_MANA_AMALGAM;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import org.junit.Test;

public class LeverGuardTest
{
	@Test
	public void onlyTheNextLeverIsAllowed()
	{
		Map<PotionComponent, java.util.List<Integer>> mal = Vessel.pullNumbers(MIXALOT, new int[]{0, 0, 0});
		assertFalse(ClickGuard.blockLever(mal, false, MOX));
		assertTrue(ClickGuard.blockLever(mal, false, AGA));
		assertTrue(ClickGuard.blockLever(mal, false, LYE));
		Map<PotionComponent, java.util.List<Integer>> afterMox = Vessel.pullNumbers(MIXALOT, Vessel.pushed(new int[]{0, 0, 0}, MOX));
		assertFalse(ClickGuard.blockLever(afterMox, false, AGA));
		assertTrue(ClickGuard.blockLever(afterMox, false, MOX));
	}

	@Test
	public void mmaTakesMoxTwiceThenAga()
	{
		int[] slots = {0, 0, 0};
		assertFalse(ClickGuard.blockLever(Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, slots), false, MOX));
		slots = Vessel.pushed(slots, MOX);
		assertFalse(ClickGuard.blockLever(Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, slots), false, MOX));
		assertTrue(ClickGuard.blockLever(Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, slots), false, AGA));
		slots = Vessel.pushed(slots, MOX);
		assertFalse(ClickGuard.blockLever(Vessel.pullNumbers(MYSTIC_MANA_AMALGAM, slots), false, AGA));
	}

	@Test
	public void everyLeverBlockedWhileThePotionWaits()
	{
		assertTrue(ClickGuard.blockLever(Map.of(), true, MOX));
	}

	@Test
	public void pushedShiftsTheQueue()
	{
		assertArrayEquals(new int[]{2, 1, 3}, Vessel.pushed(new int[]{1, 3, 3}, AGA));
	}

	@Test
	public void stationLabelCountsDown()
	{
		Step make7 = Step.builder(Phase.PROCESS, "Alembic ×7").station(Station.ALEMBIC).stationCount(7).build();
		assertEquals("Make 7", LabLabelsOverlay.stationLabel(make7));
		Step last = Step.builder(Phase.PROCESS, "Finish at Alembic").station(Station.ALEMBIC).stationCount(0).build();
		assertEquals("Last one", LabLabelsOverlay.stationLabel(last));
		assertNull(LabLabelsOverlay.stationLabel(Step.builder(Phase.MAKE, "Make MMA").build()));
	}

	@Test
	public void labelGoesToTheFarSide()
	{
		// Player two tiles south of the object: the label moves north.
		int[] offset = LabLabelsOverlay.awayOffset(1000, 1000, 1000, 744);
		assertEquals(0, offset[0]);
		assertEquals(LabLabelsOverlay.AWAY, offset[1]);
		// Standing on it, or far away: no offset.
		assertArrayEquals(new int[]{0, 0}, LabLabelsOverlay.awayOffset(1000, 1000, 1000, 1000));
		assertArrayEquals(new int[]{0, 0}, LabLabelsOverlay.awayOffset(1000, 1000, 1000 + LabLabelsOverlay.NEAR + 5, 1000));
	}

	@Test
	public void makeLabelColoursEachLetter()
	{
		java.util.List<LabLabelsOverlay.Segment> parts = LabLabelsOverlay.makeLabel(MYSTIC_MANA_AMALGAM);
		assertEquals("Make ", parts.get(0).text);
		assertEquals("M", parts.get(1).text);
		assertEquals(MOX.color(), parts.get(1).colour);
		assertEquals(AGA.color(), parts.get(3).colour);
	}

	@Test
	public void pathLeadsWhereTheStepPoints()
	{
		Step make = Step.builder(Phase.MAKE, "Make MMA").build();
		assertEquals(LabObject.MIXING_VESSEL.coordinate(), PathTargets.target(make));
		Step belt = Step.builder(Phase.BELT, "Click the belt").belt(BeltColour.GREEN).build();
		assertEquals(LabObject.CONVEYOR_BELT.coordinate(), PathTargets.target(belt));
		Step pickup = Step.builder(Phase.BELT, "Pick up the digweed (north east)").digweedPickup(true)
			.digweedSpot(LabObject.DIGWEED_NORTH_EAST).build();
		assertEquals(LabObject.DIGWEED_NORTH_EAST.coordinate(), PathTargets.target(pickup));
		assertNull(PathTargets.target(Step.builder(Phase.DORMANT, "Paused").build()));
	}
}
