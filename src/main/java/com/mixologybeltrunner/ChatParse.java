package com.mixologybeltrunner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the lab's game messages.
 */
public final class ChatParse
{
	private static final Pattern TAGS = Pattern.compile("<[^>]*>|@[^@\\s]*@");
	private static final Pattern FAILED_DEPOSIT = Pattern.compile("^You deposit some (.+), but fail to fulfil an order\\.$");

	private ChatParse()
	{
	}

	/**
	 * The potion named in "You deposit some X, but fail to fulfil an order.", or null for
	 * any other message. Colour tags and the game's {@code @mes_hl_pur@} style markers are
	 * stripped first.
	 */
	public static PotionType failedDeposit(String message)
	{
		if (message == null)
		{
			return null;
		}
		Matcher m = FAILED_DEPOSIT.matcher(TAGS.matcher(message).replaceAll("").trim());
		return m.matches() ? PotionType.fromItemName(m.group(1).trim()) : null;
	}
}
