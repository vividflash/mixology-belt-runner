package com.mixologybeltrunner;

import static com.mixologybeltrunner.PotionType.AQUALUX_AMALGAM;
import static com.mixologybeltrunner.PotionType.MIXALOT;
import static com.mixologybeltrunner.Station.AGITATOR;
import static com.mixologybeltrunner.Station.RETORT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import org.junit.Test;

public class SlotMapCodecTest
{
	private final Gson gson = new Gson();

	@Test
	public void roundTrip()
	{
		SlotMap m = new SlotMap();
		m.put(3, new PotionSlot(MIXALOT, true, RETORT, true));
		m.put(9, new PotionSlot(AQUALUX_AMALGAM, false, AGITATOR, false));
		SlotMap back = SlotMapCodec.decode(gson, SlotMapCodec.encode(gson, m));
		assertArrayEquals(m.expectedItemIds(), back.expectedItemIds());
		assertTrue(back.get(3).digweed());
		assertEquals(AGITATOR, back.get(9).station());
	}

	@Test
	public void garbageDecodesToNull()
	{
		assertNull(SlotMapCodec.decode(gson, "not json"));
		assertNull(SlotMapCodec.decode(gson, "[{\"slot\":40,\"type\":\"MAL\",\"processed\":true,\"station\":\"RETORT\",\"digweed\":false}]"));
		assertNull(SlotMapCodec.decode(gson, "[{\"slot\":1,\"type\":\"XYZ\",\"processed\":true,\"station\":\"RETORT\",\"digweed\":false}]"));
		assertNull(SlotMapCodec.decode(gson, "[{\"slot\":1,\"type\":\"MAL\",\"processed\":true,\"station\":\"NOPE\",\"digweed\":false}]"));
		assertNull(SlotMapCodec.decode(gson, null));
		assertNull(SlotMapCodec.decode(gson, "  "));
	}

	@Test
	public void emptyMapRoundTrips()
	{
		SlotMap back = SlotMapCodec.decode(gson, SlotMapCodec.encode(gson, new SlotMap()));
		assertTrue(back.isEmpty());
	}
}
