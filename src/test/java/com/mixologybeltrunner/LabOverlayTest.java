package com.mixologybeltrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static com.mixologybeltrunner.PotionComponent.LYE;
import static com.mixologybeltrunner.PotionComponent.MOX;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LabOverlayTest
{
	@Test
	public void stationIsOutlinedOnlyWhileProcessing()
	{
		Step process = Step.builder(Phase.PROCESS, "Retort ×7").station(Station.RETORT).build();
		assertEquals(LabObject.RETORT, LabOverlay.stationToOutline(process));
		Step make = Step.builder(Phase.MAKE, "Make MAL").make(PotionType.MIXALOT).station(Station.AGITATOR).build();
		assertNull(LabOverlay.stationToOutline(make));
		Step locked = Step.builder(Phase.BELT, "Make LLL → Agitator").make(PotionType.LIPLACK_LIQUOR)
			.station(Station.AGITATOR).build();
		assertNull(LabOverlay.stationToOutline(locked));
	}

	@Test
	public void leverLabelsJoinNumbers()
	{
		assertEquals("1 2", LabOverlay.leverLabel(Map.of(MOX, List.of(1, 2)), MOX));
		assertNull(LabOverlay.leverLabel(Map.of(MOX, List.of(1, 2)), LYE));
	}

	@Test
	public void stationUsesBonusColourDuringWindow()
	{
		assertEquals(Color.GREEN, LabOverlay.stationColour(Station.ALEMBIC, Station.ALEMBIC, Color.ORANGE, Color.GREEN));
		assertEquals(Color.ORANGE, LabOverlay.stationColour(Station.ALEMBIC, null, Color.ORANGE, Color.GREEN));
	}
}
