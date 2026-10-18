package com.mixologybeltrunner;

import static com.mixologybeltrunner.Fixtures.fin;
import static com.mixologybeltrunner.Fixtures.firstEmpty;
import static com.mixologybeltrunner.Fixtures.orders;
import static com.mixologybeltrunner.Fixtures.removeType;
import static com.mixologybeltrunner.Fixtures.unf;
import static com.mixologybeltrunner.PotionType.ALCO_AUGMENTATOR;
import static com.mixologybeltrunner.PotionType.ANTI_LEECH_LOTION;
import static com.mixologybeltrunner.PotionType.AZURE_AURA_MIX;
import static com.mixologybeltrunner.PotionType.LIPLACK_LIQUOR;
import static com.mixologybeltrunner.PotionType.MAMMOTH_MIGHT_MIX;
import static com.mixologybeltrunner.PotionType.MARLEYS_MOONLIGHT;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.PotionType.MYSTIC_MANA_AMALGAM;
import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class GuideTest
{
	@Test
	public void rivalMakesItDormant()
	{
		GuideInput in = input();
		in.rival = "Mastering Mixology Extended";
		Step s = new Guide().next(in);
		assertEquals(Phase.DORMANT, s.phase());
		assertEquals("Belt Runner is paused", s.headline());
		assertEquals("Turn off Mastering Mixology Extended to use it", s.status());
		assertEquals(0, s.stockTotal());
	}

	@Test
	public void freshStartRestocksInRounds()
	{
		Step s = new Guide().next(input());
		assertEquals(Phase.MAKE, s.phase());
		assertEquals("Make MMA", s.headline());
		assertEquals(Map.of(PotionComponent.MOX, List.of(1, 2), PotionComponent.AGA, List.of(3)), s.leverNumbers());
		assertEquals("Restocking · round 1 of 3 · stock 0 / 21", s.status());
		assertEquals(MYSTIC_MANA_AMALGAM, s.make());
		assertEquals(ALEMBIC, s.station());
		assertEquals(21, s.stockTotal());
		assertEquals(7, s.rows().size());
	}

	@Test
	public void fullStockGoesToProcessThenBelt()
	{
		Guide g = new Guide();
		GuideInput in = input();
		g.next(in);
		in.map = fullStock(in.plan, false);
		in.orders = orders(MIXALOT, ALEMBIC, MYSTIC_MANA_AMALGAM, RETORT, ANTI_LEECH_LOTION, AGITATOR);
		Step s = g.next(in);
		assertEquals(Phase.PROCESS, s.phase());
		assertEquals("Alembic ×7", s.headline());
		assertEquals("then Retort ×7, Agitator ×7", s.status());
		assertEquals(0, s.nextPotionSlot());
		assertEquals(ALEMBIC, s.station());
		in.map = fullStock(in.plan, true);
		s = g.next(in);
		assertEquals(Phase.BELT, s.phase());
		assertEquals("Click the belt", s.headline());
		assertEquals("fills 3 of 3 · stock 21 / 21", s.status());
		assertEquals(BeltColour.GREEN, s.belt());
	}

	@Test
	public void lockedBeltTopsUpUnderThreshold()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		removeType(in.map, ANTI_LEECH_LOTION, AGITATOR);
		in.orders = orders(MIXALOT, ALEMBIC, ANTI_LEECH_LOTION, AGITATOR, MYSTIC_MANA_AMALGAM, RETORT);
		Step s = g.next(in);
		assertEquals(Phase.MAKE, s.phase());
		assertEquals("Make ALL → Agitator", s.headline());
		assertEquals("Top up · for the current order", s.status());
		assertEquals(BeltColour.NONE, s.belt());
		assertEquals(AGITATOR, s.station());
	}

	@Test
	public void lockedBeltRestocksAtThreshold()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		removeFinished(in.map, 11);
		in.orders = orders(MIXALOT, ALEMBIC, LIPLACK_LIQUOR, AGITATOR, MYSTIC_MANA_AMALGAM, RETORT);
		Step s = g.next(in);
		assertTrue(s.status().startsWith("Restocking"));
	}

	@Test
	public void belowLevel81WarnsAndKeepsGuiding()
	{
		GuideInput in = input();
		in.herbloreLevel = 70;
		in.plan = StockPlan.create(25, 70);
		in.lowLevelWarning = true;
		Step s = new Guide().next(in);
		assertEquals(Phase.MAKE, s.phase());
		assertEquals("Make MMA", s.headline());
		assertTrue(s.warning().startsWith("Not tested below Herblore 81"));
		assertEquals(9, in.plan.size());                  // MMA, MML, AAM only
		in.lowLevelWarning = false;
		assertEquals("", new Guide().next(in).warning());
	}

	@Test
	public void emptyPlanStillTopsUp()
	{
		GuideInput in = input();
		in.plan = StockPlan.create(5);
		in.orders = orders(ALCO_AUGMENTATOR, RETORT, LIPLACK_LIQUOR, ALEMBIC, MAMMOTH_MIGHT_MIX, AGITATOR);
		Step s = new Guide().next(in);
		assertEquals("Make AAA → Retort", s.headline());
		assertFalse(s.status().contains("/ 0"));
	}

	@Test
	public void digweedTargetsAnUnboostedMixalot()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.digweedHeld = true;
		Step s = g.next(in);
		assertEquals("Use digweed on MAL", s.headline());
		assertEquals(MIXALOT, in.map.get(s.digweedSlot()).type());
		assertEquals(Phase.BELT, s.phase());
	}

	@Test
	public void holdModeWaitsForAMixalotOrder()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.digweedHeld = true;
		in.holdDigweed = true;
		in.orders = orders(MYSTIC_MANA_AMALGAM, ALEMBIC, MARLEYS_MOONLIGHT, RETORT, AZURE_AURA_MIX, AGITATOR);
		Step s = g.next(in);
		assertEquals(-1, s.digweedSlot());
		assertTrue(s.status().endsWith("holding digweed for a Mixalot"));
	}

	@Test
	public void readyVesselComesFirst()
	{
		GuideInput in = input();
		in.vesselReady = MYSTIC_MANA_AMALGAM;
		Step s = new Guide().next(in);
		assertEquals("Take the potion", s.headline());
		assertTrue(s.highlightVessel());
		assertEquals(MYSTIC_MANA_AMALGAM, s.make());
		assertEquals(ALEMBIC, s.station());
	}

	@Test
	public void offScriptVesselPotionGetsItsFirstMissingCell()
	{
		GuideInput in = input();
		in.map = Fixtures.map(unf(MIXALOT, ALEMBIC));
		in.vesselReady = MIXALOT;
		Step s = new Guide().next(in);
		assertEquals("Take the potion", s.headline());
		assertEquals(MIXALOT, s.make());
		assertEquals(RETORT, s.station());
	}

	@Test
	public void processHeadlineCountsTheFirstRunOnly()
	{
		GuideInput in = input();
		in.plan = StockPlan.create(5);
		in.map = Fixtures.map(fin(MIXALOT, AGITATOR), unf(MIXALOT, ALEMBIC), unf(MARLEYS_MOONLIGHT, ALEMBIC),
			unf(AZURE_AURA_MIX, ALEMBIC), unf(MIXALOT, RETORT), unf(ANTI_LEECH_LOTION, ALEMBIC),
			unf(MYSTIC_MANA_AMALGAM, ALEMBIC));
		in.orders = orders(MIXALOT, ALEMBIC, MIXALOT, RETORT, ANTI_LEECH_LOTION, ALEMBIC);
		Step s = new Guide().next(in);
		assertEquals(Phase.PROCESS, s.phase());
		assertEquals("Alembic ×3", s.headline());
		assertEquals("then Retort ×1, Alembic ×2", s.status());
		assertEquals(1, s.nextPotionSlot());
	}

	@Test
	public void potionInAStationIsNotMadeAgain()
	{
		Guide g = new Guide();
		GuideInput in = input();
		in.inStations.put(ALEMBIC, unf(MYSTIC_MANA_AMALGAM, ALEMBIC));
		assertEquals("Finish at Alembic", g.next(in).headline());
		in.inStations.remove(ALEMBIC);
		in.map = Fixtures.map(fin(MYSTIC_MANA_AMALGAM, ALEMBIC));
		Step s = g.next(in);
		assertEquals("Make MML", s.headline());
		assertEquals(ALEMBIC, s.station());
	}

	@Test
	public void assignerFollowsTheStepForItsType()
	{
		StockPlan plan = StockPlan.create(25);
		Step step = Step.builder(Phase.MAKE, "Make MAL").make(MIXALOT).station(AGITATOR).build();
		EnumMap<Station, PotionSlot> none = new EnumMap<>(Station.class);
		assertEquals(AGITATOR, Guide.stationFor(step, plan, List.of(), new SlotMap(), none, MIXALOT));
		assertEquals(ALEMBIC, Guide.stationFor(step, plan, List.of(), new SlotMap(), none, MYSTIC_MANA_AMALGAM));
		assertEquals(ALEMBIC, Guide.stationFor(null, plan, List.of(), new SlotMap(), none, MIXALOT));
		Step process = Step.builder(Phase.PROCESS, "Retort ×1").station(RETORT).build();
		assertEquals(ALEMBIC, Guide.stationFor(process, plan, List.of(), new SlotMap(), none, MIXALOT));
	}

	@Test
	public void needsClearShowsThePauseReason()
	{
		GuideInput in = input();
		in.trackerState = TrackerState.NEEDS_CLEAR;
		Step s = new Guide().next(in);
		assertEquals(Phase.NEEDS_CLEAR, s.phase());
		assertEquals("Deposit or destroy your potions to start", s.headline());
		assertEquals("Tracking starts from an inventory with no potions", s.status());
		in.pauseReason = "A potion vanished";
		assertEquals("A potion vanished", new Guide().next(in).status());
	}

	@Test
	public void wrongPullStartsTheCountAgain()
	{
		GuideInput in = input();
		in.vesselSlots = new int[]{3, 0, 0};                 // pulled L for an MMA
		Step s = new Guide().next(in);
		assertEquals("Make MMA", s.headline());
		assertTrue(s.status().endsWith("wrong lever, start again"));
		assertEquals(Map.of(PotionComponent.MOX, List.of(1, 2), PotionComponent.AGA, List.of(3)), s.leverNumbers());
		assertFalse(s.highlightVessel());
	}

	@Test
	public void unwantedReadyPotionIsNotTaken()
	{
		GuideInput in = input();
		in.vesselSlots = new int[]{3, 3, 3};                 // three Lye pulls make an LLL nobody wants
		in.vesselReady = LIPLACK_LIQUOR;
		Step s = new Guide().next(in);
		assertEquals("Make MMA", s.headline());
		assertTrue(s.status().endsWith("wrong lever, start again"));
		assertEquals(Map.of(PotionComponent.MOX, List.of(1, 2), PotionComponent.AGA, List.of(3)), s.leverNumbers());
	}

	@Test
	public void finishAtStationWhenOnlyAStationHoldsAPotion()
	{
		Guide g = new Guide();
		GuideInput in = input();
		g.next(in);
		in.map = fullStock(in.plan, false);
		assertEquals(Phase.PROCESS, g.next(in).phase());
		in.map = fullStock(in.plan, true);
		in.map.remove(20);
		in.inStations.put(AGITATOR, unf(MIXALOT, AGITATOR));
		Step s = g.next(in);
		assertEquals(Phase.PROCESS, s.phase());
		assertEquals("Finish at Agitator", s.headline());
		assertEquals(AGITATOR, s.station());
	}

	@Test
	public void skippingTriplesNamesThem()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.orders = orders(MYSTIC_MANA_AMALGAM, ALEMBIC, LIPLACK_LIQUOR, RETORT, ALCO_AUGMENTATOR, AGITATOR);
		Step s = g.next(in);
		assertEquals("Click the belt (skipping LLL, AAA)", s.headline());
		assertEquals(BeltColour.GREEN, s.belt());
	}

	@Test
	public void unknownOrdersWait()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.orders = List.of();
		assertEquals("Waiting for orders", g.next(in).headline());
	}

	@Test
	public void digweedSpawnIsPointedOut()
	{
		GuideInput in = input();
		in.landingSlot = 0;
		in.digweedSpawn = LabObject.DIGWEED_NORTH_EAST;
		Step s = new Guide().next(in);
		assertEquals(LabObject.DIGWEED_NORTH_EAST, s.digweedSpot());
		assertEquals("Pick up the digweed (north east)", s.headline());
		assertFalse(s.highlightVessel());
	}

	@Test
	public void digweedDirectionsAreFixed()
	{
		assertEquals("north east", Guide.digweedDirection(LabObject.DIGWEED_NORTH_EAST));
		assertEquals("south east", Guide.digweedDirection(LabObject.DIGWEED_SOUTH_EAST));
		assertEquals("south west", Guide.digweedDirection(LabObject.DIGWEED_SOUTH_WEST));
		assertEquals("north west", Guide.digweedDirection(LabObject.DIGWEED_NORTH_WEST));
	}

	@Test
	public void busyStationFinishesFirst()
	{
		Guide g = new Guide();
		GuideInput in = input();
		g.next(in);
		in.map = fullStock(in.plan, false);
		for (int slot = 0; slot < 7; slot++)
		{
			// Alembic block taken
			in.map.remove(slot);
		}
		in.inStations = Map.of(ALEMBIC, new PotionSlot(MIXALOT, false, ALEMBIC, false));
		Step s = g.next(in);
		assertEquals("Finish at Alembic", s.headline());
		assertEquals("then Retort ×7, Agitator ×7", s.status());
		assertEquals(ALEMBIC, s.station());
		assertEquals(-1, s.nextPotionSlot());
		assertTrue(s.leverNumbers().isEmpty());
		assertFalse(s.highlightVessel());
	}

	@Test
	public void spawnBecomesTheHeadline()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.landingSlot = 21;
		in.digweedSpawn = LabObject.DIGWEED_NORTH_WEST;
		Step s = g.next(in);
		assertEquals("Pick up the digweed (north west)", s.headline());
		assertTrue(s.digweedPickup());
		assertEquals(LabObject.DIGWEED_NORTH_WEST, s.digweedSpot());
	}

	@Test
	public void spawnWhileStationBusyIsOnlyAHint()
	{
		Guide g = new Guide();
		GuideInput in = input();
		g.next(in);
		in.inStations = Map.of(RETORT, new PotionSlot(MIXALOT, false, RETORT, false));
		in.digweedSpawn = LabObject.DIGWEED_SOUTH_EAST;
		Step s = g.next(in);
		assertEquals("Finish at Retort", s.headline());
		assertFalse(s.digweedPickup());
		assertTrue(s.status().endsWith("digweed ready south east"));
		assertEquals(LabObject.DIGWEED_SOUTH_EAST, s.digweedSpot());
	}

	@Test
	public void halfMadePotionIsFinishedBeforeTheDigweed()
	{
		GuideInput in = input();
		in.vesselSlots = new int[]{1, 0, 0};
		in.landingSlot = 0;
		in.digweedSpawn = LabObject.DIGWEED_NORTH_EAST;
		Step s = new Guide().next(in);
		assertEquals("Make MMA", s.headline());
		assertFalse(s.digweedPickup());
		assertFalse(s.leverNumbers().isEmpty());
		assertTrue(s.status().endsWith("digweed ready north east"));
		assertEquals(LabObject.DIGWEED_NORTH_EAST, s.digweedSpot());
	}

	@Test
	public void spawnWithAFullInventoryIsOnlyAHint()
	{
		GuideInput in = input();
		in.landingSlot = -1;
		in.digweedSpawn = LabObject.DIGWEED_NORTH_EAST;
		Step s = new Guide().next(in);
		assertEquals("Make MMA", s.headline());
		assertFalse(s.digweedPickup());
		assertTrue(s.status().endsWith("digweed ready north east"));
		assertEquals(LabObject.DIGWEED_NORTH_EAST, s.digweedSpot());
	}

	@Test
	public void wrongMixIsSortedBeforeTheDigweed()
	{
		GuideInput in = input();
		in.vesselSlots = new int[]{3, 0, 0};
		in.landingSlot = 0;
		in.digweedSpawn = LabObject.DIGWEED_NORTH_EAST;
		Step s = new Guide().next(in);
		assertEquals("Make MMA", s.headline());
		assertFalse(s.digweedPickup());
		assertTrue(s.status().contains("wrong lever"));
	}

	@Test
	public void processingPointsAtTheNextStationThenTheBelt()
	{
		SlotMap m = Fixtures.map(unf(MIXALOT, ALEMBIC), unf(MYSTIC_MANA_AMALGAM, ALEMBIC), unf(MIXALOT, RETORT));
		assertEquals(LabObject.RETORT, Guide.nextAfter(m, ALEMBIC));
		assertEquals(LabObject.CONVEYOR_BELT, Guide.nextAfter(Fixtures.map(unf(MIXALOT, RETORT)), RETORT));
		GuideInput in = input();
		in.map = Fixtures.map(unf(MIXALOT, RETORT));
		in.inStations.put(ALEMBIC, unf(MYSTIC_MANA_AMALGAM, ALEMBIC));
		Step s = new Guide().next(in);
		assertEquals("Finish at Alembic", s.headline());
		assertEquals(LabObject.RETORT, s.next());
		assertEquals(LabObject.RETORT, LabOverlay.nextToOutline(s));
	}

	@Test
	public void mixingShowsNoNext()
	{
		Step s = new Guide().next(input());
		assertEquals(Phase.MAKE, s.phase());
		assertNull(LabOverlay.nextToOutline(s));
	}

	@Test
	public void busyStationBeatsAReadyVessel()
	{
		GuideInput in = input();
		in.inStations.put(ALEMBIC, unf(MYSTIC_MANA_AMALGAM, ALEMBIC));
		in.vesselReady = MIXALOT;
		Step s = new Guide().next(in);
		assertEquals("Finish at Alembic", s.headline());
		assertEquals(ALEMBIC, s.station());
		assertFalse(s.highlightVessel());
	}

	@Test
	public void readyVesselBeatsThePickup()
	{
		GuideInput in = input();
		in.vesselReady = MYSTIC_MANA_AMALGAM;
		in.digweedSpawn = LabObject.DIGWEED_NORTH_EAST;
		assertEquals("Take the potion", new Guide().next(in).headline());
	}

	@Test
	public void beltOutlinedOnlyWhenClickable()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		assertEquals(BeltColour.GREEN, g.next(in).belt());
		removeType(in.map, ANTI_LEECH_LOTION, AGITATOR);
		in.orders = orders(MIXALOT, ALEMBIC, ANTI_LEECH_LOTION, AGITATOR, MYSTIC_MANA_AMALGAM, RETORT);
		// The locked top-up step.
		assertEquals(BeltColour.NONE, g.next(in).belt());
	}

	@Test
	public void digweedFallsBackToTheBestOrderPotion()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		for (int slot : new int[]{6, 13, 20})
		{
			in.map.put(slot, in.map.get(slot).withDigweed());
		}
		in.digweedHeld = true;
		in.orders = orders(MYSTIC_MANA_AMALGAM, ALEMBIC, ANTI_LEECH_LOTION, RETORT, AZURE_AURA_MIX, AGITATOR);
		Step s = g.next(in);
		assertEquals("Use digweed on ALL", s.headline());
		assertEquals(12, s.digweedSlot());
	}

	@Test
	public void duplicateOrderMakesASecondCopy()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.orders = orders(MIXALOT, ALEMBIC, MIXALOT, ALEMBIC, MYSTIC_MANA_AMALGAM, RETORT);
		Step s = g.next(in);
		assertEquals(Phase.MAKE, s.phase());
		assertEquals("Make MAL → Alembic", s.headline());
		assertEquals(BeltColour.NONE, s.belt());
		assertEquals(Map.of(PotionComponent.MOX, List.of(1), PotionComponent.AGA, List.of(2), PotionComponent.LYE, List.of(3)), s.leverNumbers());
		in.map.put(21, unf(MIXALOT, ALEMBIC));
		s = g.next(in);
		assertEquals(Phase.PROCESS, s.phase());
		assertEquals("Alembic ×1", s.headline());
		assertEquals(21, s.nextPotionSlot());
		in.map.put(21, fin(MIXALOT, ALEMBIC));
		s = g.next(in);
		assertEquals(Phase.BELT, s.phase());
		assertEquals("Click the belt", s.headline());
		assertEquals(BeltColour.GREEN, s.belt());
	}

	@Test
	public void offScriptPotionIsProcessedNotRemade()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		removeType(in.map, ANTI_LEECH_LOTION, AGITATOR);
		in.map.put(21, unf(ANTI_LEECH_LOTION, AGITATOR));
		in.orders = orders(MIXALOT, ALEMBIC, ANTI_LEECH_LOTION, AGITATOR, MYSTIC_MANA_AMALGAM, RETORT);
		Step s = g.next(in);
		assertEquals(Phase.PROCESS, s.phase());
		assertEquals("Agitator ×1", s.headline());
		assertEquals(21, s.nextPotionSlot());
		assertEquals(BeltColour.NONE, s.belt());
	}

	@Test
	public void digweedWaitsUntilTheMakingIsDone()
	{
		GuideInput in = input();
		in.map = Fixtures.map(unf(MIXALOT, ALEMBIC));
		in.digweedHeld = true;
		Step s = new Guide().next(in);
		assertEquals(Phase.MAKE, s.phase());
		assertEquals(MYSTIC_MANA_AMALGAM, s.make());
		assertEquals("Make MMA", s.headline());
		assertEquals(-1, s.digweedSlot());
		assertFalse(s.leverNumbers().isEmpty());
		assertTrue(s.status().endsWith("use digweed after making"));
		in.vesselReady = MYSTIC_MANA_AMALGAM;
		s = new Guide().next(in);
		assertEquals("Take the potion", s.headline());
		assertTrue(s.highlightVessel());
		assertEquals(-1, s.digweedSlot());
	}

	@Test
	public void digweedIsUsedOnceTheMakingIsDone()
	{
		Guide g = new Guide();
		GuideInput in = input();
		g.next(in);
		in.map = fullStock(in.plan, false);
		in.digweedHeld = true;
		Step s = g.next(in);
		assertEquals(Phase.PROCESS, s.phase());
		assertEquals("Use digweed on MAL", s.headline());
		assertEquals(6, s.digweedSlot());
		assertTrue(s.leverNumbers().isEmpty());
		assertFalse(s.highlightVessel());
		// No station outline or next potion competing with the digweed.
		assertNull(s.station());
		assertEquals(-1, s.nextPotionSlot());
	}

	@Test
	public void focusSkipsEveryUnfilledOrder()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.mode = Mode.FOCUS_XP;
		removeType(in.map, ANTI_LEECH_LOTION, AGITATOR);
		in.orders = orders(MIXALOT, ALEMBIC, ANTI_LEECH_LOTION, AGITATOR, MYSTIC_MANA_AMALGAM, RETORT);
		Step s = g.next(in);
		assertEquals(Phase.BELT, s.phase());
		assertEquals(BeltColour.GREEN, s.belt());
		assertEquals("Click the belt (skipping ALL)", s.headline());
	}

	@Test
	public void lockedTopUpHoldsTheDigweed()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		in.digweedHeld = true;
		removeType(in.map, ANTI_LEECH_LOTION, AGITATOR);
		in.orders = orders(MIXALOT, ALEMBIC, ANTI_LEECH_LOTION, AGITATOR, MYSTIC_MANA_AMALGAM, RETORT);
		Step s = g.next(in);
		assertEquals("Make ALL → Agitator", s.headline());
		assertEquals(-1, s.digweedSlot());
		assertEquals("Top up · for the current order · use digweed after making", s.status());
	}

	@Test
	public void lockedBoardMakesBothPotionsBeforeProcessing()
	{
		Guide g = new Guide();
		GuideInput in = beltPhase(g);
		removeType(in.map, ANTI_LEECH_LOTION, RETORT);
		removeType(in.map, MIXALOT, AGITATOR);
		in.orders = orders(ANTI_LEECH_LOTION, RETORT, MIXALOT, AGITATOR, MYSTIC_MANA_AMALGAM, ALEMBIC);
		assertEquals("Make ALL → Retort", g.next(in).headline());
		in.map.put(firstEmpty(in.map), new PotionSlot(ANTI_LEECH_LOTION, false, RETORT, false));
		// Not "Retort ×1": the second order potion comes first.
		assertEquals("Make MAL → Agitator", g.next(in).headline());
	}

	@Test
	public void landingSlotKeepsBlocksTogether()
	{
		Guide g = new Guide();
		GuideInput in = input();
		g.next(in);
		// A stray Retort potion made first; the next potion lands after it.
		in.map.put(0, new PotionSlot(MYSTIC_MANA_AMALGAM, false, RETORT, false));
		in.landingSlot = 1;
		Step s = g.next(in);
		// Station-major alone would pick an Alembic cell.
		assertEquals(RETORT, s.station());
		// The first Retort candidate: MMA at Retort is covered.
		assertEquals("Make MML", s.headline());
	}

	@Test
	public void fullInventoryFallsBack()
	{
		GuideInput in = input();
		in.landingSlot = -1;
		Step s = new Guide().next(in);
		assertEquals("Make MMA", s.headline());
		assertEquals(ALEMBIC, s.station());
	}

	private static GuideInput input()
	{
		GuideInput in = new GuideInput();
		in.trackerState = TrackerState.TRACKING;
		in.herbloreLevel = 99;
		in.map = new SlotMap();
		in.plan = StockPlan.create(25);
		in.orders = orders(MIXALOT, ALEMBIC, MYSTIC_MANA_AMALGAM, RETORT, ANTI_LEECH_LOTION, AGITATOR);
		in.inStations = new EnumMap<>(Station.class);
		in.vesselSlots = new int[3];
		in.restockThreshold = 50;
		in.mode = Mode.BALANCED;
		return in;
	}

	/**
	 * The plan made in rounds: slots 0-6 Alembic, 7-13 Retort, 14-20 Agitator.
	 */
	private static SlotMap fullStock(StockPlan plan, boolean processed)
	{
		SlotMap m = new SlotMap();
		int slot = 0;
		for (Station station : Station.PROCESS_ORDER)
		{
			for (PotionType type : plan.types())
			{
				m.put(slot++, processed ? fin(type, station) : unf(type, station));
			}
		}
		return m;
	}

	private static GuideInput beltPhase(Guide g)
	{
		GuideInput in = input();
		g.next(in);
		in.map = fullStock(in.plan, true);
		for (int i = 0; i < 5; i++)
		{
			if (g.next(in).phase() == Phase.BELT)
			{
				return in;
			}
		}
		throw new AssertionError("guide never reached the belt");
	}

	private static void removeFinished(SlotMap map, int n)
	{
		for (int i = 0; i < SlotMap.SIZE && n > 0; i++)
		{
			PotionSlot p = map.get(i);
			if (p != null && p.processed())
			{
				map.remove(i);
				n--;
			}
		}
	}
}
