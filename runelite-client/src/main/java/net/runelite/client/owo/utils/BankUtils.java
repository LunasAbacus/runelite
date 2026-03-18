package net.runelite.client.owo.utils;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.List;
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
        Widget bankItems = client.getWidget(InterfaceID.Bankmain.ITEMS);
        return bankItems != null && !bankItems.isSelfHidden();
    }

    public static void depositInventoryInBank(final OwoPlugin plugin) {
        Widget depositInv = plugin.getClient().getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        if (depositInv != null && !depositInv.isHidden())
        {
            Rectangle bounds = depositInv.getBounds();
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;
            Command command = InstructionFactory.createBankDepositCommand(cx, cy);
            plugin.getServer().updateCommand(command);
            plugin.setDebugText("Depositing inventory in bank");
        } else {
            plugin.setDebugText("Failed to find deposit button");
        }
    }

    public static void depositItemInBank(final OwoPlugin plugin, Item[] inventoryItems, final int itemID) {
        Optional<Point> point = InventoryUtils.findInventoryItemPoint(plugin.getClient(), inventoryItems, itemID);
        if (point.isEmpty()) {
            plugin.setDebugText("No bracelets found in inventory to deposit");
            plugin.getServer().updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        plugin.getServer().updateCommand(InstructionFactory.createBankDepositCommand(point.get().getX(), point.get().getY()));
        plugin.setDebugText("Depositing item");
        plugin.setDebugTargetPoint(point.get());
    }

    public static void clickClosestBank(final OwoPlugin plugin, List<GameObject> activeBanks) {
        Optional<GameObject> closestBank = OwoUtils.findClosestGameObject(activeBanks, plugin.getClient().getLocalPlayer().getWorldLocation());

        if (closestBank.isEmpty()) {
            plugin.setDebugText("No bank object tracked");
            plugin.getServer().updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        Optional<Point> point = OwoUtils.getGameObjectClickPoint(closestBank.get());
        if (point.isEmpty()) {
            plugin.setDebugText("Bank is off screen");
            plugin.getServer().updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        plugin.getServer().updateCommand(InstructionFactory.createClickCommand(point.get().getX(), point.get().getY()));
        plugin.setDebugText("Opening bank");
        plugin.setDebugTargetPoint(point.get());
    }
}
