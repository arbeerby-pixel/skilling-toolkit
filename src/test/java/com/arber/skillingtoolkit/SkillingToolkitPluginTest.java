package com.arber.skillingtoolkit;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SkillingToolkitPluginTest
{
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SkillingToolkitPlugin.class);
		RuneLite.main(args);
	}
}
