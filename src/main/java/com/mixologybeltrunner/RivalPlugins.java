package com.mixologybeltrunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import net.runelite.client.plugins.Plugin;

/**
 * Finds the other mixology plugins Belt Runner steps aside for.
 */
public final class RivalPlugins
{
	static final List<String> NAMES = Arrays.asList("Mastering Mixology", "Mastering Mixology Extended");

	private RivalPlugins()
	{
	}

	/**
	 * The name of the first active rival, or null.
	 */
	public static String activeRival(Collection<Plugin> plugins, Predicate<Plugin> isActive)
	{
		for (Plugin plugin : plugins)
		{
			String name = plugin.getName();
			if (NAMES.contains(name) && isActive.test(plugin))
			{
				return name;
			}
		}
		return null;
	}
}
