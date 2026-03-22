package net.runelite.client.owo;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.owo.instruction.InstructionFactory;
import net.runelite.client.owo.instruction.OwoServer;
import net.runelite.client.owo.modules.InteractionManager;
import net.runelite.client.owo.modules.PlayerModule;
import net.runelite.client.owo.utils.OwoUtils;
import net.runelite.client.owo.instruction.Command;
import net.runelite.client.plugins.owo.OwoPlugin;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static net.runelite.api.TileItem.OWNERSHIP_SELF;

@Slf4j
public abstract class OwoLogic<S extends Enum<S>> {
    protected S state;
    private long nextActionTime = 0;

    public enum TaskIntensity {
        LOW, MEDIUM, HIGH
    }

    private static final int STUCK_TIMEOUT = 300;

    protected final OwoServer server;
    protected final Client client;
    protected final OwoPlugin plugin;

    // TODO Refactor other logics to use interactionManager and remove this variable
    protected Item[] inventoryItems = new Item[28];

    protected Set<Integer> desiredLoot = new HashSet<>();
    protected List<Pair<TileItem, Tile>> loot = new ArrayList<>();

    protected final InteractionManager interactionManager;

    protected final PlayerModule playerModule;


    public OwoLogic(OwoPlugin plugin, S initialState) {
        this(plugin, initialState, List.of(), List.of());
    }

    public OwoLogic(OwoPlugin plugin, S initialState, List<Integer> npcsToTrack, List<Integer> gameObjectsToTrack) {
        this.server = plugin.getServer();
        this.client = plugin.getClient();
        this.plugin = plugin;
        this.state = initialState;
        this.playerModule = new PlayerModule(plugin);
        this.interactionManager = new InteractionManager(plugin, npcsToTrack, gameObjectsToTrack);
    }

    public void startUp() { }

    public void shutDown() { }

    protected boolean canAct() {
        return System.currentTimeMillis() >= nextActionTime;
    }

    protected void debounce(long delayMs) {
        nextActionTime = System.currentTimeMillis() + delayMs;
    }

    private int lastStateChangeTick = Integer.MAX_VALUE;
    protected void setState(S newState) {
        if (state != newState) {
            state = newState;
            lastStateChangeTick = client.getTickCount();
            nextActionTime = 0;
            plugin.setCurrentState(newState.name());
        }
    }

    private WorldPoint lastLocation;
    private int ticksSinceAction = 0;

    protected boolean isPerformingAction() {
        return ticksSinceAction < 7;
    }

    protected boolean isPerformingAction(int ticks) {
        return ticksSinceAction < ticks;
    }

    protected void idle(String msgDetail) {
        // Wait for action to complete
        Command command = InstructionFactory.createDefaultIdle();
        server.updateCommand(command);
        plugin.setDebugText("Performing action - " + msgDetail);
        plugin.setDebugTargetPoint(null);
    }

    private int ticksSinceLastIdle = 0;
    private int nextIdleAfterTicks = -1;
    private TaskIntensity nextIdleIntensity;

    /**
     * Calculates when and for how long to idle based on a given task intensity. Tracks time since last idle.
     * @param taskIntensity How intense the task is. For example Gemstone Crab -> Smelting -> Cleaning herbs
     * @return Suggested number of ticks to idle based on the intensity
     */
    protected int shouldRandomIdle(final TaskIntensity taskIntensity) {
        final int baseFrequencyTicks;
        final int baseDurationTicks;

        switch (taskIntensity) {
            case HIGH:
                baseFrequencyTicks = 500;
                baseDurationTicks = 50;
                break;
            case MEDIUM:
                baseFrequencyTicks = 1500;
                baseDurationTicks = 100;
                break;
            case LOW:
            default:
                baseFrequencyTicks = 3000;
                baseDurationTicks = 200;
                break;
        }

        if (nextIdleAfterTicks < 0 || nextIdleIntensity != taskIntensity) {
            nextIdleAfterTicks = varyByTwentyPercent(baseFrequencyTicks);
            nextIdleIntensity = taskIntensity;
        }

        if (ticksSinceLastIdle < nextIdleAfterTicks) {
            return 0;
        }

        ticksSinceLastIdle = 0;
        nextIdleAfterTicks = -1;
        return varyByTwentyPercent(baseDurationTicks);
    }

    private int varyByTwentyPercent(int baseTicks) {
        int minTicks = Math.max(1, (int) Math.floor(baseTicks * 0.8));
        int maxTicks = Math.max(minTicks, (int) Math.ceil(baseTicks * 1.2));
        return ThreadLocalRandom.current().nextInt(minTicks, maxTicks + 1);
    }

    protected void idle() {
        idle("");
    }

    public void postDiscordMessage(final String message) {
        server.postDiscordMessage(message);
    }

    public void onGameTick(GameTick e) {
        Player local = client.getLocalPlayer();
        if (local == null) {
            return;
        }

        // Alert if stuck for more than 3 minutes and idle
        if (lastStateChangeTick + STUCK_TIMEOUT < client.getTickCount() && ticksSinceLastIdle > STUCK_TIMEOUT) {
            lastStateChangeTick = Integer.MAX_VALUE;
            log.debug("No state change in {} ticks. Requesting manual help.", STUCK_TIMEOUT);
            postDiscordMessage("Father, I am stuck! Please provide assistance.");
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

        ticksSinceLastIdle++;
    }

    public void onWorldChanged(WorldChanged worldChanged) {
        interactionManager.clearTrackedObjects();
    }

    public void onGameStateChanged(GameStateChanged event) { }

    public void onItemSpawned(ItemSpawned itemSpawned)
    {
        TileItem item = itemSpawned.getItem();
        Tile tile = itemSpawned.getTile();
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

    public void onWallObjectSpawned(WallObjectSpawned event) {
    }

    public void onWallObjectDespawned(WallObjectDespawned event) {
    }

    public void onGroundObjectSpawned(GroundObjectSpawned event) {
    }

    public void onGroundObjectDespawned(GroundObjectDespawned event) {
    }

    public void onStatChanged(StatChanged event) {
    }

    // TODO Migrate this out somewhere else
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
        interactionManager.setInventoryItems(event.getItemContainer().getItems());
        playerModule.setInventoryItems(event.getItemContainer().getItems());
    }

    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject object = event.getGameObject();
        interactionManager.trackGameObject(object);
    }

    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject object = event.getGameObject();
        interactionManager.untrackGameObject(object);
    }

    public void onNpcSpawned(NpcSpawned npcSpawned) {
        NPC npc = npcSpawned.getNpc();
        interactionManager.trackNPC(npc);
    }

    public void onNpcDespawned(NpcDespawned npcDespawned) {
        NPC npc = npcDespawned.getNpc();
        interactionManager.untrackNPC(npc);
    }

    public void onVarbitChanged(VarbitChanged varbitChanged) {}

    public void onAnimationChanged(AnimationChanged event) {}

    public void onMenuOptionClicked(MenuOptionClicked event) {}

    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getMessage().contains("You've been stunned")) {
            playerModule.reportStunned();
        }
    }
}
