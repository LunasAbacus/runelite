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

    public static Optional<Point> findSpellPoint(final Client client) {
        Widget spellWidget = client.getWidget(InterfaceID.MagicSpellbook.HIGH_ALCHEMY);
        if (spellWidget != null && !spellWidget.isHidden())
        {
            Rectangle bounds = spellWidget.getBounds();

            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;

            return Optional.of(new Point(cx, cy));
        } else {
            return Optional.empty();
        }
    }
}
