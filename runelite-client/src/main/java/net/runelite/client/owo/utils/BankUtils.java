package net.runelite.client.owo.utils;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.owo.instruction.InstructionFactory;

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
            Point point = new Point(x, y);
            if (!OwoUtils.isPointOnScreen(point, client)) {
                return Optional.empty();
            }
            return Optional.of(point);
        }

        return Optional.empty();
    }

    public static Optional<Point> findDepositAllButton(Client client) {
        Widget depositInv = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        if (depositInv != null && !depositInv.isHidden()) {
            Rectangle bounds = depositInv.getBounds();
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;
            Point point = new Point(cx, cy);
            if (OwoUtils.isPointOnScreen(point, client)) {
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }

    public static boolean isBankInterfaceOpen(final Client client) {
        Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
        return bankItems != null && !bankItems.isSelfHidden();
    }
}
