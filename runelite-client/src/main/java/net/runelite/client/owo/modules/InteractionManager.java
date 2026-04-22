package net.runelite.client.owo.modules;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.annotations.Interface;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.instruction.OwoServer;
import net.runelite.client.owo.utils.BankUtils;
import net.runelite.client.owo.utils.InventoryUtils;
import net.runelite.client.owo.utils.OwoUtils;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
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

    public InteractionManager(OwoPlugin plugin,
                              Collection<Integer> npcsToTrack,
                              Collection<Integer> gameObjectsToTrack,
                              Collection<Integer> decorationsToTrack,
                              Collection<Integer> groundObjectsToTrack,
                              Collection<Integer> wallObjectsToTrack) {
        this.worldTrackingModule = new WorldTrackingModule(npcsToTrack, gameObjectsToTrack, decorationsToTrack, groundObjectsToTrack, wallObjectsToTrack);
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

    public void closeInterface(String name) {
        plugin.setDebugText("Closing interface: " + name);
        server.updateCommand(new Command(List.of(InstructionFactory.createTypeInstruction("{Esc}"))));
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

    public void trackDecorativeObject(final DecorativeObject decorativeObject) {
        worldTrackingModule.trackDecorativeObject(decorativeObject);
    }

    public void untrackDecorativeObject(final DecorativeObject decorativeObject) {
        worldTrackingModule.untrackDecorativeObject(decorativeObject);
    }

    public void trackGroundObject(final GroundObject groundObject) {
        worldTrackingModule.trackGroundObject(groundObject);
    }

    public void untrackGroundObject(final GroundObject groundObject) {
        worldTrackingModule.untrackGroundObject(groundObject);
    }

    public void trackWallObject(final WallObject wallObject) {
        worldTrackingModule.trackWallObject(wallObject);
    }

    public void untrackWallObject(final WallObject wallObject) {
        worldTrackingModule.untrackWallObject(wallObject);
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

    public void clickGameObject(final GameObject gameObject) {
        Optional<Point> point = OwoUtils.getGameObjectClickPoint(gameObject, client);
        if (point.isPresent()) {
            server.updateCommand(InstructionFactory.createClickCommand(point.get()));
        } else {
            plugin.setDebugText("Failed to find game objects point on screen");
            log.debug("Failed to find game objects point on screen");
        }
    }

    public Collection<GameObject> getTrackedObjects(final int id) {
        return worldTrackingModule.getGameObjects(id);
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

    public void useItemOnAnother(final int firstItem, final int secondItem, final String actionName) {
        Optional<Point> point1 = InventoryUtils.findInventoryItemPoint(client, inventoryItems, firstItem);
        Optional<Point> point2 = InventoryUtils.findInventoryItemPoint(client, inventoryItems, secondItem);
        if (point1.isPresent() && point2.isPresent()) {
            server.updateCommand(new Command(List.of(
                    InstructionFactory.createClickInstruction(point1.get()),
                    InstructionFactory.createClickInstruction(point2.get())
            )));
        } else {
            plugin.setDebugText("Failed to find inventory items for action: " + actionName);
            log.debug("Failed to find inventory item for action: {}", actionName);
        }
    }

    // TODO Nate if can ever figure out a reliable way to see confirmation popup would be better than waiting x amount of time
    // TODO Nate use isWidgetVisible with SKILLMULTI
    public void useItemOnAnotherAndConfirm(final int firstItem, final int secondItem, final String actionName) {
        Optional<Point> point1 = InventoryUtils.findInventoryItemPoint(client, inventoryItems, firstItem);
        Optional<Point> point2 = InventoryUtils.findInventoryItemPoint(client, inventoryItems, secondItem);
        if (point1.isPresent() && point2.isPresent()) {
            server.updateCommand(new Command(List.of(
                    InstructionFactory.createClickInstruction(point1.get()),
                    InstructionFactory.createClickInstruction(point2.get()),
                    InstructionFactory.createIdleByMillisInstruction(800, 1000),
                    InstructionFactory.createTypeInstruction(" ")
            )));
        } else {
            plugin.setDebugText("Failed to find inventory items for action: " + actionName);
            log.debug("Failed to find inventory item for action: {}", actionName);
        }
    }

    public void confirmSelection() {
        server.updateCommand(InstructionFactory.createTypeCommandWithPreWait(" ", 600));
    }


    public void bankDepositAll() {
        Optional<Point> point = BankUtils.findDepositAllButton(client);
        if (point.isPresent()) {
            plugin.setDebugText("Clicking deposit all button");
            server.updateCommand(InstructionFactory.createClickCommand(point.get()));
        } else {
            plugin.setDebugText("Failed to find deposit all button");
            log.debug("Failed to find deposit all button");
        }
    }

    public boolean isWidgetVisible(int componentId) {
        Widget widget = client.getWidget(componentId);
        if (widget == null) {
            log.debug("Widget {} is null", componentId);
            return false;
        }
        if (widget.isSelfHidden()) {
            log.debug("Widget {} is self-hidden", componentId);
            return false;
        }
        return true;
    }

    public WorldPoint findPlayerLocation() {
        return client.getLocalPlayer().getWorldLocation();
    }
}
