package com.mixologybeltrunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Everything the guide reads, filled in by the plugin each tick.
 */
public final class GuideInput
{
	/**
	 * Name of an enabled rival plugin, or null.
	 */
	public String rival;
	/**
	 * Real Herblore level, not boosted.
	 */
	public int herbloreLevel;
	/**
	 * True below Herblore 81 until the first potion of the session is made.
	 */
	public boolean lowLevelWarning;
	public TrackerState trackerState = TrackerState.NEEDS_CLEAR;
	public String pauseReason;
	public SlotMap map = new SlotMap();
	public StockPlan plan = StockPlan.create(SlotMap.SIZE);
	public List<Order> orders = Collections.emptyList();
	/**
	 * Potions loaded into a station, still unprocessed.
	 */
	public Map<Station, PotionSlot> inStations = Collections.emptyMap();
	public int[] vesselSlots = new int[3];
	/**
	 * The potion waiting in the mixing vessel, or null.
	 */
	public PotionType vesselReady;
	public boolean digweedHeld;
	/**
	 * The spot where digweed has grown, or null.
	 */
	public LabObject digweedSpawn;
	public boolean holdDigweed;
	public int restockThreshold = 10;
	public Mode mode = Mode.FOCUS_XP;
	/**
	 * The inventory slot the next potion lands in: the lowest empty one, -1 when full.
	 */
	public int landingSlot = -1;
}
