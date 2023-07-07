/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.objectlabels;

import com.google.common.base.Strings;
import net.runelite.api.*;
import net.runelite.client.plugins.objectlabels.types.IconLabelType;
import net.runelite.client.plugins.objectlabels.types.IconTileObject;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

class ObjectLabelsOverlay extends Overlay
{
	private final Client client;
	private final ObjectLabelsConfig config;
	private final ObjectLabelsPlugin plugin;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private Image testImage;

	private Map<IconLabelType, Image> iconLabelImages;

	@Inject
	private ObjectLabelsOverlay(Client client, ObjectLabelsConfig config, ObjectLabelsPlugin plugin,
								ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);

		iconLabelImages = new HashMap<>();

		iconLabelImages.put(IconLabelType.BANK, ImageUtil.loadImageResource(getClass(), "bank.png"));
		iconLabelImages.put(IconLabelType.FURNACE, ImageUtil.loadImageResource(getClass(), "furnace.png"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (IconTileObject iconTileObject : plugin.getObjects())
		{
			TileObject object = iconTileObject.getTileObject();

			if (object.getPlane() != client.getPlane())
			{
				continue;
			}

			ObjectComposition composition = iconTileObject.getComposition();
			if (composition.getImpostorIds() != null)
			{
				// This is a multiloc
				composition = composition.getImpostor();
				// Only mark the object if the name still matches
				if (composition == null
					|| Strings.isNullOrEmpty(composition.getName())
					|| "null".equals(composition.getName())
					|| !composition.getName().equals(iconTileObject.getName()))
				{
					continue;
				}
			}

			// TODO Render icon in center of object instead of tile
			graphics.drawImage(iconLabelImages.get(iconTileObject.getIconLabelType()),
						object.getCanvasLocation().getX(),
						object.getCanvasLocation().getY(),
						null);
		}

		return null;
	}
}
