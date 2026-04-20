package net.runelite.client.owo.utils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class InventoryUtils {
    private static final int INVENTORY_SLOT_COUNT = 28;

    public static Optional<Point> findInventoryItemPoint(Client client, Item[] inventoryItems, int itemId) {
        if (client == null || inventoryItems == null) {
            log.debug("Client or Inventory Items are null");
            return Optional.empty();
        }

        int canonicalItemId = canonicalizeItemId(client, itemId);

        if (findInventoryRootWidgets(client).isEmpty()) {
            log.debug("Inventory is missing or closed");
            return Optional.empty();
        }

        int maxSlots = Math.min(INVENTORY_SLOT_COUNT, inventoryItems.length);
        for (int slot = 0; slot < maxSlots; slot++) {
            Item item = inventoryItems[slot];
            if (item == null || item.getId() <= 0 || canonicalizeItemId(client, item.getId()) != canonicalItemId) {
                continue;
            }

            Optional<Widget> slotWidget = findInventorySlotWidget(client, slot);
            if (slotWidget.isEmpty()) {
                continue;
            }

            Rectangle bounds = slotWidget.get().getBounds();
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

        log.debug("No items in inventory matched given itemId: {}", itemId);
        return Optional.empty();
    }

    public static Optional<Point> findInventorySlotPoint(Client client, int slotNumber) {
        if (client == null || slotNumber < 0 || slotNumber >= INVENTORY_SLOT_COUNT) {
            return Optional.empty();
        }

        Optional<Widget> slotWidget = findInventorySlotWidget(client, slotNumber);
        if (slotWidget.isEmpty()) {
            return Optional.empty();
        }

        Rectangle bounds = slotWidget.get().getBounds();
        if (bounds == null || bounds.isEmpty()) {
            return Optional.empty();
        }

        int x = (int) bounds.getCenterX();
        int y = (int) bounds.getCenterY();
        Point point = new Point(x, y);

        if (!OwoUtils.isPointOnScreen(point, client)) {
            return Optional.empty();
        }

        return Optional.of(point);
    }

    private static Optional<Widget> findInventorySlotWidget(Client client, int slotNumber) {
        for (Widget inventoryWidget : findInventoryRootWidgets(client)) {
            Optional<Widget> directMatch = findSlotFromWidgetChildren(inventoryWidget, slotNumber);
            if (directMatch.isPresent()) {
                return directMatch;
            }

            Optional<Widget> nestedMatch = findSlotInDescendants(inventoryWidget, slotNumber);
            if (nestedMatch.isPresent()) {
                return nestedMatch;
            }
        }

        log.debug("Unable to find inventory slot widget for slot {}", slotNumber);
        return Optional.empty();
    }

    private static List<Widget> findInventoryRootWidgets(Client client) {
        List<Widget> widgets = new ArrayList<>();
        addVisibleWidget(widgets, client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER));
        addVisibleWidget(widgets, client.getWidget(WidgetInfo.INVENTORY));
        addVisibleWidget(widgets, client.getWidget(InterfaceID.Shopside.ITEMS));
        addVisibleWidget(widgets, client.getWidget(InterfaceID.OmnishopSide.ITEMS));
        return widgets;
    }

    private static void addVisibleWidget(List<Widget> widgets, Widget widget) {
        if (widget != null && !widget.isHidden()) {
            widgets.add(widget);
        }
    }

    private static Optional<Widget> findSlotFromWidgetChildren(Widget widget, int slotNumber) {
        Widget directChild = widget.getChild(slotNumber);
        if (isUsableSlotWidget(directChild)) {
            return Optional.of(directChild);
        }

        Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null && dynamicChildren.length > slotNumber) {
            Widget dynamicChild = dynamicChildren[slotNumber];
            if (isUsableSlotWidget(dynamicChild)) {
                return Optional.of(dynamicChild);
            }
        }

        Widget[] children = widget.getChildren();
        if (children != null && children.length > slotNumber) {
            Widget child = children[slotNumber];
            if (isUsableSlotWidget(child)) {
                return Optional.of(child);
            }
        }

        return Optional.empty();
    }

    private static Optional<Widget> findSlotInDescendants(Widget widget, int slotNumber) {
        for (Widget child : getAllChildren(widget)) {
            if (child == null || child.isHidden()) {
                continue;
            }

            Optional<Widget> directMatch = findSlotFromWidgetChildren(child, slotNumber);
            if (directMatch.isPresent()) {
                return directMatch;
            }

            Optional<Widget> nestedMatch = findSlotInDescendants(child, slotNumber);
            if (nestedMatch.isPresent()) {
                return nestedMatch;
            }
        }

        return Optional.empty();
    }

    private static List<Widget> getAllChildren(Widget widget) {
        List<Widget> children = new ArrayList<>();
        addChildren(children, widget.getChildren());
        addChildren(children, widget.getDynamicChildren());
        addChildren(children, widget.getStaticChildren());
        addChildren(children, widget.getNestedChildren());
        return children;
    }

    private static void addChildren(List<Widget> destination, Widget[] source) {
        if (source == null) {
            return;
        }

        for (Widget child : source) {
            if (child != null) {
                destination.add(child);
            }
        }
    }

    private static boolean isUsableSlotWidget(Widget widget) {
        if (widget == null || widget.isHidden()) {
            return false;
        }

        Rectangle bounds = widget.getBounds();
        return bounds != null && !bounds.isEmpty();
    }

    private static int canonicalizeItemId(Client client, int itemId) {
        if (client == null || itemId <= 0) {
            return itemId;
        }

        ItemComposition itemComposition = client.getItemDefinition(itemId);
        return itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
    }

    public static int inventorySpotsUsed(Item[] inventoryItems) {
        if (inventoryItems == null || inventoryItems.length == 0) {
            return 0;
        }
        int usedSlots = 0;
        for (Item item : inventoryItems) {
            if (item != null && item.getId() < 0) {
                usedSlots++;
            }
        }
        return usedSlots;
    }

    public static boolean doesInventoryContainItems(Item[] inventoryItems, List<ItemAmount> requiredItems) {
        if (requiredItems == null || requiredItems.isEmpty()) {
            return true;
        }
        if (inventoryItems == null || inventoryItems.length == 0) {
            return false;
        }

        Map<Integer, Integer> inventoryCounts = new HashMap<>();
        int maxSlots = Math.min(INVENTORY_SLOT_COUNT, inventoryItems.length);
        for (int slot = 0; slot < maxSlots; slot++) {
            Item item = inventoryItems[slot];
            if (item == null || item.getId() <= 0) {
                continue;
            }

            inventoryCounts.merge(item.getId(), item.getQuantity(), Integer::sum);
        }

        for (ItemAmount requiredItem : requiredItems) {
            if (requiredItem == null || requiredItem.getInventoryCount() <= 0) {
                continue;
            }

            int available = inventoryCounts.getOrDefault(requiredItem.getItemId(), 0);
            if (available < requiredItem.getInventoryCount() * requiredItem.getStackSize()) {
                return false;
            }
        }

        return true;
    }
}
