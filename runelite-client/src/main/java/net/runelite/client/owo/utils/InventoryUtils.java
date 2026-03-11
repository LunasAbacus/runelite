package net.runelite.client.owo.utils;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InventoryUtils {

    public static Optional<Point> findInventoryItemPoint(Client client, Item[] inventoryItems, int itemId) {
        if (client == null || inventoryItems == null) {
            return Optional.empty();
        }

        int canonicalItemId = canonicalizeItemId(client, itemId);

        Widget inventoryWidget = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
        if (inventoryWidget == null || inventoryWidget.isHidden()) {
            inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        }
        if (inventoryWidget == null || inventoryWidget.isHidden()) {
            return Optional.empty();
        }

        int maxSlots = Math.min(28, inventoryItems.length);
        for (int slot = 0; slot < maxSlots; slot++) {
            Item item = inventoryItems[slot];
            if (item == null || item.getId() <= 0 || canonicalizeItemId(client, item.getId()) != canonicalItemId) {
                continue;
            }

            Widget slotWidget = inventoryWidget.getChild(slot);
            if (slotWidget == null) {
                continue;
            }

            Rectangle bounds = slotWidget.getBounds();
            if (bounds == null || bounds.isEmpty()) {
                continue;
            }

            int x = (int) bounds.getCenterX();
            int y = (int) bounds.getCenterY();
            return Optional.of(new Point(x, y));
        }

        return Optional.empty();
    }

    private static int canonicalizeItemId(Client client, int itemId) {
        if (client == null || itemId <= 0) {
            return itemId;
        }

        ItemComposition itemComposition = client.getItemDefinition(itemId);
        if (itemComposition == null) {
            return itemId;
        }

        return itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
    }

    public static boolean doesInventoryContainItems(Item[] inventoryItems, List<ItemAmount> requiredItems) {
        if (requiredItems == null || requiredItems.isEmpty()) {
            return true;
        }
        if (inventoryItems == null || inventoryItems.length == 0) {
            return false;
        }

        Map<Integer, Integer> inventoryCounts = new HashMap<>();
        int maxSlots = Math.min(28, inventoryItems.length);
        for (int slot = 0; slot < maxSlots; slot++) {
            Item item = inventoryItems[slot];
            if (item == null || item.getId() <= 0) {
                continue;
            }

            // Count by occupied slots, not stack quantity.
            inventoryCounts.merge(item.getId(), 1, Integer::sum);
        }

        for (ItemAmount requiredItem : requiredItems) {
            if (requiredItem == null || requiredItem.getAmount() <= 0) {
                continue;
            }

            int available = inventoryCounts.getOrDefault(requiredItem.getItemId(), 0);
            if (available < requiredItem.getAmount()) {
                return false;
            }
        }

        return true;
    }
}
