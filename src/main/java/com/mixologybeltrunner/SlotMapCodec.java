package com.mixologybeltrunner;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the slot map as JSON so it survives a relog.
 */
public final class SlotMapCodec
{
	private SlotMapCodec()
	{
	}

	private static final class Entry
	{
		private int slot;
		private String type;
		private boolean processed;
		private String station;
		private boolean digweed;
	}

	public static String encode(Gson gson, SlotMap map)
	{
		List<Entry> out = new ArrayList<>();
		for (int slot : map.occupiedSlots())
		{
			PotionSlot p = map.get(slot);
			Entry e = new Entry();
			e.slot = slot;
			e.type = p.type().code();
			e.processed = p.processed();
			e.station = p.station().name();
			e.digweed = p.digweed();
			out.add(e);
		}
		return gson.toJson(out);
	}

	/**
	 * Returns null when the text is missing, malformed or names anything unknown.
	 */
	public static SlotMap decode(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}
		Entry[] entries;
		try
		{
			entries = gson.fromJson(json, Entry[].class);
		}
		catch (JsonParseException e)
		{
			return null;
		}
		if (entries == null)
		{
			return null;
		}
		SlotMap map = new SlotMap();
		for (Entry e : entries)
		{
			if (e == null || e.slot < 0 || e.slot >= SlotMap.SIZE)
			{
				return null;
			}
			PotionType type = typeOf(e.type);
			Station station = stationOf(e.station);
			if (type == null || station == null)
			{
				return null;
			}
			map.put(e.slot, new PotionSlot(type, e.processed, station, e.digweed));
		}
		return map;
	}

	private static PotionType typeOf(String code)
	{
		for (PotionType t : PotionType.values())
		{
			if (t.code().equals(code))
			{
				return t;
			}
		}
		return null;
	}

	private static Station stationOf(String name)
	{
		for (Station s : Station.values())
		{
			if (s.name().equals(name))
			{
				return s;
			}
		}
		return null;
	}
}
