package com.arber.skillingtoolkit;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;

public class SkillingToolkitPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(getBuiltinPlugins());
		RuneLite.main(args);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Class<? extends Plugin>[] getBuiltinPlugins() throws Exception
	{
		try
		{
			Class<? extends Plugin> pvmToolkitClass = (Class) Class.forName("com.arber.prayerpottimer.CombatPotionTimersPlugin");
			return new Class[] { SkillingToolkitPlugin.class, pvmToolkitClass };
		}
		catch (ClassNotFoundException ignored)
		{
			return new Class[] { SkillingToolkitPlugin.class };
		}
	}
}
