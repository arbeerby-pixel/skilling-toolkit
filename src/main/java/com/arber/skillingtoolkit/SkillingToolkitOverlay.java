package com.arber.skillingtoolkit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

class SkillingToolkitOverlay extends Overlay
{
	private static final int PROTECTION_CHECK_SIZE = 28;
	private static final int PROTECTION_CHECK_SHARED_PATCH_Z_OFFSET = 50;
	private static final int PROTECTION_CHECK_SOLO_Z_OFFSET = 42;
	private static final int PROTECTION_CHECK_SHARED_PATCH_X_OFFSET = 24;
	private static final int PROTECTION_CHECK_SHARED_PATCH_Y_OFFSET = -4;
	private static final int PATCH_TIMER_Z_OFFSET = 76;
	private static final int PATCH_TIMER_BOX_HORIZONTAL_PADDING = 5;
	private static final int PATCH_TIMER_BOX_VERTICAL_PADDING = 2;
	private static final int PATCH_TIMER_BOX_RADIUS = 5;
	private static final Color PROTECTION_CHECK_COLOR = new Color(60, 255, 90);
	private static final Color PROTECTION_CHECK_SHADOW = new Color(0, 0, 0, 190);
	private static final Color PATCH_TIMER_COLOR = new Color(255, 220, 80);
	private static final Color PATCH_TIMER_BACKGROUND = new Color(24, 20, 15, 180);
	private static final Color PATCH_TIMER_BORDER = new Color(255, 220, 80, 140);

	private final Client client;
	private final SkillingToolkitConfig config;
	private final SkillingToolkitPlugin plugin;
	private final ItemManager itemManager;
	private BufferedImage compostIcon;
	private BufferedImage needsCompostIcon;
	private BufferedImage protectionCheckIcon;

