package com.mixologybeltrunner;

import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ClickGuardTest
{
	@Test
	public void blocksTheWrongIdleStation()
	{
		assertTrue(ClickGuard.blockStation(RETORT, false, ALEMBIC));
		assertFalse(ClickGuard.blockStation(ALEMBIC, false, ALEMBIC));
	}

	@Test
	public void neverBlocksAnOccupiedStation()
	{
		assertFalse(ClickGuard.blockStation(RETORT, true, ALEMBIC));
	}

	@Test
	public void noPlanNoBlock()
	{
		assertFalse(ClickGuard.blockStation(RETORT, false, null));
	}

	@Test
	public void nullVerdictNeverBlocks()
	{
		assertFalse(ClickGuard.blockBelt(null));
	}

	@Test
	public void blocksOnlyAClosedBelt()
	{
		Order order = new Order(0, PotionType.MIXALOT, ALEMBIC);
		assertTrue(ClickGuard.blockBelt(new BeltVerdict(BeltState.NEED_ORDER, order, Collections.emptyList(), List.of(order), 0)));
		assertFalse(ClickGuard.blockBelt(new BeltVerdict(BeltState.ALL_FILLABLE, null, Collections.emptyList(), Collections.emptyList(), 3)));
		assertFalse(ClickGuard.blockBelt(new BeltVerdict(BeltState.SKIPPING, null, Collections.emptyList(), Collections.emptyList(), 1)));
		BeltVerdict open = new BeltVerdict(BeltState.ALL_FILLABLE, null, Collections.emptyList(), Collections.emptyList(), 3);
		assertFalse(ClickGuard.blockBelt(open, false));
		assertTrue(ClickGuard.blockBelt(open, true));
	}
}
