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
package net.runelite.client.plugins.objectindicators;

import com.google.common.base.Strings;

import java.awt.*;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

class ObjectIndicatorsOverlay extends Overlay
{
	private final Client client;
	private final ObjectIndicatorsConfig config;
	private final ObjectIndicatorsPlugin plugin;
	private final ModelOutlineRenderer modelOutlineRenderer;

	private Image testImage;

	@Inject
	private ObjectIndicatorsOverlay(Client client, ObjectIndicatorsConfig config, ObjectIndicatorsPlugin plugin,
		ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.config = config;
		this.plugin = plugin;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);

		this.testImage = ImageUtil.loadImageResource(getClass(), "sapphire.png");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Stroke stroke = new BasicStroke((float) config.borderWidth());
		for (ColorTileObject colorTileObject : plugin.getObjects())
		{
			TileObject object = colorTileObject.getTileObject();
			Color color = colorTileObject.getColor();

			if (object.getPlane() != client.getPlane())
			{
				continue;
			}

			ObjectComposition composition = colorTileObject.getComposition();
			if (composition.getImpostorIds() != null)
			{
				// This is a multiloc
				composition = composition.getImpostor();
				// Only mark the object if the name still matches
				if (composition == null
					|| Strings.isNullOrEmpty(composition.getName())
					|| "null".equals(composition.getName())
					|| !composition.getName().equals(colorTileObject.getName()))
				{
					continue;
				}
			}

			if (true) {
				graphics.drawImage(testImage,
						object.getCanvasLocation().getX(),
						object.getCanvasLocation().getY(),
						null);
			}

			if (color == null || !config.rememberObjectColors())
			{
				// Fallback to the current config if the object is marked before the addition of multiple colors
				color = config.markerColor();
			}

			if (config.highlightHull())
			{
				renderConvexHull(graphics, object, color, stroke);
			}

			if (config.highlightOutline())
			{
				modelOutlineRenderer.drawOutline(object, (int)config.borderWidth(), color, config.outlineFeather());
			}

			if (config.highlightClickbox())
			{
				Shape clickbox = object.getClickbox();
				if (clickbox != null)
				{
					Color clickBoxColor = ColorUtil.colorWithAlpha(color, color.getAlpha() / 12);
					OverlayUtil.renderPolygon(graphics, clickbox, color, clickBoxColor, stroke);
				}
			}

			if (config.highlightTile())
			{
				Polygon tilePoly = object.getCanvasTilePoly();
				if (tilePoly != null)
				{
					Color tileColor = ColorUtil.colorWithAlpha(color, color.getAlpha() / 12);
					OverlayUtil.renderPolygon(graphics, tilePoly, color, tileColor, stroke);
				}
			}
		}

		return null;
	}

	private void renderConvexHull(Graphics2D graphics, TileObject object, Color color, Stroke stroke)
	{
		final Shape polygon;
		Shape polygon2 = null;

		if (object instanceof GameObject)
		{
			polygon = ((GameObject) object).getConvexHull();
		}
		else if (object instanceof WallObject)
		{
			polygon = ((WallObject) object).getConvexHull();
			polygon2 = ((WallObject) object).getConvexHull2();
		}
		else if (object instanceof DecorativeObject)
		{
			polygon = ((DecorativeObject) object).getConvexHull();
			polygon2 = ((DecorativeObject) object).getConvexHull2();
		}
		else if (object instanceof GroundObject)
		{
			polygon = ((GroundObject) object).getConvexHull();
		}
		else
		{
			polygon = object.getCanvasTilePoly();
		}

		if (polygon != null)
		{
			OverlayUtil.renderPolygon(graphics, polygon, color, stroke);
		}

		if (polygon2 != null)
		{
			OverlayUtil.renderPolygon(graphics, polygon2, color, stroke);
		}
	}
}