package com.mixologybeltrunner;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BeltRunnerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BeltRunnerPlugin.class);
		RuneLite.main(args);
	}
}
