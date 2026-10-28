package com.arber.skillingtoolkit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Player;
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
	private static final int COMPOST_MARKER_MERGE_DISTANCE = 5;
	private static final Color PROTECTION_CHECK_COLOR = new Color(60, 255, 90);
	private static final Color PROTECTION_CHECK_SHADOW = new Color(0, 0, 0, 190);
	private static final Color PATCH_TIMER_COLOR = new Color(255, 220, 80);
	private static final Color PATCH_TIMER_BACKGROUND = new Color(24, 20, 15, 180);
	private static final Color PATCH_TIMER_BORDER = new Color(255, 220, 80, 140);
	private static final Color IMPLING_ALERT_COLOR = new Color(255, 55, 55);
	private static final Color IMPLING_PATH_COLOR = new Color(255, 200, 65, 210);
	private static final Color IMPLING_PATH_SHADOW = new Color(0, 0, 0, 190);
	private static final Color IMPLING_LAST_SEEN_TEXT = new Color(210, 210, 210, 180);
	private static final Color IMPLING_LAST_SEEN_FILL = new Color(255, 200, 65, 35);
	private static final Color IMPLING_LAST_SEEN_OUTLINE = new Color(255, 200, 65, 130);
	private static final Color IMPLING_BOX_BACKGROUND = new Color(18, 18, 18, 205);
	private static final Color IMPLING_BOX_BORDER = new Color(190, 135, 35, 220);
	private static final Color IMPLING_BOX_TITLE = new Color(255, 170, 0);
	private static final Color IMPLING_BOX_TEXT = new Color(240, 235, 220);
	private static final int IMPLING_BOX_X = 8;
	private static final int IMPLING_BOX_Y = 44;
	private static final int IMPLING_BOX_PADDING = 7;
	private static final int IMPLING_BOX_ROW_HEIGHT = 15;
	private static final int IMPLING_BOX_SWATCH_SIZE = 8;
	private static final long IMPLING_PULSE_MILLIS = 450L;

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
			&& !config.showPatchTimers()
			&& !config.showImplingScanner())
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

		if (config.showCompostIndicator() && plugin.shouldRenderOwnCompostIndicators())
		{
			Set<WorldPoint> renderedCompostMarkers = new HashSet<>();
			for (Map.Entry<WorldPoint, CompostPatch> entry : plugin.getCompostedPatches().entrySet())
			{
				WorldPoint patchPoint = entry.getKey();
				if (!hasNearbyMarker(renderedCompostMarkers, patchPoint))
				{
					renderPatch(graphics, entry.getValue());
					renderedCompostMarkers.add(patchPoint);
				}
			}

			if (config.showNeedsCompostIcon())
			{
				for (WorldPoint patchPoint : plugin.getNeedsCompostPatches())
				{
					if (!hasNearbyMarker(renderedCompostMarkers, patchPoint))
					{
						renderCompostIcon(graphics, patchPoint, getNeedsCompostIcon());
						renderedCompostMarkers.add(patchPoint);
					}
				}
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

		if (config.showImplingScanner())
		{
			renderImplingScanner(graphics);
		}

		return null;
	}

	static boolean hasNearbyMarker(Set<WorldPoint> renderedMarkers, WorldPoint candidate)
	{
		for (WorldPoint marker : renderedMarkers)
		{
			if (marker.getPlane() == candidate.getPlane()
				&& marker.distanceTo(candidate) <= COMPOST_MARKER_MERGE_DISTANCE)
			{
				return true;
			}
		}

		return false;
	}

	private void renderPatch(Graphics2D graphics, CompostPatch patch)
	{
		if (config.showCompostIcon())
		{
			renderCompostIcon(graphics, patch.getWorldPoint(), getCompostIcon());
		}
	}

	private void renderCompostIcon(Graphics2D graphics, WorldPoint worldPoint, BufferedImage icon)
	{
		if (icon == null)
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

	private void renderImplingScanner(Graphics2D graphics)
	{
		List<TrackedImpling> implings = plugin.getTrackedImplings();
		if (implings.isEmpty())
		{
			return;
		}

		TrackedImpling nearest = getNearestImpling(implings);
		if (config.showImplingPath() && nearest != null)
		{
			renderImplingPath(graphics, nearest);
		}

		for (TrackedImpling impling : implings)
		{
			renderImplingMinimap(graphics, impling);
			renderImpling(graphics, impling);
		}

		if (config.showImplingFinderBox())
		{
			renderImplingFinderBox(graphics, implings);
		}
	}

	private TrackedImpling getNearestImpling(List<TrackedImpling> implings)
	{
		for (TrackedImpling impling : implings)
		{
			if (impling.isNearest())
			{
				return impling;
			}
		}

		return null;
	}

	private void renderImpling(Graphics2D graphics, TrackedImpling impling)
	{
		if (!impling.isLive())
		{
			renderLastSeenImpling(graphics, impling);
			return;
		}

		Color color = impling.getType().getColor();
		if (impling.isNearest() && config.flashImplingTarget() && isImplingPulseVisible())
		{
			color = IMPLING_ALERT_COLOR;
		}

		String label = impling.getType().getDisplayName();
		if (impling.isNearest())
		{
			label += " " + impling.getDistance();
		}

		OverlayUtil.renderActorOverlay(graphics, impling.getNpc(), label, color);
	}

	private void renderLastSeenImpling(Graphics2D graphics, TrackedImpling impling)
	{
		LocalPoint localPoint = LocalPoint.fromWorld(client, impling.getWorldPoint());
		if (localPoint == null)
		{
			return;
		}

		java.awt.Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
		if (tilePoly != null)
		{
			OverlayUtil.renderPolygon(graphics, tilePoly, IMPLING_LAST_SEEN_OUTLINE, IMPLING_LAST_SEEN_FILL, new BasicStroke(1));
		}

		String label = impling.getType().getDisplayName() + " last";
		Point textLocation = Perspective.getCanvasTextLocation(client, graphics, localPoint, label, 0);
		if (textLocation != null)
		{
			OverlayUtil.renderTextLocation(graphics, textLocation, label, IMPLING_LAST_SEEN_TEXT);
		}
	}

	private void renderImplingMinimap(Graphics2D graphics, TrackedImpling impling)
	{
		Point minimapLocation = null;
		if (impling.isLive() && impling.getNpc() != null)
		{
			minimapLocation = impling.getNpc().getMinimapLocation();
		}
		else
		{
			LocalPoint localPoint = LocalPoint.fromWorld(client, impling.getWorldPoint());
			if (localPoint != null)
			{
				minimapLocation = Perspective.localToMinimap(client, localPoint);
			}
		}

		if (minimapLocation == null)
		{
			return;
		}

		Color color = impling.isLive() ? impling.getType().getColor() : IMPLING_LAST_SEEN_OUTLINE;
		if (impling.isNearest() && impling.isLive() && config.flashImplingTarget() && isImplingPulseVisible())
		{
			color = IMPLING_ALERT_COLOR;
		}

		OverlayUtil.renderMinimapLocation(graphics, minimapLocation, color);
	}

	private void renderImplingPath(Graphics2D graphics, TrackedImpling impling)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}

		LocalPoint playerPoint = player.getLocalLocation();
		LocalPoint implingPoint = impling.isLive() && impling.getNpc() != null
			? impling.getNpc().getLocalLocation()
			: LocalPoint.fromWorld(client, impling.getWorldPoint());
		if (playerPoint == null || implingPoint == null)
		{
			return;
		}

		Point from = Perspective.localToCanvas(client, playerPoint, client.getPlane());
		Point to = Perspective.localToCanvas(client, implingPoint, impling.getWorldPoint().getPlane());
		if (from == null || to == null)
		{
			return;
		}

		Stroke previousStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(IMPLING_PATH_SHADOW);
		graphics.drawLine(from.getX(), from.getY(), to.getX(), to.getY());
		graphics.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.setColor(IMPLING_PATH_COLOR);
		graphics.drawLine(from.getX(), from.getY(), to.getX(), to.getY());
		graphics.fillOval(to.getX() - 4, to.getY() - 4, 8, 8);
		graphics.setStroke(previousStroke);
	}

	private void renderImplingFinderBox(Graphics2D graphics, List<TrackedImpling> implings)
	{
		Map<ImplingType, int[]> rows = new EnumMap<>(ImplingType.class);
		for (TrackedImpling impling : implings)
		{
			int[] values = rows.computeIfAbsent(impling.getType(), ignored -> new int[] {0, 0, Integer.MAX_VALUE});
			if (impling.isLive())
			{
				values[0]++;
			}
			else
			{
				values[1]++;
			}
			values[2] = Math.min(values[2], impling.getDistance());
		}

		Font previousFont = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeFont());
		FontMetrics metrics = graphics.getFontMetrics();
		int titleWidth = metrics.stringWidth("Implings nearby");
		int width = titleWidth + IMPLING_BOX_PADDING * 2;
		for (Map.Entry<ImplingType, int[]> entry : rows.entrySet())
		{
			String rowText = formatImplingBoxRow(entry.getKey(), entry.getValue());
			width = Math.max(width, metrics.stringWidth(rowText) + IMPLING_BOX_PADDING * 2 + IMPLING_BOX_SWATCH_SIZE + 5);
		}

		int height = IMPLING_BOX_PADDING * 2 + metrics.getAscent() + 4 + rows.size() * IMPLING_BOX_ROW_HEIGHT;
		graphics.setColor(IMPLING_BOX_BACKGROUND);
		graphics.fillRoundRect(IMPLING_BOX_X, IMPLING_BOX_Y, width, height, 6, 6);
		graphics.setColor(IMPLING_BOX_BORDER);
		graphics.drawRoundRect(IMPLING_BOX_X, IMPLING_BOX_Y, width, height, 6, 6);

		int textX = IMPLING_BOX_X + IMPLING_BOX_PADDING;
		int textY = IMPLING_BOX_Y + IMPLING_BOX_PADDING + metrics.getAscent();
		drawShadowedText(graphics, "Implings nearby", textX, textY, IMPLING_BOX_TITLE);
		textY += IMPLING_BOX_ROW_HEIGHT + 2;

		for (Map.Entry<ImplingType, int[]> entry : rows.entrySet())
		{
			ImplingType type = entry.getKey();
			graphics.setColor(type.getColor());
			graphics.fillRect(textX, textY - metrics.getAscent() + 3, IMPLING_BOX_SWATCH_SIZE, IMPLING_BOX_SWATCH_SIZE);
			drawShadowedText(
				graphics,
				formatImplingBoxRow(type, entry.getValue()),
				textX + IMPLING_BOX_SWATCH_SIZE + 5,
				textY,
				IMPLING_BOX_TEXT);
			textY += IMPLING_BOX_ROW_HEIGHT;
		}

		graphics.setFont(previousFont);
	}

	private String formatImplingBoxRow(ImplingType type, int[] values)
	{
		String state = values[0] > 0
			? values[0] + " live"
			: values[1] + " seen";
		if (values[0] > 0 && values[1] > 0)
		{
			state += " +" + values[1];
		}

		return type.getDisplayName() + " " + state + " - " + values[2] + "t";
	}

	private void drawShadowedText(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	private boolean isImplingPulseVisible()
	{
		return (System.currentTimeMillis() / IMPLING_PULSE_MILLIS) % 2L == 0L;
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
