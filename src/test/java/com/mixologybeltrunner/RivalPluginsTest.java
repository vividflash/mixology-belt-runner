package com.mixologybeltrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.List;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

public class RivalPluginsTest
{
	@PluginDescriptor(name = "Mastering Mixology Extended")
	static class ExtendedStub extends Plugin
	{
	}

	@PluginDescriptor(name = "Mastering Mixology")
	static class OriginalStub extends Plugin
	{
	}

	@PluginDescriptor(name = "Some Other Plugin")
	static class OtherStub extends Plugin
	{
	}

	@Test
	public void findsActiveRivalOnly()
	{
		Plugin ext = new ExtendedStub();
		Plugin other = new OtherStub();
		assertEquals("Mastering Mixology Extended", RivalPlugins.activeRival(List.of(other, ext), p -> true));
		assertNull(RivalPlugins.activeRival(List.of(other, ext), p -> p == other));
	}

	@Test
	public void findsTheOriginalPlugin()
	{
		Plugin original = new OriginalStub();
		assertEquals("Mastering Mixology", RivalPlugins.activeRival(List.of(new OtherStub(), original), p -> true));
	}
}
