package com.mixologybeltrunner;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

/**
 * Outlines the potion the station takes next and, while digweed is to be used, the digweed
 * and the potion it goes on, tinted as well so the pair stands out.
 */
public class InventoryOverlay extends WidgetItemOverlay
{
	private final BeltRunnerPlugin plugin;
	private final BeltRunnerConfig config;
	private final ItemManager itemManager;

	@Inject
	InventoryOverlay(BeltRunnerPlugin plugin, BeltRunnerConfig config, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Step step = plugin.currentStep();
		if (step == null || !plugin.inLab())
		{
			return;
		}
		Widget widget = widgetItem.getWidget();
		if (widget == null)
		{
			return;
		}
		int slot = widget.getIndex();
		boolean usingDigweed = step.digweedSlot() >= 0;
		if (slot == step.digweedSlot() || usingDigweed && itemId == ItemID.MM_LAB_SPECIAL_HERB)
		{
			drawFilled(graphics, itemId, widgetItem, config.digweedColour());
		}
		else if (slot == step.nextPotionSlot())
		{
			draw(graphics, itemId, widgetItem, Color.WHITE);
		}
	}

	/**
	 * The outline plus a light wash of the same colour over the item, the way Quest Helper does it.
	 */
	private void drawFilled(Graphics2D graphics, int itemId, WidgetItem widgetItem, Color colour)
	{
		draw(graphics, itemId, widgetItem, colour);
		Image tint = ImageUtil.fillImage(itemManager.getImage(itemId, widgetItem.getQuantity(), false),
			ColorUtil.colorWithAlpha(colour, 65));
		graphics.drawImage(tint, widgetItem.getCanvasBounds().x, widgetItem.getCanvasBounds().y, null);
	}

	private void draw(Graphics2D graphics, int itemId, WidgetItem widgetItem, Color colour)
	{
		BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), colour);
		graphics.drawImage(outline, widgetItem.getCanvasBounds().x, widgetItem.getCanvasBounds().y, null);
	}
}
