package com.arber.skillingtoolkit;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SkillingChatTabOwnershipTest
{
	@Test
	public void staleSkillingFlagDoesNotSuppressSharedTabs()
	{
		assertTrue(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("true", "false"));
		assertTrue(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("true", "true"));
		assertFalse(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("false", "false"));
		assertFalse(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("false", "true"));
		assertFalse(SkillingToolkitPlugin.shouldSuppressSharedChatTabs(null, null));
	}

	@Test
	public void explicitPvmTabChoicesHavePriorityOverAutomaticAndVanilla()
	{
		assertFalse(SkillingToolkitPlugin.hasExplicitPvmChatTabSelection(
			"AUTOMATIC", "VANILLA", null));
		assertTrue(SkillingToolkitPlugin.hasExplicitPvmChatTabSelection(
			"AUTOMATIC", "SLAYER_XP", "VANILLA"));
	}
}
