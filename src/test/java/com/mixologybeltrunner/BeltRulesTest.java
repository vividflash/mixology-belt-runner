package com.mixologybeltrunner;

import static com.mixologybeltrunner.Fixtures.fin;
import static com.mixologybeltrunner.Fixtures.map;
import static com.mixologybeltrunner.Fixtures.orders;
import static com.mixologybeltrunner.Fixtures.unf;
import static com.mixologybeltrunner.Mode.BALANCED;
import static com.mixologybeltrunner.Mode.FOCUS_XP;
import static com.mixologybeltrunner.PotionType.ALCO_AUGMENTATOR;
import static com.mixologybeltrunner.PotionType.ANTI_LEECH_LOTION;
import static com.mixologybeltrunner.PotionType.AQUALUX_AMALGAM;
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
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class BeltRulesTest
{
	@Test
	public void allFillable()
	{
		SlotMap m = map(fin(MYSTIC_MANA_AMALGAM, ALEMBIC), fin(MIXALOT, AGITATOR), fin(ANTI_LEECH_LOTION, RETORT));
		BeltVerdict v = BeltRules.evaluate(orders(MYSTIC_MANA_AMALGAM, ALEMBIC, MIXALOT, AGITATOR, ANTI_LEECH_LOTION, RETORT), m, FOCUS_XP);
		assertEquals(BeltState.ALL_FILLABLE, v.state());
		assertEquals(3, v.fills());
	}

	@Test
	public void focusClicksWhenAnythingFills()
	{
		// Even an unfilled Mixalot is skipped: the next set of orders comes faster than a new potion.
		SlotMap m = map(fin(MYSTIC_MANA_AMALGAM, RETORT));
		BeltVerdict v = BeltRules.evaluate(orders(MYSTIC_MANA_AMALGAM, RETORT, MIXALOT, RETORT, ALCO_AUGMENTATOR, ALEMBIC), m, FOCUS_XP);
		assertEquals(BeltState.SKIPPING, v.state());
		assertTrue(v.state().open());
		assertEquals(2, v.skipped().size());
	}

	@Test
	public void balancedMakesAnUnfilledMixalotOrAntiLeechFirst()
	{
		SlotMap m = map(fin(MYSTIC_MANA_AMALGAM, RETORT));
		BeltVerdict v = BeltRules.evaluate(orders(MYSTIC_MANA_AMALGAM, RETORT, ANTI_LEECH_LOTION, AGITATOR, MIXALOT, RETORT), m, BALANCED);
		assertEquals(BeltState.NEED_ORDER, v.state());
		assertEquals(MIXALOT, v.toMake().type());
		assertEquals(List.of(new Cell(ANTI_LEECH_LOTION, AGITATOR), new Cell(MIXALOT, RETORT)), List.copyOf(BeltRules.neededCells(v, BALANCED)));
	}

	@Test
	public void balancedSkipsCheaperOrders()
	{
		SlotMap m = map(fin(MIXALOT, RETORT));
		BeltVerdict v = BeltRules.evaluate(orders(MIXALOT, RETORT, AQUALUX_AMALGAM, ALEMBIC, LIPLACK_LIQUOR, AGITATOR), m, BALANCED);
		assertEquals(BeltState.SKIPPING, v.state());
	}

	@Test
	public void wrongStationIsNotFillable()
	{
		SlotMap m = map(fin(AQUALUX_AMALGAM, RETORT));
		BeltVerdict v = BeltRules.evaluate(orders(AQUALUX_AMALGAM, ALEMBIC, MARLEYS_MOONLIGHT, ALEMBIC, LIPLACK_LIQUOR, AGITATOR), m, FOCUS_XP);
		assertEquals(BeltState.NOTHING_FILLABLE, v.state());
		assertEquals(AQUALUX_AMALGAM, v.toMake().type());
		assertEquals(ALEMBIC, v.toMake().station());
	}

	@Test
	public void nothingFillableMakesTheBestOrderOnly()
	{
		BeltVerdict v = BeltRules.evaluate(orders(LIPLACK_LIQUOR, ALEMBIC, MARLEYS_MOONLIGHT, AGITATOR, MYSTIC_MANA_AMALGAM, RETORT), new SlotMap(), FOCUS_XP);
		assertEquals(BeltState.NOTHING_FILLABLE, v.state());
		assertEquals(Set.of(new Cell(MARLEYS_MOONLIGHT, AGITATOR)), BeltRules.neededCells(v, FOCUS_XP));
	}

	@Test
	public void allTriplesNothingFillablePicksTheFirst()
	{
		BeltVerdict v = BeltRules.evaluate(orders(ALCO_AUGMENTATOR, RETORT, LIPLACK_LIQUOR, ALEMBIC, MAMMOTH_MIGHT_MIX, AGITATOR), new SlotMap(), BALANCED);
		assertEquals(BeltState.NOTHING_FILLABLE, v.state());
		assertEquals(Set.of(new Cell(ALCO_AUGMENTATOR, RETORT)), BeltRules.neededCells(v, BALANCED));
	}

	@Test
	public void duplicateOrdersNeedTwoPotions()
	{
		SlotMap m = map(fin(MIXALOT, RETORT));
		BeltVerdict v = BeltRules.evaluate(orders(MIXALOT, RETORT, MIXALOT, RETORT, AQUALUX_AMALGAM, ALEMBIC), m, BALANCED);
		assertEquals(1, v.fills());
		assertFalse(v.state().open());
	}

	@Test
	public void unprocessedPotionIsNotFillable()
	{
		SlotMap m = map(unf(MIXALOT, AGITATOR));
		BeltVerdict v = BeltRules.evaluate(orders(MYSTIC_MANA_AMALGAM, ALEMBIC, MIXALOT, AGITATOR, ANTI_LEECH_LOTION, RETORT), m, FOCUS_XP);
		assertEquals(0, v.fills());
		assertFalse(v.state().open());
	}

	@Test
	public void fewerThanThreeOrdersIsUnknown()
	{
		assertNull(BeltRules.evaluate(orders(MIXALOT, ALEMBIC), new SlotMap(), FOCUS_XP));
	}

	@Test
	public void openBeltNeedsNothing()
	{
		SlotMap m = map(fin(MYSTIC_MANA_AMALGAM, RETORT));
		BeltVerdict v = BeltRules.evaluate(orders(MYSTIC_MANA_AMALGAM, RETORT, MIXALOT, RETORT, ALCO_AUGMENTATOR, ALEMBIC), m, FOCUS_XP);
		assertTrue(BeltRules.neededCells(v, FOCUS_XP).isEmpty());
	}

	@Test
	public void modesStockFiveOrSeven()
	{
		assertEquals(5, StockPlan.create(25, 99, FOCUS_XP.stockTypes()).types().size());
		assertEquals(MYSTIC_MANA_AMALGAM, StockPlan.create(25, 99, BALANCED.stockTypes()).types().get(0));
		assertEquals(7, StockPlan.create(25, 99, BALANCED.stockTypes()).types().size());
	}
}
