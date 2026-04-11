package net.runelite.client.owo.logics;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.owo.OwoLogic;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.modules.InteractionManager;
import net.runelite.client.owo.utils.BankUtils;
import net.runelite.client.owo.utils.OwoUtils;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;

import java.util.*;
import java.util.List;

@Slf4j
public class Woodcutting extends OwoLogic<DummyState> {
    private final List<Integer> treeIds = List.of(
        // Draynor willows
        10819,10829,10831,10833,
        // Seer Maples
        10832
    );

    private final List<Integer> bankIds = List.of(
        // Draynor Kiosks
        10528, 10355,
        // Seer Kiosks
        25808, 27264
    );

    private final List<GameObject> activeTrees = new ArrayList<>();
    private final List<GameObject> activeBanks = new ArrayList<>();

    public Woodcutting(final OwoPlugin plugin) {
        super(plugin, DummyState.NO_OP);

        Command command = InstructionFactory.createDefaultIdle();
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
        if (isPerformingAction(3)) {
            idle();
            return;
        }

        // TODO Look for and pickup birdnests

        // If bank interface open, deposit
        if (BankUtils.isBankInterfaceOpen(client)) {
            interactionManager.depositInventoryInBank();
        } else if (!playerModule.isInventoryFull()) {
            clickNearestTree();
        } else {
            clickNearestBank();
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
