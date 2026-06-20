package com.arber.skillingtoolkit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.QuantityFormatter;

class SkillingToolkitPanel extends PluginPanel
{
	private static final Color XP_COLOR = new Color(255, 220, 80);
	private static final Color PROFIT_COLOR = new Color(70, 255, 100);
	private static final Color COST_COLOR = new Color(255, 90, 90);
	private static final Color ACTION_COLOR = new Color(80, 225, 255);
	private static final Color MUTED_TEXT = new Color(190, 185, 170);
	private static final Color CARD_BACKGROUND = new Color(34, 34, 34);
	private static final Color TOGGLE_ON_BACKGROUND = new Color(36, 58, 36);
	private static final Color TOGGLE_HOVER_BACKGROUND = new Color(54, 67, 54);
	private static final Color TOGGLE_OFF_HOVER_BACKGROUND = new Color(45, 45, 45);
	private static final Color ON_COLOR = new Color(100, 255, 130);
	private static final Color OFF_COLOR = new Color(255, 110, 110);
	private static final int TOOLTIP_INITIAL_DELAY_MILLIS = 120;
	private static final int TOOLTIP_RESHOW_DELAY_MILLIS = 40;

	private final SkillingToolkitPlugin plugin;
	private final Map<SkillingToolkitStatsPeriod, JButton> periodButtons = new EnumMap<>(SkillingToolkitStatsPeriod.class);
	private final List<QuickToggle> quickToggles = new ArrayList<>();
	private JLabel statProfitValue;
	private JLabel statCostValue;
	private JLabel statNetValue;
	private JLabel statXpValue;
	private JLabel statTopSkillValue;
	private JLabel statActionsValue;
	private JLabel xpValue;
	private JLabel topSkillValue;
	private JLabel profitValue;
	private JLabel costValue;
	private JLabel actionsValue;
	private JLabel statusLabel;
	private SkillingToolkitStatsPeriod selectedPeriod = SkillingToolkitStatsPeriod.DAY;

	SkillingToolkitPanel(SkillingToolkitPlugin plugin)
	{
		this.plugin = plugin;
		ToolTipManager.sharedInstance().setInitialDelay(TOOLTIP_INITIAL_DELAY_MILLIS);
		ToolTipManager.sharedInstance().setReshowDelay(TOOLTIP_RESHOW_DELAY_MILLIS);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(content, BorderLayout.NORTH);

		JLabel title = new JLabel("Skilling Toolkit", SwingConstants.CENTER);
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(CENTER_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(8));

		addStats(content);
		content.add(Box.createVerticalStrut(10));
		addOverview(content);
		content.add(Box.createVerticalStrut(10));
		addQuickControls(content);
		refresh();
	}

	void refresh()
	{
		SwingUtilities.invokeLater(() ->
		{
			for (Map.Entry<SkillingToolkitStatsPeriod, JButton> entry : periodButtons.entrySet())
			{
				boolean selected = entry.getKey() == selectedPeriod;
				entry.getValue().setBackground(selected ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR);
				entry.getValue().setForeground(selected ? Color.BLACK : Color.WHITE);
			}

			SkillingToolkitStats stats = plugin.getStatsSnapshot(selectedPeriod);
			statProfitValue.setText(formatGp(stats.getProfitValue()));
			statCostValue.setText(formatGp(stats.getCostValue()));
			statNetValue.setText(formatSignedGp(stats.getNetValue()));
			statNetValue.setForeground(stats.getNetValue() >= 0 ? PROFIT_COLOR : COST_COLOR);
			statXpValue.setText(formatXp(stats.getXp()));
			statTopSkillValue.setText(formatTopSkill(stats));
			statActionsValue.setText(formatCount(stats.getActions()));

			xpValue.setText(plugin.getQuickXpText());
			topSkillValue.setText(plugin.getQuickTopSkillText());
			profitValue.setText(plugin.getQuickProfitText());
			costValue.setText(plugin.getQuickCostText());
			actionsValue.setText(plugin.getQuickActionsText());
			refreshQuickControls();
		});
	}

