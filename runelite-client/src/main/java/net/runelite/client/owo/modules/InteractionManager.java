package net.runelite.client.owo.modules;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.instruction.OwoServer;
import net.runelite.client.owo.utils.BankUtils;
import net.runelite.client.owo.utils.InventoryUtils;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class InteractionManager {
    private final WorldTrackingModule worldTrackingModule;
    private final OwoPlugin plugin;
    private final OwoServer server;
    private final Client client;

    @Setter
    private Item[] inventoryItems = new Item[28];

    public InteractionManager(OwoPlugin plugin, List<Integer> npcsToTrack, List<Integer> gameObjectsToTrack) {
        this.worldTrackingModule = new WorldTrackingModule(npcsToTrack, gameObjectsToTrack);
        this.server = plugin.getServer();
        this.client = plugin.getClient();
        this.plugin = plugin;
    }

    public void depositInventoryInBank() {
        Widget depositInv = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        if (depositInv != null && !depositInv.isHidden()) {
            Rectangle bounds = depositInv.getBounds();
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;
            server.updateCommand(InstructionFactory.createBankTransactionCommand(List.of(new Point(cx, cy))));
        } else {
            log.debug("Failed to find deposit button");
        }
    }

    public void performBankTransaction(List<Integer> depositItems, List<Integer> withdrawItems) {
        List<Point> clickPoints = new ArrayList<>();

        for (Integer itemID : depositItems) {
            Optional<Point> point = InventoryUtils.findInventoryItemPoint(client, inventoryItems, itemID);
            point.ifPresent(clickPoints::add);
        }

        for (Integer itemID : withdrawItems) {
            Optional<Point> point = BankUtils.findBankItemPoint(client, itemID);
            point.ifPresent(clickPoints::add);
        }

        server.updateCommand(InstructionFactory.createBankTransactionCommand(clickPoints));
    }

    public void clearTrackedObjects() {
        worldTrackingModule.clear();
    }

    public void trackNPC(final NPC npc) {
        worldTrackingModule.trackNPC(npc);
    }

    public void untrackNPC(final NPC npc) {
        worldTrackingModule.untrackNPC(npc);
    }

    public void trackGameObject(final GameObject gameObject) {
        worldTrackingModule.trackGameObject(gameObject);
    }

    public void untrackGameObject(final GameObject gameObject) {
        worldTrackingModule.untrackGameObject(gameObject);
    }

    public void clickClosestGameObject(final List<Integer> gameObjectIds, final String name) {
        Optional<Point> point = worldTrackingModule.findClosestTrackedGameObject(client, gameObjectIds);
        if (point.isPresent()) {
            server.updateCommand(InstructionFactory.createClickCommand(point.get()));
        } else {
            plugin.setDebugText("Failed to find GameObject: " + name);
            log.debug("Failed to find GameObject: {}", name);
        }
    }

    public void clickClosestNpc(final List<Integer> npcIds, final String name) {
        Optional<Point> point = worldTrackingModule.findClosestTrackedNpcPoint(client, npcIds);
        if (point.isPresent()) {
            server.updateCommand(InstructionFactory.createClickCommand(point.get()));
        } else {
            plugin.setDebugText("Failed to find NPC: " + name);
            log.debug("Failed to find NPC: {}", name);
        }
    }

    public void hoverClosestNpc(final List<Integer> npcIds, final String name) {
        Optional<Point> point = worldTrackingModule.findClosestTrackedNpcPoint(client, npcIds);
        if (point.isPresent()) {
            server.updateCommand(InstructionFactory.createHoverCommand(point.get()));
        } else {
            plugin.setDebugText("Failed to hover NPC: " + name);
            log.debug("Failed to hover NPC: {}", name);
        }
    }

    public Optional<NPC> findClosestNPC(final List<Integer> npcIds) {
        return worldTrackingModule.findClosestNpc(client, npcIds);
    }

    public void clickItemInInventory(final int itemId, final String name) {
        Optional<Point> point = InventoryUtils.findInventoryItemPoint(client, inventoryItems, itemId);
        if (point.isPresent()) {
            server.updateCommand(InstructionFactory.createClickCommand(point.get()));
        } else {
            plugin.setDebugText("Failed to find inventory item: " + name);
            log.debug("Failed to find inventory item: {}", name);
        }
    }
}
