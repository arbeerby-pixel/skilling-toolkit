package com.arber.skillingtoolkit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.QuantityFormatter;

class HerbSackOverlay extends WidgetItemOverlay
{
	private static final Color COUNT_COLOR = new Color(255, 255, 0);

	private final Client client;
	private final SkillingToolkitConfig config;
	private final SkillingToolkitPlugin plugin;
	private final TooltipManager tooltipManager;

	@Inject
	private HerbSackOverlay(Client client, SkillingToolkitConfig config, SkillingToolkitPlugin plugin, TooltipManager tooltipManager)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.tooltipManager = tooltipManager;
		showOnInventory();
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.showHerbSackViewer() || !SkillingToolkitPlugin.isHerbSackItem(itemId))
		{
			return;
		}

		HerbSackContents contents = plugin.getHerbSackContents();
		if (contents.isLoaded() && contents.getTotalQuantity() > 0)
		{
			renderTotalCount(graphics, widgetItem, contents.getTotalQuantity());
		}

		if (isMouseOver(widgetItem))
		{
			tooltipManager.addFront(new Tooltip(buildTooltip(contents)));
		}
	}

	private void renderTotalCount(Graphics2D graphics, WidgetItem widgetItem, int totalQuantity)
	{
		Rectangle bounds = widgetItem.getCanvasBounds();
		String text = QuantityFormatter.quantityToStackSize(totalQuantity).toLowerCase();
		java.awt.Font previousFont = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeBoldFont());
		OverlayUtil.renderTextLocation(
			graphics,
			new Point(bounds.x + 2, bounds.y + bounds.height - 3),
			text,
			COUNT_COLOR
		);
		graphics.setFont(previousFont);
	}

	private boolean isMouseOver(WidgetItem widgetItem)
	{
		Point mouse = client.getMouseCanvasPosition();
		return mouse != null && widgetItem.getCanvasBounds().contains(mouse.getX(), mouse.getY());
	}

	private String buildTooltip(HerbSackContents contents)
	{
		if (!contents.isLoaded())
		{
			return "Herb sack<br>Use Check to load contents";
		}

		if (contents.isEmpty())
		{
			return "Herb sack<br>Empty<br>Total value: 0 gp";
		}

		long totalValue = 0L;
		StringBuilder tooltip = new StringBuilder("Herb sack");
		for (Map.Entry<HerbSackHerb, Integer> entry : contents.getQuantities().entrySet())
		{
			HerbSackHerb herb = entry.getKey();
			int quantity = entry.getValue();
			long value = plugin.getItemValueForDisplay(herb.getItemId(), quantity);
			totalValue += value;
			tooltip.append("<br>")
				.append(herb.getDisplayName())
				.append(": ")
				.append(quantity)
				.append(" - ")
				.append(formatGp(value));
		}

		tooltip.append("<br>Total herbs: ")
			.append(QuantityFormatter.quantityToStackSize(contents.getTotalQuantity()).toLowerCase())
			.append("<br>Total value: ")
			.append(formatGp(totalValue))
			.append("<br>Use Check to refresh");
		return tooltip.toString();
	}

	private static String formatGp(long value)
	{
		return QuantityFormatter.quantityToStackSize(value).toLowerCase() + " gp";
	}
}
