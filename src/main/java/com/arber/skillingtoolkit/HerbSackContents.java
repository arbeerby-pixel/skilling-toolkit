package com.arber.skillingtoolkit;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HerbSackContents
{
	private static final Pattern HERB_LINE = Pattern.compile("(?i).*?(?<quantity>[\\d,]+)\\s*x\\s*(?<herb>grimy [a-z'\\- ]+).*");

	private final EnumMap<HerbSackHerb, Integer> quantities = new EnumMap<>(HerbSackHerb.class);
	private boolean loaded;

	void setQuantities(Map<HerbSackHerb, Integer> newQuantities)
	{
		quantities.clear();
		newQuantities.forEach((herb, quantity) ->
		{
			if (herb != null && quantity != null && quantity > 0)
			{
				quantities.put(herb, quantity);
			}
		});
		loaded = true;
	}

	void clearLoaded()
	{
		quantities.clear();
		loaded = true;
	}

	void forget()
	{
		quantities.clear();
		loaded = false;
	}

	boolean isLoaded()
	{
		return loaded;
	}

	boolean isEmpty()
	{
		return quantities.isEmpty();
	}

	int getTotalQuantity()
	{
		int total = 0;
		for (int quantity : quantities.values())
		{
			total += quantity;
		}

		return total;
	}

	Map<HerbSackHerb, Integer> getQuantities()
	{
		return Collections.unmodifiableMap(quantities);
	}

	static ParsedHerbLine parseChatLine(String message)
	{
		Matcher matcher = HERB_LINE.matcher(message == null ? "" : message);
		if (!matcher.matches())
		{
			return null;
		}

		HerbSackHerb herb = HerbSackHerb.fromName(matcher.group("herb"));
		if (herb == null)
		{
			return null;
		}

		int quantity;
		try
		{
			quantity = Integer.parseInt(matcher.group("quantity").replace(",", ""));
		}
		catch (NumberFormatException ex)
		{
			return null;
		}

		return quantity > 0 ? new ParsedHerbLine(herb, quantity) : null;
	}

	static final class ParsedHerbLine
	{
		private final HerbSackHerb herb;
		private final int quantity;

		private ParsedHerbLine(HerbSackHerb herb, int quantity)
		{
			this.herb = herb;
			this.quantity = quantity;
		}

		HerbSackHerb getHerb()
		{
			return herb;
		}

		int getQuantity()
		{
			return quantity;
		}
	}
}
