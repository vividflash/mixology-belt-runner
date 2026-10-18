package com.mixologybeltrunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the mixing vessel slots (0 empty, 1 to 3 a component) against a recipe. The vessel
 * keeps the last three lever pulls: slot 0 is the newest, slot 2 the oldest, and a fourth
 * pull pushes the oldest out. The potion it makes is whatever those three pulls are.
 */
public final class Vessel
{
	private static final int SIZE = 3;

	private Vessel()
	{
	}

	/**
	 * The lever pulls still owed, as 1-based positions in the recipe per component, in the
	 * order MOX, AGA, LYE. Uses the fewest extra pulls that make the last three equal the
	 * recipe, so a wrong pull restarts the count. Empty when the recipe is complete.
	 */
	public static Map<PotionComponent, List<Integer>> pullNumbers(PotionType target, int[] vesselSlots)
	{
		List<PotionComponent> kept = new ArrayList<>(kept(target, vesselSlots));
		Map<PotionComponent, List<Integer>> owed = new EnumMap<>(PotionComponent.class);
		int number = kept.size();
		for (PotionComponent component : target.components())
		{
			if (!kept.remove(component))
			{
				owed.computeIfAbsent(component, c -> new ArrayList<>()).add(++number);
			}
		}
		return Collections.unmodifiableMap(owed);
	}

	/**
	 * The slots after one more pull: the new pull becomes slot 0 and the oldest drops out.
	 */
	public static int[] pushed(int[] vesselSlots, PotionComponent pull)
	{
		int[] out = new int[SIZE];
		out[0] = pull.ordinal() + 1;
		for (int i = 1; i < SIZE; i++)
		{
			out[i] = i - 1 < vesselSlots.length ? vesselSlots[i - 1] : 0;
		}
		return out;
	}

	/**
	 * True when some pull in the vessel no longer counts towards the recipe.
	 */
	public static boolean wrongPull(PotionType target, int[] vesselSlots)
	{
		return kept(target, vesselSlots).size() < pulls(vesselSlots).size();
	}

	/**
	 * The newest pulls that can stay: the longest tail of the pulls that fits inside the recipe.
	 */
	private static List<PotionComponent> kept(PotionType target, int[] vesselSlots)
	{
		List<PotionComponent> pulls = pulls(vesselSlots);
		for (int n = Math.min(SIZE, pulls.size()); n > 0; n--)
		{
			List<PotionComponent> tail = pulls.subList(pulls.size() - n, pulls.size());
			if (fits(tail, target))
			{
				return tail;
			}
		}
		return Collections.emptyList();
	}

	/**
	 * The pulls in the vessel, oldest first.
	 */
	private static List<PotionComponent> pulls(int[] vesselSlots)
	{
		List<PotionComponent> pulls = new ArrayList<>();
		for (int i = vesselSlots.length - 1; i >= 0; i--)
		{
			PotionComponent component = PotionComponent.fromVesselValue(vesselSlots[i]);
			if (component != null)
			{
				pulls.add(component);
			}
		}
		return pulls;
	}

	private static boolean fits(List<PotionComponent> pulls, PotionType target)
	{
		List<PotionComponent> recipe = new ArrayList<>(List.of(target.components()));
		for (PotionComponent component : pulls)
		{
			if (!recipe.remove(component))
			{
				return false;
			}
		}
		return true;
	}
}
