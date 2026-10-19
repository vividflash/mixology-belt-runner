package com.mixologybeltrunner;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(BeltRunnerConfig.GROUP)
public interface BeltRunnerConfig extends Config
{
	String GROUP = "mixologybeltrunner";

	@ConfigSection(
		name = "Colours",
		description = "Highlight colours in the lab",
		position = 10
	)
	String coloursSection = "colours";

	@ConfigItem(
		keyName = "mode",
		name = "Mode",
		description = "Focus XP: stock the five best potions and click the belt whenever anything fills. Balanced: stock all seven, make unfilled Anti-leech lotion and Mixalot orders first, for more resin, split evenly, at a few percent less XP.",
		position = -1
	)
	default Mode mode()
	{
		return Mode.FOCUS_XP;
	}

	@ConfigItem(
		keyName = "windowStyle",
		name = "Window style",
		description = "How much the guide window shows: the next step, a to-do list, or every potion and station",
		position = 0
	)
	default WindowStyle windowStyle()
	{
		return WindowStyle.NEXT_STEP;
	}

	@ConfigItem(
		keyName = "restockThreshold",
		name = "Restock at",
		description = "When the belt locks, restock everything once this much of the stock is missing. Below it, only the potion for the current order is made. Early restocks are faster: each trip to make potions costs the same walk however many you make.",
		position = 1
	)
	@Range(min = 1, max = 100)
	@Units(Units.PERCENT)
	default int restockThreshold()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "holdDigweed",
		name = "Hold digweed",
		description = "Save digweed for an ordered Mixalot. When off, it goes on any Mixalot you hold, or else the best ordered potion.",
		position = 2
	)
	default boolean holdDigweed()
	{
		return false;
	}

	@ConfigItem(
		keyName = "stationGuard",
		name = "Station guard",
		description = "Ignore clicks on an empty station when the next potion is meant for a different one",
		position = 3
	)
	default boolean stationGuard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "leverGuard",
		name = "Lever guard",
		description = "Ignore pulls on a lever that is not the next one the potion needs",
		position = 4
	)
	default boolean leverGuard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pathDisplay",
		name = "Path",
		description = "Where to draw the walking path to the next thing to click",
		position = 6
	)
	default PathDisplay pathDisplay()
	{
		return PathDisplay.FLOOR_AND_MINIMAP;
	}

	@ConfigItem(
		keyName = "beltLock",
		name = "Belt lock",
		description = "Ignore clicks on the conveyor belt while a deposit is not worth making",
		position = 5
	)
	default boolean beltLock()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightColour",
		name = "Highlight",
		description = "Station and vessel highlight",
		section = coloursSection,
		position = 0
	)
	default Color highlightColour()
	{
		return new Color(0xff981f);
	}

	@ConfigItem(
		keyName = "quickActionColour",
		name = "Bonus click colour",
		description = "Station while a bonus click is available",
		section = coloursSection,
		position = 1
	)
	default Color quickActionColour()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "beltOpenColour",
		name = "Belt open",
		description = "Conveyor belt when a deposit is worth making",
		section = coloursSection,
		position = 2
	)
	default Color beltOpenColour()
	{
		return new Color(0x00e676);
	}

	@ConfigItem(
		keyName = "digweedColour",
		name = "Digweed",
		description = "Digweed spot and the potion to use it on",
		section = coloursSection,
		position = 3
	)
	default Color digweedColour()
	{
		return new Color(0x39ff14);
	}
}
