package com.arber.skillingtoolkit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.function.LongSupplier;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

class RunCountdownInfoBox extends InfoBox
{
	private static final Color READY_COLOR = new Color(90, 255, 120);
	private static final Color TIMER_COLOR = new Color(255, 220, 80);
	private static final long MILLIS_PER_SECOND = 1_000L;
	private static final long SECONDS_PER_MINUTE = 60L;
	private static final long SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE;

	private final String name;
	private final String label;
	private final LongSupplier readyAtMillisSupplier;

	RunCountdownInfoBox(String name, String label, BufferedImage image, Plugin plugin, LongSupplier readyAtMillisSupplier)
	{
		super(image, plugin);
		this.name = name;
		this.label = label;
		this.readyAtMillisSupplier = readyAtMillisSupplier;
		setPriority(InfoBoxPriority.MED);
	}

	@Override
	public String getText()
	{
		return formatCountdown(readyAtMillisSupplier.getAsLong(), System.currentTimeMillis());
	}

	@Override
	public boolean render()
	{
		return readyAtMillisSupplier.getAsLong() > 0L;
	}

	@Override
	public boolean cull()
	{
		return readyAtMillisSupplier.getAsLong() <= 0L;
	}

	@Override
	public Color getTextColor()
	{
		long readyAtMillis = readyAtMillisSupplier.getAsLong();
		return readyAtMillis > 0L && readyAtMillis <= System.currentTimeMillis()
			? READY_COLOR
			: TIMER_COLOR;
	}

	@Override
	public String getName()
	{
		return name;
	}

	String buildTooltip()
	{
		return label + ": " + getText();
	}

	static String formatCountdown(long readyAtMillis, long nowMillis)
	{
		long remainingMillis = readyAtMillis - nowMillis;
		if (readyAtMillis <= 0L)
		{
			return "-";
		}

		if (remainingMillis <= 0L)
		{
			return "Ready";
		}

		long totalSeconds = Math.max(1L, (remainingMillis + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND);
		if (totalSeconds >= SECONDS_PER_HOUR)
		{
			long hours = totalSeconds / SECONDS_PER_HOUR;
			long minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
			return hours + ":" + String.format("%02d", minutes);
		}

		long minutes = totalSeconds / SECONDS_PER_MINUTE;
		long seconds = totalSeconds % SECONDS_PER_MINUTE;
		return minutes + ":" + String.format("%02d", seconds);
	}
}
