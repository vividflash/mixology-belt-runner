package com.mixologybeltrunner;

import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.ALEMBIC;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class QuickActionWindowTest
{
	@Test
	public void alembicWindowEndsOnTheFirstProgressUpdate()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(ALEMBIC, true, 10);
		w.onTick(10);
		assertEquals(ALEMBIC, w.active());
		w.onProgress(ALEMBIC);
		assertNull(w.active());
	}

	@Test
	public void agitatorWindowEndsOnTheSecondProgressUpdate()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(AGITATOR, true, 10);
		w.onTick(10);
		w.onProgress(AGITATOR);
		w.onTick(11);
		assertEquals(AGITATOR, w.active());
		w.onProgress(AGITATOR);
		assertNull(w.active());
	}

	@Test
	public void progressAtAnotherStationLeavesTheWindowOpen()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(ALEMBIC, true, 10);
		w.onProgress(AGITATOR);
		assertEquals(ALEMBIC, w.active());
	}

	@Test
	public void aNewEffectStartsTheCountAgain()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(AGITATOR, true, 10);
		w.onProgress(AGITATOR);
		w.onSpotAnim(AGITATOR, true, 11);
		w.onProgress(AGITATOR);
		assertEquals(AGITATOR, w.active());
		w.onProgress(AGITATOR);
		assertNull(w.active());
	}

	@Test
	public void windowExpiresThreeTicksAfterTheEffectWithoutProgress()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(AGITATOR, true, 10);
		w.onTick(10);
		w.onTick(11);
		w.onTick(12);
		assertEquals(AGITATOR, w.active());
		w.onTick(13);
		assertNull(w.active());
	}

	@Test
	public void hitEndsTheWindow()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(AGITATOR, true, 10);
		w.onSkillshotHit(AGITATOR);
		assertNull(w.active());
	}

	@Test
	public void progressDropEndsTheWindow()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(AGITATOR, true, 10);
		w.onProgressDropped(AGITATOR);
		assertNull(w.active());
	}

	@Test
	public void spotAnimIgnoredWhenStationEmpty()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(ALEMBIC, false, 10);
		assertNull(w.active());
	}

	@Test
	public void retortHasNoWindow()
	{
		QuickActionWindow w = new QuickActionWindow();
		w.onSpotAnim(RETORT, true, 10);
		assertNull(w.active());
	}
}
