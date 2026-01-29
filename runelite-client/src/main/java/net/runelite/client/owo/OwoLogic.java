package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

import static net.runelite.api.TileItem.OWNERSHIP_SELF;

@Slf4j
public abstract class OwoLogic {
    protected final OwoServer server;
    protected final Client client;
    protected final OwoPlugin plugin;

    protected Item[] inventoryItems = new Item[28];

    protected Set<Integer> desiredLoot = new HashSet<>();
    protected List<Pair<TileItem, Tile>> loot = new ArrayList<>();

    public OwoLogic(OwoPlugin plugin) {
        this.server = plugin.getServer();
        this.client = plugin.getClient();
        this.plugin = plugin;
    }

    public void startUp() { }

    public void shutDown() { }

    private WorldPoint lastLocation;
    private int ticksSinceAction = 0;

    protected boolean isPerformingAction() {
        return ticksSinceAction < 7;
    }

    protected boolean isPerformingAction(int ticks) {
        return ticksSinceAction < ticks;
    }

    protected void idle() {
        // Wait for action to complete
        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);
        plugin.setDebugText("Performing action");
        plugin.setDebugTargetPoint(null);
    }

    public void onGameTick(GameTick e) {
        Player local = client.getLocalPlayer();
        if (local == null) {
            return;
        }

        boolean performingAction = local.getAnimation() != -1 || local.getInteracting() != null;

        WorldPoint current = local.getWorldLocation();
        boolean isMoving = lastLocation != null && !current.equals(lastLocation);
        lastLocation = current;

        if (performingAction || isMoving) {
            ticksSinceAction = 0;
        } else {
            ticksSinceAction++;
        }
    }

    public void onWorldChanged(WorldChanged worldChanged) { }

    public void onGameStateChanged(GameStateChanged event) { }

    public void onItemSpawned(ItemSpawned itemSpawned)
    {
        TileItem item = itemSpawned.getItem();
        Tile tile = itemSpawned.getTile();
        ItemLayer layer = tile.getItemLayer();
        int ownership = item.getOwnership();

        // Keep track of items owned by player
        if (ownership == OWNERSHIP_SELF && (desiredLoot.contains(item.getId()) || desiredLoot.isEmpty())) {
            loot.add(Pair.of(item, tile));
        }
    }

    public void onItemDespawned(ItemDespawned itemDespawned) {
        TileItem item = itemDespawned.getItem();
        Tile tile = itemDespawned.getTile();
        loot.remove(Pair.of(item, tile));
    }

    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
    }

    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
    }

    public void onGroundObjectSpawned(GroundObjectSpawned event) {
    }

    public void onGroundObjectDespawned(GroundObjectDespawned event) {
    }

    public void onStatChanged(StatChanged event) {
    }

    public boolean pickupLoot(int distance) {
        if (loot.isEmpty()) return false;

        TileItem item = loot.get(0).getLeft();
        Tile tile = loot.get(0).getRight();
        log.debug("Picking up loot item {}", item.getId());

        WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
        WorldPoint tileWp   = tile.getWorldLocation();

        // Check that loot is reachable
        if (!isReasonablyReachable(playerWp, tileWp, distance)) {
            return false;
        }

        Optional<Point> point = OwoUtils.getTileItemClickPoint(tile, client);
        if (point.isPresent()) {
            Command command = InstructionFactory.createClickCommand(point.get().getX(), point.get().getY());
            server.updateCommand(command);

            plugin.setDebugText("Picking up loot: " + item.getId());
            plugin.setDebugTargetPoint(point.get());
            return true;
        } else {
            return false;
        }
        // How to handle if item buried in stack? Ignore for now and pickup entire stack
    }

    public static boolean isReasonablyReachable(
            WorldPoint player,
            WorldPoint target,
            int maxDistance
    ) {
        if (player == null || target == null) {
            return false;
        }

        // Must be on same plane
        if (player.getPlane() != target.getPlane()) {
            return false;
        }

        // Tile distance (Chebyshev works well for movement)
        int dx = Math.abs(player.getX() - target.getX());
        int dy = Math.abs(player.getY() - target.getY());

        int distance = Math.max(dx, dy); // Chebyshev distance

        return distance <= maxDistance;
    }

    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INV) {
            return;
        }

        inventoryItems = event.getItemContainer().getItems();
    }

    public void onGameObjectSpawned(GameObjectSpawned event) {}

    public void onGameObjectDespawned(GameObjectDespawned event) {}

    public void onVarbitChanged(VarbitChanged varbitChanged) {}

    public void onNpcSpawned(NpcSpawned npcSpawned) {}

    public void onNpcDespawned(NpcDespawned npcDespawned) {}

    public void onAnimationChanged(AnimationChanged event) {}


}
