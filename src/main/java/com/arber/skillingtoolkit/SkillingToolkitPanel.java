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
import javax.swing.JComboBox;
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
	private static final PanelSelection OVERVIEW_SELECTION = new PanelSelection("Overview", null);

	private final SkillingToolkitPlugin plugin;
	private final Map<SkillingToolkitStatsPeriod, JButton> periodButtons = new EnumMap<>(SkillingToolkitStatsPeriod.class);
	private final Map<FarmingActivityType, FarmingStatsLabels> farmingStatLabels = new EnumMap<>(FarmingActivityType.class);
	private final List<QuickToggle> quickToggles = new ArrayList<>();
	private final PanelSelection[] panelSelections = createPanelSelections();
	private JPanel dynamicContent;
	private PanelSelection selectedPanel = OVERVIEW_SELECTION;
	private JLabel statProfitValue;
	private JLabel statCostValue;
	private JButton statResetGpButton;
	private JLabel statXpValue;
	private JLabel statTopSkillValue;
	private JLabel statActiveSkillsValue;
	private JLabel statActionsValue;
	private JLabel statBirdhouseRunsValue;
	private JLabel statBirdhouseXpValue;
	private JLabel statBirdhouseNestsValue;
	private JLabel statBirdhouseNestValue;
	private JLabel statBirdhouseAvgNestsValue;
	private JLabel statBirdhouseAvgValueValue;
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
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setAlignmentX(CENTER_ALIGNMENT);
		content.add(title);
		content.add(Box.createVerticalStrut(24));

		addPanelSelector(content);
		content.add(Box.createVerticalStrut(14));

		dynamicContent = new JPanel();
		dynamicContent.setLayout(new BoxLayout(dynamicContent, BoxLayout.Y_AXIS));
		dynamicContent.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.add(dynamicContent);
		rebuildSelectedPanel();
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
			if (statProfitValue != null)
			{
				statProfitValue.setText(formatGp(stats.getProfitValue()));
			}
			if (statCostValue != null)
			{
				statCostValue.setText(formatGp(stats.getCostValue()));
			}
			if (statResetGpButton != null)
			{
				statResetGpButton.setText("Reset " + selectedPeriod.getDisplayName());
				statResetGpButton.setToolTipText("<html><b>Reset " + selectedPeriod.getDisplayName() + " GP</b><br>Sets Profit and Cost to 0 for this selected period only.</html>");
			}
			if (statXpValue != null)
			{
				statXpValue.setText(formatXp(selectedPanel.skill == null ? stats.getXp() : stats.getXp(selectedPanel.skill)));
			}
			if (statTopSkillValue != null)
			{
				statTopSkillValue.setText(formatTopSkill(stats));
			}
			if (statActiveSkillsValue != null)
			{
				statActiveSkillsValue.setText(formatCount(stats.getActiveSkillCount()));
			}
			if (statActionsValue != null)
			{
				statActionsValue.setText(formatCount(stats.getActions()));
			}
			if (statBirdhouseRunsValue != null)
			{
				statBirdhouseRunsValue.setText(formatCount(stats.getBirdhouseRuns()));
				statBirdhouseXpValue.setText(formatXp(stats.getBirdhouseXp()));
				statBirdhouseNestsValue.setText(formatCount(stats.getBirdhouseNests()));
				statBirdhouseNestValue.setText(formatGp(stats.getBirdhouseNestValue()));
				statBirdhouseAvgNestsValue.setText(formatAverageCount(stats.getBirdhouseNests(), stats.getBirdhouseRuns()));
				statBirdhouseAvgValueValue.setText(formatAverageGp(stats.getBirdhouseNestValue(), stats.getBirdhouseRuns()));
			}
			for (Map.Entry<FarmingActivityType, FarmingStatsLabels> farmingEntry : farmingStatLabels.entrySet())
			{
				FarmingActivityStats farmingStats = stats.getFarmingActivityStats(farmingEntry.getKey());
				FarmingStatsLabels labels = farmingEntry.getValue();
				labels.runs.setText(formatCount(farmingStats.getRuns()));
				labels.seeds.setText(formatCount(farmingStats.getSeedsUsed()));
				labels.seedCost.setText(formatGp(farmingStats.getSeedCost()));
				labels.harvested.setText(formatCount(farmingStats.getHarvested()));
				labels.harvestValue.setText(formatGp(farmingStats.getHarvestValue()));
				labels.avgHarvested.setText(formatAverageCount(farmingStats.getHarvested(), farmingStats.getRuns()));
				if (labels.profit != null)
				{
					labels.profit.setText(formatProfitGp(farmingStats.getProfit()));
					labels.profit.setForeground(farmingStats.getProfit() >= 0 ? PROFIT_COLOR : COST_COLOR);
				}
			}

			refreshQuickControls();
		});
	}

	private void addPanelSelector(JPanel content)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

		JLabel label = new JLabel("Panel");
		label.setForeground(ColorScheme.TEXT_COLOR);
		JComboBox<PanelSelection> selector = new JComboBox<>(panelSelections);
		selector.setFocusable(false);
		selector.setSelectedItem(selectedPanel);
		selector.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		selector.setForeground(Color.WHITE);
		selector.setToolTipText("<html><b>Panel</b><br>Choose which skill panel to show.</html>");
		selector.addActionListener(e ->
		{
			PanelSelection selection = (PanelSelection) selector.getSelectedItem();
			if (selection == null || selection == selectedPanel)
			{
				return;
			}

			selectedPanel = selection;
			rebuildSelectedPanel();
			refresh();
		});

		row.add(label, BorderLayout.WEST);
		row.add(selector, BorderLayout.CENTER);
		content.add(row);
	}

	private void rebuildSelectedPanel()
	{
		clearPanelState();
		dynamicContent.removeAll();

		if (selectedPanel.skill == null)
		{
			addOverviewPanel(dynamicContent);
		}
		else if (selectedPanel.skill == Skill.FARMING)
		{
			addFarmingPanel(dynamicContent);
		}
		else if (selectedPanel.skill == Skill.HUNTER)
		{
			addHunterPanel(dynamicContent);
		}
		else
		{
			addSkillPlaceholderPanel(dynamicContent, selectedPanel.skill);
		}

		dynamicContent.revalidate();
		dynamicContent.repaint();
	}

	private void clearPanelState()
	{
		periodButtons.clear();
		farmingStatLabels.clear();
		quickToggles.clear();
		statProfitValue = null;
		statCostValue = null;
		statResetGpButton = null;
		statXpValue = null;
		statTopSkillValue = null;
		statActiveSkillsValue = null;
		statActionsValue = null;
		statBirdhouseRunsValue = null;
		statBirdhouseXpValue = null;
		statBirdhouseNestsValue = null;
		statBirdhouseNestValue = null;
		statBirdhouseAvgNestsValue = null;
		statBirdhouseAvgValueValue = null;
		statusLabel = null;
	}

	private void addOverviewPanel(JPanel content)
	{
		content.add(section("Overview"));
		addPeriodControls(content);

		JPanel gpCard = collapsibleCard("All skills GP", true, body ->
		{
			statProfitValue = addRow(body, "Profit", PROFIT_COLOR);
			statCostValue = addRow(body, "Cost", COST_COLOR);
			body.add(Box.createVerticalStrut(6));
			statResetGpButton = secondaryButton("Reset Day");
			statResetGpButton.setForeground(COST_COLOR);
			statResetGpButton.setToolTipText("<html><b>Reset Day GP</b><br>Sets all-skills Profit and Cost to 0 for Day only.</html>");
			statResetGpButton.addActionListener(e ->
			{
				plugin.resetGpStatsPeriodQuick(selectedPeriod);
				setStatus(selectedPeriod.getDisplayName() + " GP reset");
			});
			body.add(statResetGpButton);
		});
		content.add(gpCard);
		content.add(Box.createVerticalStrut(8));

		JPanel xpCard = collapsibleCard("XP", true, body ->
		{
			statXpValue = addRow(body, "Total XP", XP_COLOR);
			statTopSkillValue = addRow(body, "Top skill", XP_COLOR);
			statActiveSkillsValue = addRow(body, "Active skills", XP_COLOR);
			statActionsValue = addRow(body, "Actions", ACTION_COLOR);
		});
		content.add(xpCard);
		content.add(Box.createVerticalStrut(8));
		addQuickControls(content);
	}

	private void addFarmingPanel(JPanel content)
	{
		content.add(section("Farming"));
		addPeriodControls(content);
		content.add(farmingStatsCard(FarmingActivityType.HERBS, "Herb runs", "Herbs", "Herb value", true));
		content.add(Box.createVerticalStrut(8));
		content.add(farmingStatsCard(FarmingActivityType.ALLOTMENTS, "Allotments", "Crops", "Crop value", true));
		content.add(Box.createVerticalStrut(8));
		content.add(farmingStatsCard(FarmingActivityType.HOPS, "Hops", "Hops", "Hop value", false));
	}

	private void addHunterPanel(JPanel content)
	{
		content.add(section("Hunter"));
		addPeriodControls(content);

		JPanel birdhouseCard = collapsibleCard("Birdhouses", true, body ->
		{
			statBirdhouseRunsValue = addRow(body, "Runs", ACTION_COLOR);
			statBirdhouseXpValue = addRow(body, "XP", XP_COLOR);
			statBirdhouseNestsValue = addRow(body, "Nests", XP_COLOR);
			statBirdhouseNestValue = addRow(body, "Nest value", PROFIT_COLOR);
			statBirdhouseAvgNestsValue = addRow(body, "Avg nests", XP_COLOR);
			statBirdhouseAvgValueValue = addRow(body, "Avg value", PROFIT_COLOR);
		});
		content.add(birdhouseCard);
	}

	private void addSkillPlaceholderPanel(JPanel content, Skill skill)
	{
		String skillName = formatSkillName(skill);
		content.add(section(skillName));
		addPeriodControls(content);

		JPanel card = collapsibleCard(skillName, true, body ->
		{
			statXpValue = addRow(body, "XP", XP_COLOR);
			JLabel placeholder = new JLabel("Detailed tracker coming later", SwingConstants.CENTER);
			placeholder.setForeground(MUTED_TEXT);
			placeholder.setBorder(BorderFactory.createEmptyBorder(5, 0, 1, 0));
			body.add(centeredRow(placeholder, CARD_BACKGROUND));
		});
		content.add(card);
	}

	private void addPeriodControls(JPanel content)
	{
		JPanel periodPanel = new JPanel();
		periodPanel.setLayout(new BoxLayout(periodPanel, BoxLayout.Y_AXIS));
		periodPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		periodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 104));
		periodPanel.add(periodButtonRow(SkillingToolkitStatsPeriod.DAY, SkillingToolkitStatsPeriod.WEEK));
		periodPanel.add(Box.createVerticalStrut(4));
		periodPanel.add(periodButtonRow(SkillingToolkitStatsPeriod.MONTH, SkillingToolkitStatsPeriod.YEAR));
		periodPanel.add(Box.createVerticalStrut(4));
		periodPanel.add(singlePeriodButtonRow(SkillingToolkitStatsPeriod.ALL_TIME));
		content.add(periodPanel);
		content.add(Box.createVerticalStrut(8));
	}

	private JPanel periodButtonRow(SkillingToolkitStatsPeriod leftPeriod, SkillingToolkitStatsPeriod rightPeriod)
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		row.add(periodButton(leftPeriod));
		row.add(periodButton(rightPeriod));
		return row;
	}

	private JPanel singlePeriodButtonRow(SkillingToolkitStatsPeriod period)
	{
		JPanel row = new JPanel(new GridLayout(1, 1));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		row.add(periodButton(period));
		return row;
	}

	private JButton periodButton(SkillingToolkitStatsPeriod period)
	{
		JButton button = new JButton(period.getDisplayName());
		button.setFocusable(false);
		button.addActionListener(e ->
		{
			selectedPeriod = period;
			refresh();
		});
		periodButtons.put(period, button);
		return button;
	}

	private void addQuickControls(JPanel content)
	{
		JPanel quickOptionsCard = collapsibleCard("Quick options", false, body ->
		{
			JPanel toggles = new JPanel(new GridLayout(0, 1, 4, 4));
			toggles.setBackground(CARD_BACKGROUND);
			toggles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
			addQuickToggle(toggles, "Compost", "<html><b>Compost</b><br>Show or hide compost patch markers.</html>", plugin::isCompostQuickEnabled, plugin::setCompostQuickEnabled);
			addQuickToggle(toggles, "Protection", "<html><b>Protection</b><br>Show or hide gardener payment checks.</html>", plugin::isProtectionQuickEnabled, plugin::setProtectionQuickEnabled);
			addQuickToggle(toggles, "Timers", "<html><b>Timers</b><br>Show or hide Anima patch timers.</html>", plugin::isPatchTimerQuickEnabled, plugin::setPatchTimerQuickEnabled);
			addQuickToggle(toggles, "Birdhouses", "<html><b>Birdhouses</b><br>Show or hide seeded birdhouse checks.</html>", plugin::isBirdhouseQuickEnabled, plugin::setBirdhouseQuickEnabled);
			addQuickToggle(toggles, "Inventory", "<html><b>Inventory</b><br>Show or hide inventory spaces infobox.</html>", plugin::isInventoryQuickEnabled, plugin::setInventoryQuickEnabled);
			addQuickToggle(toggles, "Herb sack", "<html><b>Herb sack</b><br>Show or hide herb sack contents on hover.</html>", plugin::isHerbSackViewerQuickEnabled, plugin::setHerbSackViewerQuickEnabled);
			addQuickToggle(toggles, "Clock", "<html><b>Clock</b><br>Show or hide the Trade tab clock.</html>", plugin::isClockQuickEnabled, plugin::setClockQuickEnabled);
			addQuickToggle(toggles, "Chat tabs", "<html><b>Chat tabs</b><br>Show or hide all Skilling Toolkit chat-tab trackers.</html>", plugin::isChatTrackersQuickEnabled, plugin::setChatTrackersQuickEnabled);
			body.add(toggles);

			body.add(Box.createVerticalStrut(8));
			JPanel resetRow = new JPanel(new GridLayout(0, 1, 4, 4));
			resetRow.setBackground(CARD_BACKGROUND);
			resetRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
			JButton resetTrackers = secondaryButton("Reset live trackers");
			resetTrackers.setToolTipText("<html><b>Reset live trackers</b><br>Clears current chat-tab XP, profit, cost, and actions.</html>");
			resetTrackers.addActionListener(e ->
			{
				plugin.resetAllTrackersQuick();
				setStatus("Trackers reset");
			});
			JButton resetMarkers = secondaryButton("Reset all markers");
			resetMarkers.setToolTipText("<html><b>Reset markers</b><br>Clears compost, protection, birdhouse, and patch timer data.</html>");
			resetMarkers.addActionListener(e ->
			{
				plugin.resetAllMarkersQuick();
				setStatus("Markers reset");
			});
			resetRow.add(resetTrackers);
			resetRow.add(resetMarkers);
			body.add(resetRow);

			statusLabel = new JLabel("Detailed options stay in settings", SwingConstants.CENTER);
			statusLabel.setForeground(MUTED_TEXT);
			statusLabel.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
			body.add(centeredRow(statusLabel, CARD_BACKGROUND));
		});
		content.add(quickOptionsCard);
	}

	private JPanel farmingStatsCard(FarmingActivityType type, String title, String harvestLabel, String valueLabel, boolean showProfit)
	{
		return collapsibleCard(title, true, body ->
		{
			JLabel runs = addRow(body, "Runs", ACTION_COLOR);
			JLabel seeds = addRow(body, "Seeds", XP_COLOR);
			JLabel seedCost = addRow(body, "Seed cost", COST_COLOR);
			JLabel harvested = addRow(body, harvestLabel, XP_COLOR);
			JLabel harvestValue = addRow(body, valueLabel, PROFIT_COLOR);
			JLabel profit = showProfit ? addRow(body, "Profit", PROFIT_COLOR) : null;
			addDivider(body);
			JLabel avgHarvested = addRow(body, "Avg " + harvestLabel.toLowerCase(Locale.ENGLISH), XP_COLOR);
			farmingStatLabels.put(type, new FarmingStatsLabels(runs, seeds, seedCost, harvested, harvestValue, profit, avgHarvested));
		});
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
			toggle.button.setText(toggle.label + ": " + (enabled ? "On" : "Off"));
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

	private JPanel collapsibleCard(String title, boolean initiallyExpanded, Consumer<JPanel> buildBody)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD_BACKGROUND);
		card.setBorder(BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR));
		card.setAlignmentX(CENTER_ALIGNMENT);
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		JButton header = new JButton();
		header.setFocusable(false);
		header.setHorizontalAlignment(SwingConstants.CENTER);
		header.setFont(FontManager.getRunescapeBoldFont());
		header.setForeground(ColorScheme.BRAND_ORANGE);
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(6, 8, 5, 8));
		header.setToolTipText("<html><b>" + title + "</b><br>Click to show or hide this section.</html>");
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		JPanel headerRow = new JPanel(new BorderLayout());
		headerRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerRow.setAlignmentX(CENTER_ALIGNMENT);
		headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		headerRow.add(header, BorderLayout.CENTER);

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(CARD_BACKGROUND);
		body.setBorder(BorderFactory.createEmptyBorder(4, 7, 7, 7));
		body.setAlignmentX(CENTER_ALIGNMENT);
		body.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		buildBody.accept(body);
		body.setVisible(initiallyExpanded);

		updateCollapsibleHeader(header, title, initiallyExpanded);
		header.addActionListener(e ->
		{
			boolean expanded = !body.isVisible();
			body.setVisible(expanded);
			updateCollapsibleHeader(header, title, expanded);
			card.revalidate();
			card.repaint();
		});

		card.add(headerRow);
		card.add(body);
		return card;
	}

	private void updateCollapsibleHeader(JButton header, String title, boolean expanded)
	{
		header.setText((expanded ? "[-] " : "[+] ") + title);
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

	private void addDivider(JPanel parent)
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(parent.getBackground());
		wrapper.setBorder(BorderFactory.createEmptyBorder(5, 0, 4, 0));
		wrapper.setAlignmentX(CENTER_ALIGNMENT);
		wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));

		JPanel line = new JPanel();
		line.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		line.setPreferredSize(new Dimension(1, 1));

		wrapper.add(line, BorderLayout.CENTER);
		parent.add(wrapper);
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

	private String formatProfitGp(long value)
	{
		String prefix = value < 0 ? "-" : "";
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

	private String formatAverageCount(long total, long runs)
	{
		if (runs <= 0)
		{
			return "-";
		}

		double average = (double) total / runs;
		long rounded = Math.round(average);
		if (Math.abs(average - rounded) < 0.05)
		{
			return formatCount(rounded);
		}

		if (average >= 1000D)
		{
			return formatCount(rounded);
		}

		return String.format(Locale.ENGLISH, "%.1f", average);
	}

	private String formatAverageGp(long total, long runs)
	{
		if (runs <= 0)
		{
			return "-";
		}

		return formatGp(Math.round((double) total / runs));
	}

	private static PanelSelection[] createPanelSelections()
	{
		List<PanelSelection> selections = new ArrayList<>();
		selections.add(OVERVIEW_SELECTION);
		for (Skill skill : SkillingToolkitPlugin.SKILLING_TRACKER_SKILLS)
		{
			selections.add(new PanelSelection(getSkillDisplayName(skill), skill));
		}

		return selections.toArray(new PanelSelection[0]);
	}

	private static String getSkillDisplayName(Skill skill)
	{
		String name = skill.getName();
		if (name == null || name.isBlank())
		{
			name = skill.name().toLowerCase(Locale.ENGLISH);
			return Character.toUpperCase(name.charAt(0)) + name.substring(1);
		}

		return name;
	}

	private static final class PanelSelection
	{
		private final String displayName;
		private final Skill skill;

		private PanelSelection(String displayName, Skill skill)
		{
			this.displayName = displayName;
			this.skill = skill;
		}

		@Override
		public String toString()
		{
			return displayName;
		}
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

	private static final class FarmingStatsLabels
	{
		private final JLabel runs;
		private final JLabel seeds;
		private final JLabel seedCost;
		private final JLabel harvested;
		private final JLabel harvestValue;
		private final JLabel profit;
		private final JLabel avgHarvested;

		private FarmingStatsLabels(
			JLabel runs,
			JLabel seeds,
			JLabel seedCost,
			JLabel harvested,
			JLabel harvestValue,
			JLabel profit,
			JLabel avgHarvested)
		{
			this.runs = runs;
			this.seeds = seeds;
			this.seedCost = seedCost;
			this.harvested = harvested;
			this.harvestValue = harvestValue;
			this.profit = profit;
			this.avgHarvested = avgHarvested;
		}
	}
}
