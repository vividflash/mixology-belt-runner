package com.mixologybeltrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class StationTest
{
	@Test
	public void modifierVarbitMapping()
	{
		assertEquals(Station.AGITATOR, Station.fromOrderModifier(1));
		assertEquals(Station.RETORT, Station.fromOrderModifier(2));
		assertEquals(Station.ALEMBIC, Station.fromOrderModifier(3));
		assertNull(Station.fromOrderModifier(0));
	}

	@Test
	public void menuTargetStripsTags()
	{
		assertEquals(Station.RETORT, Station.fromMenuTarget("<col=ffff>Retort"));
		assertNull(Station.fromMenuTarget("Conveyor belt"));
		assertEquals(Station.ALEMBIC, Station.fromMenuTarget("<col=ffff>Alembic"));
		assertEquals(Station.AGITATOR, Station.fromMenuTarget("Agitator"));
	}

	@Test
	public void menuOptionsAreInGameClickOptions()
	{
		assertEquals("Crystalise-potion", Station.ALEMBIC.menuOption());
		assertEquals("Concentrate-potion", Station.RETORT.menuOption());
		assertEquals("Homogenise-potion", Station.AGITATOR.menuOption());
		assertEquals("Alembic", Station.ALEMBIC.displayName());
	}

	@Test
	public void componentVesselValues()
	{
		assertEquals(PotionComponent.MOX, PotionComponent.fromVesselValue(1));
		assertEquals(PotionComponent.AGA, PotionComponent.fromVesselValue(2));
		assertEquals(PotionComponent.LYE, PotionComponent.fromVesselValue(3));
		assertNull(PotionComponent.fromVesselValue(0));
	}
}