	private void addStats(JPanel content)
	{
		content.add(section("Stats"));

		JPanel periodPanel = new JPanel(new GridLayout(0, 2, 4, 4));
		periodPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		periodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
		for (SkillingToolkitStatsPeriod period : SkillingToolkitStatsPeriod.values())
		{
			JButton button = new JButton(period.getDisplayName());
			button.setFocusable(false);
			button.addActionListener(e ->
			{
				selectedPeriod = period;
				refresh();
			});
			periodButtons.put(period, button);
			periodPanel.add(button);
		}
		content.add(periodPanel);
		content.add(Box.createVerticalStrut(8));

		JPanel gpCard = card("GP");
		statProfitValue = addRow(gpCard, "Profit", PROFIT_COLOR);
		statCostValue = addRow(gpCard, "Cost", COST_COLOR);
		statNetValue = addRow(gpCard, "Net", PROFIT_COLOR);
		content.add(gpCard);
		content.add(Box.createVerticalStrut(8));

		JPanel xpCard = card("XP");
		statXpValue = addRow(xpCard, "Total XP", XP_COLOR);
		statTopSkillValue = addRow(xpCard, "Top skill", XP_COLOR);
		statActionsValue = addRow(xpCard, "Actions", ACTION_COLOR);
		content.add(xpCard);
	}

	private void addOverview(JPanel content)
	{
		JPanel card = card("Current trackers");
		xpValue = addRow(card, "XP", XP_COLOR);
		topSkillValue = addRow(card, "Top skill", XP_COLOR);
		profitValue = addRow(card, "Profit", PROFIT_COLOR);
		costValue = addRow(card, "Cost", COST_COLOR);
		actionsValue = addRow(card, "Actions", ACTION_COLOR);
		content.add(card);
	}

