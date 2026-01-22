package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.awt.*;
import java.util.*;
import java.util.List;

@Slf4j
public class Woodcutting extends OwoLogic {

    private Random random = new Random();

    private final List<Integer> treeIds = List.of(
        // Draynor willows
        10819,10829,10831,10833
    );

    private final List<Integer> bankIds = List.of(
        // Draynor Kiosks
        10528, 10355
    );

    private final List<GameObject> activeTrees = new ArrayList<>();
    private final List<GameObject> activeBanks = new ArrayList<>();

    public Woodcutting(final OwoPlugin plugin) {
        super(plugin);

        Command command = InstructionFactory.createSimpleIdleCommand(500, 1000);
        server.updateCommand(command);

        plugin.setDebugText("Loaded Woodcutting");
    }

    @Override
    public void onWorldChanged(WorldChanged worldChanged) {
        activeTrees.clear();
        activeBanks.clear();
    }

    @Override
    public void onGameTick(GameTick t) {
        super.onGameTick(t);

        // If performing an action, idle
        if (isPerformingAction()) {
            idle();
            return;
        }

        // If bank interface open, deposit
        if (isBankInterfaceOpen()) {
            depositInventoryInBank();
        } else if (!isInventoryFull()) {
            clickNearestTree();
        } else {
            clickNearestBank();
        }
    }

    public boolean isBankInterfaceOpen() {
        // For deposit boxes
        // client.getItemContainer(InventoryID.DEPOSIT_BOX) != null;
        return client.getItemContainer(InventoryID.BANK) != null;
    }

    public boolean isInventoryFull() {
        if (inventoryItems == null) {
            return false;
        }

        // Take item count
        long filledSlots = Arrays.stream(inventoryItems).filter(i -> i != null && i.getId() > 0).count();
        return filledSlots == 28;
    }

    public void depositInventoryInBank() {
        Widget depositInv = client.getWidget(WidgetInfo.BANK_DEPOSIT_INVENTORY);
        if (depositInv != null && !depositInv.isHidden())
        {
            Rectangle bounds = depositInv.getBounds();
            int x = bounds.x;
            int y = bounds.y;
            int w = bounds.width;
            int h = bounds.height;

            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;


            Command command = InstructionFactory.createBankDepositCommand(cx, cy);
            server.updateCommand(command);
            plugin.setDebugText("Depositing inventory in bank");
        } else {
            plugin.setDebugText("Failed to find deposit button");
        }
    }

    public void clickNearestTree() {
        // Set debug click point + action
        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        Optional<GameObject> closestTree = OwoUtils.findClosestGameObject(activeTrees, playerWp);
        if (closestTree.isEmpty()) {
            plugin.setDebugText("No tree found");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        // Pick random tree every x times to shake things up
        // This is causing to click the far off willows on accident
//        if (random.nextInt(6) < 1) { // ~15% chance
//            closestTree = Optional.of(activeTrees.get(random.nextInt(activeTrees.size())));
//        }

        Optional<Point> point = OwoUtils.getGameObjectClickPoint(closestTree.get());
        if (point.isEmpty()) {
            return;
        }
        Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY());
        server.updateCommand(command);

        plugin.setDebugText("Chopping closest tree");
        plugin.setDebugTargetPoint(point.get());
    }

    public void clickNearestBank() {
        // Set debug click point + action
        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        Optional<GameObject> closestBank = OwoUtils.findClosestGameObject(activeBanks, playerWp);
        if (closestBank.isEmpty()) {
            plugin.setDebugText("No banks found");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }

        Optional<Point> point = OwoUtils.getGameObjectClickPoint(closestBank.get());
        if(point.isEmpty()) {
            plugin.setDebugText("Bank is off screen");
            server.updateCommand(InstructionFactory.createDefaultIdle());
            return;
        }
        Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY());
        server.updateCommand(command);

        plugin.setDebugText("Opening closest bank");
        plugin.setDebugTargetPoint(point.get());
    }

    @Override
    public void onGameObjectSpawned(GameObjectSpawned event) {
        final GameObject gameObject = event.getGameObject();
        // Track current trees / bank available
        if (bankIds.contains(gameObject.getId())) {
            activeBanks.add(gameObject);
        }

        if (treeIds.contains(gameObject.getId())) {
            activeTrees.add(gameObject);
        }
    }

    @Override
    public void onGameObjectDespawned(GameObjectDespawned event) {
        final GameObject gameObject = event.getGameObject();
        // Remove from trees / bank available
        if (bankIds.contains(gameObject.getId())) {
            activeBanks.remove(gameObject);
        }

        if (treeIds.contains(gameObject.getId())) {
            activeTrees.remove(gameObject);
        }
    }
}