	@Inject
	private SkillingToolkitOverlay(Client client, SkillingToolkitConfig config, SkillingToolkitPlugin plugin, ItemManager itemManager)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.itemManager = itemManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showCompostIndicator()
			&& !config.showCropProtectionIndicator()
			&& !config.showBirdhouseSeedIndicator()
			&& !config.showPatchTimers())
		{
			return null;
		}

		if (config.showPatchTimers())
		{
			for (PatchTimer timer : plugin.getPatchTimers())
			{
				renderPatchTimer(graphics, timer);
			}
		}

		if (config.showCompostIndicator())
		{
			if (config.showNeedsCompostIcon())
			{
				for (WorldPoint patchPoint : plugin.getNeedsCompostPatches())
				{
					if (!plugin.getCompostedPatches().containsKey(patchPoint))
					{
						renderCompostIcon(graphics, patchPoint, getNeedsCompostIcon());
					}
				}
			}

			for (Map.Entry<WorldPoint, CompostPatch> entry : plugin.getCompostedPatches().entrySet())
			{
				renderPatch(graphics, entry.getValue());
			}
		}

		if (config.showCropProtectionIndicator())
		{
			for (WorldPoint patchPoint : plugin.getProtectedPatches().keySet())
			{
				renderProtectedPatch(graphics, patchPoint);
			}
		}

		if (config.showBirdhouseSeedIndicator())
		{
			for (WorldPoint birdhousePoint : plugin.getSeededBirdhouses())
			{
				renderBirdhouseSeeded(graphics, birdhousePoint);
			}
		}

		return null;
	}

	private void renderPatch(Graphics2D graphics, CompostPatch patch)
	{
		renderCompostIcon(graphics, patch.getWorldPoint(), getCompostIcon());
	}

	private void renderCompostIcon(Graphics2D graphics, WorldPoint worldPoint, BufferedImage icon)
	{
		if (!config.showCompostIcon() || icon == null)
		{
			return;
		}

		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint);
		if (localPoint == null)
		{
			return;
		}

		OverlayUtil.renderImageLocation(client, graphics, localPoint, icon, worldPoint.getPlane());
	}

	private void renderProtectedPatch(Graphics2D graphics, WorldPoint patchPoint)
	{
		if (!plugin.shouldRenderProtectedPatch(patchPoint))
		{
			return;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(client, patchPoint);
		if (localPoint == null)
		{
			return;
		}

		BufferedImage icon = getProtectionCheckIcon();
		boolean hasCompostMarker = plugin.getCompostedPatches().containsKey(patchPoint)
			|| plugin.getNeedsCompostPatches().contains(patchPoint);
		boolean useSharedPatchOffset = hasCompostMarker || plugin.isAllotmentPatch(patchPoint);
		int zOffset = useSharedPatchOffset
			? PROTECTION_CHECK_SHARED_PATCH_Z_OFFSET
			: PROTECTION_CHECK_SOLO_Z_OFFSET;
		Point iconLocation = Perspective.getCanvasImageLocation(client, localPoint, icon, zOffset);
		if (iconLocation != null)
		{
			if (useSharedPatchOffset)
			{
				iconLocation = new Point(
					iconLocation.getX() + PROTECTION_CHECK_SHARED_PATCH_X_OFFSET,
					iconLocation.getY() + PROTECTION_CHECK_SHARED_PATCH_Y_OFFSET
				);
			}

			OverlayUtil.renderImageLocation(graphics, iconLocation, icon);
		}
	}

	private void renderPatchTimer(Graphics2D graphics, PatchTimer timer)
	{
		String text = timer.formatRemaining(System.currentTimeMillis());
		if (text.isEmpty())
		{
			return;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(client, timer.getWorldPoint());
		if (localPoint == null)
		{
			return;
		}

		Font previousFont = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeFont());
		Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, text, PATCH_TIMER_Z_OFFSET);
		if (textLocation != null)
		{
			renderTimerBox(graphics, textLocation, text);
		}
		graphics.setFont(previousFont);
	}

	private void renderTimerBox(Graphics2D graphics, Point textLocation, String text)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int textWidth = metrics.stringWidth(text);
		int textHeight = metrics.getAscent();
		int boxWidth = textWidth + PATCH_TIMER_BOX_HORIZONTAL_PADDING * 2;
		int boxHeight = textHeight + PATCH_TIMER_BOX_VERTICAL_PADDING * 2;
		int boxX = textLocation.getX() - PATCH_TIMER_BOX_HORIZONTAL_PADDING;
		int boxY = textLocation.getY() - metrics.getAscent() - PATCH_TIMER_BOX_VERTICAL_PADDING + 1;

		Object previousAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		RoundRectangle2D box = new RoundRectangle2D.Float(
			boxX,
			boxY,
			boxWidth,
			boxHeight,
			PATCH_TIMER_BOX_RADIUS,
			PATCH_TIMER_BOX_RADIUS
		);
		graphics.setColor(PATCH_TIMER_BACKGROUND);
		graphics.fill(box);
		graphics.setColor(PATCH_TIMER_BORDER);
		graphics.draw(box);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAntialiasing);

		OverlayUtil.renderTextLocation(graphics, textLocation, text, PATCH_TIMER_COLOR);
	}

	private void renderBirdhouseSeeded(Graphics2D graphics, WorldPoint birdhousePoint)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client, birdhousePoint);
		if (localPoint == null)
		{
			return;
		}

		BufferedImage icon = getProtectionCheckIcon();
		Point iconLocation = Perspective.getCanvasImageLocation(client, localPoint, icon, PROTECTION_CHECK_SOLO_Z_OFFSET);
		if (iconLocation != null)
		{
			OverlayUtil.renderImageLocation(graphics, iconLocation, icon);
		}
	}

	private BufferedImage getCompostIcon()
	{
		if (compostIcon == null)
		{
			compostIcon = itemManager.getImage(ItemID.BOTTOMLESS_COMPOST_BUCKET_22997);
		}

		return compostIcon;
	}

	private BufferedImage getNeedsCompostIcon()
	{
		if (needsCompostIcon == null)
		{
			BufferedImage icon = getCompostIcon();
			if (icon != null)
			{
				needsCompostIcon = createGrayscaleImage(icon);
			}
		}

		return needsCompostIcon;
	}

	private BufferedImage getProtectionCheckIcon()
	{
		if (protectionCheckIcon == null)
		{
			protectionCheckIcon = createProtectionCheckIcon();
		}

		return protectionCheckIcon;
	}

	private BufferedImage createGrayscaleImage(BufferedImage source)
	{
		BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < source.getHeight(); y++)
		{
			for (int x = 0; x < source.getWidth(); x++)
			{
				int argb = source.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xFF;
				int red = (argb >>> 16) & 0xFF;
				int green = (argb >>> 8) & 0xFF;
				int blue = argb & 0xFF;
				int gray = (red * 30 + green * 59 + blue * 11) / 100;
				image.setRGB(x, y, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
			}
		}

		return image;
	}

	private BufferedImage createProtectionCheckIcon()
	{
		BufferedImage image = new BufferedImage(PROTECTION_CHECK_SIZE, PROTECTION_CHECK_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.setColor(PROTECTION_CHECK_SHADOW);
			graphics.drawLine(6, 15, 12, 21);
			graphics.drawLine(12, 21, 23, 7);

			graphics.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.setColor(PROTECTION_CHECK_COLOR);
			graphics.drawLine(6, 15, 12, 21);
			graphics.drawLine(12, 21, 23, 7);
		}
		finally
		{
			graphics.dispose();
		}

		return image;
	}
}
