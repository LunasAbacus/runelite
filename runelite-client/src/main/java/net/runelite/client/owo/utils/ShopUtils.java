package net.runelite.client.owo.utils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.awt.*;
import java.util.Optional;

@Slf4j
public class ShopUtils {
    public static Optional<Point> findShopItemPoint(Client client, int itemId) {
        if (client == null) {
            return Optional.empty();
        }

        Widget shopItemWidget = client.getWidget(InterfaceID.Shopmain.ITEMS);

        if (shopItemWidget == null || shopItemWidget.isHidden()) {
            log.debug("shopItemWidget is null or hidden");
            return Optional.empty();
        }

        Widget[] children = shopItemWidget.getChildren();
        if (children == null) {
            log.debug("no children found in shop item widget");
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
            Point point = new Point(x, y);
            if (!OwoUtils.isPointOnScreen(point, client)) {
                return Optional.empty();
            }
            return Optional.of(point);
        }
        log.debug("No items in inventory matched given item id");
        return Optional.empty();
    }

    public static boolean isShopInterfaceOpen(final Client client, int inventoryID) {
        return client.getItemContainer(inventoryID) != null;
    }
}
