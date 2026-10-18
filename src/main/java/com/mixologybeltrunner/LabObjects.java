package com.mixologybeltrunner;

import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * Finds the scene object behind a {@link LabObject}. Client thread only.
 */
final class LabObjects
{
	private LabObjects()
	{
	}

	/**
	 * Looks on the object's tile for a game object, then a decorative object (the Aga lever is one), or null.
	 */
	static TileObject find(Client client, LabObject object)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}
		LocalPoint point = LocalPoint.fromWorld(worldView, object.coordinate());
		if (point == null)
		{
			return null;
		}
		Scene scene = worldView.getScene();
		Tile[][][] tiles = scene.getTiles();
		Tile tile = tiles[worldView.getPlane()][point.getSceneX()][point.getSceneY()];
		if (tile == null)
		{
			return null;
		}
		for (GameObject gameObject : tile.getGameObjects())
		{
			if (gameObject != null && gameObject.getId() == object.objectId())
			{
				return gameObject;
			}
		}
		DecorativeObject decorative = tile.getDecorativeObject();
		if (decorative != null && decorative.getId() == object.objectId())
		{
			return decorative;
		}
		return null;
	}
}