	private void addQuickControls(JPanel content)
	{
		content.add(section("Quick options"));

		JPanel toggles = new JPanel(new GridLayout(0, 2, 4, 4));
		toggles.setBackground(ColorScheme.DARK_GRAY_COLOR);
		toggles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));
		addQuickToggle(toggles, "Compost", "<html><b>Compost</b><br>Show or hide compost patch markers.</html>", plugin::isCompostQuickEnabled, plugin::setCompostQuickEnabled);
		addQuickToggle(toggles, "Protection", "<html><b>Protection</b><br>Show or hide gardener payment checks.</html>", plugin::isProtectionQuickEnabled, plugin::setProtectionQuickEnabled);
		addQuickToggle(toggles, "Timers", "<html><b>Timers</b><br>Show or hide Anima patch timers.</html>", plugin::isPatchTimerQuickEnabled, plugin::setPatchTimerQuickEnabled);
		addQuickToggle(toggles, "Birdhouses", "<html><b>Birdhouses</b><br>Show or hide seeded birdhouse checks.</html>", plugin::isBirdhouseQuickEnabled, plugin::setBirdhouseQuickEnabled);
		addQuickToggle(toggles, "Inventory", "<html><b>Inventory</b><br>Show or hide inventory spaces infobox.</html>", plugin::isInventoryQuickEnabled, plugin::setInventoryQuickEnabled);
		addQuickToggle(toggles, "Clock", "<html><b>Clock</b><br>Show or hide the Trade tab clock.</html>", plugin::isClockQuickEnabled, plugin::setClockQuickEnabled);
		addQuickToggle(toggles, "Chat tabs", "<html><b>Chat tabs</b><br>Show or hide all Skilling Toolkit chat-tab trackers.</html>", plugin::isChatTrackersQuickEnabled, plugin::setChatTrackersQuickEnabled);
		content.add(toggles);

		content.add(Box.createVerticalStrut(8));
		JPanel resetRow = new JPanel(new GridLayout(1, 2, 4, 4));
		resetRow.setBackground(ColorScheme.DARK_GRAY_COLOR);
		resetRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		JButton resetTrackers = secondaryButton("Reset trackers");
		resetTrackers.setToolTipText("<html><b>Reset trackers</b><br>Clears XP, profit, cost, and actions.</html>");
		resetTrackers.addActionListener(e ->
		{
			plugin.resetAllTrackersQuick();
			setStatus("Trackers reset");
		});
		JButton resetMarkers = secondaryButton("Reset markers");
		resetMarkers.setToolTipText("<html><b>Reset markers</b><br>Clears compost, protection, birdhouse, and patch timer data.</html>");
		resetMarkers.addActionListener(e ->
		{
			plugin.resetAllMarkersQuick();
			setStatus("Markers reset");
		});
		resetRow.add(resetTrackers);
		resetRow.add(resetMarkers);
		content.add(resetRow);

		statusLabel = new JLabel("Detailed options stay in settings", SwingConstants.CENTER);
		statusLabel.setForeground(MUTED_TEXT);
		statusLabel.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
		content.add(centeredRow(statusLabel));
	}

	private JButton secondaryButton(String text)
	{
		JButton button = new JButton(text);
		button.setFocusable(false);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setForeground(Color.WHITE);
		return button;
	}

	private void addQuickToggle(JPanel parent, String label, String tooltip, BooleanSupplier isEnabled, Consumer<Boolean> setEnabled)
	{
		JButton button = new JButton();
		button.setFocusable(false);
		button.setToolTipText(tooltip);
		QuickToggle toggle = new QuickToggle(label, isEnabled, setEnabled, button);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				button.setBackground(toggle.isEnabled.getAsBoolean() ? TOGGLE_HOVER_BACKGROUND : TOGGLE_OFF_HOVER_BACKGROUND);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				refreshQuickControls();
			}
		});
		button.addActionListener(e ->
		{
			boolean enabled = !toggle.isEnabled.getAsBoolean();
			toggle.setEnabled.accept(enabled);
			setStatus(label + (enabled ? " on" : " off"));
			refresh();
		});
		quickToggles.add(toggle);
		parent.add(button);
	}

	private void refreshQuickControls()
	{
		for (QuickToggle toggle : quickToggles)
		{
			boolean enabled = toggle.isEnabled.getAsBoolean();
			toggle.button.setText("<html><center>" + toggle.label + "<br>" + (enabled ? "On" : "Off") + "</center></html>");
			toggle.button.setForeground(enabled ? ON_COLOR : OFF_COLOR);
			toggle.button.setBackground(enabled ? TOGGLE_ON_BACKGROUND : ColorScheme.DARKER_GRAY_COLOR);
		}
	}

	private void setStatus(String text)
	{
		if (statusLabel != null)
		{
			statusLabel.setText(text);
		}
	}

	private JPanel card(String title)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD_BACKGROUND);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(6, 7, 7, 7)
		));
		card.setAlignmentX(CENTER_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		card.add(section(title, CARD_BACKGROUND));
		return card;
	}

	private JPanel section(String text)
	{
		return section(text, ColorScheme.DARK_GRAY_COLOR);
	}

	private JPanel section(String text, Color background)
	{
		JLabel label = new JLabel(text, SwingConstants.CENTER);
		label.setForeground(ColorScheme.BRAND_ORANGE);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setBorder(BorderFactory.createEmptyBorder(7, 0, 3, 0));
		return centeredRow(label, background);
	}

	private JLabel addRow(JPanel parent, String labelText, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(parent.getBackground());
		row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

		JLabel label = new JLabel(labelText);
		label.setForeground(ColorScheme.TEXT_COLOR);
		JLabel value = new JLabel("0", SwingConstants.RIGHT);
		value.setForeground(valueColor);
		value.setFont(FontManager.getDefaultBoldFont());

		row.add(label, BorderLayout.WEST);
		row.add(value, BorderLayout.EAST);
		parent.add(row);
		return value;
	}

	private JPanel centeredRow(JLabel label)
	{
		return centeredRow(label, ColorScheme.DARK_GRAY_COLOR);
	}

	private JPanel centeredRow(JLabel label, Color background)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(background);
		row.setAlignmentX(CENTER_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height + 8));
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	private String formatTopSkill(SkillingToolkitStats stats)
	{
		Skill skill = stats.getTopSkill();
		if (skill == null)
		{
			return "-";
		}

		return formatSkillName(skill) + " " + QuantityFormatter.quantityToStackSize(stats.getTopSkillXp()).toLowerCase(Locale.ENGLISH);
	}

	private String formatSkillName(Skill skill)
	{
		String name = skill.getName();
		if (name == null || name.isBlank())
		{
			name = skill.name().toLowerCase(Locale.ENGLISH);
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}

		return name;
	}

	private String formatGp(long value)
	{
		return QuantityFormatter.quantityToStackSize(value).toLowerCase(Locale.ENGLISH) + " gp";
	}

	private String formatSignedGp(long value)
	{
		String prefix = value > 0 ? "+" : value < 0 ? "-" : "";
		return prefix + QuantityFormatter.quantityToStackSize(Math.abs(value)).toLowerCase(Locale.ENGLISH) + " gp";
	}

	private String formatXp(long xp)
	{
		return QuantityFormatter.quantityToStackSize(xp).toLowerCase(Locale.ENGLISH) + " xp";
	}

	private String formatCount(long count)
	{
		return QuantityFormatter.quantityToStackSize(count).toLowerCase(Locale.ENGLISH);
	}

	private static final class QuickToggle
	{
		private final String label;
		private final BooleanSupplier isEnabled;
		private final Consumer<Boolean> setEnabled;
		private final JButton button;

		private QuickToggle(String label, BooleanSupplier isEnabled, Consumer<Boolean> setEnabled, JButton button)
		{
			this.label = label;
			this.isEnabled = isEnabled;
			this.setEnabled = setEnabled;
			this.button = button;
		}
	}
}
