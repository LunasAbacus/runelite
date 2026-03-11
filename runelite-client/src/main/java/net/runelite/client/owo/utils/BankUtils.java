package net.runelite.client.owo.utils;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.*;
import java.util.Optional;

public class BankUtils {
    public static Optional<Point> findBankItemPoint(Client client, int itemId) {
        if (client == null) {
            return Optional.empty();
        }

        Widget bankItemWidget = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
        if (bankItemWidget == null || bankItemWidget.isHidden()) {
            return Optional.empty();
        }

        Widget[] children = bankItemWidget.getChildren();
        if (children == null) {
            return Optional.empty();
        }

        for (Widget child : children) {
            if (child == null || child.getItemId() != itemId) {
                continue;
            }

            Rectangle bounds = child.getBounds();
            if (bounds == null || bounds.isEmpty()) {
                continue;
            }

            int x = (int) bounds.getCenterX();
            int y = (int) bounds.getCenterY();
            return Optional.of(new Point(x, y));
        }

        return Optional.empty();
    }

    public static boolean isBankInterfaceOpen(final Client client) {
        return client.getItemContainer(InventoryID.BANK) != null;
    }
}
