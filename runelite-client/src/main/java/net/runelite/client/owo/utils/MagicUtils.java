package net.runelite.client.owo.utils;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.awt.Rectangle;
import java.util.Optional;

public class MagicUtils {
    public static boolean isSpellbookOpen(final Client client) {
        Widget spellbook = client.getWidget(InterfaceID.MagicSpellbook.SPELLLAYER);
        return spellbook != null && !spellbook.isHidden();
    }

    public static Optional<Point> findSpellPoint(final Client client, int widgetId) {
        Widget spellWidget = client.getWidget(widgetId);
        if (spellWidget != null && !spellWidget.isHidden())
        {
            Rectangle bounds = spellWidget.getBounds();

            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;
            Point point = new Point(cx, cy);
            if (!OwoUtils.isPointOnScreen(point, client)) {
                return Optional.empty();
            }
            return Optional.of(point);
        } else {
            return Optional.empty();
        }
    }
}
