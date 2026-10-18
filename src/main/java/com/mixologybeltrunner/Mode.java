package com.mixologybeltrunner;

/**
 * What the guide plays for. Both stock finished potions and restock early; they differ in how
 * many types they stock and which unfilled orders are worth making before clicking the belt.
 */
public enum Mode
{
	/**
	 * The five best potions in stock, and the belt is clicked whenever anything fills: a new
	 * set of orders costs a tick or two, making a potion for an old one costs far more.
	 */
	FOCUS_XP("Focus XP", 5, Integer.MAX_VALUE),
	/**
	 * All seven non-triples in stock, so resin comes in evenly, and an unfilled Anti-leech
	 * lotion or Mixalot order is made before the belt is clicked. Gives up a few percent of
	 * XP for more resin.
	 */
	BALANCED("Balanced", 7, 3400);

	private final String label;
	private final int stockTypes;
	private final int makeFromXp;

	Mode(String label, int stockTypes, int makeFromXp)
	{
		this.label = label;
		this.stockTypes = stockTypes;
		this.makeFromXp = makeFromXp;
	}

	/**
	 * How many non-triple types to keep in stock, best first.
	 */
	public int stockTypes()
	{
		return stockTypes;
	}

	/**
	 * True when an unfilled order for this potion is worth making before clicking the belt.
	 */
	public boolean worthMaking(PotionType type)
	{
		return type.xp() >= makeFromXp;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
