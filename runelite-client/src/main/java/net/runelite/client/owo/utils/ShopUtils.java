package net.runelite.client.owo.utils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.List;
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
            return Optional.of(new Point(x, y));
        }
        log.debug("No items in inventory matched given item id");
        return Optional.empty();
    }

    public static boolean isShopInterfaceOpen(final Client client, int inventoryID) {
        return client.getItemContainer(inventoryID) != null;
    }

    public static void openShop(final OwoPlugin plugin, final List<NPC> activeShopNPCs) {
        WorldPoint playerWp = plugin.getClient().getLocalPlayer().getWorldLocation();
        Optional<NPC> closestTarget = OwoUtils.findClosestNPC(activeShopNPCs, playerWp);
        if (closestTarget.isEmpty()) {
            plugin.setDebugText("No target NPCs found.");
            plugin.getServer().updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        Point point = OwoUtils.getNpcClickPoint(closestTarget.get());
        Command command = InstructionFactory.createClickCommand(point.getX(), point.getY());
        plugin.getServer().updateCommand(command);

        plugin.setDebugText("Clicking closest target target NPC");
        plugin.setDebugTargetPoint(point);
    }
}
