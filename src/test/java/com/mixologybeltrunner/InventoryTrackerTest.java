package com.mixologybeltrunner;

import static com.mixologybeltrunner.Fixtures.orders;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class InventoryTrackerTest
{
	private static final PotionType MMA = PotionType.MYSTIC_MANA_AMALGAM;
	private static final PotionType AAA = PotionType.ALCO_AUGMENTATOR;
	private static final PotionType LLL = PotionType.LIPLACK_LIQUOR;
	private static final PotionType MML = PotionType.MARLEYS_MOONLIGHT;

	@Test
	public void probeSessionReplay()
	{
		InventoryTracker t = tracker(); // assigner: ALEMBIC, then RETORT, then AGITATOR for successive MMAs
		t.start(inv(), Map.of());
		t.onInventory(inv(U(MMA)), 1);
		t.onStationLoaded(ALEMBIC, MMA); t.onInventory(inv(), 2);
		t.onStationCleared(ALEMBIC); t.onInventory(inv(F(MMA)), 3);
		t.onInventory(inv(F(MMA), U(MMA)), 4);
		t.onStationLoaded(RETORT, MMA); t.onInventory(inv(F(MMA)), 5);
		t.onStationCleared(RETORT); t.onInventory(inv(F(MMA), F(MMA)), 6);
		t.onInventory(inv(F(MMA), F(MMA), U(MMA)), 7);
		t.onStationLoaded(AGITATOR, MMA); t.onInventory(inv(F(MMA), F(MMA)), 8);
		t.onStationCleared(AGITATOR); t.onInventory(inv(F(MMA), F(MMA), F(MMA)), 9);
		t.onDrag(0, 1);                                   // identical swap, inventory unchanged
		t.onDrag(2, 3); t.onInventory(inv(F(MMA), F(MMA), -1, F(MMA)), 10);
		t.onInventory(inv(F(MMA), F(MMA), U(AAA), F(MMA)), 11);
		t.onDrag(3, 2); t.onInventory(inv(F(MMA), F(MMA), F(MMA), U(AAA)), 12);
		t.onDepositClick(orders(AAA, RETORT, MMA, ALEMBIC, LLL, AGITATOR));
		t.onInventory(inv(F(MMA), -1, F(MMA), U(AAA)), 13);    // belt took slot 1
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(RETORT, t.map().get(0).station());
		assertEquals(AGITATOR, t.map().get(2).station());
		t.onTick(20);
		assertEquals(TrackerState.TRACKING, t.state());
	}

	@Test
	public void stationLoadInventoryFirst()
	{
		InventoryTracker t = trackingWith(inv(U(MIXALOT)));
		t.onInventory(inv(), 5);
		t.onStationLoaded(RETORT, MIXALOT);
		t.onTick(8);
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(MIXALOT, t.inStation(RETORT).type());
	}

	@Test
	public void stationLoadVarbitFirst()
	{
		InventoryTracker t = trackingWith(inv(U(MIXALOT)));
		t.onStationLoaded(RETORT, MIXALOT);
		t.onInventory(inv(), 5);
		t.onTick(8);
		assertEquals(MIXALOT, t.inStation(RETORT).type());
	}

	@Test
	public void mismatchedDepositPausesAfterGrace()
	{
		InventoryTracker t = trackingWithFinished(MIXALOT, ALEMBIC);
		t.onDepositClick(orders(MIXALOT, RETORT, MMA, ALEMBIC, LLL, AGITATOR));
		t.onInventory(inv(), 5);
		t.onTick(6);
		assertEquals(TrackerState.TRACKING, t.state());
		t.onTick(7);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
		assertTrue(t.consumePauseEvent());
		assertFalse(t.consumePauseEvent());
	}

	@Test
	public void doubleClickDepositsAcrossABoardRefreshAreBothExplained()
	{
		InventoryTracker t = trackingWith(inv(F(MMA), F(MIXALOT), F(AAA)));
		List<Order> first = orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		List<Order> refreshed = orders(MIXALOT, ALEMBIC, LLL, RETORT, MMA, AGITATOR);
		t.onDepositClick(first);
		t.onDepositClick(first);
		t.onInventory(inv(-1, F(MIXALOT), F(AAA)), 5, first);
		t.onInventory(inv(-1, -1, F(AAA)), 6, refreshed);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertNull(t.map().get(0));
		assertNull(t.map().get(1));
		assertEquals(AAA, t.map().get(2).type());
	}

	@Test
	public void depositStaysArmedForTheWholeWindow()
	{
		InventoryTracker t = trackingWith(inv(F(MMA), F(MIXALOT), F(AAA)));
		t.onDepositClick(orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR));
		t.onInventory(inv(-1, F(MIXALOT), F(AAA)), 5);
		t.onTick(25);
		t.onInventory(inv(-1, -1, F(AAA)), 25, orders(MIXALOT, ALEMBIC, LLL, RETORT, MMA, AGITATOR));
		t.onTick(30);
		assertEquals(TrackerState.TRACKING, t.state());
	}

	@Test
	public void lossAfterTheArmedWindowStillPauses()
	{
		InventoryTracker t = trackingWith(inv(F(MMA), F(MIXALOT), F(AAA)));
		t.onDepositClick(orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR));
		t.onInventory(inv(-1, F(MIXALOT), F(AAA)), 5);
		t.onTick(26);
		assertEquals(TrackerState.TRACKING, t.state());
		t.onInventory(inv(-1, -1, F(AAA)), 26, orders(MIXALOT, ALEMBIC, LLL, RETORT, MMA, AGITATOR));
		t.onTick(28);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void dropAndDestroyRemoveCleanly()
	{
		InventoryTracker t = trackingWith(inv(U(MIXALOT), U(MMA)));
		t.onDropClick(1);
		t.onInventory(inv(U(MIXALOT)), 5);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertNull(t.map().get(1));
	}

	@Test
	public void digweedMarksTheSlot()
	{
		int dw = ItemID.MM_LAB_SPECIAL_HERB;
		InventoryTracker t = trackingWith(inv(dw, -1, -1, U(AAA)));
		t.onDigweedUsed(3);
		t.onInventory(inv(-1, -1, -1, U(AAA)), 5);
		assertTrue(t.map().get(3).digweed());
	}

	@Test
	public void dragOntoPasteSlotMovesPotion()
	{
		int paste = 12345; // any non-potion id
		InventoryTracker t = trackingWith(inv(paste, F(MIXALOT)));
		t.onDrag(1, 0);
		t.onInventory(inv(F(MIXALOT), paste), 5);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(MIXALOT, t.map().get(0).type());
	}

	@Test
	public void startWithPotionsNeedsClearThenRecovers()
	{
		InventoryTracker t = tracker();
		t.start(inv(F(MIXALOT)), Map.of());
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
		t.onInventory(inv(), 3);
		assertEquals(TrackerState.TRACKING, t.state());
	}

	@Test
	public void startSeedsLoadedStations()
	{
		InventoryTracker t = tracker();
		t.start(inv(), Map.of(AGITATOR, LLL));
		t.onStationCleared(AGITATOR);
		t.onInventory(inv(F(LLL)), 4);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(AGITATOR, t.map().get(0).station());
	}

	@Test
	public void restoreRequiresExactMatch()
	{
		SlotMap saved = new SlotMap();
		saved.put(0, new PotionSlot(MIXALOT, true, RETORT, true));
		InventoryTracker ok = tracker();
		ok.restore(saved, inv(F(MIXALOT)), Map.of());
		assertEquals(TrackerState.TRACKING, ok.state());
		assertTrue(ok.map().get(0).digweed());
		InventoryTracker bad = tracker();
		bad.restore(saved, inv(-1, F(MIXALOT)), Map.of());
		assertEquals(TrackerState.NEEDS_CLEAR, bad.state());
	}

	@Test
	public void stationClearInventoryFirst()
	{
		InventoryTracker t = tracker();
		t.start(inv(), Map.of(RETORT, MIXALOT));
		t.onInventory(inv(F(MIXALOT)), 4);
		t.onStationCleared(RETORT);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(RETORT, t.map().get(0).station());
		assertNull(t.inStation(RETORT));
	}

	@Test
	public void unexplainedPotionPausesAndClearsMap()
	{
		InventoryTracker t = trackingWith(inv(U(MIXALOT)));
		t.onInventory(inv(U(MIXALOT), F(MMA)), 4);
		t.onTick(6);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
		assertTrue(t.map().isEmpty());
		assertNotNull(t.pauseReason());
	}

	@Test
	public void recoveryKeepsStationLoadedWhilePausedVarbitFirst()
	{
		InventoryTracker t = tracker();
		t.start(inv(U(MIXALOT)), Map.of());
		t.onStationLoaded(RETORT, MIXALOT);
		t.onInventory(inv(), 3);
		assertCollectedAtRetortWithoutPause(t);
	}

	@Test
	public void recoveryKeepsStationLoadedWhilePausedInventoryFirst()
	{
		InventoryTracker t = tracker();
		t.start(inv(U(MIXALOT)), Map.of());
		t.onInventory(inv(), 3);
		t.onStationLoaded(RETORT, MIXALOT);
		assertCollectedAtRetortWithoutPause(t);
	}

	@Test
	public void pendingDropFollowsDrag()
	{
		InventoryTracker t = trackingWith(inv(U(MIXALOT), U(MMA)));
		t.onDropClick(0);
		t.onDrag(0, 1);
		t.onInventory(inv(U(MMA)), 5);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(MMA, t.map().get(0).type());
		assertNull(t.map().get(1));
	}

	@Test
	public void pendingDigweedFollowsDrag()
	{
		int dw = ItemID.MM_LAB_SPECIAL_HERB;
		InventoryTracker t = trackingWith(inv(dw, U(AAA), U(MIXALOT)));
		t.onDigweedUsed(1);
		t.onDrag(1, 2);
		t.onInventory(inv(-1, U(MIXALOT), U(AAA)), 5);
		assertTrue(t.map().get(2).digweed());
		assertFalse(t.map().get(1).digweed());
	}

	@Test
	public void ambiguousClearsLeaveTheGainUnexplained()
	{
		InventoryTracker t = tracker();
		t.start(inv(), Map.of(RETORT, MIXALOT, AGITATOR, MIXALOT));
		t.onStationCleared(RETORT);
		t.onStationCleared(AGITATOR);
		t.onInventory(inv(F(MIXALOT), F(MIXALOT)), 4);
		t.onTick(6);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void twoClearsOnOneStationDoNotThrow()
	{
		InventoryTracker t = tracker();
		t.start(inv(), Map.of(RETORT, MIXALOT));
		t.onStationCleared(RETORT);
		t.onStationLoaded(RETORT, MIXALOT);
		t.onStationCleared(RETORT);
		t.onInventory(inv(F(MIXALOT)), 4);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void stationEntriesAreUnprocessed()
	{
		InventoryTracker seeded = tracker();
		seeded.start(inv(), Map.of(AGITATOR, LLL));
		assertFalse(seeded.inStation(AGITATOR).processed());
		InventoryTracker loaded = trackingWith(inv(U(MIXALOT)));
		loaded.onStationLoaded(RETORT, MIXALOT);
		loaded.onInventory(inv(), 5);
		assertFalse(loaded.inStation(RETORT).processed());
		assertEquals(RETORT, loaded.inStation(RETORT).station());
	}

	@Test
	public void loadOfAnotherTypeLeavesTheLossUnexplained()
	{
		InventoryTracker t = trackingWith(inv(U(MIXALOT)));
		t.onStationLoaded(RETORT, MMA);
		t.onInventory(inv(), 5);
		t.onTick(7);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
		assertEquals(MMA, t.inStation(RETORT).type());
	}

	@Test
	public void loadPairsOnlyWithTheFirstUnfinishedPotion()
	{
		InventoryTracker t = trackingWith(inv(U(MIXALOT), U(MIXALOT)));
		t.onStationLoaded(RETORT, MIXALOT);
		t.onInventory(inv(U(MIXALOT)), 5);
		t.onTick(7);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void loadOnAnOccupiedStationIsIgnored()
	{
		SlotMap m = new SlotMap();
		m.put(0, new PotionSlot(MIXALOT, false, RETORT, false));
		InventoryTracker t = tracker();
		t.restore(m, inv(U(MIXALOT)), Map.of(RETORT, LLL));
		t.onStationLoaded(RETORT, MIXALOT);
		assertEquals(LLL, t.inStation(RETORT).type());
		t.onInventory(inv(), 5);
		t.onTick(7);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
		assertEquals(LLL, t.inStation(RETORT).type());
	}

	@Test
	public void failedDepositTakesTheFirstFinishedPotion()   // chat first
	{
		InventoryTracker t = trackingWith(inv(-1, -1, -1, F(MMA), F(MML)));
		t.onFailedDeposit(MMA);
		t.onInventory(inv(-1, -1, -1, -1, F(MML)), 5);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertNull(t.map().get(3));
	}

	@Test
	public void failedDepositInventoryFirst()
	{
		InventoryTracker t = trackingWith(inv(-1, -1, -1, F(MMA), F(MML)));
		t.onInventory(inv(-1, -1, -1, -1, F(MML)), 5);
		t.onFailedDeposit(MMA);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertNull(t.map().get(3));
	}

	@Test
	public void failedDepositOfAnotherTypePauses()
	{
		InventoryTracker t = trackingWith(inv(-1, -1, -1, F(MMA), F(MML)));
		t.onFailedDeposit(MML);                          // first finished is an MMA
		t.onInventory(inv(-1, -1, -1, -1, F(MML)), 5);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositOfAnotherTypeInventoryFirstPausesAtOnce()
	{
		InventoryTracker t = trackingWith(inv(-1, -1, -1, F(MMA), F(MML)));
		t.onInventory(inv(-1, -1, -1, -1, F(MML)), 5);
		t.onFailedDeposit(MML);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositWhileADepositIsArmed()
	{
		// The session order: belt click, then chat and inventory in the same tick.
		InventoryTracker t = trackingWith(inv(-1, -1, -1, F(MMA), F(MML)));
		List<Order> board = orders(LLL, AGITATOR, AAA, ALEMBIC, MIXALOT, RETORT);
		t.onDepositClick(board);
		t.onTick(4);
		t.onFailedDeposit(MMA);
		t.onInventory(inv(-1, -1, -1, -1, F(MML)), 5, board);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertNull(t.map().get(3));
		assertEquals(MML, t.map().get(4).type());
	}

	@Test
	public void failedDepositTheArmedBoardContradictsPauses()
	{
		// The map says slot 0 fills the MMA order, yet the game says the deposit failed:
		// the map is wrong somewhere, so neither the claim nor the failed deposit explains it.
		InventoryTracker t = trackingWith(inv(F(MMA), F(MMA)));
		List<Order> board = orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onFailedDeposit(MMA);
		t.onInventory(inv(-1, F(MMA)), 5, board);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void contradictedLossIsNotClaimedByALaterClick()
	{
		InventoryTracker t = trackingWith(inv(F(MMA), F(MMA)));
		List<Order> board = orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onFailedDeposit(MMA);
		t.onInventory(inv(-1, F(MMA)), 5, board);
		t.onDepositClick(board);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositOfAnotherTypePausesAtOnceWhileArmed()
	{
		// The board wants the MMA, so the claim would explain its loss; the game named an MML.
		InventoryTracker t = trackingWith(inv(F(MMA), F(MML)));
		List<Order> board = orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onFailedDeposit(MML);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
		t.onInventory(inv(-1, F(MML)), 5, board);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositOfAnotherTypeAfterTheClaimPausesAtOnce()
	{
		InventoryTracker t = trackingWith(inv(F(MMA), F(MML)));
		List<Order> board = orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onInventory(inv(-1, F(MML)), 5, board);
		assertEquals(TrackerState.TRACKING, t.state());
		t.onFailedDeposit(MML);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositTheLiveBoardContradictsPausesWithoutAClick()
	{
		InventoryTracker t = trackingWith(inv(F(MMA)));
		t.onFailedDeposit(MMA);
		t.onInventory(inv(), 5, orders(LLL, RETORT, MMA, ALEMBIC, AAA, AGITATOR));
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositOfATypeTheBoardWantsElsewhereIsAccepted()
	{
		// The board wants an MMA from the Retort; this one went through the Alembic.
		InventoryTracker t = trackingWith(inv(F(MMA), F(MML)));
		List<Order> board = orders(MMA, RETORT, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onFailedDeposit(MMA);
		t.onInventory(inv(-1, F(MML)), 5, board);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertNull(t.map().get(0));
	}

	@Test
	public void failedDepositAfterTheBoardClaimedItPauses()
	{
		// Inventory first: the armed deposit already claimed slot 0 when the chat says it failed.
		InventoryTracker t = trackingWith(inv(F(MMA), F(MML)));
		List<Order> board = orders(MMA, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onInventory(inv(-1, F(MML)), 5, board);
		assertEquals(TrackerState.TRACKING, t.state());
		t.onFailedDeposit(MMA);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositThenAnotherSlotLeavesPauses()
	{
		// The game names the first finished potion's type, yet a different slot leaves:
		// one the board wants, so a claim could explain it. The failed deposit forbids that.
		InventoryTracker t = trackingWith(inv(F(MMA), F(MML)));
		List<Order> board = orders(MML, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onFailedDeposit(MMA);
		t.onInventory(inv(F(MMA), -1), 5, board);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositAfterAClaimOnAnotherSlotPausesAtOnce()
	{
		// Inventory first: the armed deposit claimed slot 1, not the lowest finished potion,
		// then the game says a deposit failed. Its potion should have been slot 0.
		InventoryTracker t = trackingWith(inv(F(MMA), F(MML)));
		List<Order> board = orders(MML, ALEMBIC, LLL, RETORT, AAA, AGITATOR);
		t.onDepositClick(board);
		t.onInventory(inv(F(MMA), -1), 5, board);
		assertEquals(TrackerState.TRACKING, t.state());
		t.onFailedDeposit(MMA);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositDoesNotOutliveItsInventoryChange()
	{
		InventoryTracker t = trackingWith(inv(F(MMA)));
		t.onFailedDeposit(MMA);
		t.onInventory(inv(F(MMA), U(MML)), 5);           // removes nothing finished
		t.onTick(6);
		t.onInventory(inv(-1, U(MML)), 6);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositAfterAnUnrelatedChangeInItsTickDoesNotWait()
	{
		InventoryTracker t = trackingWith(inv(F(MMA)));
		t.onInventory(inv(F(MMA), U(MML)), 5);
		t.onFailedDeposit(MMA);
		t.onTick(6);
		t.onInventory(inv(-1, U(MML)), 6);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositOnlyTakesTheLowestFinishedSlot()
	{
		InventoryTracker t = trackingWith(inv(-1, -1, -1, F(MMA), F(MMA)));
		t.onFailedDeposit(MMA);
		t.onInventory(inv(-1, -1, -1, F(MMA), -1), 5);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void unfinishedPotionsDoNotCountAsTheFirstFinished()
	{
		InventoryTracker t = trackingWith(inv(U(MML), F(MMA)));
		t.onFailedDeposit(MMA);
		t.onInventory(inv(U(MML), -1), 5);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(MML, t.map().get(0).type());
	}

	@Test
	public void failedDepositWithNoFinishedPotionPauses()
	{
		InventoryTracker t = trackingWith(inv(U(MMA)));
		t.onFailedDeposit(MMA);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositExplainsOneLossOnly()
	{
		InventoryTracker t = trackingWith(inv(F(MMA), F(MMA)));
		t.onFailedDeposit(MMA);
		t.onInventory(inv(-1, F(MMA)), 5);
		t.onInventory(inv(), 6);
		t.onTick(10);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	@Test
	public void failedDepositExpires()
	{
		InventoryTracker t = trackingWith(inv(F(MMA)));
		t.onFailedDeposit(MMA);
		t.onTick(5);
		t.onTick(11);
		t.onInventory(inv(), 11);
		t.onTick(15);
		assertEquals(TrackerState.NEEDS_CLEAR, t.state());
	}

	private static void assertCollectedAtRetortWithoutPause(InventoryTracker t)
	{
		assertEquals(TrackerState.TRACKING, t.state());
		assertEquals(MIXALOT, t.inStation(RETORT).type());
		t.onStationCleared(RETORT);
		t.onInventory(inv(F(MIXALOT)), 6);
		t.onTick(10);
		assertEquals(TrackerState.TRACKING, t.state());
		assertTrue(t.map().get(0).processed());
		assertEquals(RETORT, t.map().get(0).station());
		assertFalse(t.consumePauseEvent());
	}

	/**
	 * A 28-slot inventory with the given ids from slot 0, the rest empty.
	 */
	private static int[] inv(int... ids)
	{
		int[] out = new int[SlotMap.SIZE];
		Arrays.fill(out, -1);
		System.arraycopy(ids, 0, out, 0, ids.length);
		return out;
	}

	private static int U(PotionType type)
	{
		return type.unfinishedId();
	}

	private static int F(PotionType type)
	{
		return type.finishedId();
	}

	/**
	 * Successive mystic mana amalgams go to the Alembic, Retort, then Agitator; anything else to the Retort.
	 */
	private static InventoryTracker tracker()
	{
		Station[] mmaStations = {ALEMBIC, RETORT, AGITATOR};
		int[] mmaCount = {0};
		return new InventoryTracker((m, inStations, type) ->
			type == MMA ? mmaStations[mmaCount[0]++ % mmaStations.length] : RETORT);
	}

	/**
	 * A tracker restored onto {@code ids}: unfinished potions planned for the Alembic,
	 * finished ones processed there.
	 */
	private static InventoryTracker trackingWith(int[] ids)
	{
		SlotMap m = new SlotMap();
		for (int slot = 0; slot < ids.length; slot++)
		{
			PotionType type = PotionType.fromItemId(ids[slot]);
			if (type != null)
			{
				m.put(slot, new PotionSlot(type, type.isFinishedItem(ids[slot]), ALEMBIC, false));
			}
		}
		InventoryTracker t = tracker();
		t.restore(m, ids, Map.of());
		assertEquals(TrackerState.TRACKING, t.state());
		return t;
	}

	private static InventoryTracker trackingWithFinished(PotionType type, Station station)
	{
		SlotMap m = new SlotMap();
		m.put(0, new PotionSlot(type, true, station, false));
		InventoryTracker t = tracker();
		t.restore(m, inv(F(type)), Map.of());
		assertEquals(TrackerState.TRACKING, t.state());
		return t;
	}
}
