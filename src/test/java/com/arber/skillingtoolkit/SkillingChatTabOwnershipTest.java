package com.arber.skillingtoolkit;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SkillingChatTabOwnershipTest
{
	@Test
	public void eitherPluginDisabledRestoresNativeChatTabs()
	{
		assertTrue(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("true", "false"));
		assertTrue(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("false", "true"));
		assertTrue(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("true", "true"));
		assertFalse(SkillingToolkitPlugin.shouldSuppressSharedChatTabs("false", "false"));
		assertFalse(SkillingToolkitPlugin.shouldSuppressSharedChatTabs(null, null));
	}
}
